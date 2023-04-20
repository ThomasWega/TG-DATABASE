package net.trustgames.toolkit.database.player.data.level;

import net.trustgames.toolkit.Middleware;
import net.trustgames.toolkit.cache.PlayerDataCache;
import net.trustgames.toolkit.database.player.data.PlayerDataFetcher;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.IntConsumer;

import static net.trustgames.toolkit.utils.LevelUtils.*;

/**
 * Additional class to PlayerData, which is used to get, calculate or modify
 * player's level by amount of xp he holds
 */
public final class PlayerLevel {
    private final PlayerDataFetcher dataFetcher;
    private final PlayerDataCache dataCache;
    private final UUID uuid;

    public PlayerLevel(@NotNull Middleware middleware,
                       @NotNull UUID uuid) {
        this.uuid = uuid;
        this.dataFetcher = new PlayerDataFetcher(middleware, PlayerDataType.XP);
        this.dataCache = new PlayerDataCache(middleware, uuid, PlayerDataType.XP);
    }

    /**
     * @param levelIncrease The amount of levels to add to the current level
     */
    public void addLevel(int levelIncrease) {
        dataCache.get(currentXp -> getLevel(currentLevel -> {
            if (currentXp == null) return;
            int intCurrentXp = Integer.parseInt(currentXp);
            int newLevel = currentLevel + levelIncrease;
            int newThreshold = getThreshold(newLevel);
            float progress = getProgress(intCurrentXp);
            int newXP = (int) Math.floor(newThreshold + ((getThreshold(newLevel + 1) - newThreshold) * progress));

            dataFetcher.update(uuid, newXP);
        }));
    }

    /**
     * Retrieve the player's level by getting his
     * XP and calculating the level by the XP.
     * The result is saved in the callback
     *
     * @param callback Callback where the result will be saved
     */
    public void getLevel(IntConsumer callback) {
        dataCache.get(xp -> {
            if (xp == null) return;
            int intXp = Integer.parseInt(xp);
            int level = getLevelByXp(intXp);
            callback.accept(level);
        });
    }

    /**
     * @param targetLevel The final level the player will have
     */
    public void setLevel(int targetLevel) {
        int xpNeeded = getThreshold(targetLevel);
        dataFetcher.update(uuid, xpNeeded);
    }

    /**
     * @param levelDecrease The amount of levels to remove from the total level
     */
    public void removeLevel(int levelDecrease) {
        dataCache.get(currentXp -> getLevel(currentLevel -> {
            if (currentXp == null) return;
            int intCurrentXp = Integer.parseInt(currentXp);
            int newLevel = currentLevel - levelDecrease;
            int newThreshold = getThreshold(newLevel);
            float progress = getProgress(intCurrentXp);
            int newXP = (int) Math.floor(newThreshold + ((getThreshold(newLevel + 1) - newThreshold) * progress));

            dataFetcher.update(uuid, newXP);
        }));
    }
}
