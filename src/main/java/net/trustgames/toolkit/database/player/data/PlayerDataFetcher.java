package net.trustgames.toolkit.database.player.data;

import com.rabbitmq.client.AMQP;
import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.cache.PlayerDataCache;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.database.player.data.uuid.PlayerUUIDFetcher;
import net.trustgames.toolkit.managers.HikariManager;
import net.trustgames.toolkit.managers.rabbit.RabbitManager;
import net.trustgames.toolkit.managers.rabbit.extras.RabbitQueues;
import net.trustgames.toolkit.utils.LevelUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static net.trustgames.toolkit.database.player.data.PlayerDataDB.tableName;


/**
 * This class is used to fetch and update the player data database table
 */
public final class PlayerDataFetcher {

    private final Toolkit toolkit;
    @Nullable
    private final HikariManager hikariManager;
    @Nullable
    private final RabbitManager rabbitManager;
    private PlayerDataType dataType;


    public PlayerDataFetcher(@NotNull Toolkit toolkit,
                             @NotNull PlayerDataType dataType) {
        this.toolkit = toolkit;
        this.hikariManager = toolkit.getHikariManager();
        this.rabbitManager = toolkit.getRabbitManager();
        this.dataType = dataType;

        if (dataType == PlayerDataType.UUID) {
            throw new RuntimeException(this.getClass().getName() + " can't be used to retrieve UUID. " +
                    "Use the " + PlayerUUIDFetcher.class.getName() + " instead!");
        }
    }

    /**
     * Get the player data Object from the column "label" that corresponds
     * to the given uuid. This whole operation is run async, and the result is saved
     * in the callback. If no result is found, int "0" is returned
     *
     * @param callback Callback where the result will be saved
     * @implNote Can't fetch player's UUID!
     * @see PlayerDataFetcher#fetch(UUID, Consumer)
     */
    public void fetch(@NotNull UUID uuid, Consumer<@Nullable String> callback) {
        if (hikariManager == null) {
            Toolkit.getLogger().severe("HikariManager is not initialized");
            return;
        }

        CompletableFuture.runAsync(() -> {

            /*
            in database, only the xp is saved, that means first the XP
            needs to be fetched and after that converted to the player's level
             */
            PlayerDataType dataTypeFinal;
            if (dataType == PlayerDataType.LEVEL) {
                dataTypeFinal = PlayerDataType.XP;
            } else {
                dataTypeFinal = dataType;
            }

            String label = dataTypeFinal.getColumnName();
            try (Connection connection = hikariManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT " + label + " FROM " + tableName + " WHERE uuid = ?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        String fetchedData = results.getObject(label).toString();

                        // convert the XP retrieved to LEVEL
                        if (dataType == PlayerDataType.LEVEL) {
                            fetchedData = String.valueOf(LevelUtils.getLevelByXp(Integer.parseInt(fetchedData)));
                        }
                        callback.accept(fetchedData);
                        return;
                    }
                    callback.accept(null);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Updates the given DataType column with the given object
     * It uses transactions, to ensure that other updates don't interfere
     * with each other.
     * Data is also updated in the redis cache.
     *
     * @param object Object to update the DataType with
     */
    public void update(@NotNull UUID uuid, @NotNull Object object) {
        if (hikariManager == null) {
            Toolkit.getLogger().severe("HikariManager is not initialized");
            return;
        }

        // if XP, the level also needs to be recalculated and updated
        if (dataType == PlayerDataType.XP) {
            int level = LevelUtils.getLevelByXp(Integer.parseInt(object.toString()));
            PlayerDataCache levelCache = new PlayerDataCache(toolkit, uuid, PlayerDataType.LEVEL);
            levelCache.update(String.valueOf(level));
        }

        // if LEVEL, it needs to be recalculated to XP and updated in the cache
        if (dataType == PlayerDataType.LEVEL) {
            PlayerDataCache levelCache = new PlayerDataCache(toolkit, uuid, PlayerDataType.LEVEL);
            levelCache.update(object.toString());
            object = LevelUtils.getThreshold(Integer.parseInt(object.toString()));
            dataType = PlayerDataType.XP;
        }

        String label = dataType.getColumnName();

        Connection connection = hikariManager.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO " + tableName + "(uuid, " + label + ") VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE " + label + " = VALUES(" + label + ")")) {
            statement.setString(1, uuid.toString());
            statement.setObject(2, object);
            connection.setAutoCommit(false); // disable auto-commit mode to start a transaction
            statement.executeUpdate();
            connection.commit();

            // update the cache
            PlayerDataCache playerDataCache = new PlayerDataCache(toolkit, uuid, dataType);
            playerDataCache.update(object.toString());

            // call the event from the main thread

            if (rabbitManager != null) {
                rabbitManager.fireAndForget(
                        RabbitQueues.EVENT_PLAYER_DATA_UPDATE,
                        new AMQP.BasicProperties().builder()
                                .expiration("5000")
                                .build(),
                        new JSONObject().put("uuid", uuid)
                );
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.close();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}