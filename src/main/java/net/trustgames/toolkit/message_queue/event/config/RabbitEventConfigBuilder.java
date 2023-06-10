package net.trustgames.toolkit.message_queue.event.config;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import net.trustgames.toolkit.message_queue.event.RabbitEvent;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.function.Function;

/**
 * Builder for {@link RabbitEventConfig}.
 * Provides a cleaner way to specify the values
 *
 * @param <E> Event type
 */
public class RabbitEventConfigBuilder<E extends RabbitEvent> {
    private String exchangeName;
    private BuiltinExchangeType exchangeType;
    private String exchangeRoutingKey;
    private AMQP.BasicProperties properties;
    private Function<E, JSONObject> toJson;
    private Function<JSONObject, E> fromJson;

    public RabbitEventConfigBuilder<E> exchangeName(@NotNull String exchangeName) {
        this.exchangeName = exchangeName;
        return this;
    }

    public RabbitEventConfigBuilder<E> exchangeType(@NotNull BuiltinExchangeType exchangeType) {
        this.exchangeType = exchangeType;
        return this;
    }

    public RabbitEventConfigBuilder<E> exchangeRoutingKey(@NotNull String exchangeRoutingKey) {
        this.exchangeRoutingKey = exchangeRoutingKey;
        return this;
    }

    public RabbitEventConfigBuilder<E> properties(@NotNull AMQP.BasicProperties properties) {
        this.properties = properties;
        return this;
    }

    public RabbitEventConfigBuilder<E>
    toJson(@NotNull Function<E, JSONObject> toJson) {
        this.toJson = toJson;
        return this;
    }

    public RabbitEventConfigBuilder<E> fromJson(@NotNull Function<JSONObject, E> fromJson) {
        this.fromJson = fromJson;
        return this;
    }

    public RabbitEventConfig<E> build() {
        return new RabbitEventConfig<>() {
            @Override
            public @NotNull String exchangeName() {
                return exchangeName;
            }

            @Override
            public @NotNull BuiltinExchangeType exchangeType() {
                return exchangeType;
            }

            @Override
            public @NotNull String exchangeRoutingKey() {
                return exchangeRoutingKey;
            }

            @Override
            public AMQP.@NotNull BasicProperties properties() {
                return properties;
            }

            @Override
            public @NotNull Function<E, JSONObject> toJson() {
                return toJson;
            }

            @Override
            public @NotNull Function<JSONObject, E> fromJson() {
                return fromJson;
            }
        };
    }
}
