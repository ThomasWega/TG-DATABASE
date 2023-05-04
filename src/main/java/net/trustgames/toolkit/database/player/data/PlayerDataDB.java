package net.trustgames.toolkit.database.player.data;


import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.managers.HikariManager;

import java.util.Arrays;

/**
 * This class handles the creation of the data database table
 */
public final class PlayerDataDB {

    public static final String tableName = "player_data";
    private final HikariManager hikariManager;

    public PlayerDataDB(HikariManager hikariManager) {
        this.hikariManager = hikariManager;
        initializeTable();
    }

    /**
     * use external method from MariaDB class
     * with specified SQL statement to create a new table
     * (is run async)
     */
    public void initializeTable() {
        StringBuilder statement = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append(tableName)
                .append("(");

        Arrays.stream(PlayerDataType.values())
                .filter(dataType -> dataType.getColumnName() != null || dataType.getColumnType() != null)
                .forEach(dataType ->
                        statement.append(dataType.getColumnName())
                                .append(" ")
                                .append(dataType.getColumnType())
                                .append(",")
                );
        statement.deleteCharAt(statement.length() - 1);
        statement.append(")");

        hikariManager.initializeTable(tableName, statement.toString());
    }
}
