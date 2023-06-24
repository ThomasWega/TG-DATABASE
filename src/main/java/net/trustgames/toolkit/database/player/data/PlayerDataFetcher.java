package net.trustgames.toolkit.database.player.data;

import lombok.Getter;
import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.database.HikariManager;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.database.player.data.event.PlayerDataUpdateEvent;
import net.trustgames.toolkit.database.player.data.event.PlayerDataUpdateEventConfig;
import net.trustgames.toolkit.message_queue.event.RabbitEventManager;
import net.trustgames.toolkit.utils.LevelUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static net.trustgames.toolkit.utils.LevelUtils.getProgress;
import static net.trustgames.toolkit.utils.LevelUtils.getThreshold;

public final class PlayerDataFetcher {
    private final HikariManager hikariManager;
    private final PlayerDataCache dataCache;
    private final RabbitEventManager eventManager;
    private static final Logger LOGGER = Toolkit.LOGGER;
    private static final String tableName = PlayerDataDB.getTableName();

    /**
     * Handles the fetching data types, from the cache,
     * or if not present - from the database. Also handles the
     * modifying of data types
     *
     * @param toolkit Instance
     */
    public PlayerDataFetcher(@NotNull Toolkit toolkit) {
        this.hikariManager = toolkit.getHikariManager();
        this.eventManager = toolkit.getRabbitEventManager();
        this.dataCache = new PlayerDataCache(toolkit.getJedisPool());
    }


    /**
     * Fetches a player's data by Key from the database synchronously.
     *
     * @param key Key to determine the row by
     * @param keyValue The value of the key parameter
     * @param dataType The data type to fetch.
     * @return An Optional that contains the fetched data, or empty if the data is not found.
     */
    private Optional<Object> fetchByKey(@NotNull FetchKey key,
                                         @NotNull String keyValue,
                                         @NotNull PlayerDataType dataType) {
        String label = dataType.getColumnName();
        try (Connection connection = hikariManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(generateSQLQueryForFetch(key, dataType))) {
            statement.setString(1, keyValue);
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    Object object = results.getObject(label);
                    return Optional.of(object);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while getting " + dataType.getColumnName() + " data type from the database by " + key.getDataType().getColumnName() + keyValue, e);
            return Optional.empty();
        }
    }

    /**
     * Fetches a collection of player's data types by Key from the database synchronously.
     *
     * @param key Key to determine the row by
     * @param keyValue The value of the key parameter
     * @param dataTypes  The collection of data types to fetch.
     * @return A map containing the fetched data, where the keys are the data types and the values are the corresponding data objects.
     */
    private Map<PlayerDataType, Optional<Object>> fetchCollectionByKey(@NotNull FetchKey key,
                                                                       @NotNull String keyValue,
                                                                       @NotNull Collection<PlayerDataType> dataTypes) {
        Map<PlayerDataType, Optional<Object>> fetchedData = new HashMap<>();
        try (Connection connection = hikariManager.getConnection(); PreparedStatement statement = connection.prepareStatement(generateSQLQueryForCollection(key, keyValue, dataTypes))) {
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    for (PlayerDataType dataType : dataTypes){
                        Object object = results.getObject(dataType.getColumnName());
                        fetchedData.put(dataType, Optional.ofNullable(object));
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while getting collection of " + dataTypes + " data types from the database by " + key.getDataType().getColumnName() + keyValue, e);
        }
        return fetchedData;
    }

    /**
     * Fetches a collection of player's data types by UUID from the database synchronously.
     *
     * @param key Key to determine the row by
     * @param keyValue The value of the key parameter
     * @param dataTypes  The collection of data types to fetch.
     * @return A map containing the fetched data, where the keys are the data types and the values are the corresponding data objects.
     */
    Map<PlayerDataType, Optional<String>> resolveFetchCollectionByKey(@NotNull FetchKey key,
                                                                      @NotNull String keyValue,
                                                                      @NotNull Collection<PlayerDataType> dataTypes) {
        // LEVEL cannot be explicitly fetched from database, so convert to XP for now
        List<PlayerDataType> convertedLevelToXP = dataTypes.stream()
                .map(dataType -> dataType == PlayerDataType.LEVEL ? PlayerDataType.XP : dataType)
                .distinct()
                .toList();

        // collection values in Optional<Object> to Optional<String>
        Map<PlayerDataType, Optional<String>> fetchedDataTypes = fetchCollectionByKey(key, keyValue, convertedLevelToXP)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, e -> e.getValue().map(Object::toString))
                );

