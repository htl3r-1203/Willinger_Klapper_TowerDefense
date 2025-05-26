package com.example.willinger_klapper_towerdefense;

import java.nio.file.*;

public class HighScoreManager {

    private static final Path FILE = Paths.get("highscore.dat");

    public static int load() {
        try {
            if (Files.exists(FILE)) {
                return Integer.parseInt(Files.readString(FILE));
            }
        } catch (Exception ignored) { }
        return 0;
    }

    public static void save(int score) {
        try {
            Files.writeString(
                    FILE,
                    Integer.toString(score),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception ignored) { }
    }
}
