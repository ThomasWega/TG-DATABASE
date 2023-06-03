package net.trustgames.toolkit.database.player.activity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.trustgames.toolkit.database.player.activity.config.PlayerAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * This class is just used as getters, setters and constructors
 * for the database player_activity.
 * Using lombok for this
 */
@Getter
@Setter
@AllArgsConstructor
public final class PlayerActivity {
    private final UUID uuid;
    private List<Activity> activities;

    /**
     * Add a new Activity to the list of Player Activities
     *
     * @param id     ID of the Activity
     * @param ip     IP of the player
     * @param action Action the player has done
     * @param time   Time the action happened
     */
    public void add(long id,
                    @Nullable String ip,
                    @NotNull PlayerAction action,
                    @NotNull Timestamp time) {
        activities.add(new Activity(id, uuid, ip, action, time));
    }


    @AllArgsConstructor
    @Getter
    @Setter
    public static class Activity {
        private long id;
        @NotNull
        private UUID uuid;
        @Nullable
        private String ip;
        @NotNull
        private PlayerAction action;
        @NotNull
        private Timestamp time;

        public Activity(@NotNull UUID uuid,
                        @Nullable String ip,
                        @NotNull PlayerAction action,
                        @NotNull Timestamp time) {
            this.id = -1; // set the id to -1 since it's not known yet
            this.uuid = uuid;
            this.ip = ip;
            this.action = action;
            this.time = time;
        }
    }
}
