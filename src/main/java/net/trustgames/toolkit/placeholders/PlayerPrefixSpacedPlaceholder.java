package net.trustgames.toolkit.placeholders;

import io.github.miniplaceholders.api.placeholder.AudiencePlaceholder;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.luckperms.api.model.user.User;
import net.trustgames.toolkit.luckperms.LuckPermsManager;
import net.trustgames.toolkit.utils.ColorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class PlayerPrefixSpacedPlaceholder implements AudiencePlaceholder {

    @Override
    public @Nullable Tag tag(@NotNull Audience audience, @NotNull ArgumentQueue queue, @NotNull Context ctx) {
        return audience.get(Identity.UUID)
                .map(uuid -> Tag.selfClosingInserting(formatPrefix(uuid)))
                .orElseGet(() -> Tag.selfClosingInserting(Component.empty()));
    }

    private Component formatPrefix(UUID uuid) {
        Optional<User> optUser = LuckPermsManager.getOnlineUser(uuid);
        if (optUser.isEmpty()){
            return Component.empty();
        }
        User user = optUser.get();
        String primaryGroup = user.getPrimaryGroup();
        Component prefix = ColorUtils.color(LuckPermsManager.getOnlinePlayerPrefix(user));
        if (!(primaryGroup.equals("default"))) {
            prefix = prefix.appendSpace();
        }
        return prefix;
    }
}
