package net.trustgames.middleware.utils;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class UUIDUtils {

    public static boolean isValidUUID(@Nullable String uuidString) {
        boolean valid = false;
        try {
            if (uuidString != null) {
                //noinspection ResultOfMethodCallIgnored
                UUID.fromString(uuidString);
                valid = true;
            }
        } catch (IllegalArgumentException ignored) {
        }
        return valid;
    }

}
