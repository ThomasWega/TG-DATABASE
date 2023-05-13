package net.trustgames.toolkit.config.cooldown;

import lombok.Getter;

public enum CooldownValueConfig {
    SMALL(1d),
    MEDIUM(5d),
    LARGE(15d),
    WARN_MESSAGES_LIMIT_SEC(0.5d);

    @Getter
    private final double value;

    CooldownValueConfig(double value) {
        this.value = value;
    }
}
