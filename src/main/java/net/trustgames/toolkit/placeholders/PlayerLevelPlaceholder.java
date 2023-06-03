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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PlayerLevelPlaceholder implements AudiencePlaceholder {

    private final Toolkit toolkit;

    public PlayerLevelPlaceholder(Toolkit toolkit) {
        this.toolkit = toolkit;
    }

    @Override
    public @Nullable Tag tag(@NotNull Audience audience, @NotNull ArgumentQueue queue, @NotNull Context ctx) {
        return audience.get(Identity.UUID)
                .map(uuid -> Tag.selfClosingInserting(Component.text(getLevel(uuid))))
                .orElseGet(() -> Tag.selfClosingInserting(Component.text(0)));
    }

    private int getLevel(UUID uuid) {
        return new PlayerDataFetcher(toolkit).resolveIntData(uuid, PlayerDataType.LEVEL).orElse(0);
    }
}
