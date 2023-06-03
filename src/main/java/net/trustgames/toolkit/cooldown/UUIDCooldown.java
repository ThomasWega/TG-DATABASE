package net.trustgames.toolkit.cooldown;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class UUIDCooldown {

    private final ConcurrentHashMap<UUID, Long> commandCooldownTime = new ConcurrentHashMap<>();

    /**
     * Ensures, that the action is not being spammed too much.
     * There is already one of this check in the CommandCooldownManager,
     * but that allows certain number of commands per second.
     * This method allows only one execution of the action per given time.
     *
     * @param uuid UUID of the Player
     * @param cooldownTime Time needed to pass to not be in a cooldown anymore
     * @return True if the player is on cooldown
     */
    public boolean onCooldown(@NotNull UUID uuid, double cooldownTime) {
        /*
         if the player is not in the cooldown yet, or if his cooldown expired,
         put him in the hashmap with the new time
        */
        if (!commandCooldownTime.containsKey(uuid)) {
            commandCooldownTime.put(uuid, System.currentTimeMillis());
            return false;
        } else return checkCooldown(uuid, cooldownTime);
    }

    /**
     * @param uuid   UUID of the Player to check cooldown on
     * @param cooldownTime UUIDCooldown time in seconds
     * @return if player is on cooldown
     */
    private boolean checkCooldown(@NotNull UUID uuid, double cooldownTime) {
        return !(cooldownTime <= (System.currentTimeMillis() - commandCooldownTime.get(uuid)) / 1000d);
    }

    /**
     * Remove the UUID from the cooldown map all together
     *
     * @param uuid UUID of the Player
     */
    public void remove(UUID uuid) {
        commandCooldownTime.remove(uuid);
    }
}

