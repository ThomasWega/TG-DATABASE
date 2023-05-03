package net.trustgames.toolkit.config;

import lombok.Getter;

public enum ServerConfig {
    IP("play.trustgames.net"),
    WEBSITE("discord.trustgames.net"),
    STORE("store.trustgames.net"),
    DISCORD("discord.trustgames.net");

    @Getter
    private final String value;

    ServerConfig(String value) {
        this.value = value;
    }
}
