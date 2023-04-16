package net.trustgames.middleware;

import lombok.Getter;
import lombok.Setter;
import net.trustgames.middleware.managers.HikariManager;
import net.trustgames.middleware.managers.RabbitManager;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.JedisPool;

import java.util.logging.Logger;

public final class Middleware {

    @Getter
    @Setter
    @Nullable
    private HikariManager hikariManager = null;
    @Getter
    @Setter
    @Nullable
    private RabbitManager rabbitManager = null;

    @Getter
    @Setter
    @Nullable
    private JedisPool jedisPool = null;

    @Getter
    private static final Logger logger = Logger.getLogger("Middleware");


    public static void main(String[] args) {
        // TODO add thread pooling
        // TODO when modifying level the progress resets
    }
}