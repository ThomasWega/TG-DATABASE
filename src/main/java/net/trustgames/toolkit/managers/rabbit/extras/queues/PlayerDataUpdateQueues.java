package net.trustgames.toolkit.managers.rabbit.extras.queues;

import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.managers.rabbit.extras.exchanges.RabbitExchanges;
import org.jetbrains.annotations.Nullable;

public enum PlayerDataUpdateQueues {
    PLAYER_DATA_UPDATE_ALL("event.player-data-update", "player-data-update", RabbitExchanges.EVENTS),
    PLAYER_DATA_UPDATE_UUID(PLAYER_DATA_UPDATE_ALL.name + ".uuid", "player-data-update.uuid", RabbitExchanges.EVENTS),
    PLAYER_DATA_UPDATE_NAME(PLAYER_DATA_UPDATE_ALL.name + ".name", "player-data-update.name", RabbitExchanges.EVENTS),
    PLAYER_DATA_UPDATE_KILLS(PLAYER_DATA_UPDATE_ALL.name + ".kills", "player-data-update.kills", RabbitExchanges.EVENTS),
    PLAYER_DATA_UPDATE_DEATHS(PLAYER_DATA_UPDATE_ALL.name + ".deaths", "player-data-update.deaths", RabbitExchanges.EVENTS),
    PLAYER_DATA_UPDATE_GAMES_PLAYED(PLAYER_DATA_UPDATE_ALL.name + ".games-played", "player-data-update.games-played", RabbitExchanges.EVENTS),
    PLAYER_DATA_UPDATE_PLAYTIME(PLAYER_DATA_UPDATE_ALL.name + ".playtime", "player-data-update.playtime", RabbitExchanges.EVENTS),
    PLAYER_DATA_UPDATE_XP(PLAYER_DATA_UPDATE_ALL.name + ".xp", "player-data-update.xp", RabbitExchanges.EVENTS),
    PLAYER_DATA_UPDATE_GEMS(PLAYER_DATA_UPDATE_ALL.name + ".gems", "player-data-update.gems.#", RabbitExchanges.EVENTS),
    PLAYER_DATA_UPDATE_RUBIES(PLAYER_DATA_UPDATE_ALL.name + ".rubies", "player-data-update.rubies.#", RabbitExchanges.EVENTS);

    public final String name;
    public final String routingKey;
    @Nullable public final RabbitExchanges[] exchanges;

    PlayerDataUpdateQueues(String name, String routingKey, @Nullable RabbitExchanges... exchanges) {
        this.name = name;
        this.routingKey = routingKey;
        this.exchanges = exchanges;
    }

    public static PlayerDataUpdateQueues queueOf(PlayerDataType type) {
        switch (type) {
            case UUID:
                return PLAYER_DATA_UPDATE_UUID;
            case NAME:
                return PLAYER_DATA_UPDATE_NAME;
            case KILLS:
                return PLAYER_DATA_UPDATE_KILLS;
            case DEATHS:
                return PLAYER_DATA_UPDATE_DEATHS;
            case GAMES:
                return PLAYER_DATA_UPDATE_GAMES_PLAYED;
            case PLAYTIME:
                return PLAYER_DATA_UPDATE_PLAYTIME;
            case XP:
                return PLAYER_DATA_UPDATE_XP;
            case GEMS:
                return PLAYER_DATA_UPDATE_GEMS;
            case RUBIES:
                return PLAYER_DATA_UPDATE_RUBIES;
            default:
                throw new IllegalArgumentException("Invalid player data type: " + type);
        }
    }
}