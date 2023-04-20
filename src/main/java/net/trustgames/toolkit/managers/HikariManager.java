package net.trustgames.toolkit.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.trustgames.toolkit.Middleware;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * This class handles the basic MariaDB and HikariCP methods such as getting connection,
 * creating the database, table (if not exists) and closing the hikari connection. Note that the
 * plugin#getLogger is used instead of Bukkit#getLogger, because async methods should not access Bukkit API
 */
public final class HikariManager {

    private static final Logger logger = Middleware.getLogger();
    private static HikariDataSource dataSource;

    /**
     * Sets parameters and creates new pool.
     * (is run async)
     */
    public HikariManager(@NotNull String user,
                         @NotNull String password,
                         @NotNull String ip,
                         @NotNull String port,
                         @NotNull String database,
                         @NotNull Integer poolSize) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("org.mariadb.jdbc.Driver");
        hikariConfig.setJdbcUrl("jdbc:mariadb://" + ip + ":" + port + "/" + database);
        hikariConfig.addDataSourceProperty("user", user);
        hikariConfig.addDataSourceProperty("password", password);
        hikariConfig.setMaximumPoolSize(poolSize);

        dataSource = new HikariDataSource(hikariConfig);
    }

    /**
     * gets a new connection from the hikaricp pool
     */
    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            logger.severe("Getting a new connection from HikariCP");
            throw new RuntimeException(e);
        }
    }

    /**
     * @param callback When datasource is initialized
     */
    public void onDataSourceInitialized(Runnable callback) {
        CompletableFuture.runAsync(() -> {
                    if (dataSource != null) {
                        callback.run();
                    } else {
                        // if dataSource is null, schedule the callback to be run when it is initialized
                        try {
                            Thread.sleep(500L);
                            onDataSourceInitialized(callback);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).orTimeout(10L, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    Middleware.getLogger().severe("HikariCP data source initialization timed out!");
                    return null;
                });
    }

    /**
     * checks if the table exists, if it doesn't, it creates one using the given SQL statement
     * (is run async)
     *
     * @param tableName       The name of the table
     * @param stringStatement The SQL statement in String
     */
    public void initializeTable(@NotNull String tableName, @NotNull String stringStatement) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(stringStatement)) {
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                logger.severe("Unable to create missing " + tableName + " table in the database!");
                throw new RuntimeException(e);
            }
        });
    }

    public void close() {
        dataSource.close();
    }
}
