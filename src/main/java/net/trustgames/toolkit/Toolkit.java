package net.trustgames.toolkit;

import lombok.Getter;
import lombok.Setter;
import net.trustgames.toolkit.database.HikariManager;
import net.trustgames.toolkit.message_queue.RabbitManager;
import net.trustgames.toolkit.message_queue.event.RabbitEventManager;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.JedisPool;

import java.util.logging.Logger;

public final class Toolkit {
    public static final Logger LOGGER = Logger.getLogger("TG-Toolkit");
    @Getter
    @Setter
    private HikariManager hikariManager = null;
    @Getter
    @Setter
    private RabbitManager rabbitManager = null;
    @Getter
    @Setter
    @Nullable
    private JedisPool jedisPool = null;
    @Getter
    @Setter
    private RabbitEventManager rabbitEventManager = null;

    public static void main(String[] args) {
    }


    /**
     * Closes all connections that Toolkit instance uses
     */
    public void closeConnections() {
        if (hikariManager != null && hikariManager.isDataSourceInitialized())
            hikariManager.close();

        if (rabbitManager != null && rabbitManager.isChannelInitialized())
            rabbitManager.close();

        if (jedisPool != null) {
            LOGGER.info("Jedis active connections: " + jedisPool.getNumActive());
            jedisPool.destroy();
        }
    }
}