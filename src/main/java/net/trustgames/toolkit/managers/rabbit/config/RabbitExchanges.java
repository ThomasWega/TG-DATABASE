package net.trustgames.toolkit.managers.rabbit.config;

import com.rabbitmq.client.BuiltinExchangeType;
import lombok.Getter;

/**
 * Here all the exchanges are configured and then in RabbitManager they are automatically created
 * @see RabbitQueues
 */
public enum RabbitExchanges {
    EVENTS("events", BuiltinExchangeType.TOPIC);

    @Getter
    private final String name;
    @Getter
    private final BuiltinExchangeType type;

    RabbitExchanges(String name, BuiltinExchangeType type) {
        this.name = name;
        this.type = type;
    }
}
