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

    public PlayerDataFetcher(@NotNull Toolkit toolkit) {
        this.hikariManager = toolkit.getHikariManager();
        this.rabbitManager = toolkit.getRabbitManager();
        this.dataCache = new PlayerDataCache(toolkit.getJedisPool());
    }

    /**
     * Fetches a player's data by UUID and data type synchronously.
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
                    System.out.println("THIS IS GOOD YEY - " + object);
                    return Optional.of(object);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {

            throw new RuntimeException(e);
        }
    }

    /**
     * Fetches a player's data by name and data type synchronously.
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

    private void modifyByUUID(@NotNull UUID uuid,
                              @NotNull PlayerDataType dataType,
                              @NotNull Object newValue) {
        System.out.println("MODIFY :P -- " + dataType.name());
        String label = dataType.getColumnName();
        System.out.println("INSERT INTO " + tableName + "(" + PlayerDataType.UUID.getColumnName() + ", " + label + ") " + "VALUES ( " + uuid + ", " + newValue + ") " + "ON DUPLICATE KEY UPDATE " + label + " = VALUES(" + label + ")");
        try (Connection connection = hikariManager.getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO " + tableName + "(" + PlayerDataType.UUID.getColumnName() + ", " + label + ") " + "VALUES (?, ?) " + "ON DUPLICATE KEY UPDATE " + label + " = VALUES(" + label + ")")) {
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

    private void modifyAllData(@NotNull UUID uuid,
                               @NotNull PlayerData playerData) {
        try (Connection connection = hikariManager.getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO " + tableName + "(" + PlayerDataType.UUID.getColumnName() + ", " + PlayerDataType.NAME.getColumnName() + ", " + PlayerDataType.KILLS.getColumnName() + ", " + PlayerDataType.DEATHS.getColumnName() + ", " + PlayerDataType.GAMES_PLAYED.getColumnName() + ", " + PlayerDataType.PLAYTIME.getColumnName() + ", " + PlayerDataType.XP.getColumnName() + ", " + PlayerDataType.GEMS.getColumnName() + ", " + PlayerDataType.RUBIES.getColumnName() + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " + "ON DUPLICATE KEY UPDATE " + PlayerDataType.NAME.getColumnName() + " = VALUES(" + PlayerDataType.NAME.getColumnName() + "), " + PlayerDataType.KILLS.getColumnName() + " = VALUES(" + PlayerDataType.KILLS.getColumnName() + "), " + PlayerDataType.DEATHS.getColumnName() + " = VALUES(" + PlayerDataType.DEATHS.getColumnName() + "), " + PlayerDataType.GAMES_PLAYED.getColumnName() + " = VALUES(" + PlayerDataType.GAMES_PLAYED.getColumnName() + "), " + PlayerDataType.PLAYTIME.getColumnName() + " = VALUES(" + PlayerDataType.PLAYTIME.getColumnName() + "), " + PlayerDataType.XP.getColumnName() + " = VALUES(" + PlayerDataType.XP.getColumnName() + "), " + PlayerDataType.GEMS.getColumnName() + " = VALUES(" + PlayerDataType.GEMS.getColumnName() + "), " + PlayerDataType.RUBIES.getColumnName() + " = VALUES(" + PlayerDataType.RUBIES.getColumnName() + ");")) {
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

    public void setData(@NotNull UUID uuid,
                        @NotNull PlayerDataType dataType,
                        @NotNull Object newValue) {
        if (dataType == PlayerDataType.LEVEL) {
            dataCache.updateData(uuid, PlayerDataType.LEVEL, newValue.toString());

            int xpThreshold = LevelUtils.getThreshold(Integer.parseInt(newValue.toString()));
            dataCache.updateData(uuid, PlayerDataType.XP, String.valueOf(xpThreshold));
            modifyByUUID(uuid, PlayerDataType.XP, String.valueOf(xpThreshold));
            return;
        }
        System.out.println("SET - " + newValue);
        modifyByUUID(uuid, dataType, newValue);
    }

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

    public void setDataAsync(@NotNull UUID uuid,
                             @NotNull PlayerDataType dataType,
                             @NotNull Object newValue) {
        CompletableFuture.runAsync(() -> setData(uuid, dataType, newValue));
    }

    public void addDataAsync(@NotNull UUID uuid,
                             @NotNull PlayerDataType dataType,
                             int addValue) {
        CompletableFuture.runAsync(() -> addData(uuid, dataType, addValue));
    }

    public void subtractDataAsync(@NotNull UUID uuid,
                                  @NotNull PlayerDataType dataType,
                                  int subtractValue) {
        CompletableFuture.runAsync(() -> subtractData(uuid, dataType, subtractValue));
    }


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

    public CompletableFuture<Optional<UUID>> resolveUUIDAsync(@NotNull String playerName) {
        return CompletableFuture.supplyAsync(() -> resolveUUID(playerName));
    }

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

    public OptionalInt resolveIntData(@NotNull UUID uuid,
                                      @NotNull PlayerDataType dataType) {
        System.out.println("DATA INT - UUID");
        return convertOptional(
                resolveData(uuid, dataType)
                        .map(o -> Integer.parseInt(o.toString()))
        );
    }

    public OptionalInt resolveIntData(@NotNull String playerName, 
                                      @NotNull PlayerDataType dataType) {
        System.out.println("DATA INT - NAME");
        Optional<UUID> optUuid = resolveUUID(playerName);
        if (optUuid.isEmpty()) {
            return OptionalInt.empty();
        }
        return resolveIntData(optUuid.get(), dataType);
    }

    public CompletableFuture<OptionalInt> resolveIntDataAsync(@NotNull UUID uuid,
                                                              @NotNull PlayerDataType dataType) {
        return CompletableFuture.supplyAsync(() -> resolveIntData(uuid, dataType));
    }
    
    public CompletableFuture<OptionalInt> resolveIntDataAsync(@NotNull String playerName,
                                                              @NotNull PlayerDataType dataType) {
        return CompletableFuture.supplyAsync(() -> resolveIntData(playerName, dataType));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private OptionalInt convertOptional(Optional<Integer> optional) {
        return optional.map(OptionalInt::of)
                .orElse(OptionalInt.empty());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<Integer> convertXpToLevels(Optional<Integer> xpData) {
        return xpData.map(LevelUtils::getLevelByXp);
    }

    private OptionalInt calculateLevelXpModification(UUID uuid, int value, ModifyAction action) {
        OptionalInt optCurrentXp = resolveIntData(uuid, PlayerDataType.XP);
        if (optCurrentXp.isEmpty()) {
            return OptionalInt.empty();
        }
        int currentXp = optCurrentXp.getAsInt();
        int currentLevel = LevelUtils.getLevelByXp(currentXp);
        int newLevel = currentLevel;
        if (action == ModifyAction.ADD) {
            newLevel = currentLevel + value;
        } else if (action == ModifyAction.SUBTRACT) {
            newLevel = currentLevel - value;
            if (newLevel < 0) {
                newLevel = 0;
            }
        }
        int newThreshold = getThreshold(newLevel);
        float progress = getProgress(currentXp);
        return OptionalInt.of(Math.round(newThreshold + ((getThreshold(newLevel + 1) - newThreshold) * progress)));
    }

    private enum ModifyAction {
        ADD, SUBTRACT
    }
}