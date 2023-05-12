package net.trustgames.toolkit.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.trustgames.toolkit.Toolkit;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class handles the basic MariaDB and HikariCP methods such as getting connection,
 * creating the database, table (if not exists) and closing the hikari connection. Note that the
 * plugin#getLogger is used instead of Bukkit#getLogger, because async methods should not access Bukkit API
 */
public final class HikariManager {

    private static final Logger logger = Toolkit.getLogger();
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
            System.out.println("RUNTIME EXCEPTION 1");
            throw new RuntimeException("Exception occurred while getting a new connection from HikariCP Pool", e);
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
                            System.out.println("RUNTIME EXCEPTION 10");
                            throw new RuntimeException("Exception occurred while sleeping the HikariCP data source initialization thread", e);
                        }
                    }
                })
                .orTimeout(10L, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    logger.log(Level.SEVERE, "HikariCP data source initialization timed out!", throwable);
                    return null;
                });
    }

    /**
     * @return true - if datasource is initialized<p>
     * false - if datasource is not initialized
     */
    public boolean isDataSourceInitialized() {
        return dataSource != null;
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
                        System.out.println("RUNTIME EXCEPTION 11");
                        throw new RuntimeException("Database access error occurred while trying to create missing " + tableName + " table in the database", e);
                    }
                })
                .exceptionally(throwable -> {
                    logger.log(Level.SEVERE, "Exception occurred while trying to create missing " + tableName + " table in the database", throwable);
                    return null;
                });
    }

    public void close() {
        Toolkit.getLogger().warning("HikariCP activity connections: " + dataSource.getHikariPoolMXBean().getActiveConnections());
        dataSource.close();
    }
}
