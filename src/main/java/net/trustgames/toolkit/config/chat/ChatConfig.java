package net.trustgames.toolkit.config.chat;

import io.github.miniplaceholders.api.MiniPlaceholders;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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
    ON_SWEAR(PREFIX.value + "<dark_gray>Please keep the chat friendly :)"),
    ON_ADVERTISEMENT(PREFIX.value + "<dark_gray>Please don't advertise any website outside of TrustGames.net domain"),
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
     * Replaces the placeholder tags
     *
     * @param audience Audience (Player) that sent the message
     * @return New formatted Component message with replaced tags
     */
    public final Component formatMessage(@NotNull Audience audience) {
        return MiniMessage.miniMessage().deserialize(
                value,
                MiniPlaceholders.getAudiencePlaceholders(audience)
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
