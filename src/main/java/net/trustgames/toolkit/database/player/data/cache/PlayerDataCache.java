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

    public void updateUUID(@NotNull String playerName,
                           @NotNull UUID uuid) {
        if (pool == null) return;
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(playerName, PlayerDataType.UUID.getColumnName(), uuid.toString());
            jedis.expire(playerName, PlayerDataIntervalConfig.DATA_EXPIRY.getSeconds());
        }
    }

    public void expire(@NotNull String key) {
        expire(key, PlayerDataIntervalConfig.DATA_EXPIRY.getSeconds());
    }

    public void expire(@NotNull String key, long seconds) {
        if (pool == null) return;
        try (Jedis jedis = pool.getResource()) {
            jedis.expire(key, seconds);
        }
    }
}
