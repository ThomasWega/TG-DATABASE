package net.trustgames.toolkit.database.player.data.event;

import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.managers.rabbit.RabbitManager;
import net.trustgames.toolkit.managers.rabbit.extras.queues.PlayerDataUpdateQueues;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerDataUpdateEventManager {
    protected static final List<PlayerDataUpdateListener> registeredListeners = new ArrayList<>();

    private final RabbitManager rabbitManager;

    public PlayerDataUpdateEventManager(RabbitManager rabbitManager) {
        this.rabbitManager = rabbitManager;
    }

    public static void register(PlayerDataUpdateListener listener) {
        registeredListeners.add(listener);
    }

    public static void unregister(PlayerDataUpdateListener listener) {
        registeredListeners.remove(listener);
    }

    public void receiveEvents() {
        rabbitManager.onDelivery(PlayerDataUpdateQueues.BULK.name, jsonObject -> {
            System.out.println("TOOLKIT - RECEIVED EVENT!");
            PlayerDataUpdateEvent event = new PlayerDataUpdateEvent(
                    rabbitManager,
                    UUID.fromString(jsonObject.getString("uuid")),
                   PlayerDataType.XP
                   // jsonObject.getEnum(PlayerDataType.class, "data-type")
            );
            for (PlayerDataUpdateListener listener : registeredListeners){
                listener.onPlayerDataUpdate(event);
            }
        });
    }
}
