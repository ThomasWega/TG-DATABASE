package net.trustgames.middleware.managers.rabbit.extras;

import com.rabbitmq.client.BuiltinExchangeType;

public enum RabbitExchanges {
    EVENTS("events", BuiltinExchangeType.DIRECT);


    public final String name;
    public final BuiltinExchangeType type;

    RabbitExchanges(String name, BuiltinExchangeType type) {
        this.name = name;
        this.type = type;
    }
}
