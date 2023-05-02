package net.trustgames.toolkit.cache;

import net.trustgames.toolkit.Toolkit;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisCache {

    @Nullable
    private final JedisPool pool;

    public RedisCache(Toolkit toolkit) {
        this.pool = toolkit.getJedisPool();
    }

    /**
     * Removes all the data associated with the specified key
     *
     * @param key Name of the key
     */
    public void removeKey(String key) {
        if (pool == null) return;
        try (Jedis jedis = pool.getResource()) {
            jedis.del(key);
        }
    }
}
