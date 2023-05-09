package net.trustgames.toolkit.database.player.data.config;

public enum PlayerDataIntervalConfig {
    UPDATE(5000),
    DATA_EXPIRY(300000);

    // values are in milliseconds

    private final long value;

    PlayerDataIntervalConfig(long value) {
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
