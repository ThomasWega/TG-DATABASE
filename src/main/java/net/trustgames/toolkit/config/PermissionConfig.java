package net.trustgames.toolkit.config;

public enum PermissionConfig {
    ADMIN("tg.admin"),
    STAFF("tg.staff"),
    TITAN("tg.titan"),
    LORD("tg.lord"),
    KNIGHT("tg.knight"),
    PRIME("tg.prime"),
    DEFAULT("tg.default");

    public final String permission;

    PermissionConfig(String permission) {
        this.permission = permission;
    }
}
