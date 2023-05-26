package net.trustgames.toolkit.utils;

import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the conversion from Components to other Types
 */
public final class ComponentUtils {

    /**
     * Converts the Component to String.
     * Will preserve only color codes
     *
     * @param component Component to convert
     * @return String from Component with only unformatted color codes
     */
    public static String toString(@NotNull Component component) {
        return LegacyComponentSerializer.legacyAmpersand().serialize(component);
    }

    /**
     * Convert component to JSON
     * Will preserve everything (colors, events, ...)
     *
     * @param component Component to convert
     * @return JSONElement from Component
     */
    public static JsonElement toJson(@NotNull Component component) {
        return GsonComponentSerializer.gson().serializeToTree(component);
    }

    /**
     * Convert component to String of JSON
     * Will preserve everything (colors, events, ...)
     *
     * @param component Component to convert
     * @return String of JSON from Component
     */
    public static String toJsonString(@NotNull Component component) {
        return GsonComponentSerializer.gson().serialize(component);
    }
}
