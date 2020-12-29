package ru.klavogonki.kgparser;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.klavogonki.kgparser.jsonParser.ApiErrors;
import ru.klavogonki.kgparser.jsonParser.JacksonUtils;
import ru.klavogonki.kgparser.jsonParser.PlayerSummary;
import ru.klavogonki.openapi.model.GetIndexDataResponse;
import ru.klavogonki.openapi.model.GetIndexDataStats;
import ru.klavogonki.openapi.model.Microtime;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Parses the json files saved by {@link PlayerDataDownloader}.
 */
public class PlayerJsonParser {
    private static final Logger logger = LogManager.getLogger(PlayerJsonParser.class);

    public static final int REQUIRED_ARGUMENTS_COUNT = 4;

    static class ParserException extends RuntimeException {
        public ParserException(final String message, final Object... messageArguments) {
            super(String.format(message, messageArguments));
        }

        public ParserException(final String message) {
            super(message);
        }

        public ParserException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    public static void main(String[] args) {
        // todo: pass a path to a json file with config instead

        if (args.length != REQUIRED_ARGUMENTS_COUNT) {
            // todo: use logger instead of System.out??
            System.out.printf("Usage: %s <rootJsonDir> <minPlayerId> <maxPlayerId> <yyyy-MM-dd HH-mm-ss> %n", PlayerJsonParser.class.getSimpleName());
            return;
        }

        PlayerDataDownloader.Config config = PlayerDataDownloader.Config.parseFromArguments(args);
        config.setStartDate(args[3]);
        config.log();

        List<PlayerJsonData> players = new ArrayList<>();
        List<Integer> nonExistingPlayerIds = new ArrayList<>();

        BiConsumer<Integer, Optional<PlayerJsonData>> playerHandler = (playerId, playerOptional) -> {
            if (playerOptional.isPresent()) {
                PlayerJsonData player = playerOptional.get();
                players.add(player);

                if (player.summary.err.equals(ApiErrors.INVALID_USER_ID_ERROR)) {
                    nonExistingPlayerIds.add(playerId);
                }
            }
            else {
                nonExistingPlayerIds.add(playerId);
            }
        };

        handlePlayers(config, playerHandler);

        logger.info("=======================================================");
        logger.info("Total player ids handled: {}", config.maxPlayerId - config.minPlayerId + 1);
        logger.info("Total existing players parsed: {}", players.size());
        logger.info("Total non existing players: {}", nonExistingPlayerIds.size());

        // todo: validate over all users
        // todo: all users must have unique id
        // todo: should login be unique over all users?
    }

    public static void handlePlayers(final PlayerDataDownloader.Config config, final BiConsumer<Integer, Optional<PlayerJsonData>> playerHandler) {
        int totalPlayersToHandle = config.maxPlayerId - config.minPlayerId + 1;

        for (int playerId = config.minPlayerId; playerId <= config.maxPlayerId; playerId++) {
            logger.info("=======================================================");
            int indexOfCurrentPlayer = playerId - config.minPlayerId + 1; // starting from 1
            logger.info("Handling player {} (player {} / {})...", playerId, indexOfCurrentPlayer, totalPlayersToHandle);

            File summaryFile = new File(config.getPlayerSummaryFilePath(playerId));
            File indexDataFile = new File(config.getPlayerIndexDataFilePath(playerId));

            Optional<PlayerJsonData> playerOptional = readPlayerData(config.startDate, playerId, summaryFile, indexDataFile);

            playerHandler.accept(playerId, playerOptional);
        }
    }

    static Optional<PlayerJsonData> readPlayerData(final LocalDateTime importDate, final int playerId, final File summaryFile, final File indexDataFile) {
        String summaryFilePath = summaryFile.getPath();
        String indexDataFilePath = indexDataFile.getPath();

        // parse summary file
        PlayerSummary summary = JacksonUtils.parse(summaryFile, PlayerSummary.class);

        // parse index-data file
        GetIndexDataResponse indexData = JacksonUtils.parse(indexDataFile, GetIndexDataResponse.class);

        // validate expected data
        // todo: use some validation framework instead of this manual code hell
        validate(playerId, summary, summaryFilePath);
        validate(playerId, summary.blocked, indexData, indexDataFilePath);

        // check whether this is a parse error

        // validate erratic cases
        boolean isErrorCase = validateErrorCase(summaryFilePath, indexDataFilePath, summary, indexData);
        if (isErrorCase) {
            // both files contain same errors -> return empty result, there is no such player
            logger.info("Player with id = {} is not found according to both summary file {} and index data file {}.", playerId, summaryFilePath, indexDataFilePath);
            PlayerJsonData result = new PlayerJsonData(importDate, summary, indexData);
            return Optional.of(result); // we will save not found players as well, for the database consistency (and FGJ as well!)
        }

        // player validation passed -> return parsed player object
        logger.info("Player {} was successfully parsed from summary file {} and index data file {}.", playerId, summaryFilePath, indexDataFilePath);
        PlayerJsonData result = new PlayerJsonData(importDate, summary, indexData);
        return Optional.of(result);
    }

