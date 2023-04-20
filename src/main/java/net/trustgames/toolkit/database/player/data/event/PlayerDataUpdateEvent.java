package net.trustgames.toolkit.database.player.data.event;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Called when player's data in database updates
 */
public class PlayerDataUpdateEvent {
    private static final List<PlayerDataUpdateListener> listeners = new ArrayList<>();

    @Getter
    private final UUID uuid;

    public PlayerDataUpdateEvent(UUID uuid) {
        this.uuid = uuid;
    }

    public static void add(PlayerDataUpdateListener listener) {
        listeners.add(listener);
    }

    public static void remove(PlayerDataUpdateListener listener) {
        listeners.remove(listener);
    }

    public static void fire(PlayerDataUpdateEvent event) {
        for (PlayerDataUpdateListener listener : listeners) {
            listener.onPlayerDataUpdate(event);
        }
    }
}
