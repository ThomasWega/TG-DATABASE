package net.trustgames.toolkit.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.trustgames.toolkit.Toolkit;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles getting the skin texture and signature from the mojang servers
 * and also caching it to save mojang api calls.
 */
public final class SkinFetcher {

    private static final Logger LOGGER = Toolkit.LOGGER;

    private final SkinCache skinCache;

    public SkinFetcher(JedisPool jedisPool) {
        this.skinCache = new SkinCache(jedisPool);
    }

    /**
     * First tries to retrieve the skin from the redis cache.
     * If it's not in the redis cache, it tries to get it from the mojang servers,
     * and then it updates it in the cache (if successfully fetched).
     *
     * @param playerName Name of the player (paid account)
     * @implNote API-Calls are rate limited by Mojang
     */
    public Optional<Skin> fetch(String playerName) {
        Optional<Skin> optSkinData = skinCache.getSkin(playerName);
        if (optSkinData.isPresent()) {
            System.out.println("SKIN - FROM CACHE");
            return optSkinData;
        }
        try {
            URL nameURL = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            InputStreamReader reader_0 = new InputStreamReader(nameURL.openStream());
            String trimmedUUID = JsonParser.parseReader(reader_0).getAsJsonObject().get("id").getAsString();

            URL uuidURL = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + trimmedUUID + "?unsigned=false");
            InputStreamReader reader_1 = new InputStreamReader(uuidURL.openStream());
            JsonObject textureProperty = JsonParser.parseReader(reader_1).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();
            String texture = textureProperty.get("value").getAsString();
            String signature = textureProperty.get("signature").getAsString();


            Skin skin = new Skin(texture, signature);
            skinCache.updateSkin(playerName, skin);
            System.out.println("SKIN - FROM MOJANG");
            return Optional.of(skin);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not get skin data for name " + playerName + " from session servers!", e);
            return Optional.empty();
        }
    }

    /**
     * @see SkinFetcher#fetch(String)
     */
    public CompletableFuture<Optional<Skin>> fetchAsync(String playerName) {
        return CompletableFuture.supplyAsync(() -> fetch(playerName));
    }
}