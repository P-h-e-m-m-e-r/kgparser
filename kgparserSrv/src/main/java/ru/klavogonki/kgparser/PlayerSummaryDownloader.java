package ru.klavogonki.kgparser;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class PlayerSummaryDownloader { // todo: use logger instead of System.out
//    private static final Logger logger = Logger.getLogger(PlayerSummaryDownloader.class);

    private static final int NOSFERATUM_PLAYER_ID = 242585;
    private static final int MIN_PLAYER_ID = 30000; // todo: confirm
    private static final int MAX_PLAYER_ID = 623112; // todo: at 02.12.2020 00:20 (nice date)

    static class Config {
        public static final int REQUIRED_ARGUMENTS_COUNT = 3;

        String rootDir;
        int minPlayerId;
        int maxPlayerId;
        String startDate;

        public String getPlayerSummaryFilePath(final int playerId) {
            return rootDir + File.separator + startDate + File.separator + "summary" + File.separator + playerId + ".json";
        }

        public String getPlayerIndexDataFilePath(final int playerId) {
            return rootDir + File.separator + startDate + File.separator + "index-data" + File.separator + playerId + ".json";
        }

        public static Config parseFromArguments(final String[] args) {
            int index = 0;

            Config config = new Config();
            config.rootDir = args[index++];
            config.minPlayerId = Integer.parseInt(args[index++]);
            config.maxPlayerId = Integer.parseInt(args[index++]);
            return config;
        }
    }

    public static void main(String[] args) throws IOException {
        // todo: pass a path to a json file with config instead

        if (args.length != Config.REQUIRED_ARGUMENTS_COUNT) {
            System.out.printf("Usage: %s <rootJsonDir> <minPlayerId> <maxPlayerId> %n", PlayerSummaryDownloader.class.getSimpleName());
            return;
        }

        Config config = Config.parseFromArguments(args);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");
        config.startDate = LocalDateTime.now().format(dateTimeFormatter);

        for (int playerId = config.minPlayerId; playerId <= config.maxPlayerId; playerId++) {
            savePlayerSummaryToJsonFile(config, playerId);
            savePlayerIndexDataToJsonFile(config, playerId);
        }
    }

    private static void savePlayerSummaryToJsonFile(final Config config, final int playerId) throws IOException {
        System.out.printf("Loading player summary for player %d...%n", playerId);

        String urlString = getSummaryUrl(playerId);

        String out = loadUrlToString(urlString);

        String jsonFilePath = config.getPlayerSummaryFilePath(playerId);
        FileUtils.writeStringToFile(new File(jsonFilePath), out, StandardCharsets.UTF_8);
        System.out.printf("Summary for player %d successfully written to file %s.%n", playerId, jsonFilePath);
    }

    private static void savePlayerIndexDataToJsonFile(final Config config, final int playerId) throws IOException {
        System.out.printf("Loading player index data for player %d...%n", playerId);

        String urlString = getIndexDataUrl(playerId);

        String out = loadUrlToString(urlString);

        String jsonFilePath = config.getPlayerIndexDataFilePath(playerId);
        FileUtils.writeStringToFile(new File(jsonFilePath), out, StandardCharsets.UTF_8);
        System.out.printf("Index data for player %d successfully written to file %s.%n", playerId, jsonFilePath);
    }

    private static String loadUrlToString(final String urlString) throws IOException {
        System.out.println("Url to load: " + urlString);

        // todo: use some nice library instead
        URL url = new URL(urlString);
        String out = new Scanner(url.openStream(), StandardCharsets.UTF_8) // this line will require java 10
            .useDelimiter("\\A")
            .next();

        System.out.printf("Response for url %s:%n", urlString);
        System.out.println(out);
        return out;
    }

    private static String getSummaryUrl(final int playerId) {
        return String.format("https://klavogonki.ru/api/profile/get-summary?id=%d", playerId);
    }

    private static String getIndexDataUrl(final int playerId) {
        return String.format("http://klavogonki.ru/api/profile/get-index-data?userId=%d", playerId);
    }
}
