package net.trustgames.toolkit.database.player.data;

import lombok.*;
import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.database.player.data.cache.PlayerDataCache;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.utils.LevelUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.trustgames.toolkit.database.player.data.PlayerDataDB.tableName;

@Data
@AllArgsConstructor
public class PlayerData {
    private UUID uuid;
    private String name;
    private int kills;
    private int deaths;
    private int gamesPlayed;
    private int playtimeSeconds;
    private int xp;
    private int level;
    private int gems;
    private int rubies;

    /**
     * @see PlayerData#getPlayerData(Toolkit, UUID)
     */
    public static CompletableFuture<Optional<PlayerData>> getPlayerDataAsync(@NotNull Toolkit toolkit,
                                                                             @NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> getPlayerData(toolkit, uuid));
    }

    /**
     * Tries to get all the data for the player from the cache.
     * In case that fails, it tries to get the data from the database.
     * Then converts everything into new PlayerData object with filled in values.
     *
     * @param toolkit instance of Toolkit
     * @param uuid UUID of the player
     * @return Optional with PlayerData with filled in values or empty
     */
    public static Optional<PlayerData> getPlayerData(@NotNull Toolkit toolkit,
                                                     @NotNull UUID uuid) {
        System.out.println("DATA HAH");
        PlayerDataCache dataCache = new PlayerDataCache(toolkit.getJedisPool());
        System.out.println("HHH");
        Optional<PlayerData> cachedOptData = dataCache.getAllData(uuid);
        System.out.println("CACHED - " + cachedOptData);
        if (cachedOptData.isPresent())
            return cachedOptData;

        // no data in cache
        System.out.println("DATABASE NOW");
        try (Connection connection = toolkit.getHikariManager().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + tableName + " WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    PlayerData playerData = new PlayerData(
                            UUID.fromString(rs.getString(PlayerDataType.UUID.getColumnName())),
                            rs.getString(PlayerDataType.NAME.getColumnName()),
                            rs.getInt(PlayerDataType.KILLS.getColumnName()),
                            rs.getInt(PlayerDataType.DEATHS.getColumnName()),
                            rs.getInt(PlayerDataType.GAMES_PLAYED.getColumnName()),
                            rs.getInt(PlayerDataType.PLAYTIME.getColumnName()),
                            rs.getInt(PlayerDataType.XP.getColumnName()),
                            LevelUtils.getLevelByXp(rs.getInt(PlayerDataType.XP.getColumnName())),
                            rs.getInt(PlayerDataType.GEMS.getColumnName()),
                            rs.getInt(PlayerDataType.RUBIES.getColumnName())
                    );
                    System.out.println("OK UPDATE?");
                    dataCache.updateAllData(uuid, playerData);
                    return Optional.of(playerData);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            System.out.println("RUNTIME EXCEPTION 22");
            throw new RuntimeException(e);
        }
    }
}
