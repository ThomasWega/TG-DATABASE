package net.trustgames.toolkit.database.player.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.managers.HikariManager;
import net.trustgames.toolkit.utils.LevelUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static net.trustgames.toolkit.database.player.data.PlayerDataDB.tableName;

@Getter
@Setter
@AllArgsConstructor
public class PlayerDataTest {
    private final UUID uuid;
    private final String name;
    private final int kills;
    private final int deaths;
    private final int gamesPlayed;
    private final int playtimeMinutes;
    private final int xp;
    private final int level;
    private final int gems;
    private final int rubies;


    public static void getPlayerDataAsync(@NotNull HikariManager hikariManager,
                                          @NotNull UUID uuid,
                                          Consumer<Optional<PlayerDataTest>> callback) {
        CompletableFuture.runAsync(() -> callback.accept(getPlayerDataSync(hikariManager, uuid)));
    }

    public static Optional<PlayerDataTest> getPlayerDataSync(@NotNull HikariManager hikariManager,
                                                             @NotNull UUID uuid) {
        try (Connection connection = hikariManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + tableName + " WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new PlayerDataTest(
                            UUID.fromString(rs.getString(PlayerDataType.UUID.getColumnName())),
                            rs.getString(PlayerDataType.NAME.getColumnName()),
                            rs.getInt(PlayerDataType.KILLS.getColumnName()),
                            rs.getInt(PlayerDataType.DEATHS.getColumnName()),
                            rs.getInt(PlayerDataType.GAMES.getColumnName()),
                            (rs.getInt(PlayerDataType.PLAYTIME.getColumnName()) / 60),
                            rs.getInt(PlayerDataType.XP.getColumnName()),
                            LevelUtils.getLevelByXp(rs.getInt(PlayerDataType.XP.getColumnName())),
                            rs.getInt(PlayerDataType.GEMS.getColumnName()),
                            rs.getInt(PlayerDataType.RUBIES.getColumnName())
                    ));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            System.out.println("RUNTIME EXCEPTION 22");
            throw new RuntimeException(e);
        }
    }
}
