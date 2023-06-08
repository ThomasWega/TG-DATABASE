package net.trustgames.toolkit.database.player.vanish;


import net.trustgames.toolkit.database.HikariManager;

/**
 * This class handles the creation of the data database table
 */
public final class PlayerVanishDB {

    public static final String tableName = "player_vanished";
    private final HikariManager hikariManager;

    public PlayerVanishDB(HikariManager hikariManager) {
        this.hikariManager = hikariManager;
        initializeTable();
    }

    public void initializeTable() {
        String statement = "CREATE TABLE IF NOT EXISTS " + tableName + "(uuid VARCHAR(36) primary key, time TIMESTAMP)";
        hikariManager.initializeTable(tableName, statement);
    }
}
