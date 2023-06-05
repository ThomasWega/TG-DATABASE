package net.trustgames.toolkit.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.json.JSONObject;

/**
 * Handles the conversion from JSON to other Types
 */
public class JSONUtils {

    private JSONUtils() {}

    /**
     * All data will be preserved (colors, events, ...)
     *
     * @param json Json to convert
     * @return Converted JSON to Component
     */
    public static Component toComponent(JSONObject json) {
        return GsonComponentSerializer.gson().deserialize(json.toString());
    }
}
