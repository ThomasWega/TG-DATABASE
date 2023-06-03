package net.trustgames.toolkit.database.player.data.event;

import com.rabbitmq.client.AMQP;
import lombok.Getter;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.message_queue.RabbitManager;
import net.trustgames.toolkit.message_queue.config.RabbitExchange;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.UUID;

public record PlayerDataUpdateEvent(@Getter RabbitManager rabbitManager, @Getter UUID uuid,
                                    @Getter PlayerDataType dataType) {

    /**
     * Called when player's data in database updates
     *
     * @param rabbitManager RabbitManager instance
     * @param uuid          UUID of the player whose data was updated
     * @param dataType      The type of data that was updates
     */
    public PlayerDataUpdateEvent(@NotNull RabbitManager rabbitManager,
                                 @NotNull UUID uuid,
                                 @NotNull PlayerDataType dataType) {
        this.rabbitManager = rabbitManager;
        this.uuid = uuid;
        this.dataType = dataType;
    }

    /**
     * Publish the event to RabbitMQ
     *
     * @see PlayerDataUpdateEvent#publish(RabbitManager, PlayerDataUpdateEvent)
     */
    public void publish() {
        publish(this.rabbitManager, this);
    }

    /**
     * Publish the event to RabbitMQ
     *
     * @param rabbitManager RabbitManager instance
     * @param event         Event with filled in data
     */
    public static void publish(RabbitManager rabbitManager, PlayerDataUpdateEvent event) {
        rabbitManager.fireAndForget(
                RabbitExchange.EVENT_PLAYER_DATA_UPDATE,
                new AMQP.BasicProperties().builder()
                        .expiration("5000")
                        .build(),
                new JSONObject()
                        .put("uuid", event.uuid())
                        .put("data-type", event.dataType())
        );
    }
}
