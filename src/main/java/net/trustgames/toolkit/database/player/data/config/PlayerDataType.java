package net.trustgames.toolkit.database.player.data.config;

import lombok.Getter;

/**
 * Stores all the PlayerData types and their column names
 * which are saved in the player data table.
 */
public enum PlayerDataType {
    UUID("uuid","uuid", "VARCHAR(36) primary key"),
    NAME("name","name", "VARCHAR(16)"),
    KILLS("kills","kills", "INT DEFAULT 0"),
    DEATHS("deaths","deaths", "INT DEFAULT 0"),
    GAMES("games played","games_played", "INT DEFAULT 0"),
    PLAYTIME("playtime","playtime", "INT DEFAULT 0"),
    XP("xp","xp", "INT DEFAULT 0"),
    LEVEL("levels", null, null),
    GEMS("gems","gems", "INT DEFAULT 100"),
    RUBIES("rubies","rubies", "INT DEFAULT 0");

    @Getter
    private final String displayName;
    @Getter
    private final String columnName;
    @Getter
    private final String columnType;

    PlayerDataType(String displayName, String columnName, String columnType) {
        this.displayName = displayName;
        this.columnName = columnName;
        this.columnType = columnType;
    }
}
