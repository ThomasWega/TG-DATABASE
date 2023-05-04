package net.trustgames.toolkit.database.player.activity.config;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum PlayerActivityType {
    JOIN("Join"),
    LEAVE("Leave");


    /**
     * List of all Action Strings for the action types
     */
    public static final Set<String> ALL_ACTIONS = EnumSet.allOf(PlayerActivityType.class)
            .stream()
            .map(playerActivityType -> playerActivityType.action)
            .collect(Collectors.toSet());

    @Getter
    private final String action;

    PlayerActivityType(String action) {
        this.action = action;
    }
}
