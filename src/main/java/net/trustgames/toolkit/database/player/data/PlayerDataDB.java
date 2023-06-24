package net.trustgames.toolkit.database.player.data;


import lombok.Getter;
import net.trustgames.toolkit.database.DatabaseTable;
import net.trustgames.toolkit.database.HikariManager;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * This class handles the creation of the data database table
 */
public final class PlayerDataDB extends DatabaseTable {

    @Getter
    private static final String tableName = "player_data";

    public PlayerDataDB(@NotNull HikariManager hikariManager) {
        super(hikariManager, tableName);
    }

    @Override
    protected String sqlStatement() {
        StringBuilder statement = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append(tableName)
                .append("(");

        Arrays.stream(PlayerDataType.values())
                .filter(dataType -> dataType.getColumnType() != null)
                .forEach(dataType ->
                        statement.append(dataType.getColumnName())
                                .append(" ")
                                .append(dataType.getColumnType())
                                .append(",")
                );
        statement.deleteCharAt(statement.length() - 1);
        statement.append(")");

        return statement.toString();
    }
}
