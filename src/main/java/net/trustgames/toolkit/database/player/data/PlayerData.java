package net.trustgames.toolkit.database.player.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.database.player.data.cache.PlayerDataCache;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.utils.LevelUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static net.trustgames.toolkit.database.player.data.PlayerDataDB.tableName;

@Data
@AllArgsConstructor
public class PlayerData {
    private UUID uuid;
    private String name;
    private int kills;
    private int deaths;
    private int gamesPlayed;
    private int playtimeSeconds;
    private int xp;
    private int level;
    private float levelProgress;
    private int gems;
    private int rubies;

    /**
     * @see PlayerData#getPlayerData(Toolkit, UUID)
     */
    public static CompletableFuture<Optional<PlayerData>> getPlayerDataAsync(@NotNull Toolkit toolkit,
                                                                             @NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> getPlayerData(toolkit, uuid))
                .exceptionally(throwable -> {
                    Toolkit.LOGGER.log(Level.SEVERE, "Exception occurred while getting PlayerData object by UUID " + uuid + " async", throwable);
                    return Optional.empty();
                });
    }

    /**
     * @see PlayerData#getPlayerData(Toolkit, String)
     */
    public static CompletableFuture<Optional<PlayerData>> getPlayerDataAsync(@NotNull Toolkit toolkit,
                                                                             @NotNull String playerName) {
        return CompletableFuture.supplyAsync(() -> getPlayerData(toolkit, playerName))
                .exceptionally(throwable -> {
                    Toolkit.LOGGER.log(Level.SEVERE, "Exception occurred while getting PlayerData object by name " + playerName + " async", throwable);
                    return Optional.empty();
                });
    }

    /**
     * Tries to get all the data for the player from the cache.
     * In case any data wasn't in the cache, it gets it from the database.
     * Then converts everything into new PlayerData object with filled in values.
     *
     * @param toolkit instance of Toolkit
     * @param uuid    UUID of the player
     * @return Optional with PlayerData with filled in values or empty
     * @see PlayerData#getPlayerData(Toolkit, String)
     */
    public static Optional<PlayerData> getPlayerData(@NotNull Toolkit toolkit,
                                                     @NotNull UUID uuid) {
        PlayerDataCache dataCache = new PlayerDataCache(toolkit.getJedisPool());
        Optional<HashMap<PlayerDataType, Optional<String>>> cachedOptData = dataCache.getAllData(uuid);
        if (cachedOptData.isPresent()) {
            Map<PlayerDataType, Optional<String>> cachedData = cachedOptData.get();

            // PlayerDataTypes which have empty value
            List<PlayerDataType> missingDataTypeValues = cachedData.entrySet().stream()
                    .filter(entry -> entry.getValue().isEmpty())
                    .map(Map.Entry::getKey)
                    .toList();

            // if none values are missing, that means all the data was successfully retrived from the cache
            if (missingDataTypeValues.isEmpty()) {
                return Optional.of(initializePlayerDataFromHashMap(uuid, cachedData));
            } else {
                // get the missing DataTypes values
                Map<PlayerDataType, Optional<String>> databaseOptData = new PlayerDataFetcher(toolkit).resolveFetchCollectionByKey(PlayerDataFetcher.FetchKey.UUID, uuid.toString(), missingDataTypeValues);

                // means the missing data types values are not even in the database
                if (databaseOptData.isEmpty())
                    return Optional.empty();

                // Present values of the missing DataTypes, which can be updated in the cache
                Map<PlayerDataType, String> cacheUpdateData = databaseOptData.entrySet()
                        .stream()
                        .filter(entry -> entry.getValue().isPresent())
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
                dataCache.updateData(uuid, cacheUpdateData);

                // merge the retrieved data from cache (if any is there) and database
                Map<PlayerDataType, Optional<String>> finalMap = new HashMap<>(databaseOptData);
                if (!cachedData.isEmpty()){
                    finalMap.putAll(cachedData);
                }
                finalMap.putAll(databaseOptData);

                return Optional.of(initializePlayerDataFromHashMap(uuid, finalMap));
            }
        } else {
            return getAllDataFromDatabase(toolkit, uuid);
        }
    }

    /**
     * First tries to get the UUID of the player by his name
     * from cache or if not present, from the database (done by method).
     * Then it just calls the UUID PlayerData method
     *
     * @param toolkit instance of Toolkit
     * @param playerName Name of the Player
     * @return Optional with PlayerData with filled in values or empty
     * @see PlayerData#getPlayerData(Toolkit, UUID)
     */
    public static Optional<PlayerData> getPlayerData(@NotNull Toolkit toolkit,
                                                     @NotNull String playerName) {
        Optional<UUID> optUUID = new PlayerDataFetcher(toolkit).resolveUUID(playerName);
        if (optUUID.isEmpty()){
            return Optional.empty();
        }
        return getPlayerData(toolkit, optUUID.get());
    }

    /**
     * create a new PlayerData object from the supplied Map
     */
    private static PlayerData initializePlayerDataFromHashMap(UUID uuid,
                                                              Map<PlayerDataType, Optional<String>> finalMap) {
        return new PlayerData(
                uuid,
                finalMap.get(PlayerDataType.NAME).orElse("UNKNOWN"),
                Integer.parseInt(finalMap.get(PlayerDataType.KILLS).orElse("0")),
                Integer.parseInt(finalMap.get(PlayerDataType.DEATHS).orElse("0")),
                Integer.parseInt(finalMap.get(PlayerDataType.GAMES_PLAYED).orElse("0")),
                Integer.parseInt(finalMap.get(PlayerDataType.PLAYTIME).orElse("0")),
                Integer.parseInt(finalMap.get(PlayerDataType.XP).orElse("0")),
                Integer.parseInt(finalMap.get(PlayerDataType.LEVEL).orElse("0")),
                LevelUtils.getProgress(Integer.parseInt(finalMap.get(PlayerDataType.XP).orElse("0"))),
                Integer.parseInt(finalMap.get(PlayerDataType.GEMS).orElse("0")),
                Integer.parseInt(finalMap.get(PlayerDataType.RUBIES).orElse("0"))
        );
    }

    /**
     * Retrieve all the data for the PlayerData object from the database
     */
    private static Optional<PlayerData> getAllDataFromDatabase(Toolkit toolkit, UUID uuid) {
        PlayerDataCache dataCache = new PlayerDataCache(toolkit.getJedisPool());
        try (Connection connection = toolkit.getHikariManager().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + tableName + " WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    PlayerData playerData = new PlayerData(
                            UUID.fromString(rs.getString(PlayerDataType.UUID.getColumnName())),
                            rs.getString(PlayerDataType.NAME.getColumnName()),
                            rs.getInt(PlayerDataType.KILLS.getColumnName()),
                            rs.getInt(PlayerDataType.DEATHS.getColumnName()),
                            rs.getInt(PlayerDataType.GAMES_PLAYED.getColumnName()),
                            rs.getInt(PlayerDataType.PLAYTIME.getColumnName()),
                            rs.getInt(PlayerDataType.XP.getColumnName()),
                            LevelUtils.getLevelByXp(rs.getInt(PlayerDataType.XP.getColumnName())),
                            LevelUtils.getProgress(rs.getInt(PlayerDataType.XP.getColumnName())),
                            rs.getInt(PlayerDataType.GEMS.getColumnName()),
                            rs.getInt(PlayerDataType.RUBIES.getColumnName())
                    );
                    dataCache.updateAllData(uuid, playerData);
                    return Optional.of(playerData);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            Toolkit.LOGGER.log(Level.SEVERE, "Exception occurred while getting PlayerData object by UUID " + uuid, e);
            return Optional.empty();
        }
    }
}