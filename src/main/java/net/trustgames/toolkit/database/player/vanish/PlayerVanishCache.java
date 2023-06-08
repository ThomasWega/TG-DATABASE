package net.trustgames.toolkit.database.player.vanish;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import static net.trustgames.toolkit.cache.RedisCache.expire;

public class PlayerVanishCache {

    private static final String field = "vanish";
    private static final String status = "status";
    private static final String time = "time";
    @Nullable
    private final JedisPool pool;

    public PlayerVanishCache(@Nullable JedisPool pool) {
        this.pool = pool;
    }

    public Optional<Boolean> checkVanish(@NotNull UUID uuid) {
        if (pool == null) {
            return Optional.empty();
        }

        try (Jedis jedis = pool.getResource()) {
            String stringValue = jedis.hget(uuid.toString(), field + ":" + status);
            if (stringValue == null) {
                return Optional.empty();
            }

            System.out.println("CACHE IS VANISHED: " + stringValue);
            boolean isVanished = Boolean.parseBoolean(stringValue);
            expire(pool, uuid.toString());
            return Optional.of(isVanished);
        }
    }

    public Optional<Timestamp> checkVanishTime(@NotNull UUID uuid) {
        if (pool == null) {
            return Optional.empty();
        }

        try (Jedis jedis = pool.getResource()) {
            String stringValue = jedis.hget(uuid.toString(), field + ":" + time);
            if (stringValue == null) {
                return Optional.empty();
            }

            Timestamp time = Timestamp.valueOf(stringValue);
            expire(pool, uuid.toString());
            return Optional.of(time);
        }
    }

    public void setVanish(@NotNull UUID uuid,
                          boolean isVanished) {
        if (pool == null) return;
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(uuid.toString(), field + ":" + status, String.valueOf(isVanished));
            expire(pool, uuid.toString());
        }
    }

    public void setVanishTime(@NotNull UUID uuid,
                              @NotNull Timestamp timestamp) {
        if (pool == null) return;
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(uuid.toString(), field + ":" + time, timestamp.toString());
            expire(pool, uuid.toString());
        }
    }

    public void removeVanishTime(@NotNull UUID uuid) {
        if (pool == null) return;
        try (Jedis jedis = pool.getResource()) {
            jedis.hdel(uuid.toString(), field + ":" + time);
        }
    }
}
