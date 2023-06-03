package net.trustgames.toolkit.skin;

import org.jetbrains.annotations.Nullable;

/**
 * Stores the texture and signature data of given skin
 */
public record SkinData(@Nullable String texture, @Nullable String signature) {
}