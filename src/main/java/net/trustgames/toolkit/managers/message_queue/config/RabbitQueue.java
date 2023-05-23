package net.trustgames.toolkit.managers.message_queue.config;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * Here all the queues are configured and then in RabbitManager they are automatically created
 * @see RabbitExchange
 */
public enum RabbitQueue {
    ;

    @Getter
    private final String name;
    @Getter
    private final String routingKey;
    @Getter
    @Nullable public final RabbitExchange[] exchanges;

    RabbitQueue(String name, String routingKey, @Nullable RabbitExchange... exchanges) {
        this.name = name;
        this.routingKey = routingKey;
        this.exchanges = exchanges;
    }
}
