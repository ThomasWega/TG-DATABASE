package net.trustgames.toolkit.database.player.data.cache;

import net.trustgames.toolkit.database.player.data.PlayerData;
import net.trustgames.toolkit.database.player.data.config.PlayerDataIntervalConfig;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataCache {

    @Nullable
    private final JedisPool pool;

    public PlayerDataCache(@Nullable JedisPool pool) {
        this.pool = pool;
    }

    /**
     * Get the specified data type from the cache
     *
     * @param uuid UUID of the Player
     * @param dataType which data type to get the data of
     * @return Optional with the value or empty
     */
    public Optional<String> getData(@NotNull UUID uuid,
                                    @NotNull PlayerDataType dataType) {
        if (dataType == PlayerDataType.UUID){
            throw new RuntimeException("Use the method specified for getting UUID from the cache!");
        }

        if (pool == null) {
            return Optional.empty();
        }

        try (Jedis jedis = pool.getResource()) {
            String result = jedis.hget(uuid.toString(), dataType.getColumnName());
            jedis.expire(uuid.toString(), PlayerDataIntervalConfig.DATA_EXPIRY.getSeconds());
            return Optional.ofNullable(result);
        }
    }

    /**
     * Get all the data from the cache
     *
     * @param uuid UUID of the Player
     * @return Optional with PlayerData Object with filled in values or empty optional
     */
    public Optional<PlayerData> getAllData(@NotNull UUID uuid){
        if (pool == null) {
            return Optional.empty();
        }

        // todo all values need to be present to work
        Map<PlayerDataType, String> data = new ConcurrentHashMap<>();
        try (Jedis jedis = pool.getResource()) {
            for (PlayerDataType dataType : PlayerDataType.values()) {
                String result = jedis.hget(uuid.toString(), dataType.getColumnName());
                if (result == null) {
                    return Optional.empty();
                }
                data.put(dataType, result);
            }
            jedis.expire(uuid.toString(), PlayerDataIntervalConfig.DATA_EXPIRY.getSeconds());
        }
        return Optional.of(new PlayerData(
                uuid,
                data.get(PlayerDataType.NAME),
                Integer.parseInt(data.get(PlayerDataType.KILLS)),
                Integer.parseInt(data.get(PlayerDataType.DEATHS)),
                Integer.parseInt(data.get(PlayerDataType.GAMES_PLAYED)),
                Integer.parseInt(data.get(PlayerDataType.PLAYTIME)),
                Integer.parseInt(data.get(PlayerDataType.XP)),
                Integer.parseInt(data.get(PlayerDataType.LEVEL)),
                Integer.parseInt(data.get(PlayerDataType.GEMS)),
                Integer.parseInt(data.get(PlayerDataType.RUBIES))
        ));
    }

    /**
     * Get the UUID by the name
     *
     * @param playerName Name of the player
     * @return UUID of the player or empty
     */
    public Optional<UUID> getUUID(@Nullable String playerName) {

        if (pool == null) {
            return Optional.empty();
        }

        String uuidString;
        try (Jedis jedis = pool.getResource()) {
            uuidString = jedis.hget(playerName, PlayerDataType.UUID.getColumnName());
            jedis.expire(playerName, PlayerDataIntervalConfig.DATA_EXPIRY.getSeconds());
        }

        if (uuidString == null){
            return Optional.empty();
        }

        return Optional.of(UUID.fromString(uuidString));
    }

    /**
     * Replace the specified data type in the cache with the given value
     *
     * @param uuid UUID of the player
     * @param dataType Data type to set the value of
     * @param value The new value to be set
     */
    public void updateData(@NotNull UUID uuid,
                       @NotNull PlayerDataType dataType,
                       @NotNull String value) {
        if (pool == null) return;
        try (Jedis jedis = pool.getResource()) {
            String column = dataType.getColumnName();
            jedis.hset(uuid.toString(), column, value);
            jedis.expire(uuid.toString(), PlayerDataIntervalConfig.DATA_EXPIRY.getSeconds());
        }
    }

    /**
     * Updates all the data in the cache by one operation
     *
     * @param uuid UUID of the Player
     * @param data PlayerData Object with filled in values
     */
    public void updateAllData(@NotNull UUID uuid,
                              @NotNull PlayerData data) {
        System.out.println("UPDATE HMM???");
        if (pool == null) return;

        try (Jedis jedis = pool.getResource()) {
            jedis.hset(data.getName(), PlayerDataType.UUID.getColumnName(), uuid.toString());
            jedis.hmset(uuid.toString(), Map.of(
                    PlayerDataType.NAME.getColumnName(), data.getName(),
                    PlayerDataType.KILLS.getColumnName(), String.valueOf(data.getKills()),
                    PlayerDataType.DEATHS.getColumnName(), String.valueOf(data.getDeaths()),
                    PlayerDataType.GAMES_PLAYED.getColumnName(), String.valueOf(data.getGamesPlayed()),
                    PlayerDataType.PLAYTIME.getColumnName(), String.valueOf(data.getPlaytimeSeconds()),
                    PlayerDataType.XP.getColumnName(), String.valueOf(data.getXp()),
                    PlayerDataType.GEMS.getColumnName(),String.valueOf( data.getGems()),
                    PlayerDataType.RUBIES.getColumnName(), String.valueOf(data.getRubies())
            ));
            jedis.expire(uuid.toString(), PlayerDataIntervalConfig.DATA_EXPIRY.getSeconds());
        }
    }

    /**
     * Update the UUID of the Player in the cache
     *
     * @param playerName Name of the player (primary key)
     * @param uuid UUID of the player (value)
     */
    public void updateUUID(@NotNull String playerName,
                           @NotNull UUID uuid) {
        if (pool == null) return;
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(playerName, PlayerDataType.UUID.getColumnName(), uuid.toString());
            jedis.expire(playerName, PlayerDataIntervalConfig.DATA_EXPIRY.getSeconds());
        }
    }

    /**
     * Expire the specified key in the cache with the duration configured in config
     *
     * @param key Key to expire
     * @see PlayerDataCache#expire(String, long)
     */
    public void expire(@NotNull String key) {
        expire(key, PlayerDataIntervalConfig.DATA_EXPIRY.getSeconds());
    }

    /**
     * Expire the specified key in the cache with the given duration
     *
     * @param key Key to expire
     * @see PlayerDataCache#expire(String)
     */
    public void expire(@NotNull String key, long seconds) {
        if (pool == null) return;
        try (Jedis jedis = pool.getResource()) {
            jedis.expire(key, seconds);
        }
    }
}
