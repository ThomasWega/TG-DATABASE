package net.trustgames.toolkit.database.player.data.config;

import lombok.Getter;

/**

 Stores all the PlayerData types and their column names

 which are saved in the player data table.
 */
public enum PlayerDataType {
    UUID("uuid", "VARCHAR(36) primary key"),
    NAME("name", "VARCHAR(16)"),
    KILLS("kills", "INT DEFAULT 0"),
    DEATHS("deaths", "INT DEFAULT 0"),
    GAMES("games played", "INT DEFAULT 0"),
    PLAYTIME("hours of playtime", "INT DEFAULT 0"),
    XP("xp", "INT DEFAULT 0"),
    LEVEL("level", null),
    GEMS("gems", "INT DEFAULT 100"),
    RUBIES("rubies", "INT DEFAULT 0");

    @Getter
    private final String columnName;
    @Getter
    private final String columnType;

    PlayerDataType(String columnName, String columnType) {
        this.columnName = columnName;
        this.columnType = columnType;
    }
}