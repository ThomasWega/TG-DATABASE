package net.trustgames.toolkit.database.player.data.uuid;

import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.cache.UUIDCache;
import net.trustgames.toolkit.database.player.data.PlayerDataFetcher;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.database.player.data.event.PlayerDataUpdateEvent;
import net.trustgames.toolkit.managers.HikariManager;
import net.trustgames.toolkit.utils.UUIDUtils;
import org.jetbrains.annotations.NotNull;

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
     * @see PlayerUUIDFetcher#updateName(UUID, String)
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
                System.out.println("RUNTIME EXCEPTION 6");
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * A new row is created for the player either if none existed yet, or if
     * his name is different from the last time.
     *
     * @param uuid       UUID of the Player (primary key)
     * @param playerName Name of the Player (to be replaced the column with)
     */
    public void updateName(@NotNull UUID uuid, @NotNull String playerName) {
        CompletableFuture.runAsync(() -> {
            Connection connection = hikariManager.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO " + tableName + " (uuid, name) " +
                            "VALUES (?, ?) " +
                            "ON DUPLICATE KEY UPDATE name = VALUES(name)")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, playerName);
                connection.setAutoCommit(false); // disable auto-commit mode to start a transaction
                statement.executeUpdate();
                connection.commit();

                new UUIDCache(toolkit, playerName).update(uuid);
                new PlayerDataUpdateEvent(toolkit.getRabbitManager(), uuid, PlayerDataType.UUID).publish();
            } catch (SQLException e) {
                try {
                    connection.rollback();
                    connection.close();
                } catch (SQLException ex) {
                    System.out.println("RUNTIME EXCEPTION 7");
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
                        System.out.println("RUNTIME EXCEPTION 8");
                        throw new RuntimeException("INVALID UUID FOR: " + stringUuid, e);
                    }
                }
                callback.accept(Optional.empty());
            } catch (SQLException e) {
                System.out.println("RUNTIME EXCEPTION 9");
                throw new RuntimeException(e);
            }
        });
    }
}
