package net.trustgames.toolkit.placeholders;

import io.github.miniplaceholders.api.Expansion;
import net.trustgames.toolkit.Toolkit;

public class PlaceholderManager {

    /**
     * @param toolkit instance of Toolkit
     * @return Builder with registered placeholders
     */
    public static Expansion.Builder createPlaceholders(Toolkit toolkit) {
        return Expansion.builder("tg")
                //   .filter(Player.class)
                .audiencePlaceholder("player_prefix_spaced", new PlayerPrefixSpacedPlaceholder())
                .audiencePlaceholder("player_level", new PlayerLevelPlaceholder(toolkit))
                .audiencePlaceholder("player_level_progress", new PlayerLevelProgressPlaceholder(toolkit));
    }
}
