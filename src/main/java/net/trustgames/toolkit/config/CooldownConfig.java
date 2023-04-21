package net.trustgames.toolkit.config;

public enum CooldownConfig {
    PREFIX("<color:#2472f0>Cooldown | </color>"),
    SPAM(PREFIX.value + "<dark_gray>Please don't spam the activity!");

    public final String value;

    CooldownConfig(String value) {
        this.value = value;
    }
}
