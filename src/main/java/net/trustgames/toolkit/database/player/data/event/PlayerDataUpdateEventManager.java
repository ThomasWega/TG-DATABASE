package net.trustgames.toolkit.database.player.data.event;

import com.rabbitmq.client.Channel;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.managers.rabbit.RabbitManager;
import net.trustgames.toolkit.managers.rabbit.config.RabbitExchange;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerDataUpdateEventManager {
    protected static final Set<PlayerDataUpdateListener> registeredListeners = Collections.synchronizedSet(new HashSet<>());

    private final RabbitManager rabbitManager;

    /**
     * Handles the registration, un-registration of listeners
     * and receiving the events from RabbitMQ and sending them to listeners
     *
     * @param rabbitManager RabbitManager instance
     */
    public PlayerDataUpdateEventManager(RabbitManager rabbitManager) {
        this.rabbitManager = rabbitManager;
    }

    /**
     * Add the listener to the list. In case the receiving of events is on,
     * it will receive events from now on
     *
     * @see PlayerDataUpdateEventManager#receiveEvents(RabbitExchange)
     */
    public static void register(PlayerDataUpdateListener listener) {
        registeredListeners.add(listener);
    }

    /**
     * Remove the listener from the list. It will no longer receive events
     */
    public static void unregister(PlayerDataUpdateListener listener) {
        registeredListeners.remove(listener);
    }

    /**
     * Receive the messages from RabbitMQ and transfer them to PlayerDataUpdateEvent,
     * which are then sent to the listeners
     *
     * @see PlayerDataUpdateEvent
     */
    public void receiveEvents(RabbitExchange exchange) {
        Channel channel = rabbitManager.getChannel();
        String queue;
        try {
            queue = channel.queueDeclare("", false, true, false, null).getQueue();
            channel.queueBind(queue, exchange.getName(), exchange.getRoutingKey());
        } catch (IOException e) {
            System.out.println("RUNTIME EXCEPTION 21");
            throw new RuntimeException(e);
        }
        rabbitManager.onDelivery(queue, jsonObject -> {
            PlayerDataUpdateEvent event = new PlayerDataUpdateEvent(rabbitManager,
                    UUID.fromString(jsonObject.getString("uuid")),
                    jsonObject.getEnum(PlayerDataType.class, "data-type")
            );
            for (PlayerDataUpdateListener listener : registeredListeners){
                listener.onPlayerDataUpdate(event);
            }
        });
    }
}
