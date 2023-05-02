package net.trustgames.toolkit.managers.rabbit.extras.queues;

import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.managers.rabbit.extras.exchanges.RabbitExchanges;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public enum PlayerDataUpdateQueues {
    BULK("event.player-data-update", "player-data-update.#", RabbitExchanges.EVENTS),
    PLAYER_DATA_UPDATE_UUID(BULK.name + ".uuid", "player-data-update.uuid", RabbitExchanges.EVENTS),
    PLAYER_DATA_UPDATE_NAME(BULK.name + ".name", "player-data-update.name", RabbitExchanges.EVENTS),
    PLAYER_DATA_UPDATE_KILLS(BULK.name + ".kills", "player-data-update.kills", RabbitExchanges.EVENTS),
    PLAYER_DATA_UPDATE_DEATHS(BULK.name + ".deaths", "player-data-update.deaths", RabbitExchanges.EVENTS),
    PLAYER_DATA_UPDATE_GAMES_PLAYED(BULK.name + ".games-played", "player-data-update.games-played", RabbitExchanges.EVENTS),
    PLAYER_DATA_UPDATE_PLAYTIME(BULK.name + ".playtime", "player-data-update.playtime", RabbitExchanges.EVENTS),
    PLAYER_DATA_UPDATE_XP(BULK.name + ".xp", "player-data-update.xp", RabbitExchanges.EVENTS),
    PLAYER_DATA_UPDATE_GEMS(BULK.name + ".gems", "player-data-update.gems", RabbitExchanges.EVENTS),
    PLAYER_DATA_UPDATE_RUBIES(BULK.name + ".rubies", "player-data-update.rubies", RabbitExchanges.EVENTS);

    public final String name;
    public final String routingKey;
    @Nullable public final RabbitExchanges[] exchanges;

    PlayerDataUpdateQueues(String name, String routingKey, @Nullable RabbitExchanges... exchanges) {
        this.name = name;
        this.routingKey = routingKey;
        this.exchanges = exchanges;
    }

    public static PlayerDataUpdateQueues queueOf(PlayerDataType type) {
        return Arrays.stream(values())
                .filter(queue -> queue.name.endsWith("." + type.name().toLowerCase()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid player data type: " + type));
    }
}
