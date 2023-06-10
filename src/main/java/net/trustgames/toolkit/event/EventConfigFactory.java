package net.trustgames.toolkit.event;

import org.jetbrains.annotations.NotNull;

/**
 * This can be used to provide a cleaner way to create an {@link EventConfig},
 * as a builder can be used
 *
 * @param <E> Event type
 */
public interface EventConfigFactory<E extends Event> {
    @NotNull EventConfig<E> config();
}
