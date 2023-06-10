package net.trustgames.toolkit.event;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.function.Function;

/**
 * Configuration of specific event
 * @param <E> event type
 */
public interface EventConfig<E extends Event> {
    /**
     * @return Event converted to JSONObject
     */
    @NotNull Function<E, JSONObject> toJson();

    /**
     * @return JSONObject converted back to Event
     */
    @NotNull Function<JSONObject, E> fromJson();
}
