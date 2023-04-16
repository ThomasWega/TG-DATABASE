package net.trustgames.middleware.cache;

import net.trustgames.middleware.Middleware;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisCache {

    @Nullable
    private final JedisPool pool;

    public RedisCache(Middleware middleware) {
        this.pool = middleware.getJedisPool();
    }

    /**
     * Removes all the data associated with the specified key
     *
     * @param string Name of the key
     */
    public void remove(String string) {
        if (pool == null) return;
        try (Jedis jedis = pool.getResource()) {
            jedis.del(string);
        }
    }
}
