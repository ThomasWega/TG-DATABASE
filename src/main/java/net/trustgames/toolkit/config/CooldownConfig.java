package net.trustgames.toolkit.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public enum CooldownConfig {
    PREFIX("<color:#2472f0>Cooldown | </color>"),
    SPAM(PREFIX.value + "<dark_gray>Please don't spam the activity!");

    private final String value;

    CooldownConfig(String value) {
        this.value = value;
    }

    /**
     * @return Formatted component message
     */
    public final Component getFormatted() {
        return MiniMessage.miniMessage().deserialize(value);
    }
}
