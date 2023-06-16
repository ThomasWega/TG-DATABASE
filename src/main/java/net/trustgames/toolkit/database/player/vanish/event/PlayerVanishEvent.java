package net.trustgames.toolkit.database.player.vanish.event;

import net.trustgames.toolkit.message_queue.event.RabbitEvent;

import java.util.UUID;

public record PlayerVanishEvent(UUID uuid,
                                PlayerVanishEvent.Action action) implements RabbitEvent {
    public enum Action {
        ON, OFF
    }
}
