package net.trustgames.middleware.managers.rabbit.extras;

import org.jetbrains.annotations.Nullable;

public enum RabbitQueues {
    EVENT_PLAYER_DATA("event_player_data", "player_data", RabbitExchanges.EVENTS);


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
