package net.trustgames.middleware.database.player.data;


import net.trustgames.middleware.Middleware;
import net.trustgames.middleware.cache.UUIDCache;
import net.trustgames.middleware.database.player.data.config.PlayerDataType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PlayerData {
    private final PlayerDataFetcher dataFetcher;
    private final UUID uuid;

    public PlayerData(@NotNull Middleware middleware,
                      @NotNull UUID uuid,
                      @NotNull PlayerDataType dataType) {
        if (dataType == PlayerDataType.UUID) {
            throw new RuntimeException(this.getClass().getName() + " can't be used to retrieve UUID. " +
                    "Use the " + UUIDCache.class.getName() + " instead!");
        }
        this.uuid = uuid;
        this.dataFetcher = new PlayerDataFetcher(middleware, dataType);
    }

    /**
     * Add Data to the player and update it in the database
     *
     * @param increase Amount of Data to add to the current amount
     */
    public void addData(int increase) {
        dataFetcher.fetch(uuid, data -> {
            if (data == null) return;
            int intData = Integer.parseInt(data);
            intData += increase;
            dataFetcher.update(uuid, intData);
        });
    }

    /**
     * @param target The final amount of Data the player will have
     */
    public void setData(int target) {
        dataFetcher.update(uuid, target);
    }

    /**
     * Removes the amount from the total data.
     * Makes sure that the data will not be set to less than 0
     *
     * @param decrease The amount of Data to remove from the total amount.
     */
    public void removeData(int decrease) {
        dataFetcher.fetch(uuid, data -> {
            if (data == null) return;
            int intData = Integer.parseInt(data);
            if (decrease >= intData) {
                setData(0);
                return;
            }
            setData(intData - decrease);
        });
    }
}
