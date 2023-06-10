package net.trustgames.toolkit.message_queue.event.config;

import net.trustgames.toolkit.message_queue.event.RabbitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * If builder is used for config declaration, {@link RabbitEventConfigFactory}
 * needs to be implemented rather then {@link RabbitEventConfig}
 * @param <E> Event type
 */
public interface RabbitEventConfigFactory<E extends RabbitEvent> {
    @NotNull RabbitEventConfig<E> config();
}

