package net.trustgames.toolkit;

import lombok.Getter;
import lombok.Setter;
import net.trustgames.toolkit.managers.HikariManager;
import net.trustgames.toolkit.managers.rabbit.RabbitManager;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.JedisPool;

import java.util.logging.Logger;

public final class Middleware {

    @Getter
    private static final Logger logger = Logger.getLogger("Middleware");
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

    public static void main(String[] args) {
        // TODO when modifying level the progress resets
        // TODO use optional for fetching data
    }
}