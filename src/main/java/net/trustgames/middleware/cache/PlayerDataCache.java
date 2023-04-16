package net.trustgames.middleware.cache;

import net.trustgames.middleware.Middleware;
import net.trustgames.middleware.database.player.data.PlayerDataFetcher;
import net.trustgames.middleware.database.player.data.config.PlayerDataIntervalConfig;
import net.trustgames.middleware.database.player.data.config.PlayerDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Get or update player's data in the redis cache
 */
public final class PlayerDataCache {

    private final JedisPool pool;
    private final Middleware middleware;
    private final UUID uuid;
    private final PlayerDataType dataType;


    public PlayerDataCache(@NotNull Middleware middleware,
                           @NotNull UUID uuid,
                           @NotNull PlayerDataType dataType) {
        this.pool = middleware.getJedisPool();
        this.middleware = middleware;
        this.uuid = uuid;
        this.dataType = dataType;
    }

    /**
     * Update the specified data of player in the
     * redis cache with the given value
     *
     * @param value Value to update the data with
     */
    public void update(@NotNull String value) {
        if (pool == null) return;
        try (Jedis jedis = pool.getResource()) {
            String column = dataType.getColumnName();
            jedis.hset(uuid.toString(), column, value);
        }
    }

    /**
     * Gets the specified value of data from the cache.
     * The cache should always be up-to-date with the database.
     * <p></p>
     * If the cache is off (pool == null), the data will be taken
     * straight from the database
     *
     * @param callback Callback where the result is saved
     */
    public void get(Consumer<@Nullable String> callback) {
        String result = null;

        // cache
        if (pool != null) {
            try (Jedis jedis = pool.getResource()) {
                result = jedis.hget(uuid.toString(), dataType.getColumnName());
                jedis.expire(uuid.toString(), PlayerDataIntervalConfig.DATA_EXPIRY.getSeconds());
            }
        }

        // database
        if (result == null) {
            PlayerDataFetcher dataFetcher = new PlayerDataFetcher(middleware, dataType);
            dataFetcher.fetch(uuid, data -> {
                // if still null, there is no data on the player even in the database
                if (data != null)
                    update(data);
                callback.accept(data);
            });
            return;
        }
        callback.accept(result);
    }
}