        /*
         if LEVEL is also supposed to be fetched, convert the XP to LEVEL
         and add it to the map of fetched values
        */
        if (dataTypes.contains(PlayerDataType.LEVEL)) {
            fetchedDataTypes.get(PlayerDataType.XP).ifPresent(string -> {
                int level = LevelUtils.getLevelByXp(Integer.parseInt(string));
                fetchedDataTypes.put(PlayerDataType.LEVEL, Optional.of(String.valueOf(level)));
            });
        }
        return fetchedDataTypes;
    }

    /**
     * @see PlayerDataFetcher#resolveFetchCollectionByKey(FetchKey, String, Collection)
     */
    CompletableFuture<Map<PlayerDataType, Optional<String>>> resolveFetchCollectionByKeyAsync(@NotNull FetchKey key,
                                                                                              @NotNull String keyValue,
                                                                                              @NotNull Collection<PlayerDataType> dataTypes) {
        return CompletableFuture.supplyAsync(() -> resolveFetchCollectionByKey(key, keyValue, dataTypes))
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Exception occurred while fetching collection of data types " + dataTypes + " by " + key.getDataType().getColumnName() + keyValue + " async", throwable);
                    return null;
                });
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
        try (Connection connection = hikariManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(generateSQLQueryForModify(dataType))) {
            connection.setAutoCommit(false);
            statement.setString(1, uuid.toString());
            statement.setObject(2, newValue);
            statement.executeUpdate();
            connection.commit();

            dataCache.updateData(uuid, dataType, newValue.toString());

            PlayerDataUpdateEvent event = new PlayerDataUpdateEvent(uuid, dataType);
            eventManager.publish(event, new PlayerDataUpdateEventConfig().config());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while modifying " + dataType.getColumnName() + " data type in the database by UUID " + uuid, e);
        }
    }

    /**
     * Set the data type in the database to the specified one.
     * If the data type is LEVEL, it converts it to xp and updates
     * the xp in the database, as well as the level in the cache.
     * If the datatype is XP, it updates it in the database and
     * converts it to level to update the level amount in cache
     *
     * @param uuid     UUID of the Player
     * @param dataType The data type to set the value to
     * @param newValue The value to set
     * @see PlayerDataFetcher#setData(String, PlayerDataType, Object)
     * @see PlayerDataFetcher#addData(UUID, PlayerDataType, int)
     * @see PlayerDataFetcher#subtractData(UUID, PlayerDataType, int)
     */
    public void setData(@NotNull UUID uuid,
                        @NotNull PlayerDataType dataType,
                        @NotNull Object newValue) {

        if (dataType == PlayerDataType.XP) {
            handleXpUpdate(uuid, Integer.parseInt(newValue.toString()), ModifyAction.SET);
            return;
        }

        if (dataType == PlayerDataType.LEVEL) {
            handleLevelUpdate(uuid, Integer.parseInt(newValue.toString()), ModifyAction.SET);
            return;
        }
        modifyByUUID(uuid, dataType, newValue);
    }

    /**
     * @see PlayerDataFetcher#setData(UUID, PlayerDataType, Object)
     */
    public void setData(@NotNull String playerName,
                        @NotNull PlayerDataType dataType,
                        @NotNull Object newValue) {
        Optional<UUID> optUuid = resolveUUID(playerName);
        if (optUuid.isEmpty()) return;
        setData(optUuid.get(), dataType, newValue);
    }

    /**
     * Add the value to the data type value in the database.
     * If the data type is LEVEL, it converts it to xp and updates
     * the xp in the database, as well as the level in the cache.
     * If the datatype is XP, it updates it in the database and
     * converts it to level to update the level amount in cache
     *
     * @param uuid     UUID of the Player
     * @param dataType The data type to set the value to
     * @param addValue The value to add to the current value
     * @see PlayerDataFetcher#addData(String, PlayerDataType, int)
     * @see PlayerDataFetcher#setData(UUID, PlayerDataType, Object)
     * @see PlayerDataFetcher#subtractData(UUID, PlayerDataType, int)
     */
    public void addData(@NotNull UUID uuid,
                        @NotNull PlayerDataType dataType,
                        int addValue) {
        Optional<?> currentValue = resolveData(uuid, dataType);
        if (currentValue.isEmpty()) return;

        if (dataType == PlayerDataType.XP) {
            handleXpUpdate(uuid, addValue, ModifyAction.ADD);
            return;
        }
        if (dataType == PlayerDataType.LEVEL) {
            handleLevelUpdate(uuid, addValue, ModifyAction.ADD);
            return;
        }

        int newValue = Integer.parseInt(currentValue.get().toString()) + addValue;

        modifyByUUID(uuid, dataType, newValue);
    }

    /**
     * @see PlayerDataFetcher#addData(UUID, PlayerDataType, int)
     */
    public void addData(@NotNull String playerName,
                        @NotNull PlayerDataType dataType,
                        int addValue) {
        Optional<UUID> optUuid = resolveUUID(playerName);
        if (optUuid.isEmpty()) return;
        addData(optUuid.get(), dataType, addValue);
    }


    /**
     * Subtract the value to the data type value in the database.
     * If the data type is LEVEL, it converts it to xp and updates
     * the xp in the database, as well as the level in the cache.
     * If the datatype is XP, it updates it in the database and
     * converts it to level to update the level amount in cache
     *
     * @param uuid          UUID of the Player
     * @param dataType      The data type to set the value to
     * @param subtractValue The value to remove from the current value
     * @see PlayerDataFetcher#subtractData(String, PlayerDataType, int)
     * @see PlayerDataFetcher#setData(UUID, PlayerDataType, Object)
     * @see PlayerDataFetcher#addData(UUID, PlayerDataType, int)
     */
    public void subtractData(@NotNull UUID uuid,
                             @NotNull PlayerDataType dataType,
                             int subtractValue) {
        Optional<?> currentValue = resolveData(uuid, dataType);

        if (currentValue.isEmpty()) return;

        if (dataType == PlayerDataType.XP) {
            handleXpUpdate(uuid, subtractValue, ModifyAction.SUBTRACT);
            return;
        }
        if (dataType == PlayerDataType.LEVEL) {
            handleLevelUpdate(uuid, subtractValue, ModifyAction.SUBTRACT);
            return;
        }

        int newValue = Integer.parseInt(currentValue.get().toString()) - subtractValue;

        modifyByUUID(uuid, dataType, newValue);
    }

    /**
     * @see PlayerDataFetcher#subtractData(UUID, PlayerDataType, int)
     */
    public void subtractData(@NotNull String playerName,
                             @NotNull PlayerDataType dataType,
                             int subtractValue) {
        Optional<UUID> optUuid = resolveUUID(playerName);
        if (optUuid.isEmpty()) return;
        subtractData(optUuid.get(), dataType, subtractValue);
    }

    /**
     * @see PlayerDataFetcher#setData(UUID, PlayerDataType, Object)
     * @see PlayerDataFetcher#setDataAsync(String, PlayerDataType, Object)
     */
    public void setDataAsync(@NotNull UUID uuid,
                             @NotNull PlayerDataType dataType,
                             @NotNull Object newValue) {
        CompletableFuture.runAsync(() -> setData(uuid, dataType, newValue))
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Exception occurred while setting player data by UUID " + uuid + " async", throwable);
                    return null;
                });
    }

    /**
     * @see PlayerDataFetcher#setData(UUID, PlayerDataType, Object)
     * @see PlayerDataFetcher#setDataAsync(UUID, PlayerDataType, Object)
     */
    public void setDataAsync(@NotNull String playerName,
                             @NotNull PlayerDataType dataType,
                             @NotNull Object newValue) {
        CompletableFuture.runAsync(() -> setData(playerName, dataType, newValue))
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Exception occurred while setting player data by name " + playerName + " async", throwable);
                    return null;
                });
    }

    /**
     * @see PlayerDataFetcher#addData(UUID, PlayerDataType, int)
     * @see PlayerDataFetcher#addDataAsync(String, PlayerDataType, int)
     */
    public void addDataAsync(@NotNull UUID uuid,
                             @NotNull PlayerDataType dataType,
                             int addValue) {
        CompletableFuture.runAsync(() -> addData(uuid, dataType, addValue))
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Exception occurred while adding player data by UUID " + uuid + " async", throwable);
                    return null;
                });
    }

    /**
     * @see PlayerDataFetcher#addData(UUID, PlayerDataType, int)
     * @see PlayerDataFetcher#addDataAsync(UUID, PlayerDataType, int)
     */
    public void addDataAsync(@NotNull String playerName,
                             @NotNull PlayerDataType dataType,
                             int addValue) {
        CompletableFuture.runAsync(() -> addData(playerName, dataType, addValue))
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Exception occurred while adding player data by name " + playerName + " async", throwable);
                    return null;
                });
    }

    /**
     * @see PlayerDataFetcher#subtractData(UUID, PlayerDataType, int)
     * @see PlayerDataFetcher#subtractDataAsync(String, PlayerDataType, int)
     */
    public void subtractDataAsync(@NotNull UUID uuid,
                                  @NotNull PlayerDataType dataType,
                                  int subtractValue) {
        CompletableFuture.runAsync(() -> subtractData(uuid, dataType, subtractValue))
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Exception occurred while subtracting player data by UUID " + uuid + " async", throwable);
                    return null;
                });
    }

    /**
     * @see PlayerDataFetcher#subtractData(UUID, PlayerDataType, int)
     * @see PlayerDataFetcher#subtractDataAsync(UUID, PlayerDataType, int)
     */
    public void subtractDataAsync(@NotNull String playerName,
                                  @NotNull PlayerDataType dataType,
                                  int subtractValue) {
        CompletableFuture.runAsync(() -> subtractData(playerName, dataType, subtractValue))
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Exception occurred while subtracting player data by name " + playerName + " async", throwable);
                    return null;
                });
    }

    /**
     * Tries to get the UUID from the cache.
     * If it's not in the cache, it tries to get it from the database
     *
     * @param playerName Name of the player
     * @return Optional of the player's UUID or empty
     */
    public Optional<UUID> resolveUUID(@NotNull String playerName) {
        Optional<UUID> optCachedUuid = dataCache.getUUID(playerName);
        if (optCachedUuid.isEmpty()) {
            Optional<?> optDatabaseUuid = fetchByKey(FetchKey.NAME, playerName, PlayerDataType.UUID);
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
        return CompletableFuture.supplyAsync(() -> resolveUUID(playerName))
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Exception occurred while resolving player UUID by name " + playerName + " async", throwable);
                    return Optional.empty();
                });
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
        Optional<String> optCachedUuid = dataCache.getData(uuid, dataType);
        if (optCachedUuid.isEmpty()) {
            if (dataType == PlayerDataType.LEVEL) {
                Optional<Integer> levelData = convertXpToLevels(
                        fetchByKey(FetchKey.UUID, uuid.toString(), PlayerDataType.XP)
                                .map(o -> Integer.parseInt(o.toString()))
                );
                levelData.ifPresent(level -> dataCache.updateData(uuid, dataType, String.valueOf(level)));
                return levelData;
            }
            Optional<Object> optDatabaseData = fetchByKey(FetchKey.UUID, uuid.toString(), dataType);
            optDatabaseData.ifPresent(data -> dataCache.updateData(uuid, dataType, data.toString()));
            return optDatabaseData;
        }
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
        Optional<UUID> optUuid = resolveUUID(playerName);
        if (optUuid.isEmpty()) {
            return Optional.empty();
        }
        return resolveData(optUuid.get(), dataType);
    }

    public CompletableFuture<Optional<?>> resolveDataAsync(@NotNull UUID uuid,
                                                           @NotNull PlayerDataType dataType) {
        return CompletableFuture.supplyAsync(() -> resolveData(uuid, dataType))
                .handle((result, exception) -> {
                    if (exception != null) {
                        LOGGER.log(Level.SEVERE, "Exception occurred while resolving player data by UUID " + uuid + " async", exception);
                        return Optional.empty();
                    } else {
                        return result;
                    }
                });
    }

    public CompletableFuture<Optional<?>> resolveDataAsync(@NotNull String playerName,
                                                           @NotNull PlayerDataType dataType) {
        return CompletableFuture.supplyAsync(() -> resolveData(playerName, dataType))
                .handle((result, exception) -> {
                    if (exception != null) {
                        LOGGER.log(Level.SEVERE, "Exception occurred while resolving player data by name " + playerName + " async", exception);
                        return Optional.empty();
                    } else {
                        return result;
                    }
                });
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
        return CompletableFuture.supplyAsync(() -> resolveIntData(uuid, dataType))
                .handle((result, exception) -> {
                    if (exception != null) {
                        LOGGER.log(Level.SEVERE, "Exception occurred while resolving player int data by UUID " + uuid + " async", exception);
                        return OptionalInt.empty();
                    } else {
                        return result;
                    }
                });
    }

    /**
     * @see PlayerDataFetcher#resolveIntData(String, PlayerDataType) 
     */
    public CompletableFuture<OptionalInt> resolveIntDataAsync(@NotNull String playerName,
                                                              @NotNull PlayerDataType dataType) {
        return CompletableFuture.supplyAsync(() -> resolveIntData(playerName, dataType))
                .handle((result, exception) -> {
                    if (exception != null) {
                        LOGGER.log(Level.SEVERE, "Exception occurred while resolving player int data by name " + playerName + " async", exception);
                        return OptionalInt.empty();
                    } else {
                        return result;
                    }
                });
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
        int futureLevel;

        // if action == SET, just use the given value
        if (action == ModifyAction.ADD) {
            futureLevel = currentLevel + value;
        } else if (action == ModifyAction.SUBTRACT) {
            futureLevel = currentLevel - value;
            if (futureLevel < 0) {
                futureLevel = 0;
            }
            // if action == SET, just use the given value
        } else {
            futureLevel = value;
        }
        int levelWithoutProgress = getThreshold(futureLevel);

        if (action == ModifyAction.SET){
            return OptionalInt.of(levelWithoutProgress);
        }

        // calculate the progress towards the next level, to preserve the previous progress
        int levelWithProgress = Math.round(levelWithoutProgress + ((getThreshold(futureLevel + 1) - levelWithoutProgress) * getProgress(currentLevel)));
        return OptionalInt.of(levelWithProgress);
    }

    /**
     * Updates the level and xp data in cache on level update
     * and modifies the data in the database
     *
     * @param uuid UUID of the Player
     * @param value Value to modify the current value with
     * @param action What action to modify the current value with (eg. add/remove)
     */
    private void handleLevelUpdate(UUID uuid, int value, ModifyAction action){
        calculateLevelXpModification(uuid, value, action).ifPresent(newXpValue -> {
            dataCache.updateData(uuid, PlayerDataType.LEVEL, String.valueOf(LevelUtils.getLevelByXp(newXpValue)));
            modifyByUUID(uuid, PlayerDataType.XP, newXpValue);
        });
    }

    /**
     * Updates the level and xp data in cache on xp update
     * and modifies the data in the database
     *
     * @param uuid UUID of the Player
     * @param value Value to modify the current value with
     * @param action What action to modify the current value with (eg. add/remove)
     */
    private void handleXpUpdate(UUID uuid, int value, ModifyAction action){
        resolveIntData(uuid, PlayerDataType.XP).ifPresent(currentXp -> {
            int newXp;

            if (action == ModifyAction.ADD) {
                newXp = currentXp + value;
            } else if (action == ModifyAction.SUBTRACT) {
                newXp = currentXp - value;
                if (newXp < 0) {
                    newXp = 0;
                }
                // if action == SET, just use the given value
            } else {
                newXp = value;
            }
            dataCache.updateData(uuid, PlayerDataType.LEVEL, String.valueOf(LevelUtils.getLevelByXp(newXp)));
            modifyByUUID(uuid, PlayerDataType.XP, newXp);
        });
    }

    /**
     * Generates the SQL statement based on the given data types.
     *
     * @param key Key to determine the row by
     * @param keyValue The value of the key parameter
     * @param dataTypes The collection of data types
     * @return The generated SQL statement
     */
    private String generateSQLQueryForCollection(FetchKey key,
                                                 String keyValue,
                                                 Collection<PlayerDataType> dataTypes) {
        StringBuilder queryBuilder = new StringBuilder("SELECT ");
        List<String> labels = dataTypes.stream()
                .map(PlayerDataType::getColumnName)
                .toList();

        queryBuilder.append(String.join(", ", labels));
        queryBuilder.append(" FROM ").append(tableName);
        queryBuilder.append(" WHERE ").append(key.getDataType().getColumnName()).append(" = \"").append(keyValue).append("\"");
        queryBuilder.append(" LIMIT 1");

        return queryBuilder.toString();
    }

    /**
     * Generates the SQL statement based on the given values
     *
     * @param fetchKey Key to determine the row by
     * @param dataType Data type to fetch
     * @return SQL statement
     */
    private String generateSQLQueryForFetch(FetchKey fetchKey,  PlayerDataType dataType) {
        return "SELECT " + dataType.getColumnName() + " FROM " + tableName + " WHERE " + fetchKey.getDataType().getColumnName() + " = ? LIMIT 1";
    }

    /**
     * Generates the SQL statement to modify data type by UUID
     *
     * @param dataType Data type to modify
     * @return SQL statement
     */
    private String generateSQLQueryForModify(PlayerDataType dataType){
        String label = dataType.getColumnName();
        return "INSERT INTO " + tableName + "(" + PlayerDataType.UUID.getColumnName() + ", " + label + ") " +
                "VALUES (?, ?) " + "ON DUPLICATE KEY UPDATE " + label + " = VALUES(" + label + ")";
    }

    /**
     * Generates the SQL statement to modify all data types by UUID
     *
     * @return SQL statement
     */
    private String generateSQLQueryForModifyAll() {
        List<PlayerDataType> dataTypes = Arrays.stream(PlayerDataType.values())
                .filter(dataType -> dataType.getColumnType() != null)
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(tableName).append("(");
        dataTypes.forEach(dataType -> sb.append(dataType.getColumnName()).append(", "));
        sb.setLength(sb.length() - 2); // Remove the extra comma and space
        sb.append(") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE ");
        dataTypes.forEach(dataType ->
                sb.append(dataType.getColumnName()).append(" = VALUES(").append(dataType.getColumnName()).append("), ")
        );
        sb.setLength(sb.length() - 2); // Remove the extra comma and space
        sb.append(";");

        return sb.toString();
    }

    /**
     * Action to do with the current value
     */
    private enum ModifyAction {
        ADD, SUBTRACT, SET
    }

    /**
     * Type of key to fetch the value by
     */
    enum FetchKey {
        UUID(PlayerDataType.UUID),
        NAME(PlayerDataType.NAME);

        @Getter
        private final PlayerDataType dataType;

        FetchKey(PlayerDataType dataType) {
            this.dataType = dataType;
        }
    }
}