package net.trustgames.toolkit.database.player.data.config;

import net.kyori.adventure.text.Component;
import net.trustgames.toolkit.utils.MiniMessageUtils;
import org.jetbrains.annotations.NotNull;

/**
 * All configurable messages for PlayerDataCommand
 */
public enum PlayerDataConfig {
    PREFIX("<color:#3498db>Economy | </color>"),
    SET_SENDER(PREFIX.message + "<dark_gray>You have set <yellow><value> <player_data><dark_gray> to <white><player_name>"),
    SET_TARGET(PREFIX.message + "<dark_gray>You have been set <yellow><value> <player_data><dark_gray> by <white><player_name>"),
    ADD_SENDER(PREFIX.message + "<dark_gray>You have added <yellow><value> <player_data><dark_gray> to <white><player_name>"),
    ADD_TARGET(PREFIX.message + "<dark_gray>You have been added <yellow><value> <player_data><dark_gray> by <white><player_name>"),
    REMOVE_SENDER(PREFIX.message + "<dark_gray>You have removed <yellow><value> <player_data><dark_gray> from <white><player_name>"),
    REMOVE_TARGET(PREFIX.message + "<dark_gray>You have been removed <yellow><value> <player_data><dark_gray> by <white><player_name>"),
    GET_OTHER(PREFIX.message + "<dark_gray><white><player_name><dark_gray> has <yellow><value> <player_data>"),
    GET_PERSONAL(PREFIX.message + "<dark_gray>You have <yellow><value> <player_data>"),
    INVALID_ACTION(PREFIX.message + "<red>Invalid action for <white><player_data>");

    private final String message;

    PlayerDataConfig(String message) {
        this.message = message;
    }

    /**
     * Replace player data tags with player data
     *
     * @param playerName Name of the Player to replace the tags with value of
     * @return New formatted Component message with replaced tags
     */
    public final Component formatMessage(@NotNull String playerName,
                                         @NotNull PlayerDataType dataType,
                                         @NotNull String value) {
        return MiniMessageUtils.playerData(playerName, dataType, value).deserialize(message);
    }
}
