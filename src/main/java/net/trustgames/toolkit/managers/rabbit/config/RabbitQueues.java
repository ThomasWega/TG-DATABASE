package net.trustgames.toolkit.managers.rabbit.config;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * Here all the queues are configured and then in RabbitManager they are automatically created
 * @see RabbitExchanges
 */
public enum RabbitQueues {
    PLAYER_DATA_UPDATE("event.player-data-update", "player-data-update.#", RabbitExchanges.EVENTS);

    @Getter
    private final String name;
    @Getter
    private final String routingKey;
    @Getter
    @Nullable public final RabbitExchanges[] exchanges;

    RabbitQueues(String name, String routingKey, @Nullable RabbitExchanges... exchanges) {
        this.name = name;
        this.routingKey = routingKey;
        this.exchanges = exchanges;
    }
}
