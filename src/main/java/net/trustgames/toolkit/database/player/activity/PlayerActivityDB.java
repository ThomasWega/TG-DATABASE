package net.trustgames.toolkit.database.player.activity;

import net.trustgames.toolkit.database.HikariManager;
import org.jetbrains.annotations.NotNull;

/**
 * This class is used to handle the database table for player activity
 * including its methods, functions etc.
 */
public final class PlayerActivityDB {

    public static final String tableName = "player_activity";

    private final HikariManager hikariManager;

    public PlayerActivityDB(@NotNull HikariManager hikariManager) {
        this.hikariManager = hikariManager;
        initializeTable();
    }

    /**
     * use external method from MariaDB class
     * with specified SQL statement to create a new table
     * (is run async)
     */
    public void initializeTable() {
        String statement = "CREATE TABLE IF NOT EXISTS " + tableName + "(id BIGINT unsigned primary key AUTO_INCREMENT, uuid VARCHAR(36), ip VARCHAR(15), action TINYTEXT, time DATETIME)";
        hikariManager.initializeTable(tableName, statement);
    }
}
