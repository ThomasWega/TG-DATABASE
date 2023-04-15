package net.trustgames.middleware.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.trustgames.middleware.utils.MiniMessageUtils;

public enum CommandConfig {

    PREFIX("<color:#2472f0>Command | </color>"),
    PREFIX_DB("<color:#ed7168>Database | </color>"),
    PREFIX_MQ("<color:#edc168>Database | </color>"),
    MAX_PER_SEC(5),
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
    COMMAND_NO_PLAYER_DATA(PREFIX_DB.value + "<dark_gray>No data for player <white><component>"),
    COMMAND_NO_ID_DATA(PREFIX_DB.value + "<dark_gray>No data for ID <white><component>");


    public final Object value;


    CommandConfig(Object value) {
        this.value = value;
    }

    /**
     * @return double value of the enum
     */
    public final double getDouble() {
        return Double.parseDouble(value.toString());
    }

    /**
     * @return Formatted component message
     */
    public final Component getText() {
        return MiniMessage.miniMessage().deserialize(value.toString());
    }

    /**
     * {@literal Replace <component> tag with given Component}
     *
     * @param component Component to replace the tag with
     * @return New formatted Component with replaced id tag
     */
    public final Component addComponent(Component component) {
        return MiniMessageUtils.component(component).deserialize(value.toString());
    }
}
