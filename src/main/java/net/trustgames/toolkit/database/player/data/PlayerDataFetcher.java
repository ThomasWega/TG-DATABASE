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
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.trustgames.toolkit.database.player.data.PlayerDataDB.tableName;
import static net.trustgames.toolkit.utils.LevelUtils.getProgress;
import static net.trustgames.toolkit.utils.LevelUtils.getThreshold;

public final class PlayerDataFetcher {

    private final HikariManager hikariManager;
    private final RabbitManager rabbitManager;
    private final PlayerDataCache dataCache;

    private final Logger logger = Toolkit.getLogger();

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
            throw new RuntimeException("Exception occurred while getting " + dataType.getColumnName() + " data type from the database by UUID " + uuid, e);
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
            throw new RuntimeException("Exception occurred while getting " + dataType.getColumnName() + " data type from the database by name " + playerName, e);
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
            throw new RuntimeException("Exception occurred while modifying " + dataType.getColumnName() + " data type in the database by UUID " + uuid, e);
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
            throw new RuntimeException("Exception occurred while modifying " + dataType.getColumnName() + " data type in the database by name " + playerName, e);
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
            throw new RuntimeException("Exception occurred while modifying all player data type in the database by UUID " + uuid, e);
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
        System.out.println("SET - " + newValue);
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
        System.out.println("ADD - " + addValue);
        Optional<?> currentValue = resolveData(uuid, dataType);
        System.out.println("CURRENT ADD - " + currentValue);
        if (currentValue.isEmpty()) return;

        if (dataType == PlayerDataType.XP) {
            handleXpUpdate(uuid, addValue, ModifyAction.ADD);
            return;
        }
        if (dataType == PlayerDataType.LEVEL) {
            handleLevelUpdate(uuid, addValue, ModifyAction.ADD);
            return;
        }

        System.out.println("HIHIHIHIH - " + currentValue);
        int newValue = Integer.parseInt(currentValue.get().toString()) + addValue;
        System.out.println("ADD FINAL - " + newValue);

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
        System.out.println("REMOVE - " + subtractValue);
        Optional<?> currentValue = resolveData(uuid, dataType);
        System.out.println("CURRENT REMOVE - " + currentValue);

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
        System.out.println("REMOVE FINAL - " + newValue);

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
                    logger.log(Level.SEVERE, "Exception occurred while setting player data by UUID " + uuid + " async", throwable);
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
                    logger.log(Level.SEVERE, "Exception occurred while setting player data by name " + playerName + " async", throwable);
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
                    logger.log(Level.SEVERE, "Exception occurred while adding player data by UUID " + uuid + " async", throwable);
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
                    logger.log(Level.SEVERE, "Exception occurred while adding player data by name " + playerName + " async", throwable);
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
                    logger.log(Level.SEVERE, "Exception occurred while subtracting player data by UUID " + uuid + " async", throwable);
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
                    logger.log(Level.SEVERE, "Exception occurred while subtracting player data by name " + playerName + " async", throwable);
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
        return CompletableFuture.supplyAsync(() -> resolveUUID(playerName))
                .exceptionally(throwable -> {
                    logger.log(Level.SEVERE, "Exception occurred while resolving player UUID by name " + playerName + " async", throwable);
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
        return CompletableFuture.supplyAsync(() -> resolveData(uuid, dataType))
                .handle((result, exception) -> {
                    if (exception != null) {
                        logger.log(Level.SEVERE, "Exception occurred while resolving player data by UUID " + uuid + " async", exception);
                        return Optional.empty();
                    } else {
                        return result;
                    }
                });
    }

    public CompletableFuture<Optional<?>> resolveDataAsync(@NotNull String playerName,
                                                           @NotNull PlayerDataType dataType) {
        System.out.println("BEFORE DATA - NAME (CORRECT?)");
        return CompletableFuture.supplyAsync(() -> resolveData(playerName, dataType))
                .handle((result, exception) -> {
                    if (exception != null) {
                        logger.log(Level.SEVERE, "Exception occurred while resolving player data by name " + playerName + " async", exception);
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
        return CompletableFuture.supplyAsync(() -> resolveIntData(uuid, dataType))
                .handle((result, exception) -> {
                    if (exception != null) {
                        logger.log(Level.SEVERE, "Exception occurred while resolving player int data by UUID " + uuid + " async", exception);
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
                        logger.log(Level.SEVERE, "Exception occurred while resolving player int data by name " + playerName + " async", exception);
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
        int newLevel;

        // if action == SET, just use the given value
        if (action == ModifyAction.ADD) {
            newLevel = currentLevel + value;
        } else if (action == ModifyAction.SUBTRACT) {
            newLevel = currentLevel - value;
            if (newLevel < 0) {
                newLevel = 0;
            }
            // if action == SET, just use the given value
        } else {
            newLevel = value;
        }
        int newThreshold = getThreshold(newLevel);

        // calculate the progress towards the next level, to preserve the previous progress
        float progress = getProgress(currentXp);
        return OptionalInt.of(Math.round(newThreshold + ((getThreshold(newLevel + 1) - newThreshold) * progress)));
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
     * Action to do with the current value
     */
    private enum ModifyAction {
        ADD, SUBTRACT, SET
    }
}