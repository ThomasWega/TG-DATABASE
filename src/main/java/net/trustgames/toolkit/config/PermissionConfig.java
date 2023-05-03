package net.trustgames.toolkit.config;

import lombok.Getter;

public enum PermissionConfig {
    ADMIN("tg.admin"),
    STAFF("tg.staff"),
    TITAN("tg.titan"),
    LORD("tg.lord"),
    KNIGHT("tg.knight"),
    PRIME("tg.prime"),
    DEFAULT("tg.default");

    @Getter
    private final String permission;

    PermissionConfig(String permission) {
        this.permission = permission;
    }
}
