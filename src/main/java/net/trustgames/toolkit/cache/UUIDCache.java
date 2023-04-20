package net.trustgames.toolkit.cache;

import net.trustgames.toolkit.Middleware;
import net.trustgames.toolkit.database.player.data.config.PlayerDataIntervalConfig;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.database.player.data.uuid.PlayerUUIDFetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Get or update player's uuid in the redis cache
 */
public final class UUIDCache {

    private static final String field = PlayerDataType.UUID.getColumnName();
    private final JedisPool pool;
    private final Middleware middleware;
    private final String playerName;

    public UUIDCache(@NotNull Middleware middleware,
                     @NotNull String playerName) {
        this.pool = middleware.getJedisPool();
        this.middleware = middleware;
        this.playerName = playerName;
    }

    /**
     * Get the UUID of the player from the cache.
     * If it's not in the cache or the cache is disabled,
     * it gets it from the database and updates in cache (if on)
     * In case it is still null, that means the player never joined the network
     *
     * @param callback Where the UUID of the player, or null will be saved
     */
    public void get(Consumer<@Nullable UUID> callback) {
        String uuidString = null;

        // cache
        if (pool != null) {
            try (Jedis jedis = pool.getResource()) {
                uuidString = jedis.hget(playerName, field);
                jedis.expire(playerName, PlayerDataIntervalConfig.DATA_EXPIRY.getSeconds());
            }
        }

        // database
        if (uuidString == null) {
            PlayerUUIDFetcher uuidFetcher = new PlayerUUIDFetcher(middleware);
            uuidFetcher.fetch(playerName, uuid -> {
                // if still null, there is no data on the player even in the database
                if (uuid != null)
                    update(uuid);

                callback.accept(uuid);
            });
            return;
        }
        callback.accept(UUID.fromString(uuidString));
    }

    /**
     * Updates the player's UUID in the cache.
     *
     * @param uuid UUID of the player.
     */
    public void update(@NotNull UUID uuid) {
        if (pool == null) return;
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(playerName, field, uuid.toString());
        }
    }
}
