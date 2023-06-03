package net.trustgames.toolkit.utils;

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

    /**
     * Converts both undashed and dashed UUIDs to UUID Object
     * 
     * @param uuid UUID String (undashed or dashed)
     * @return new UUID Object
     * @throws IllegalArgumentException If the String is not valid UUID
     * @see UUIDUtils#fromTrimmed(String)
     */
    public static UUID formatFromInput(String uuid) throws IllegalArgumentException{
        if(uuid == null) throw new IllegalArgumentException();
        uuid = uuid.trim();
        return uuid.length() == 32 ? fromTrimmed(uuid.replaceAll("-", "")) : UUID.fromString(uuid);
    }

    /**
     * Convert trimmed (undashed) UUID String to UUID Object
     * 
     * @param trimmedUUID Undashed UUID String
     * @return new UUID Object
     * @throws IllegalArgumentException If String is not valid undashed UUID
     * @see UUIDUtils#formatFromInput(String) 
     */
    public static UUID fromTrimmed(String trimmedUUID) throws IllegalArgumentException{
        if(trimmedUUID == null) throw new IllegalArgumentException();
        StringBuilder builder = new StringBuilder(trimmedUUID.trim());
        /* Backwards adding to avoid index adjustments */
        try {
            builder.insert(20, "-");
            builder.insert(16, "-");
            builder.insert(12, "-");
            builder.insert(8, "-");
        } catch (StringIndexOutOfBoundsException e){
            throw new IllegalArgumentException();
        }

        return UUID.fromString(builder.toString());
    }
}
