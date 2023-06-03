package net.trustgames.toolkit.skin;

import net.trustgames.toolkit.database.player.data.config.PlayerDataIntervalConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SkinCache {
    @Nullable private final JedisPool pool;

    public SkinCache(@Nullable JedisPool pool) {
        this.pool = pool;
    }

    /**
     * Get the Skin by the name
     *
     * @param playerName Name of the player
     * @return Skin with filled in texture and signature (both not null)
     */
    public Optional<Skin> getSkin(@NotNull String playerName) {
        if (pool == null) {
            return Optional.empty();
        }

        try (Jedis jedis = pool.getResource()) {
            List<String> fetchList = jedis.hmget(playerName, "skin_texture", "skin_signature");
            jedis.expire(playerName, PlayerDataIntervalConfig.DATA_EXPIRY.getSeconds());

            if (fetchList.contains(null)) return Optional.empty();
            return Optional.of(new Skin(fetchList.get(0), fetchList.get(1)));
        }
    }

    /**
     * Replace the specified skin signature and texture in the cache with the given value
     *
     * @param playerName Name of the skin holder
     * @param skin Skin with filled in texture and signature
     */
    public void updateSkin(@NotNull String playerName, @NotNull Skin skin) {
        if (pool == null) return;
        if (skin.texture() == null || skin.signature() == null) return;

        try (Jedis jedis = pool.getResource()) {
            jedis.hmset(playerName, Map.of(
                    "skin_texture", skin.texture(),
                    "skin_signature", skin.signature()
            ));
            jedis.expire(playerName, PlayerDataIntervalConfig.DATA_EXPIRY.getSeconds());
        }
    }
}
