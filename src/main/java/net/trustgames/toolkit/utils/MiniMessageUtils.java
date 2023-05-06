package net.trustgames.toolkit.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import org.jetbrains.annotations.NotNull;

public final class MiniMessageUtils {

    /**
     * MiniMessage instance, which replaces
     * {@literal the <component> tag in the message with the given Component}
     *
     * @param component Component to replace the tag with
     * @return new MiniMessage with formatter ready
     */
    public static MiniMessage component(@NotNull Component component) {
        return MiniMessage.builder()
                .tags(TagResolver.builder()
                        .resolver(StandardTags.defaults())
                        .resolver(Placeholder.component("component", component))
                        .build()
                )
                .build();
    }

    /**
     * MiniMessage instance, which replaces
     * various tags in the message with values of the player
     * Some tags work only for offline players or online players!
     *
     * @param playerName Name of the Player
     * @param prefix     Prefix of the player
     * @return new MiniMessage with formatter ready
     */
    public static MiniMessage withPrefix(@NotNull String playerName,
                                         @NotNull Component prefix) {
        if (!prefix.equals(Component.text(""))) {
            prefix = prefix.append(Component.text(" "));
        }

        return MiniMessage.builder()
                .tags(TagResolver.builder()
                        .resolver(StandardTags.defaults())
                        .resolver(Placeholder.unparsed("player_name", playerName))
                        .resolver(Placeholder.component("player_prefix", prefix))
                        .build()
                )
                .build();
    }

    /**
     * MiniMessage instance, which replaces
     * various currency tags in the message with
     * data values of the player
     *
     * @param playerName Name of the Player to replace the tags with info of
     * @return new MiniMessage with formatter ready
     */
    public static MiniMessage playerData(@NotNull String playerName,
                                         @NotNull PlayerDataType dataType,
                                         @NotNull String value) {
        return MiniMessage.builder()
                .tags(TagResolver.builder()
                        .resolver(StandardTags.defaults())
                        .resolver(Placeholder.unparsed("player_name", playerName))
                        .resolver(Placeholder.unparsed("player_data", dataType.getDisplayName()))
                        .resolver(Placeholder.unparsed("value", value))
                        .build()
                )
                .build();
    }
}
