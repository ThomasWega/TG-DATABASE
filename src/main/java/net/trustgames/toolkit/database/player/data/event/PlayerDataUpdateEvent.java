package net.trustgames.toolkit.database.player.data.event;

import com.rabbitmq.client.AMQP;
import lombok.Getter;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.managers.rabbit.RabbitManager;
import net.trustgames.toolkit.managers.rabbit.extras.queues.PlayerDataUpdateQueues;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Called when player's data in database updates
 */
public class PlayerDataUpdateEvent {

    @Getter
    private final UUID uuid;
    @Getter
    private final PlayerDataType dataType;
    private final RabbitManager rabbitManager;

    public PlayerDataUpdateEvent(@NotNull RabbitManager rabbitManager,
                                 @NotNull UUID uuid,
                                 @NotNull PlayerDataType dataType) {
        this.rabbitManager = rabbitManager;
        this.uuid = uuid;
        this.dataType = dataType;
    }

    public void fire() {
        fire(this.rabbitManager, this);
    }

    public static void fire(RabbitManager rabbitManager, PlayerDataUpdateEvent event) {
        rabbitManager.fireAndForget(
                PlayerDataUpdateQueues.BULK,
                new AMQP.BasicProperties().builder()
                        .expiration("5000")
                        .build(),
                new JSONObject()
                        .put("uuid", event.getUuid())
                        .put("data-type", event.getDataType())
        );
    }
}
