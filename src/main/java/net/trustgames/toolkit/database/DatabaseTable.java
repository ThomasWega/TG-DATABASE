package net.trustgames.toolkit.database;

import org.jetbrains.annotations.NotNull;

public abstract class DatabaseTable {

    public DatabaseTable(@NotNull HikariManager hikariManager, @NotNull String tableName) {
        hikariManager.initializeTableAsync(tableName, this.sqlStatement());
    }

    protected abstract String sqlStatement();
}
