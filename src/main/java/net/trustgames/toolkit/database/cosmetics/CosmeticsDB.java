package net.trustgames.toolkit.database.cosmetics;

import lombok.Getter;
import net.trustgames.toolkit.database.DatabaseTable;
import net.trustgames.toolkit.database.HikariManager;
import org.jetbrains.annotations.NotNull;

public class CosmeticsDB extends DatabaseTable {

    @Getter
    private static final String tableName = "cosmetics";

    public CosmeticsDB(@NotNull HikariManager hikariManager) {
        super(hikariManager, tableName);
    }

    @Override
    protected String sqlStatement() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + "(cosmetic_id VARCHAR(32))";
    }
}
