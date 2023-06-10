package net.trustgames.toolkit.message_queue.event.config;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import net.trustgames.toolkit.event.EventConfig;
import net.trustgames.toolkit.message_queue.event.RabbitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Config for {@link RabbitEvent}.
 * All the values need to be specified in order
 * to create a valid exchange and consumer queues in RabbitMQ
 *
 * @param <E> Event type
 */
public interface RabbitEventConfig<E extends RabbitEvent> extends EventConfig<E> {
    @NotNull String exchangeName();
    @NotNull BuiltinExchangeType exchangeType();
    @NotNull String exchangeRoutingKey();
    @NotNull AMQP.BasicProperties properties();
}
