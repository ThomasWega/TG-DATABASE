package net.trustgames.toolkit.database.player.data.uuid;

import com.rabbitmq.client.AMQP;
import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.cache.UUIDCache;
import net.trustgames.toolkit.database.player.data.PlayerDataFetcher;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.managers.HikariManager;
import net.trustgames.toolkit.managers.rabbit.extras.queues.PlayerDataUpdateQueues;
import net.trustgames.toolkit.utils.UUIDUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static net.trustgames.toolkit.database.player.data.PlayerDataDB.tableName;

public class PlayerUUIDFetcher {

    private final Toolkit toolkit;
    private final HikariManager hikariManager;

    public PlayerUUIDFetcher(@NotNull Toolkit toolkit) {
        this.toolkit = toolkit;
        this.hikariManager = toolkit.getHikariManager();
    }

    /**
     * Gets specifically only the Player's uuid by his name from the database.
     * This whole operation is run async, and the result is saved
     * in the callback. If no result is found, null is returned
     *
     * @param callback Callback where the result will be saved
     * @implNote Can't fetch anything other that Player's UUID!
     * @see PlayerUUIDFetcher#setIfNotExists(String, UUID)
     */
    public void exists(@NotNull String playerName, Consumer<Boolean> callback) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = hikariManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + tableName + " WHERE name = ?")) {
                statement.setString(1, playerName);
                ResultSet result = statement.executeQuery();
                if (result.next()) {
                    int count = result.getInt(1);
                    if (count > 0)
                        callback.accept(true);
                }
                callback.accept(false);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * First checks if the player's data in the database exists,
     * If it doesn't, that means the player doesn't have any data yet
     * and a new row is created for the player
     *
     * @param playerName Name of the Player
     * @param uuid       UUID of the Player
     */
    public void setIfNotExists(@NotNull String playerName, @NotNull UUID uuid) {
        CompletableFuture.runAsync(() -> {
            Connection connection = hikariManager.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT IGNORE INTO " + tableName + " (uuid, name) VALUES (?, ?)")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, playerName);
                connection.setAutoCommit(false); // disable auto-commit mode to start a transaction
                statement.executeUpdate();
                connection.commit();

                new UUIDCache(toolkit, playerName).update(uuid);
                // call an event
                toolkit.getRabbitManager().fireAndForget(
                        PlayerDataUpdateQueues.queueOf(PlayerDataType.UUID),
                        new AMQP.BasicProperties().builder()
                                .expiration("5000")
                                .build(),
                        new JSONObject().put("uuid", uuid)
                );
            } catch (SQLException e) {
                try {
                    connection.rollback();
                    connection.close();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    /**
     * Gets specifically only the Player's uuid by his name from the database.
     * This whole operation is run async, and the result is saved
     * in the callback. If no result is found, null is returned
     *
     * @param callback Callback where the result will be saved
     * @implNote Can't fetch anything other that Player's UUID!
     * @see PlayerDataFetcher#fetch(UUID, Consumer)
     */
    public void fetch(@NotNull String playerName, Consumer<Optional<UUID>> callback) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = hikariManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT uuid FROM " + tableName + " WHERE name = ?")) {
                statement.setString(1, playerName);
                ResultSet result = statement.executeQuery();
                if (result.next()) {
                    String stringUuid = result.getString("uuid");
                    try {
                        if (UUIDUtils.isValidUUID(stringUuid)){
                            callback.accept(Optional.of(UUID.fromString(stringUuid)));
                        } else {
                            callback.accept(Optional.empty());
                        }
                        return;
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException("INVALID UUID FOR: " + stringUuid, e);
                    }
                }
                callback.accept(Optional.empty());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
