package net.trustgames.toolkit;

import lombok.Getter;
import lombok.Setter;
import net.trustgames.toolkit.database.player.data.event.PlayerDataUpdateEventManager;
import net.trustgames.toolkit.managers.HikariManager;
import net.trustgames.toolkit.managers.rabbit.RabbitManager;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.JedisPool;

import java.util.logging.Logger;

public final class Toolkit {

    @Getter
    private static final Logger logger = Logger.getLogger("Toolkit");
    @Getter
    @Setter
    private static HikariManager hikariManager = null;
    @Getter
    @Setter
    private static RabbitManager rabbitManager = null;
    @Getter
    @Setter
    @Nullable
    private static JedisPool jedisPool = null;

    public static void main(String[] args) {
        // TODO when modifying level the progress resets

        new PlayerDataUpdateEventManager(rabbitManager);
    }

    /**
     * Closes all connections that Toolkit instance uses
     */
    public void closeConnections() {
        if (hikariManager.isDataSourceInitialized())
            hikariManager.close();

        if (rabbitManager.isChannelInitialized())
            rabbitManager.close();

        if (jedisPool != null)
            jedisPool.close();
    }
}