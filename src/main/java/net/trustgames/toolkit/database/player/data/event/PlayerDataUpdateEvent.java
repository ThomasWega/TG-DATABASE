package net.trustgames.toolkit.database.player.data.event;

import com.rabbitmq.client.AMQP;
import lombok.Getter;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.managers.rabbit.RabbitManager;
import net.trustgames.toolkit.managers.rabbit.config.RabbitQueues;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.UUID;

public class PlayerDataUpdateEvent {

    @Getter
    private final UUID uuid;
    @Getter
    private final PlayerDataType dataType;
    @Getter
    private final RabbitManager rabbitManager;

    /**
     * Called when player's data in database updates
     *
     * @param rabbitManager RabbitManager instance
     * @param uuid UUID of the player whose data was updated
     * @param dataType The type of data that was updates
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
     * @param event Event with filled in data
     */
    public static void publish(RabbitManager rabbitManager, PlayerDataUpdateEvent event) {
        rabbitManager.fireAndForget(
                RabbitQueues.PLAYER_DATA_UPDATE,
                new AMQP.BasicProperties().builder()
                        .expiration("5000")
                        .build(),
                new JSONObject()
                        .put("uuid", event.getUuid())
                        .put("data-type", event.getDataType())
        );
    }
}
