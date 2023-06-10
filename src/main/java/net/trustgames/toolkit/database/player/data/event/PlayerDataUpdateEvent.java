package net.trustgames.toolkit.database.player.data.event;

import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.message_queue.event.RabbitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record PlayerDataUpdateEvent(@NotNull UUID uuid,
                                    @NotNull PlayerDataType dataType)
        implements RabbitEvent {
}
