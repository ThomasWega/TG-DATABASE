package net.trustgames.toolkit.config.chat;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.trustgames.toolkit.config.PermissionConfig;
import org.jetbrains.annotations.NotNull;

public enum ChatConfig {
    PREFIX("<color:#00adc4>Chat | </color>"),
    CHAT_COLOR("<white>"),
    NAME_COLOR("<yellow>"),
    ALLOW_COLORS_PERM(PermissionConfig.KNIGHT.getPermission()),
    MENTION_COLOR("<green>"),
    ON_COOLDOWN(PREFIX.value + "<dark_gray>Wait another <component> seconds before using chat again!"),
    ON_SAME_COOLDOWN(PREFIX.value + "<dark_gray>Don't write the same message twice! (wait <component> seconds)"),
    MENTION_ACTIONBAR("<gray><player_name> mentioned you");

    @Getter
    private final String value;

    ChatConfig(String value) {
        this.value = value;
    }

    /**
     * @return Formatted component message
     */
    public final Component getFormatted() {
        return MiniMessage.miniMessage().deserialize(value);
    }

    /**
     * @return The color the Component from String Enum value will have after formatting
     */
    public final TextColor getColor() {
        return getFormatted().color();
    }

    /**
     * {@literal Replaces tags with <player_name> and <player_prefix>}
     *
     * @param playerName Name of the player
     * @param prefix LuckPerms prefix the player has
     * @return New formatted Component message with replaced tags
     */
    public final Component formatMessage(@NotNull String playerName,
                                         @NotNull Component prefix) {
        return MiniMessage.miniMessage().deserialize(
                value,
                TagResolver.builder()
                        .resolver(Placeholder.unparsed("player_name", playerName))
                        .resolver(Placeholder.component("prefix", prefix))
                        .build()
        );
    }

    /**
     * {@literal Replace <component> tag with given Component}
     *
     * @param component Component to replace the tag with
     * @return New formatted Component with replaced id tag
     */
    public final Component addComponent(Component component) {
        return MiniMessage.miniMessage().deserialize(
                value,
                Placeholder.component("component", component)
        );
    }
}
