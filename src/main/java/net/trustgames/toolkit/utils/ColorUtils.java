package net.trustgames.toolkit.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the conversion of colors
 */
public final class ColorUtils {

    private ColorUtils() {}

    /**
     * Translates the colors of the given Component text
     * and returns the colored text. Supports both normal colors
     * and HEX colors.
     *
     * @param component Component text to translate colors on
     * @return Component text with translated colors
     */
    public static Component color(@NotNull Component component) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(
                LegacyComponentSerializer.legacyAmpersand().serialize(component));
    }

    /**
     * Translates the colors of the given String text
     * and returns the colored text. Supports both normal colors
     * and HEX colors.
     *
     * @param string String text to translate colors on
     * @return Component text with translated colors
     */
    public static Component color(@NotNull String string) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(string);
    }

    /**
     * Removes the color from the given Component and returns String
     * with unformatted colors
     *
     * @param text Component to remove color from
     * @return String with unformatted colors
     */
    public static String stripColor(@NotNull Component text) {
        return PlainTextComponentSerializer.plainText().serialize(
                LegacyComponentSerializer.legacyAmpersand().deserialize(
                        PlainTextComponentSerializer.plainText().serialize(text)));
    }
}
