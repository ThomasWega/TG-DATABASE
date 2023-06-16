package net.trustgames.toolkit.database.player.data.event;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.message_queue.event.config.RabbitEventConfig;
import net.trustgames.toolkit.message_queue.event.config.RabbitEventConfigBuilder;
import net.trustgames.toolkit.message_queue.event.config.RabbitEventConfigFactory;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.UUID;

public class PlayerDataUpdateEventConfig implements RabbitEventConfigFactory<PlayerDataUpdateEvent> {

    @Override
    public @NotNull RabbitEventConfig<PlayerDataUpdateEvent> config() {
        return new RabbitEventConfigBuilder<PlayerDataUpdateEvent>()
                .exchangeName("event.player-data-update")
                .exchangeType(BuiltinExchangeType.FANOUT)
                .exchangeRoutingKey("player-data-update.#")
                .properties(new AMQP.BasicProperties().builder().expiration("10000").build())
                .toJson(event -> new JSONObject()
                        .put("uuid", event.uuid())
                        .put("data-type", event.dataType())
                )
                .fromJson(jsonObject -> new PlayerDataUpdateEvent(
                        UUID.fromString(jsonObject.getString("uuid")),
                        jsonObject.getEnum(PlayerDataType.class, "data-type"))
                )
                .build();
    }
}
