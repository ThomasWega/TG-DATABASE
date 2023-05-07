package net.trustgames.toolkit.database.player.data;


import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.cache.PlayerDataCache;
import net.trustgames.toolkit.cache.UUIDCache;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class PlayerData {

    private final Toolkit toolkit;
    private final PlayerDataFetcher dataFetcher;
    private final UUID uuid;
    private final PlayerDataType dataType;

    public PlayerData(@NotNull Toolkit toolkit,
                      @NotNull UUID uuid,
                      @NotNull PlayerDataType dataType) {
        if (dataType == PlayerDataType.UUID) {
            throw new RuntimeException(this.getClass().getName() + " can't be used to retrieve UUID. " +
                    "Use the " + UUIDCache.class.getName() + " instead!");
        }
        this.toolkit = toolkit;
        this.uuid = uuid;
        this.dataFetcher = new PlayerDataFetcher(toolkit, dataType);
        this.dataType = dataType;
    }

    /**
     * Add Data to the player and update it in the database
     *
     * @param increase Amount of Data to add to the current amount
     */
    public void addData(int increase) {
        dataFetcher.fetch(uuid, data -> {
            if (data.isEmpty()) return;

            int intData = Integer.parseInt(data.get());
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
     * Gets the data from the Cache or the database
     *
     * @param callback Consumer with the fetched data, or empty
     */
    public void getData(Consumer<Optional<Integer>> callback) {
        new PlayerDataCache(toolkit, uuid, dataType).get(optStringData -> {
            if (optStringData.isEmpty()){
                callback.accept(Optional.empty());
                return;
            }
            callback.accept(Optional.of(Integer.parseInt(optStringData.get())));
        });
    }

    /**
     * Removes the amount from the total data.
     * Makes sure that the data will not be set to less than 0
     *
     * @param decrease The amount of Data to remove from the total amount.
     */
    public void removeData(int decrease) {
        dataFetcher.fetch(uuid, data -> {
            if (data.isEmpty()) return;

            int intData = Integer.parseInt(data.get());
            if (decrease >= intData) {
                setData(0);
                return;
            }
            setData(intData - decrease);
        });
    }
}
