package net.trustgames.toolkit.config.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.trustgames.toolkit.config.PermissionConfig;
import net.trustgames.toolkit.utils.MiniMessageUtils;
import org.jetbrains.annotations.NotNull;

public enum ChatConfig {
    PREFIX("<color:#00adc4>Chat | </color>"),
    COLOR("&f"),
    NAME_COLOR("&e"),
    ALLOW_COLORS_PERM(PermissionConfig.KNIGHT.permission),
    MENTION_COLOR("&a"),
    ON_COOLDOWN(PREFIX.value + "<dark_gray>Wait another <component> seconds before using chat again!"),
    ON_SAME_COOLDOWN(PREFIX.value + "<dark_gray>Don't write the same message twice! (wait <component> seconds)"),
    MENTION_ACTIONBAR("<gray><player_name> mentioned you");

    public final String value;

    ChatConfig(String value) {
        this.value = value;
    }

    /**
     * @return Formatted component message
     */
    public final Component getText() {
        return MiniMessage.miniMessage().deserialize(value);
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
        return MiniMessageUtils.withPrefix(playerName, prefix).deserialize(value);
    }

    /**
     * {@literal Replace <component> tag with given Component}
     *
     * @param component Component to replace the tag with
     * @return New formatted Component with replaced id tag
     */
    public final Component addComponent(Component component) {
        return MiniMessageUtils.component(component).deserialize(value);
    }
}
