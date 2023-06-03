package net.trustgames.toolkit.message_queue.config;

import com.rabbitmq.client.BuiltinExchangeType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Here all the exchanges are configured and then in RabbitManager they are automatically created
 * @see RabbitQueue
 */
public enum RabbitExchange {
    EVENTS("events", null, BuiltinExchangeType.TOPIC),
    EVENT_PLAYER_DATA_UPDATE("event.player-data-update", "player-data-update", BuiltinExchangeType.FANOUT, RabbitExchange.EVENTS);


    @Getter
    private final String name;
    @Getter
    private final @Nullable String routingKey;
    @Getter
    private final BuiltinExchangeType type;

    @Getter
    private final RabbitExchange[] boundExchanges;

    RabbitExchange(@NotNull String name,
                   @Nullable String routingKey,
                   @NotNull BuiltinExchangeType type,
                   @Nullable RabbitExchange... boundExchanges) {
        this.name = name;
        this.routingKey = routingKey;
        this.type = type;
        this.boundExchanges = boundExchanges;
    }
}
