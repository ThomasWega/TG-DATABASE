package net.trustgames.toolkit;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.trustgames.toolkit.managers.database.HikariManager;
import net.trustgames.toolkit.managers.message_queue.RabbitManager;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.JedisPool;

public final class Toolkit {
    public static ComponentLogger LOGGER;
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

    public static void main(String[] args) {
        LOGGER = ComponentLogger.logger(Toolkit.class);
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
            Toolkit.LOGGER.info("Jedis activity connections: {}", jedisPool.getNumActive());
            jedisPool.destroy();
        }
    }
}