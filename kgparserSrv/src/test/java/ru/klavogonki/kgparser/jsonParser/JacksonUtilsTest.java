package ru.klavogonki.kgparser.jsonParser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.klavogonki.kgparser.Car;
import ru.klavogonki.kgparser.Rank;
import ru.klavogonki.kgparser.util.DateUtils;
import ru.klavogonki.kgparser.util.TestUtils;
import ru.klavogonki.openapi.model.Bio;
import ru.klavogonki.openapi.model.BioAssert;
import ru.klavogonki.openapi.model.GetIndexDataResponse;
import ru.klavogonki.openapi.model.GetIndexDataResponseAssert;
import ru.klavogonki.openapi.model.GetIndexDataStats;
import ru.klavogonki.openapi.model.GetIndexDataStatsAssert;
import ru.klavogonki.openapi.model.Microtime;
import ru.klavogonki.openapi.model.MicrotimeAssert;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonUtilsTest {
    private static final Logger logger = LogManager.getLogger(JacksonUtilsTest.class);

    // todo: add bio.oldTextRemoved validation to all indexData tests!
    // todo: missing tests:
    // - 109842 - blank login
    // - 141327 - blocked: 1 and negative registered.sec !!! date parse will most probably fail
    // - 142478 - blocked: 4
    // - 161997 - /get-summary works, /get-index-data returns error
    // - 368664 - bio.text == null, bio.oldText not present, bio.old_text_removed present
    // - 498727 - /get-summary works, /get-index-data returns error (special MongoDB error)

    @Test
    @DisplayName("Test parsing an existing user summary from a json file")
    void testPlayerSummary() {
        File file = TestUtils.readResourceFile("ru/klavogonki/kgparser/jsonParser/get-summary-242585.json");

        PlayerSummary summary = JacksonUtils.parse(file, PlayerSummary.class);
        logPlayerSummary(summary);

        assertThat(summary.err).isNull();
        assertThat(summary.isOnline).isTrue();
        assertThat(summary.level).isEqualTo(Rank.getLevel(Rank.superman).intValue());
        assertThat(summary.title).isEqualTo(Rank.getDisplayName(Rank.superman));
        assertThat(summary.blocked).isZero();

        assertThat(summary.user).isNotNull();
        assertThat(summary.user.id).isEqualTo(242585);
        assertThat(summary.user.login).isEqualTo("nosferatum");

        assertThat(summary.car).isNotNull();
        assertThat(summary.car.car).isEqualTo(Car.F1.id);
        assertThat(summary.car.color).isEqualTo("#BF1300");
    }

    @Test
    @DisplayName("Test parsing an existing user summary for a user with a personal car id from a json file")
    void testPlayerWithPersonalCarIdSummary() {
        File file = TestUtils.readResourceFile("ru/klavogonki/kgparser/jsonParser/get-summary-922.json");

        PlayerSummary summary = JacksonUtils.parse(file, PlayerSummary.class);
        logPlayerSummary(summary);

        assertThat(summary.err).isNull();
        assertThat(summary.isOnline).isFalse();
        assertThat(summary.level).isEqualTo(Rank.getLevel(Rank.maniac).intValue());
        assertThat(summary.title).isEqualTo(Rank.getDisplayName(Rank.maniac));
        assertThat(summary.blocked).isZero();

        assertThat(summary.user).isNotNull();
        assertThat(summary.user.id).isEqualTo(922);
        assertThat(summary.user.login).isEqualTo("lovermann");

        assertThat(summary.car).isNotNull();
        assertThat(summary.car.car).isEqualTo(Car.CARAVEL.personalId);
        assertThat(summary.car.color).isEqualTo("#000000");
    }

    @Test
    @DisplayName("Test parsing a brand new user summary from a json file")
    void testBrandNewPlayerSummary() {
        File file = TestUtils.readResourceFile("ru/klavogonki/kgparser/jsonParser/get-summary-624511.json");

        PlayerSummary summary = JacksonUtils.parse(file, PlayerSummary.class);
        logPlayerSummary(summary);

        assertThat(summary.err).isNull();
        assertThat(summary.isOnline).isTrue();
        assertThat(summary.level).isEqualTo(Rank.getLevel(Rank.novice).intValue());
        assertThat(summary.title).isEqualTo(Rank.getDisplayName(Rank.novice));
        assertThat(summary.blocked).isZero();

        assertThat(summary.user).isNotNull();
        assertThat(summary.user.id).isEqualTo(624511);
        assertThat(summary.user.login).isEqualTo("nosferatum0");

        assertThat(summary.car).isNotNull();
        assertThat(summary.car.car).isEqualTo(Car.ZAZ_965.id);
        assertThat(summary.car.color).isEqualTo("#777777");
    }

    @Test
    @DisplayName("Test parsing a summary of a klavomechanic with a hidden profile from a json file")
    void testKlavoMechanicWithHiddenProfileSummary() {
        File file = TestUtils.readResourceFile("ru/klavogonki/kgparser/jsonParser/get-summary-21.json");

        PlayerSummary summary = JacksonUtils.parse(file, PlayerSummary.class);
        logPlayerSummary(summary);

        assertThat(summary.err).isNull();
        assertThat(summary.isOnline).isFalse();
        assertThat(summary.level).isEqualTo(Rank.getLevel(Rank.superman).intValue());
        assertThat(summary.title).isEqualTo(Rank.KLAVO_MECHANIC_TITLE);
        assertThat(summary.blocked).isZero();

        assertThat(summary.user).isNotNull();
        assertThat(summary.user.id).isEqualTo(21);
        assertThat(summary.user.login).isEqualTo("Artch");

        assertThat(summary.car).isNotNull();
        assertThat(summary.car.car).isEqualTo(Car.AUDI_TT.id);
        assertThat(summary.car.color).isEqualTo("#893425");
    }

    @Test
    @DisplayName("Test parsing a non-existing user summary from a json file")
    void testInvalidPlayerSummary() {
        File file = TestUtils.readResourceFile("ru/klavogonki/kgparser/jsonParser/get-summary-30001.json");

        PlayerSummary summary = JacksonUtils.parse(file, PlayerSummary.class);
        logPlayerSummary(summary);

        assertThat(summary.err).isEqualTo(PlayerSummary.INVALID_USER_ID_ERROR);
        assertThat(summary.isOnline).isNull();
        assertThat(summary.level).isNull();
        assertThat(summary.title).isNull();
        assertThat(summary.blocked).isNull();

        assertThat(summary.user).isNull();

        assertThat(summary.car).isNull();
    }

    @Test
    @DisplayName("Test parsing an existing user index data from a json file")
    void testPlayerIndexData() {
        File file = TestUtils.readResourceFile("ru/klavogonki/kgparser/jsonParser/get-index-data-242585.json");

        GetIndexDataResponse data = JacksonUtils.parse(file, GetIndexDataResponse.class);
        logPlayerIndexData(data);

        GetIndexDataResponseAssert
            .assertThat(data)
            .hasOk(ApiErrors.OK_CORRECT_VALUE)
            .hasErr(null);

        // bio
        Bio bio = data.getBio();
        BioAssert
            .assertThat(bio)
            .isNotNull()
            .hasUserId(242585);

        assertThat(bio.getOldText()).isNotBlank(); // huge html, no validation
        assertThat(bio.getText()).isNotBlank(); // huge html, no validation

        // stats
        GetIndexDataStats stats = data.getStats();
        GetIndexDataStatsAssert
            .assertThat(stats)
            .isNotNull()
            .hasAchievesCnt(225)
            .hasTotalNumRaces(60633)
            .hasBestSpeed(626)
            .hasRatingLevel(32)
            .hasFriendsCnt(102)
            .hasVocsCnt(109)
            .hasCarsCnt(33);

        // stats.registered
        Microtime registered = stats.getRegistered();
        MicrotimeAssert
            .assertThat(registered)
            .isNotNull()
            .hasSec(1297852113)
            .hasUsec(0);

        DateUtils.convertUserRegisteredTime(data);
    }

    @Test
    @DisplayName("Test parsing an existing user index data for a user with a personal car id from a json file")
    void testPlayerWithPersonalCarIdIndexData() {
        File file = TestUtils.readResourceFile("ru/klavogonki/kgparser/jsonParser/get-index-data-922.json");

        GetIndexDataResponse data = JacksonUtils.parse(file, GetIndexDataResponse.class);
        logPlayerIndexData(data);

        GetIndexDataResponseAssert
            .assertThat(data)
            .hasOk(ApiErrors.OK_CORRECT_VALUE)
            .hasErr(null);

        // bio
        Bio bio = data.getBio();
        BioAssert
            .assertThat(bio)
            .isNotNull()
            .hasUserId(922);

        assertThat(bio.getOldText()).isNotBlank(); // huge html, no validation
        assertThat(bio.getText()).isNotBlank(); // huge html, no validation

        // bio.editedDate
        Microtime editedData = bio.getEditedDate();
        MicrotimeAssert
            .assertThat(editedData)
            .isNotNull()
            .hasSec(1508143960)
            .hasUsec(314000);

        // stats
        GetIndexDataStats stats = data.getStats();
        GetIndexDataStatsAssert
            .assertThat(stats)
            .isNotNull()
            .hasAchievesCnt(171)
            .hasTotalNumRaces(47887)
            .hasBestSpeed(554)
            .hasRatingLevel(37)
            .hasFriendsCnt(75)
            .hasVocsCnt(83)
            .hasCarsCnt(41);

        // stats.registered
        Microtime registered = stats.getRegistered();
        MicrotimeAssert
            .assertThat(registered)
            .isNotNull()
            .hasSec(1211400000)
            .hasUsec(0);

        DateUtils.convertUserRegisteredTime(data);
    }

    @Test
    @DisplayName("Test parsing a brand new user index data from a json file")
    void testBrandNewPlayerIndexData() {
        File file = TestUtils.readResourceFile("ru/klavogonki/kgparser/jsonParser/get-index-data-624511.json");

        GetIndexDataResponse data = JacksonUtils.parse(file, GetIndexDataResponse.class);
        logPlayerIndexData(data);

        GetIndexDataResponseAssert
            .assertThat(data)
            .hasOk(ApiErrors.OK_CORRECT_VALUE)
            .hasErr(null);

        Bio bio = data.getBio();
        BioAssert
            .assertThat(bio)
            .isNotNull()
            .hasUserId(624511)
            .hasOldText(null) // no oldText for the new users
            .hasText(""); // empty and not null

        // stats
        GetIndexDataStats stats = data.getStats();
        GetIndexDataStatsAssert
            .assertThat(stats)
            .isNotNull()
            .hasAchievesCnt(0)
            .hasTotalNumRaces(0)
            .hasBestSpeed(null) // no races in "Normal" -> no best speed
            .hasRatingLevel(1) // user is level 1 from the start
            .hasFriendsCnt(0)
            .hasVocsCnt(0)
            .hasCarsCnt(1); // user has 1 car from the start

        // stats.registered
        Microtime registered = stats.getRegistered();
        MicrotimeAssert
            .assertThat(registered)
            .isNotNull()
            .hasSec(1607554944)
            .hasUsec(0);

        DateUtils.convertUserRegisteredTime(data);
    }

    @Test
    @DisplayName("Test parsing index data of a klavomechanic with a hidden profile from a json file. Request returns a \"hidden profile\" error.")
    void testKlavoMechanicWithHiddenProfileIndexData() {
        File file = TestUtils.readResourceFile("ru/klavogonki/kgparser/jsonParser/get-index-data-21.json");

        GetIndexDataResponse data = JacksonUtils.parse(file, GetIndexDataResponse.class);
        logPlayerIndexData(data);

        GetIndexDataResponseAssert
            .assertThat(data)
            .hasOk(null)
            .hasErr(ApiErrors.HIDDEN_PROFILE_USER_ERROR);

        BioAssert
            .assertThat(data.getBio())
            .isNull();

        GetIndexDataStatsAssert
            .assertThat(data.getStats())
            .isNull();
    }

    @Test
    @DisplayName("Test parsing a non-existing user index data from a json file")
    void testInvalidPlayerIndexData() {
        File file = TestUtils.readResourceFile("ru/klavogonki/kgparser/jsonParser/get-index-data-30001.json");

        GetIndexDataResponse data = JacksonUtils.parse(file, GetIndexDataResponse.class);
        logPlayerIndexData(data);

        GetIndexDataResponseAssert
            .assertThat(data)
            .hasOk(null)
            .hasErr(ApiErrors.INVALID_USER_ID_ERROR);

        BioAssert
            .assertThat(data.getBio())
            .isNull();

        GetIndexDataStatsAssert
            .assertThat(data.getStats())
            .isNull();
    }

    private static void logPlayerSummary(final PlayerSummary summary) {
        logger.info("Player summary: ");
        logger.info("- err: {}", summary.err);
        logger.info("- isOnline: {}", summary.isOnline);
        logger.info("- level: {}", summary.level);
        logger.info("- title: {}", summary.title);
        logger.info("- blocked: {}", summary.blocked);

        logger.info("");

        if (summary.user != null) {
            logger.info("User:");
            logger.info("- id: {}", summary.user.id);
            logger.info("- login: {}", summary.user.login);
        }
        else {
            logger.info("User: null");
        }

        logger.info("");

        if (summary.car != null) {
            logger.info("Car:");
            logger.info("- car: {}", summary.car.car);
            logger.info("- color: {}", summary.car.color);
        }
        else {
            logger.info("User: null");
        }
    }

    private static void logPlayerIndexData(final GetIndexDataResponse data) {
        logger.info("Player index data: ");
        logger.info(data);
    }
}
