package net.trustgames.toolkit.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.utils.UUIDUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles getting the skin texture and signature from the mojang servers
 * and also caching it to save mojang api calls.
 */
public final class SkinFetcher {

    private static final Logger LOGGER = Toolkit.LOGGER;

    /**
     * Used to retrieve the skin data from the mojang servers.
     *
     * @param playerName Name of the player (paid account)
     * @implNote API-Calls are rate limited by Mojang
     * @see SkinFetcher#fetch(UUID)
     */
    public static Optional<SkinData> fetch(String playerName) {
        try {
            URL nameURL = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            InputStreamReader reader_0 = new InputStreamReader(nameURL.openStream());
            String trimmedUUID = JsonParser.parseReader(reader_0).getAsJsonObject().get("id").getAsString();

            return fetch(UUIDUtils.fromTrimmed(trimmedUUID));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not get skin data for name " + playerName + " from session servers!", e);
            return Optional.empty();
        }
    }

    /**
     * @see SkinFetcher#fetch(String)
     */
    public static CompletableFuture<Optional<SkinData>> fetchAsync(String playerName) {
        return CompletableFuture.supplyAsync(() -> fetch(playerName));
    }

    /**
     * Used to retrieve the skin data from the mojang servers.
     *
     * @param uuid UUID of the player (paid account)
     * @implNote API-Calls are rate limited by Mojang
     * @see SkinFetcher#fetch(String)
     */
    public static Optional<SkinData> fetch(UUID uuid) {
        try {
            URL uuidURL = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            InputStreamReader reader_1 = new InputStreamReader(uuidURL.openStream());
            JsonObject textureProperty = JsonParser.parseReader(reader_1).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();
            String texture = textureProperty.get("value").getAsString();
            String signature = textureProperty.get("signature").getAsString();

            return Optional.of(new SkinData(texture, signature));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not get skin data for uuid " + uuid + " from session servers!", e);
            return Optional.empty();
        }
    }

    /**
     * @see SkinFetcher#fetch(UUID)
     */
    public static CompletableFuture<Optional<SkinData>> fetchAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> fetch(uuid));
    }
}