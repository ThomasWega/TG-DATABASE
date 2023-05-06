package net.trustgames.toolkit.database.player.activity;

import net.trustgames.toolkit.managers.HikariManager;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static net.trustgames.toolkit.database.player.activity.PlayerActivityDB.tableName;

/**
 * Fetch or Insert new activity for a Player
 */
public final class PlayerActivityFetcher {

    private final HikariManager hikariManager;

    public PlayerActivityFetcher(@NotNull HikariManager hikariManager) {
        this.hikariManager = hikariManager;
    }

    /**
     * Gets the player's activity by his uuid and returns the result
     * as new list of Activities, which is saved in the callback. This
     * whole operation is run async.
     *
     * @param uuid     UUID of Player to get the activity for
     * @param callback Callback where the result will be saved
     */
    public void fetchByUUID(@NotNull UUID uuid, Consumer<Optional<PlayerActivity>> callback) {
            try (Connection connection = hikariManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + tableName + " WHERE uuid = ? ORDER BY id DESC")) {
                statement.setString(1, uuid.toString());
                try (ResultSet results = statement.executeQuery()) {
                    List<PlayerActivity.Activity> activities = new ArrayList<>();
                    while (results.next()) {
                        long id = results.getLong("id");
                        String ip = results.getString("ip");
                        String action = results.getString("action");
                        Timestamp time = results.getTimestamp("time");
                        activities.add(new PlayerActivity.Activity(id, uuid, ip, action, time));
                    }
                    callback.accept(activities.isEmpty()
                            ? Optional.empty()
                            : Optional.of(new PlayerActivity(uuid, activities)));
                }
            } catch (SQLException e) {
                System.out.println("RUNTIME EXCEPTION 2");
                throw new RuntimeException("While fetching Activity of UUID " + uuid + " from the database", e);
            }
    }

    /**
     * Returns only one Activity with the matching id in async callback
     *
     * @param id       Given ID
     * @param callback Callback where the result will be saved
     */
    public void fetchByID(long id, Consumer<Optional<PlayerActivity.Activity>> callback) {
            try (Connection conn = hikariManager.getConnection();
                 PreparedStatement statement = conn.prepareStatement("SELECT * FROM " + tableName + " WHERE id = ?")) {
                statement.setString(1, String.valueOf(id));
                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        long resultId = results.getLong("id");
                        UUID uuid = UUID.fromString(results.getString("uuid"));
                        String ip = results.getString("ip");
                        String action = results.getString("action");
                        Timestamp time = results.getTimestamp("time");
                        PlayerActivity.Activity activity = new PlayerActivity.Activity(resultId, uuid, ip, action, time);
                        callback.accept(Optional.of(activity));
                        return;
                    }
                    callback.accept(Optional.empty());
                }
            } catch (SQLException e) {
                System.out.println("RUNTIME EXCEPTION 3");
                throw new RuntimeException(e);
            }
    }

    /**
     * creates a new row with the new player activity.
     * values from playerActivity are set for each index
     * (is run async)
     *
     * @param activity Only one Activity with the corresponding data
     */
    public void insertNew(PlayerActivity.@NotNull Activity activity) {
        try (Connection connection = hikariManager.getConnection()) {
            String query = "INSERT INTO " + tableName + " (uuid, ip, action, time) VALUES (?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, activity.getUuid().toString());
                statement.setString(2, activity.getIp());
                statement.setString(3, activity.getAction());
                statement.setTimestamp(4, activity.getTime());
                statement.executeUpdate();
                try (ResultSet result = statement.getGeneratedKeys()) {
                    if (result.next()) {
                        long id = result.getLong(1);
                        activity.setId(id);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("RUNTIME EXCEPTION 20");
            throw new RuntimeException("Error inserting player activity into database", e);
        }
    }
}
