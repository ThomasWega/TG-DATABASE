package net.trustgames.toolkit.database.player.vanish;

import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.database.HikariManager;
import net.trustgames.toolkit.database.player.data.PlayerDataFetcher;
import net.trustgames.toolkit.message_queue.RabbitManager;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

import static net.trustgames.toolkit.Toolkit.LOGGER;
import static net.trustgames.toolkit.database.player.vanish.PlayerVanishDB.tableName;

public class PlayerVanishFetcher {
    private final HikariManager hikariManager;
    private final PlayerVanishCache vanishCache;
    private final RabbitManager rabbitManager;
    private final PlayerDataFetcher dataFetcher;
    // TODO add rabbitmq event

    public PlayerVanishFetcher(@NotNull Toolkit toolkit) {
        this.hikariManager = toolkit.getHikariManager();
        this.vanishCache = new PlayerVanishCache(toolkit.getJedisPool());
        this.rabbitManager = toolkit.getRabbitManager();
        this.dataFetcher = new PlayerDataFetcher(toolkit);
    }

    /**
     * Checks if the row for the given uuid exists.
     * If the row exists, that means the player has vanished turned on.
     * If the row doesn't exist, that would mean the player has vanish turned off.
     *
     * @param uuid UUID to check the vanish for
     * @return Whether the vanish is turned on
     */
    private boolean rowExists(@NotNull UUID uuid) {
        try (Connection connection = hikariManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT EXISTS(SELECT 1 FROM " + tableName + " WHERE uuid = ? LIMIT 1)")) {
            statement.setString(1, uuid.toString());
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return results.getBoolean(1); // Get the value of EXISTS function
                } else {
                    return false; // Row does not exist
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while checking if " + uuid + " is vanished in the table " + tableName, e);
            return false;
        }
    }

    /**
     * Checks if the row for the given uuid exists.
     * If the row exists, that means the player has vanished turned on.
     * If the row doesn't exist, that would mean the player has vanish turned off.
     *
     * @param uuid UUID to check the vanish for
     * @return Whether the vanish is turned on
     */
    private Optional<Timestamp> getTimestamp(@NotNull UUID uuid) {
        try (Connection connection = hikariManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT time FROM " + tableName + " WHERE uuid = ? LIMIT 1")) {
            statement.setString(1, uuid.toString());
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return Optional.ofNullable(results.getTimestamp(1));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while getting Vanish Time for " + uuid + " from the table " + tableName, e);
            return Optional.empty();
        }
    }

    /**
     * Create a row for the given UUID.
     * aka. setting the vanish on for the Player
     *
     * @param uuid UUID to create the row for
     * @param timestamp Time the Vanish was set on
     * @see PlayerVanishFetcher#rowExists(UUID)
     */
    private boolean createRow(@NotNull UUID uuid,
                              @NotNull Timestamp timestamp) {
        boolean rowCreated;
        try (Connection connection = hikariManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT IGNORE INTO  " + tableName + "(uuid, time) VALUES (?, ?)")) {
            statement.setString(1, uuid.toString());
            statement.setTimestamp(2, timestamp);
            connection.setAutoCommit(false);
            int rowsAffected = statement.executeUpdate();
            connection.commit();
            rowCreated = (rowsAffected > 0);
            return rowCreated;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while creating a row for " + uuid + " in the table " + tableName, e);
            return false;
        }
    }

    /**
     * Remove the row for the given UUID.
     * aka. removing the vanish from the Player
     *
     * @param uuid UUID to remove the row by
     */
    private void removeRow(@NotNull UUID uuid) {
        try (Connection connection = hikariManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM " + tableName + " WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            connection.setAutoCommit(false);
            statement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while removing a row for " + uuid + " in the table " + tableName, e);
        }
    }


    public boolean isVanished(@NotNull UUID uuid) {
        Optional<Boolean> cacheData = vanishCache.checkVanish(uuid);
        if (cacheData.isPresent()) {
            System.out.println("RETURNED FROM CACHE");
            return cacheData.get();
        }

        boolean dbData = rowExists(uuid);
        vanishCache.setVanish(uuid, dbData);
        return dbData;
    }

    public boolean isVanished(@NotNull String playerName) {
        Optional<UUID> optUuid = dataFetcher.resolveUUID(playerName);
        if (optUuid.isEmpty()) return false;
        UUID uuid = optUuid.get();

        return isVanished(uuid);
    }

    public Optional<Timestamp> resolveVanishTime(@NotNull UUID uuid) {
        Optional<Timestamp> cacheData = vanishCache.checkVanishTime(uuid);
        if (cacheData.isPresent()) {
            System.out.println("RETURNED FROM CACHE");
            return cacheData;
        }

        Optional<Timestamp> dbData = getTimestamp(uuid);
        dbData.ifPresent(timestamp -> vanishCache.setVanishTime(uuid, timestamp));
        return dbData;
    }

    public Optional<Timestamp> resolveVanishTime(@NotNull String playerName) {
        Optional<UUID> optUuid = dataFetcher.resolveUUID(playerName);
        if (optUuid.isEmpty()) return Optional.empty();
        UUID uuid = optUuid.get();

        return resolveVanishTime(uuid);
    }

        public void setVanish(@NotNull UUID uuid) {
        vanishCache.setVanish(uuid, true);
        Timestamp timeNow = Timestamp.from(Instant.now());
        boolean created = createRow(uuid, timeNow);
        if (created) {
            vanishCache.setVanishTime(uuid, timeNow);
        }
    }

    public void setVanish(@NotNull String playerName) {
        Optional<UUID> optUuid = dataFetcher.resolveUUID(playerName);
        if (optUuid.isEmpty()) return;
        UUID uuid = optUuid.get();

        setVanish(uuid);
    }

    public void removeVanish(@NotNull UUID uuid) {
        vanishCache.setVanish(uuid, false);
        vanishCache.removeVanishTime(uuid);
        removeRow(uuid);
    }

    public void removeVanish(@NotNull String playerName) {
        Optional<UUID> optUuid = dataFetcher.resolveUUID(playerName);
        if (optUuid.isEmpty()) return;
        UUID uuid = optUuid.get();

        removeVanish(uuid);
    }
}
