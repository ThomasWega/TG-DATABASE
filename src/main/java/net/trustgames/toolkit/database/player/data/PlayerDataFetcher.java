package net.trustgames.toolkit.database.player.data;

import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.database.player.data.cache.PlayerDataCache;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.database.player.data.event.PlayerDataUpdateEvent;
import net.trustgames.toolkit.managers.HikariManager;
import net.trustgames.toolkit.managers.rabbit.RabbitManager;
import net.trustgames.toolkit.utils.LevelUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.trustgames.toolkit.database.player.data.PlayerDataDB.tableName;
import static net.trustgames.toolkit.utils.LevelUtils.getProgress;
import static net.trustgames.toolkit.utils.LevelUtils.getThreshold;

public final class PlayerDataFetcher {

    private final HikariManager hikariManager;
    private final RabbitManager rabbitManager;
    private final PlayerDataCache dataCache;

    /**
     * Handles the fetching data types, from the cache,
     * or if not present - from the database. Also handles the
     * modifying of data types
     *
     * @param toolkit Instance
     */
    public PlayerDataFetcher(@NotNull Toolkit toolkit) {
        this.hikariManager = toolkit.getHikariManager();
        this.rabbitManager = toolkit.getRabbitManager();
        this.dataCache = new PlayerDataCache(toolkit.getJedisPool());
    }

