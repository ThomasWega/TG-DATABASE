package net.trustgames.toolkit.managers.rabbit.extras;

import org.jetbrains.annotations.Nullable;

public enum RabbitQueues {
    EVENT_PLAYER_DATA_UPDATE("event_player_data_update", "player_data_update", RabbitExchanges.EVENTS);


    public final String name;
    public final String routingKey;
    @Nullable public final RabbitExchanges[] exchanges;


    RabbitQueues(String name, String routingKey,
                 @Nullable RabbitExchanges... exchanges) {
        this.name = name;
        this.routingKey = routingKey;
        this.exchanges = exchanges;
    }
}