    private static boolean validateErrorCase( // true if user does not exist, false if user exists
        final String summaryFilePath,
        final String indexDataFilePath,
        final PlayerSummary summary,
        final GetIndexDataResponse data
    ) {
        String err = data.getErr();
        if (StringUtils.isBlank(summary.err) && StringUtils.isBlank(err)) {
            logger.info("Neither summary file {} nor index data file {} contain an error.", summaryFilePath, indexDataFilePath);
            return false;
        }

        if (StringUtils.isBlank(summary.err) && err.equals(ApiErrors.HIDDEN_PROFILE_USER_ERROR)) { // hidden profile -> ok user, there will be no index data
            logger.info("Summary file {} contains nor error, index data file {} contain a error. User exists, but will have no index data.", summaryFilePath, indexDataFilePath);
            return false;
        }

        if (StringUtils.isNotBlank(summary.err) && StringUtils.isBlank(err)) { // error only in summary
            throw new ParserException(
                "Summary file %s contains error \"%s\", but index data file %s contains no error",
                summaryFilePath,
                summary.err,
                indexDataFilePath
            );
        }

        if (StringUtils.isNotBlank(err) && StringUtils.isBlank(summary.err)) { // error only in index-data
            // https://klavogonki.ru/u/#/161997/ - possible case: /get-summary works, /get-index-data fails. Not blocked.
            logger.warn(
                "Summary file {} contains no error, but index data file {} contains error \"{}\". Player: {}",
                summaryFilePath,
                indexDataFilePath,
                err,
                summary.user.id
            );

            return false; // todo: a very-tricky case, but the user exists and his/her page can be accessed

/*
            throw new ParserException(
                "Summary file %s contains no error, but index data file %s contains error \"%s\"",
                summaryFilePath,
                indexDataFilePath,
                data.err
            );
*/
        }

        if (!summary.err.equals(err)) { // different errors in summary and index-data files
            throw new ParserException(
                "Summary file %s contains error \"%s\", but index data file %s contains different error \"%s\"",
                summaryFilePath,
                summary.err,
                indexDataFilePath,
                err
            );
        }

        // both files contain same errors -> return empty result, there is no such player
        logger.info("Both summary file {} and index data file {} contain the same correct error \"{}\".", summaryFilePath, indexDataFilePath, summary.err);
        return true;
    }

    private static void validate(int playerId, PlayerSummary summary, String summaryFilePath) {
        if (StringUtils.isNotBlank(summary.err)) { // error case
            if (!summary.err.equals(ApiErrors.INVALID_USER_ID_ERROR)) {
                throw new ParserException("Summary file %s: Unknown error: %s", summaryFilePath, summary.err);
            }

            return;
        }

        // no-error

        // isOnline
        if (summary.isOnline == null) {
            throw new ParserException("Summary file %s: summary.isOnline is null", summaryFilePath);
        }

        // level
        if (summary.level == null) {
            throw new ParserException("Summary file %s: summary.level is null", summaryFilePath);
        }

        Rank rank = Rank.getRank(summary.level);// this method will throw on incorrect input

        // level title
        if (StringUtils.isBlank(summary.title)) {
            throw new ParserException("Summary file %s: summary.title is null or blank", summaryFilePath);
        }

        String expectedRankTitle = Rank.getDisplayName(rank);
        if (!summary.title.equals(expectedRankTitle) && !summary.title.equals(Rank.KLAVO_MECHANIC_TITLE)) {
            throw new ParserException("Summary file %s: summary.title has incorrect value %s, must be %s", summaryFilePath, summary.title, expectedRankTitle);
        }

        // blocked
        if (
               (summary.blocked == null)
            || ((summary.blocked != 0) && (summary.blocked != 1) && (summary.blocked != 4))
        ) {
            // https://klavogonki.ru/u/#/141327/ - blocked == 1
            // https://klavogonki.ru/u/#/142478/ - blocked == 4
            throw new ParserException("Summary file %s: summary.blocked has incorrect value: %s", summaryFilePath, summary.blocked);
        }

        // user
        if (summary.user == null) {
            throw new ParserException("Summary file %s: summary.user is null", summaryFilePath);
        }

        if (summary.user.id != playerId) {
            throw new ParserException("Summary file %s contains incorrect summary.user.id %s. Expected playerId: %s", summaryFilePath, summary.user.id, playerId);
        }

        if (summary.user.login == null) { // login CAN be blank, see https://klavogonki.ru/u/#/109842/
            throw new ParserException("Summary file %s: summary.user.login is null", summaryFilePath);
        }

        if (StringUtils.isBlank(summary.user.login)) { // login CAN be blank, see https://klavogonki.ru/u/#/109842/
            logger.warn("Summary file {}: summary.user.login is blank: \"{}\".", summaryFilePath, summary.user.login);
        }

        // car
        if (summary.car == null) {
            throw new ParserException("Summary file %s: summary.car is null", summaryFilePath);
        }

        Car.getById(summary.car.car); // this method will throw on incorrect input

        if (StringUtils.isBlank(summary.car.color)) {
            throw new ParserException("Summary file %s: summary.car.color is null or blank", summaryFilePath);
        }
    }