    /**
     * Fetches a player's data by UUID and data type from the database synchronously.
     *
     * @param uuid     The UUID of the player.
     * @param dataType The data type to fetch.
     * @return An Optional that contains the fetched data, or empty if the data is not found.
     */
    private Optional<Object> fetchByUUID(@NotNull UUID uuid,
                                         @NotNull PlayerDataType dataType) {
        System.out.println("UUID FETCH");
        String label = dataType.getColumnName();
        try (Connection connection = hikariManager.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT " + label + " FROM " + tableName + " WHERE " + PlayerDataType.UUID.getColumnName() + " = ?")) {
            statement.setString(1, uuid.toString());
            System.out.println("IDK HNSTLY");
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    Object object = results.getObject(label);
                    System.out.println("THIS IS GOOD YEYj - " + object);
                    return Optional.of(object);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {

            throw new RuntimeException(e);
        }
    }

    /**
     * Fetches a player's data by name and data type from the database synchronously.
     *
     * @param playerName The name of the player.
     * @param dataType   The data type to fetch.
     * @return An Optional that contains the fetched data, or empty if the data is not found.
     */
    private Optional<Object> fetchByName(@NotNull String playerName,
                                         @NotNull PlayerDataType dataType) {
        System.out.println("NAME FETCH");
        String label = dataType.getColumnName();
        try (Connection connection = hikariManager.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT " + label + " FROM " + tableName + " WHERE " + PlayerDataType.NAME.getColumnName() + " = ?")) {
            statement.setString(1, playerName);
            System.out.println("WEELL");
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    Object object = results.getObject(label);
                    System.out.println("HIHI!! :)");

                    return Optional.of(object);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Inserts the specified data identified by the uuid to the database.
     * If the key is already present, it updates it
     *
     * @param uuid     UUID of the player
     * @param dataType The data type to modify
     * @param newValue Value to set the data type to
     */
    private void modifyByUUID(@NotNull UUID uuid,
                              @NotNull PlayerDataType dataType,
                              @NotNull Object newValue) {
        System.out.println("MODIFY :P -- " + dataType.name());
        String label = dataType.getColumnName();
        try (Connection connection = hikariManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO " + tableName + "(" + PlayerDataType.UUID.getColumnName() + ", " + label + ") " +
                             "VALUES (?, ?) " + "ON DUPLICATE KEY UPDATE " + label + " = VALUES(" + label + ")")
        ) {
            System.out.println("MODIFY OH?");
            connection.setAutoCommit(false);
            statement.setString(1, uuid.toString());
            statement.setObject(2, newValue);
            statement.executeUpdate();
            connection.commit();

            System.out.println("MODIFY SO WHAT IS - " + newValue);
            dataCache.updateData(uuid, dataType, newValue.toString());
            new PlayerDataUpdateEvent(rabbitManager, uuid, dataType).publish();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Inserts the specified data identified by the name to the database.
     * If the key is already present, it updates it
     *
     * @param playerName Name of the player
     * @param dataType   The data type to modify
     * @param newValue   Value to set the data type to
     */
    private void modifyByName(@NotNull String playerName,
                              @NotNull PlayerDataType dataType,
                              @NotNull Object newValue) {
        System.out.println("MODIFY :P -- " + dataType.name());
        String label = dataType.getColumnName();
        System.out.println("INSERT INTO " + tableName + "(" + PlayerDataType.UUID.getColumnName() + ", " + label + ") " + "VALUES ( " + playerName + ", " + newValue + ") " + "ON DUPLICATE KEY UPDATE " + label + " = VALUES(" + label + ")");
        try (Connection connection = hikariManager.getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO " + tableName + "(" + PlayerDataType.NAME.getColumnName() + ", " + label + ") " + "VALUES (?, ?) " + "ON DUPLICATE KEY UPDATE " + label + " = VALUES(" + label + ")")) {
            System.out.println("MODIFY OH?");
            connection.setAutoCommit(false);
            statement.setString(1, playerName);
            statement.setObject(2, newValue);
            statement.executeUpdate();
            connection.commit();

            System.out.println("MODIFY SO WHAT IS - " + newValue);
            resolveUUID(playerName).ifPresent(uuid -> {
                dataCache.updateData(uuid, dataType, newValue.toString());
                new PlayerDataUpdateEvent(rabbitManager, uuid, dataType).publish();
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Inserts all the data identified by the uuid to the database.
     * If the key is already present, it updates it
     *
     * @param uuid       UUID of the player
     * @param playerData with all filled in dataTypes!
     */
    private void modifyAllData(@NotNull UUID uuid,
                               @NotNull PlayerData playerData) {
        try (Connection connection = hikariManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO " + tableName + "(" + PlayerDataType.UUID.getColumnName() +
                             ", " + PlayerDataType.NAME.getColumnName() + ", " + PlayerDataType.KILLS.getColumnName() +
                             ", " + PlayerDataType.DEATHS.getColumnName() + ", " + PlayerDataType.GAMES_PLAYED.getColumnName() +
                             ", " + PlayerDataType.PLAYTIME.getColumnName() + ", " + PlayerDataType.XP.getColumnName() +
                             ", " + PlayerDataType.GEMS.getColumnName() + ", " + PlayerDataType.RUBIES.getColumnName() +
                             ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                             PlayerDataType.NAME.getColumnName() + " = VALUES(" + PlayerDataType.NAME.getColumnName() + "), " +
                             PlayerDataType.KILLS.getColumnName() + " = VALUES(" + PlayerDataType.KILLS.getColumnName() + "), " +
                             PlayerDataType.DEATHS.getColumnName() + " = VALUES(" + PlayerDataType.DEATHS.getColumnName() + "), " +
                             PlayerDataType.GAMES_PLAYED.getColumnName() + " = VALUES(" + PlayerDataType.GAMES_PLAYED.getColumnName() + "), " +
                             PlayerDataType.PLAYTIME.getColumnName() + " = VALUES(" + PlayerDataType.PLAYTIME.getColumnName() + "), " +
                             PlayerDataType.XP.getColumnName() + " = VALUES(" + PlayerDataType.XP.getColumnName() + "), " +
                             PlayerDataType.GEMS.getColumnName() + " = VALUES(" + PlayerDataType.GEMS.getColumnName() + "), " +
                             PlayerDataType.RUBIES.getColumnName() + " = VALUES(" + PlayerDataType.RUBIES.getColumnName() + ");")) {
            connection.setAutoCommit(false);
            statement.setString(1, uuid.toString());
            statement.setString(2, playerData.getName());
            statement.setInt(3, playerData.getKills());
            statement.setInt(4, playerData.getDeaths());
            statement.setInt(5, playerData.getGamesPlayed());
            statement.setInt(6, playerData.getPlaytimeSeconds());
            statement.setInt(7, playerData.getXp());
            statement.setInt(8, playerData.getGems());
            statement.setInt(9, playerData.getRubies());
            statement.executeUpdate();
            connection.commit();

            dataCache.updateAllData(uuid, playerData);

            // TODO different event (all update)
            // new PlayerDataUpdateEvent(rabbitManager, uuid, dataType).publish();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the data type in the database to the specified one.
     * If the data type is level, it converts it to XP
     *
     * @param uuid     UUID of the Player
     * @param dataType The data type to set the value to
     * @param newValue The value to set
     * @see PlayerDataFetcher#addData(UUID, PlayerDataType, int)
     * @see PlayerDataFetcher#addDataAsync(UUID, PlayerDataType, int)
     * @see PlayerDataFetcher#subtractData(UUID, PlayerDataType, int)
     * @see PlayerDataFetcher#subtractDataAsync(UUID, PlayerDataType, int)
     */
    public void setData(@NotNull UUID uuid,
                        @NotNull PlayerDataType dataType,
                        @NotNull Object newValue) {
        if (dataType == PlayerDataType.LEVEL) {
            dataCache.updateData(uuid, PlayerDataType.LEVEL, newValue.toString());

            int xpThreshold = LevelUtils.getThreshold(Integer.parseInt(newValue.toString()));
            dataCache.updateData(uuid, PlayerDataType.XP, String.valueOf(xpThreshold));
            modifyByUUID(uuid, PlayerDataType.XP, xpThreshold);
            return;
        }
        System.out.println("SET - " + newValue);
        modifyByUUID(uuid, dataType, newValue);
    }

    /**
     * Add the value to the data type value in the database.
     * If the data type is level, it converts it to XP
     *
     * @param uuid     UUID of the Player
     * @param dataType The data type to set the value to
     * @param addValue The value to add to the current value
     * @see PlayerDataFetcher#setData(UUID, PlayerDataType, Object)
     * @see PlayerDataFetcher#setDataAsync(UUID, PlayerDataType, Object)
     * @see PlayerDataFetcher#subtractData(UUID, PlayerDataType, int)
     * @see PlayerDataFetcher#subtractDataAsync(UUID, PlayerDataType, int)
     */
    public void addData(@NotNull UUID uuid,
                        @NotNull PlayerDataType dataType,
                        int addValue) {
        System.out.println("ADD - " + addValue);
        Optional<?> currentValue = resolveData(uuid, dataType);
        System.out.println("CURRENT ADD - " + currentValue);
        if (currentValue.isEmpty()) return;

        if (dataType == PlayerDataType.LEVEL) {
            calculateLevelXpModification(uuid, addValue, ModifyAction.ADD).ifPresent(newValue -> {
                dataCache.updateData(uuid, PlayerDataType.LEVEL, String.valueOf(LevelUtils.getLevelByXp(newValue)));
                dataCache.updateData(uuid, PlayerDataType.XP, String.valueOf(newValue));
                modifyByUUID(uuid, PlayerDataType.XP, newValue);
            });
            return;
        }

        System.out.println("HIHIHIHIH - " + currentValue);
        int newValue = Integer.parseInt(currentValue.get().toString()) + addValue;
        System.out.println("ADD FINAL - " + newValue);

        modifyByUUID(uuid, dataType, newValue);
    }


    /**
     * Subtract the value to the data type value in the database.
     * If the data type is level, it converts it to XP
     *
     * @param uuid          UUID of the Player
     * @param dataType      The data type to set the value to
     * @param subtractValue The value to remove from the current value
     * @see PlayerDataFetcher#setData(UUID, PlayerDataType, Object)
     * @see PlayerDataFetcher#setDataAsync(UUID, PlayerDataType, Object)
     * @see PlayerDataFetcher#addData(UUID, PlayerDataType, int)
     * @see PlayerDataFetcher#addDataAsync(UUID, PlayerDataType, int)
     */
    public void subtractData(@NotNull UUID uuid,
                             @NotNull PlayerDataType dataType,
                             int subtractValue) {
        System.out.println("REMOVE - " + subtractValue);
        Optional<?> currentValue = resolveData(uuid, dataType);
        System.out.println("CURRENT REMOVE - " + currentValue);

        if (currentValue.isEmpty()) return;

        if (dataType == PlayerDataType.LEVEL) {
            calculateLevelXpModification(uuid, subtractValue, ModifyAction.SUBTRACT).ifPresent(newValue -> {
                dataCache.updateData(uuid, PlayerDataType.LEVEL, String.valueOf(LevelUtils.getLevelByXp(newValue)));
                dataCache.updateData(uuid, PlayerDataType.XP, String.valueOf(newValue));
                modifyByUUID(uuid, PlayerDataType.XP, newValue);
            });
            return;
        }

        int newValue = Integer.parseInt(currentValue.get().toString()) - subtractValue;
        System.out.println("REMOVE FINAL - " + newValue);

        modifyByUUID(uuid, dataType, newValue);
    }

    /**
     * @see PlayerDataFetcher#setData(UUID, PlayerDataType, Object)
     */
    public void setDataAsync(@NotNull UUID uuid,
                             @NotNull PlayerDataType dataType,
                             @NotNull Object newValue) {
        CompletableFuture.runAsync(() -> setData(uuid, dataType, newValue));
    }

    /**
     * @see PlayerDataFetcher#addData(UUID, PlayerDataType, int)
     */
    public void addDataAsync(@NotNull UUID uuid,
                             @NotNull PlayerDataType dataType,
                             int addValue) {
        CompletableFuture.runAsync(() -> addData(uuid, dataType, addValue));
    }

    /**
     * @see PlayerDataFetcher#subtractData(UUID, PlayerDataType, int)
     */
    public void subtractDataAsync(@NotNull UUID uuid,
                                  @NotNull PlayerDataType dataType,
                                  int subtractValue) {
        CompletableFuture.runAsync(() -> subtractData(uuid, dataType, subtractValue));
    }

    /**
     * Tries to get the UUID from the cache.
     * If it's not in the cache, it tries to get it from the database
     *
     * @param playerName Name of the player
     * @return Optional of the player's UUID or empty
     */
    public Optional<UUID> resolveUUID(@NotNull String playerName) {
        System.out.println("UUID");
        Optional<UUID> optCachedUuid = dataCache.getUUID(playerName);
        if (optCachedUuid.isEmpty()) {
            Optional<?> optDatabaseUuid = fetchByName(playerName, PlayerDataType.UUID);
            if (optDatabaseUuid.isEmpty()) {
                return Optional.empty();
            }

            Optional<UUID> databaseUuid = optDatabaseUuid.map(o -> UUID.fromString(o.toString()));
            dataCache.updateUUID(playerName, databaseUuid.get());
            return databaseUuid;
        }
        return optCachedUuid;
    }


    /**
     * @see PlayerDataFetcher#resolveUUID(String)
     */
    public CompletableFuture<Optional<UUID>> resolveUUIDAsync(@NotNull String playerName) {
        return CompletableFuture.supplyAsync(() -> resolveUUID(playerName));
    }

    /**
     * Tries to get the specific data type value from the cache.
     * If it's not in the cache, it tries to get it from the database
     *
     * @param uuid UUID of the Player
     * @return Optional of the player's data or empty
     * @see PlayerDataFetcher#resolveData(String, PlayerDataType)
     * @see PlayerDataFetcher#resolveDataAsync(String, PlayerDataType)
     * @see PlayerDataFetcher#resolveDataAsync(UUID, PlayerDataType)
     */
    public Optional<?> resolveData(@NotNull UUID uuid,
                                   @NotNull PlayerDataType dataType) {
        System.out.println("DATA - UUID");
        Optional<String> optCachedUuid = dataCache.getData(uuid, dataType);
        if (optCachedUuid.isEmpty()) {
            System.out.println("NO CACHE KAŠe?");
            if (dataType == PlayerDataType.LEVEL) {
                Optional<Integer> levelData = convertXpToLevels(
                        fetchByUUID(uuid, PlayerDataType.XP)
                                .map(o -> Integer.parseInt(o.toString()))
                );
                levelData.ifPresent(level -> dataCache.updateData(uuid, dataType, String.valueOf(level)));
                return levelData;
            }
            Optional<Object> optDatabaseData = fetchByUUID(uuid, dataType);
            System.out.println("DATAB - " + optDatabaseData);
            optDatabaseData.ifPresent(data -> dataCache.updateData(uuid, dataType, data.toString()));
            return optDatabaseData;
        }
        System.out.println("KAše - " + optCachedUuid);
        return optCachedUuid;
    }

    /**
     * Tries to get the specific data type value from the cache.
     * If it's not in the cache, it tries to get it from the database
     *
     * @param playerName Name of the player
     * @return Optional of the player's data or empty
     * @see PlayerDataFetcher#resolveData(String, PlayerDataType)
     * @see PlayerDataFetcher#resolveIntData(String, PlayerDataType)
     * @see PlayerDataFetcher#resolveIntData(UUID, PlayerDataType)
     */
    public Optional<?> resolveData(@NotNull String playerName,
                                   @NotNull PlayerDataType dataType) {
        System.out.println("DATA - NAME");
        Optional<UUID> optUuid = resolveUUID(playerName);
        if (optUuid.isEmpty()) {
            return Optional.empty();
        }
        return resolveData(optUuid.get(), dataType);
    }

    public CompletableFuture<Optional<?>> resolveDataAsync(@NotNull UUID uuid,
                                                           @NotNull PlayerDataType dataType) {
        return CompletableFuture.supplyAsync(() -> resolveData(uuid, dataType));
    }

    public CompletableFuture<Optional<?>> resolveDataAsync(@NotNull String playerName,
                                                           @NotNull PlayerDataType dataType) {
        return CompletableFuture.supplyAsync(() -> resolveData(playerName, dataType));
    }

    /**
     * Tries to get the specific data type integer value from the cache.
     * If it's not in the cache, it tries to get it from the database
     *
     * @param uuid UUID of the Player
     * @return Optional of the player's data or empty
     * @see PlayerDataFetcher#resolveData(String, PlayerDataType)
     * @see PlayerDataFetcher#resolveData(UUID, PlayerDataType)
     * @see PlayerDataFetcher#resolveIntData(String, PlayerDataType)
     */
    public OptionalInt resolveIntData(@NotNull UUID uuid,
                                      @NotNull PlayerDataType dataType) {
        System.out.println("DATA INT - UUID");
        return convertOptional(
                resolveData(uuid, dataType)
                        .map(o -> Integer.parseInt(o.toString()))
        );
    }

    /**
     * Tries to get the specific data type integer value from the cache.
     * If it's not in the cache, it tries to get it from the database
     *
     * @param playerName Name of the Player
     * @return Optional of the player's data or empty
     * @see PlayerDataFetcher#resolveData(String, PlayerDataType)
     * @see PlayerDataFetcher#resolveData(UUID, PlayerDataType)
     * @see PlayerDataFetcher#resolveIntData(UUID, PlayerDataType)
     */
    public OptionalInt resolveIntData(@NotNull String playerName,
                                      @NotNull PlayerDataType dataType) {
        System.out.println("DATA INT - NAME");
        Optional<UUID> optUuid = resolveUUID(playerName);
        if (optUuid.isEmpty()) {
            return OptionalInt.empty();
        }
        return resolveIntData(optUuid.get(), dataType);
    }

    /**
     * @see PlayerDataFetcher#resolveIntData(UUID, PlayerDataType) 
     */
    public CompletableFuture<OptionalInt> resolveIntDataAsync(@NotNull UUID uuid,
                                                              @NotNull PlayerDataType dataType) {
        return CompletableFuture.supplyAsync(() -> resolveIntData(uuid, dataType));
    }

    /**
     * @see PlayerDataFetcher#resolveIntData(String, PlayerDataType) 
     */
    public CompletableFuture<OptionalInt> resolveIntDataAsync(@NotNull String playerName,
                                                              @NotNull PlayerDataType dataType) {
        return CompletableFuture.supplyAsync(() -> resolveIntData(playerName, dataType));
    }

    /**
     * @param optional Optional to be converted to OptionalInt
     * @return {@literal Converted Optional<Integer> to OptionalInt}
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private OptionalInt convertOptional(Optional<Integer> optional) {
        return optional.map(OptionalInt::of)
                .orElse(OptionalInt.empty());
    }

    /**
     * @param xpData Optional containing the xp value or empty
     * @return Converted optional of xp to optional of levels
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<Integer> convertXpToLevels(Optional<Integer> xpData) {
        return xpData.map(LevelUtils::getLevelByXp);
    }

    /**
     * Calculates the amount of xp needed to do a certain operation on level.
     * First it gets the current amount of xp, then it calculates what the future
     * level should be based on the operation (add/subtract/set).
     * The level progress is also taken into account, to preserve it when adding/subtracting.
     *
     * @param uuid UUID of the Player
     * @param value Modify value
     * @param action Action to do with the current value (eg. add/remove/set)
     * @return future XP needed to modify the level to the values given
     */
    private OptionalInt calculateLevelXpModification(UUID uuid, int value, ModifyAction action) {
        OptionalInt optCurrentXp = resolveIntData(uuid, PlayerDataType.XP);
        if (optCurrentXp.isEmpty()) {
            return OptionalInt.empty();
        }
        int currentXp = optCurrentXp.getAsInt();
        int currentLevel = LevelUtils.getLevelByXp(currentXp);
        int newLevel = currentLevel;

        // if action == SET, just use the given value
        if (action == ModifyAction.ADD) {
            newLevel = currentLevel + value;
        } else if (action == ModifyAction.SUBTRACT) {
            newLevel = currentLevel - value;
            if (newLevel < 0) {
                newLevel = 0;
            }
        }
        int newThreshold = getThreshold(newLevel);

        // calculate the progress towards the next level, to preserve the previous progress
        float progress = getProgress(currentXp);
        return OptionalInt.of(Math.round(newThreshold + ((getThreshold(newLevel + 1) - newThreshold) * progress)));
    }

    /**
     * Action to do with the current value
     */
    private enum ModifyAction {
        ADD, SUBTRACT
    }
}