package net.trustgames.toolkit.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisCache {

    private RedisCache() {
    }

    /**
     * Expire the specified key in the cache with the duration configured in config
     *
     * @param pool Instance of JedisPool
     * @param key  Key to expire
     * @see RedisCache#expire(JedisPool, String, long)
     */
    public static void expire(@Nullable JedisPool pool,
                              @NotNull String key) {
        expire(pool, key, RedisCacheIntervalConfig.EXPIRY.getSeconds());
    }

    /**
     * Expire the specified key in the cache with the given duration
     *
     * @param pool Instance of JedisPool
     * @param key  Key to expire
     * @see RedisCache#expire(JedisPool, String)
     */
    public static void expire(@Nullable JedisPool pool,
                              @NotNull String key, long seconds) {
        if (pool == null) return;
        try (Jedis jedis = pool.getResource()) {
            jedis.expire(key, seconds);
        }
    }
}
