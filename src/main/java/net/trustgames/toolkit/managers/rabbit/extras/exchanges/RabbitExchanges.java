package net.trustgames.toolkit.managers.rabbit.extras.exchanges;

import com.rabbitmq.client.BuiltinExchangeType;

public enum RabbitExchanges {
    EVENTS("events", BuiltinExchangeType.TOPIC);


    public final String name;
    public final BuiltinExchangeType type;

    RabbitExchanges(String name, BuiltinExchangeType type) {
        this.name = name;
        this.type = type;
    }
}
