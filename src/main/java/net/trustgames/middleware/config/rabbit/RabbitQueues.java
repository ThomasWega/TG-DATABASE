package net.trustgames.middleware.config.rabbit;

public enum RabbitQueues {
    PROXY_PLAYER_MESSAGES("proxy_player_messages"),
    PLAYER_DATA_UPDATE_EVENT("player_data_update_event");

    public final String name;

    RabbitQueues(String name) {
        this.name = name;
    }
}
