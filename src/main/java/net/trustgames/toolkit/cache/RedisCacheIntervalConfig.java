package net.trustgames.toolkit.cache;

// values are in milliseconds
public enum RedisCacheIntervalConfig {
    EXPIRY(300000);

    private final long value;

    RedisCacheIntervalConfig(long value) {
        this.value = value;
    }

    /**
     * @return Converted milliseconds to ticks
     */
    public final long getTicks() {
        return value / 50;
    }

    /**
     * @return Converted milliseconds to seconds
     */
    public final long getSeconds() {
        return value / 1000;
    }
}
