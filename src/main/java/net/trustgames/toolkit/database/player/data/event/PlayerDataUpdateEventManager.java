package net.trustgames.toolkit.database.player.data.event;

import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.managers.rabbit.RabbitManager;
import net.trustgames.toolkit.managers.rabbit.extras.queues.PlayerDataUpdateQueues;

import java.util.UUID;

import static net.trustgames.toolkit.database.player.data.event.PlayerDataUpdateEvent.registeredListeners;

public class PlayerDataUpdateEventManager {

    private final RabbitManager rabbitManager;

    public PlayerDataUpdateEventManager(RabbitManager rabbitManager) {
        this.rabbitManager = rabbitManager;
    }

    public void receiveEvents() {
        rabbitManager.onDelivery(PlayerDataUpdateQueues.BULK.name, jsonObject -> {
            System.out.println("TOOLKIT - RECEIVED EVENT!");
            PlayerDataUpdateEvent event = new PlayerDataUpdateEvent(
                    rabbitManager,
                    UUID.fromString(jsonObject.getString("uuid")),
                    jsonObject.getEnum(PlayerDataType.class, "data-type")
            );
            for (PlayerDataUpdateListener listener : registeredListeners){
                listener.onPlayerDataUpdate(event);
            }
        });
    }
}
