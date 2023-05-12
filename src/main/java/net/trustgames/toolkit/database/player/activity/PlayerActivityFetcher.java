package net.trustgames.toolkit.database.player.activity;

import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.managers.HikariManager;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

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
     * @return Future where the result will be saved
     */
    public CompletableFuture<Optional<PlayerActivity>> fetchByUUID(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
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
                    return activities.isEmpty()
                            ? Optional.empty()
                            : Optional.of(new PlayerActivity(uuid, activities));
                }
            } catch (SQLException e) {
                System.out.println("RUNTIME EXCEPTION 2");
                Toolkit.getLogger().log(Level.SEVERE, "Exception occurred while fetching PlayerActivity from the database by UUID " + uuid +" async", e);
                throw new RuntimeException("While fetching Activity of player with UUID " + uuid + " from the database", e);
            }
        });
    }

    /**
     * Returns only one Activity with the matching id in async callback
     *
     * @param id       Given ID
     * @return Future where the result will be saved
     */
    public CompletableFuture<Optional<PlayerActivity.Activity>> fetchByID(long id) {
        return CompletableFuture.supplyAsync(() -> {
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
                        return Optional.of(activity);
                    }
                    return Optional.empty();
                }
            } catch (SQLException e) {
                Toolkit.getLogger().log(Level.SEVERE, "Exception occurred while fetching PlayerActivity.Activity from the database by ID " + id +" async", e);
                throw new RuntimeException("While fetching Activity of ID " + id + " from the database async", e);
            }
        });
    }

    /**
     * creates a new row with the new player activity.
     * values from playerActivity are set for each index
     * (is run async)
     *
     * @param activity Only one Activity with the corresponding data
     */
    public void insertNew(PlayerActivity.@NotNull Activity activity) {
        CompletableFuture.runAsync(() -> {
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
                throw new RuntimeException("Inserting player activity with id " + activity.getId() + " into database async", e);
            }
        }).exceptionally(throwable -> {
            Toolkit.getLogger().log(Level.SEVERE, "Exception occurred while inserting new PlayerActivity.Activity to the database async", throwable);
            return null;
        });
    }
}
