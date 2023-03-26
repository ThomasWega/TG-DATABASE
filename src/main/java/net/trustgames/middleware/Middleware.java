package net.trustgames.middleware;

import lombok.Getter;
import lombok.Setter;
import net.trustgames.middleware.managers.HikariManager;
import net.trustgames.middleware.managers.RabbitManager;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.JedisPool;

import java.io.InputStream;
import java.util.Properties;
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

    public Middleware() {
        logger.info("Enabling TG-Middleware v" + getVersion());
    }

    public static void main(String[] args) {
    }

    /**
     * @return The version from pom.xml
     */
    public static String getVersion() {
        String version = null;

        // try to load from maven properties first
        try {
            Properties p = new Properties();
            InputStream is = Middleware.class.getResourceAsStream("/META-INF/maven/net.trustgames/middleware/pom.properties");
            if (is != null) {
                p.load(is);
                version = p.getProperty("version", "");
            }
        } catch (Exception e) {
            // ignore
        }

        // fallback to using Java API
        if (version == null) {
            Package aPackage = Middleware.class.getPackage();
            if (aPackage != null) {
                version = aPackage.getImplementationVersion();
                if (version == null) {
                    version = aPackage.getSpecificationVersion();
                }
            }
        }

        if (version == null) {
            // we could not compute the version so use a blank
            version = "UNKNOWN";
        }

        return version;
    }
}