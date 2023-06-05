package net.trustgames.toolkit.config;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public enum CommandConfig {

    PREFIX("<color:#2472f0>Command | </color>"),
    PREFIX_DB("<color:#ed7168>Internal | </color>"),
    PREFIX_MQ("<color:#edc168>Internal | </color>"),
    COMMAND_NO_PERM(PREFIX.value + "<dark_gray>You don't have permission to perform this action!"),
    COMMAND_DATABASE_OFF(PREFIX_DB.value + "<dark_gray>Database is disabled!"),
    COMMAND_MESSAGE_QUEUE_OFF(PREFIX_MQ.value + "<dark_gray>Message Queue is disabled!"),
    COMMAND_PLAYER_ONLY("This command can be executed by in-game players only!"),
    COMMAND_SPAM(PREFIX.value + "<dark_gray>Please don't spam the command!"),
    COMMAND_INVALID_ARG(PREFIX.value + "<dark_gray>You need to specify a valid argument!"),
    COMMAND_INVALID_ID(PREFIX.value + "<dark_gray>Invalid ID <component><id>"),
    COMMAND_INVALID_VALUE(PREFIX.value + "<red>Invalid value <white><component><red>!"),
    COMMAND_PLAYER_OFFLINE(PREFIX.value + "<dark_gray>The player <white><component><dark_gray> isn't online on this server!"),
    COMMAND_PLAYER_UNKNOWN(PREFIX.value + "<dark_gray>The player <white><component><dark_gray> never joined the network!"),
    COMMAND_NO_DATA_PLAYER(PREFIX_DB.value + "<dark_gray>No data for player <white><component>"),
    COMMAND_NO_DATA_ID(PREFIX_DB.value + "<dark_gray>No data for ID <white><component>");

    @Getter
    private final String value;

    CommandConfig(String value) {
        this.value = value;
    }

    /**
     * @return Formatted component message
     */
    public final Component getFormatted() {
        return MiniMessage.miniMessage().deserialize(value);
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
