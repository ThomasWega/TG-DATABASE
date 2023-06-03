package net.trustgames.toolkit.placeholders;

import io.github.miniplaceholders.api.placeholder.AudiencePlaceholder;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.database.player.data.PlayerDataFetcher;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.utils.LevelUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PlayerLevelProgressPlaceholder implements AudiencePlaceholder {
    private final Toolkit toolkit;

    public PlayerLevelProgressPlaceholder(Toolkit toolkit) {
        this.toolkit = toolkit;
    }

    @Override
    public @Nullable Tag tag(@NotNull Audience audience, @NotNull ArgumentQueue queue, @NotNull Context ctx) {
        return audience.get(Identity.UUID)
                .map(uuid -> Tag.selfClosingInserting(Component.text(String.format("%.1f", getLevelProgress(uuid) * 100))))
                .orElseGet(() -> Tag.selfClosingInserting(Component.text("0.0")));
    }

    private float getLevelProgress(UUID uuid) {
        int xp = new PlayerDataFetcher(toolkit).resolveIntData(uuid, PlayerDataType.XP).orElse(0);
        return LevelUtils.getProgress(xp);
    }
}
