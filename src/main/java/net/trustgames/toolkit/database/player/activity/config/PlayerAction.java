package net.trustgames.toolkit.database.player.activity.config;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum PlayerAction {
    JOIN("Join"),
    LEAVE("Leave"),
    NAME_CHANGE("Name Change");


    /**
     * List of all Action Strings for the action types
     */
    public static final Set<String> ALL_ACTION_STRINGS = EnumSet.allOf(PlayerAction.class)
            .stream()
            .map(playerAction -> playerAction.actionString)
            .collect(Collectors.toSet());

    @Getter
    private final String actionString;

    PlayerAction(String actionString) {
        this.actionString = actionString;
    }
}
