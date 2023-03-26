package net.trustgames.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * This class handles the basic MariaDB and HikariCP methods such as getting connection,
 * creating the database, table (if not exists) and closing the hikari connection. Note that the
 * plugin#getLogger is used instead of Bukkit#getLogger, because async methods should not access Bukkit API
 */
@SuppressWarnings("unused")
public class HikariManager {

    private static final Logger logger = Database.getLogger();
    private static HikariDataSource dataSource;
    @Getter
    @Setter
    private boolean disabled;

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
        CompletableFuture.runAsync(() -> {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName("org.mariadb.jdbc.Driver");
            hikariConfig.setJdbcUrl("jdbc:mariadb://" + ip + ":" + port + "/" + database);
            hikariConfig.addDataSourceProperty("user", user);
            hikariConfig.addDataSourceProperty("password", password);
            hikariConfig.setMaximumPoolSize(poolSize);

            dataSource = new HikariDataSource(hikariConfig);
        });
    }

    /**
     * Check if table exists.
     *
     * @param connection HikariCP connection
     * @param tableName  The name of the table
     * @return if the table already exists
     * @throws SQLException if it can't get the ResultSet
     * @implNote The connection isn't closed by this method
     */
    private boolean tableExist(@NotNull Connection connection,
                                      @NotNull String tableName) throws SQLException {
        boolean tExists = false;
        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null)) {
            while (rs.next()) {
                String tName = rs.getString("TABLE_NAME");
                if (tName != null && tName.equals(tableName)) {
                    tExists = true;
                    break;
                }
            }
        }
        return tExists;
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
                    Thread.sleep(10000L);
                    onDataSourceInitialized(callback);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
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
                if (tableExist(connection, tableName)) return;
                logger.info("Database table " + tableName + " doesn't exist, creating...");
                try (PreparedStatement statement = connection.prepareStatement(stringStatement)) {
                    statement.executeUpdate();
                    if (tableExist(connection, tableName)) {
                        logger.finest("Successfully created the table " + tableName);
                    }
                }
            } catch (SQLException e) {
                logger.severe("Unable to create " + tableName + " table in the database!");
                throw new RuntimeException(e);
            }
        });
    }

    public void closeHikari() {
        dataSource.close();
    }
}
