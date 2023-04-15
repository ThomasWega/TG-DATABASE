package net.trustgames.middleware.config;

public enum ServerConfig {
    IP("play.trustgames.net"),
    WEBSITE("discord.trustgames.net"),
    STORE("store.trustgames.net"),
    DISCORD("discord.trustgames.net");

    public final String value;

    ServerConfig(String value) {
        this.value = value;
    }
}