    private static void validate(int playerId, Integer blocked, GetIndexDataResponse data, String indexDataFilePath) {
        String err = data.getErr();
        if (StringUtils.isNotBlank(err)) { // error case
            if (
                !err.equals(ApiErrors.INVALID_USER_ID_ERROR)
                && !err.equals(ApiErrors.HIDDEN_PROFILE_USER_ERROR)
                && !err.equals(ApiErrors.MONGO_REFS_ERROR_USER_498727)
            ) {
                throw new ParserException("Index data file %s: Unknown error: %s", indexDataFilePath, err);
            }

            return;
        }

        // non-error case
        // ok
        Integer ok = data.getOk();
        if (ok != ApiErrors.OK_CORRECT_VALUE) { // should be null-safe
            throw new ParserException(
                "Index data file %s contains no error, but data.ok = %s. It must be = %s",
                indexDataFilePath,
                ok,
                ApiErrors.OK_CORRECT_VALUE
            );
        }

        // bio
        if (data.getBio() == null) {
            throw new ParserException("Index data file %s: data.bio is null", indexDataFilePath);
        }

        if (data.getBio().getUserId() != playerId) {
            throw new ParserException("Index data file %s contains incorrect data.bio.userId %s. Expected playerId: %s", indexDataFilePath, data.getBio().getUserId(), playerId);
        }

        // bio.oldText can be null / empty - no need to validate
        // bio.oldTextRemoved can be null / empty - no need to validate
        // bio.text can be null / empty - see https://klavogonki.ru/u/#/368664/ - no need to validate

        // stats
        GetIndexDataStats stats = data.getStats();

        if (stats == null) {
            throw new ParserException("Index data file %s: data.stats is null", indexDataFilePath);
        }

        // stats.registered
        Microtime registered = stats.getRegistered();
        if (registered == null) {
            throw new ParserException("Index data file %s: data.registered is null", indexDataFilePath);
        }

        if (registered.getSec() == null) {
            throw new ParserException("Index data file %s: data.registered.sec is null", indexDataFilePath);
        }

        // todo: validate that (data.stats.registered.sec + data.stats.registered.usec) can parse to date
        if (registered.getSec() <= 0) { // todo: compare to minimal unix date
            if ((blocked != null) && (blocked == 1)) { // todo: maybe other blocked != 0 values can also contain crazy register dates
                logger.warn("Index data file {}: User {} is blocked, data.registered.sec has invalid value: {}", indexDataFilePath, playerId, registered.getSec());
            }
            else {
                throw new ParserException("Index data file %s: User is not blocked, but data.registered.sec has invalid value: %d", indexDataFilePath, registered.getSec());
            }
        }

        if (registered.getUsec() == null) {
            throw new ParserException("Index data file %s: data.registered.usec is null", indexDataFilePath);
        }

        if (registered.getUsec() < 0) { // todo: validate range, from 0 to 1000?
            throw new ParserException("Index data file %s: data.stats.registered.usec has invalid value: %d", indexDataFilePath, registered.getUsec());
        }

        // statistics in stats
        Integer achievementsCount = stats.getAchievesCnt();
        if (achievementsCount == null || achievementsCount < 0) {
            throw new ParserException("Index data file %s: data.stats.achievementsCount has invalid value: %d", indexDataFilePath, achievementsCount);
        }

        Integer totalRacesCount = stats.getTotalNumRaces();
        if (totalRacesCount == null || totalRacesCount < 0) {
            throw new ParserException("Index data file %s: data.stats.totalRacesCount has invalid value: %d", indexDataFilePath, totalRacesCount);
        }

        // best speed can be null for player without races in Normal
        Integer bestSpeed = stats.getBestSpeed();
        if (bestSpeed != null && bestSpeed <= 0) {
            throw new ParserException("Index data file %s: data.stats.bestSpeed has invalid value: %d", indexDataFilePath, bestSpeed);
        }

        Integer ratingLevel = stats.getRatingLevel();
        if (ratingLevel == null || ratingLevel < 1) { // todo: move 1 to constant in Player etc
            throw new ParserException("Index data file %s: data.stats.ratingLevel has invalid value: %d", indexDataFilePath, ratingLevel);
        }

        Integer friendsCount = stats.getFriendsCnt();
        if (friendsCount == null || friendsCount < 0) {
            throw new ParserException("Index data file %s: data.stats.friendsCount has invalid value: %d", indexDataFilePath, friendsCount);
        }

        Integer vocabulariesCount = stats.getVocsCnt();
        if (vocabulariesCount == null || vocabulariesCount < 0) {
            throw new ParserException("Index data file %s: data.stats.vocabulariesCount has invalid value: %d", indexDataFilePath, vocabulariesCount);
        }

        // cannot own no cars or more than total cars
        Integer carsCount = stats.getCarsCnt();
        if (carsCount == null || carsCount < 1 || carsCount > Car.values().length) {
            throw new ParserException("Index data file %s: data.stats.carsCount has invalid value: %d", indexDataFilePath, carsCount);
        }
    }
}
