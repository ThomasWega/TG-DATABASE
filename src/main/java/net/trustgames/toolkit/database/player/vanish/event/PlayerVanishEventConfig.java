package net.trustgames.toolkit.database.player.vanish.event;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import net.trustgames.toolkit.message_queue.event.config.RabbitEventConfig;
import net.trustgames.toolkit.message_queue.event.config.RabbitEventConfigBuilder;
import net.trustgames.toolkit.message_queue.event.config.RabbitEventConfigFactory;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.UUID;

public class PlayerVanishEventConfig implements RabbitEventConfigFactory<PlayerVanishEvent> {

    @Override
    public @NotNull RabbitEventConfig<PlayerVanishEvent> config() {
        return new RabbitEventConfigBuilder<PlayerVanishEvent>()
                .exchangeName("event.player-vanish")
                .exchangeRoutingKey("event.player-vanish.#")
                .exchangeType(BuiltinExchangeType.FANOUT)
                .properties(new AMQP.BasicProperties().builder().expiration("5000").build())
                .toJson(event -> new JSONObject()
                        .put("uuid", event.uuid())
                        .put("action", event.action())
                )
                .fromJson(json -> new PlayerVanishEvent(
                        UUID.fromString(json.getString("uuid")),
                        json.getEnum(PlayerVanishEvent.Action.class, "action"))
                )
                .build();
    }
}
