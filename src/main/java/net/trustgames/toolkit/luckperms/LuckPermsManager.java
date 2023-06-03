package net.trustgames.toolkit.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Handles the various LuckPerms checks and events
 */
public final class LuckPermsManager {

    /**
     * @param user User to check permission for
     * @param permission Permission to check for
     * @return True if user has permission, false otherwise (eg. if unset)
     */
    public static boolean hasPermissionOnline(User user, String permission) {
        return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }

    /**
     * @see LuckPermsManager#hasPermissionOnline(User, String)
     */
    public static boolean hasPermissionOnline(UUID uuid, String permission) {
        return getOnlineUser(uuid).map(user -> hasPermissionOnline(user, permission)).orElse(false);
    }

    /**
     * @param user User to check group on
     * @param group  What group check for
     * @return if the given player is in the given group
     */
    public static boolean isOnlinePlayerInGroup(@NotNull User user, @NotNull String group) {
        return user.getCachedData().getPermissionData().checkPermission("group." + group).asBoolean();
    }

    /**
     * @see LuckPermsManager#isOnlinePlayerInGroup(User, String)
     */
    public static boolean isOnlinePlayerInGroup(@NotNull UUID uuid, @NotNull String group) {
        Optional<User> optUser = getOnlineUser(uuid);
        return optUser.map(user -> isOnlinePlayerInGroup(user, group)).orElse(false);

    }

        /**
         * @return Set of all loaded groups
         */
    public static @NotNull Set<Group> getGroups() {
        LuckPerms luckPerms = LuckPermsProvider.get();
        return luckPerms.getGroupManager().getLoadedGroups();
    }

    /**
     * Returns the first group it matches from the list
     *
     * @param user User to check on
     * @param possibleGroups List of groups to check for
     * @return Player 's group found from the list
     */
    public static Optional<String> getOnlinePlayerGroupFromList(@NotNull User user,
                                                                @NotNull Collection<String> possibleGroups) {
        for (String group : possibleGroups) {
            if (isOnlinePlayerInGroup(user, group)){
                return Optional.of(group);
            }
        }
        return Optional.empty();
    }

    /**
     * @see LuckPermsManager#getOnlinePlayerGroupFromList(User, Collection)
     */
    public static Optional<String> getOnlinePlayerGroupFromList(@NotNull UUID uuid,
                                                                @NotNull Collection<String> possibleGroups) {
        Optional<User> optUser = getOnlineUser(uuid);
        return optUser.flatMap(user -> getOnlinePlayerGroupFromList(optUser.get(), possibleGroups));
    }

    /**
     * @return LuckPerms GroupManager
     */
    public static GroupManager getGroupManager() {
        return LuckPermsProvider.get().getGroupManager();
    }

    /**
     * Get the player's prefix. If the prefix is null,
     * it will be set to empty component
     *
     * @param user User to get prefix for
     * @return Player Prefix or Empty
     */
    public static @NotNull String getOnlinePlayerPrefix(@NotNull User user) {
        String prefixString = user.getCachedData().getMetaData().getPrefix();

        return Objects.requireNonNullElse(prefixString, "");
    }

    /**
     * @see LuckPermsManager#getOnlinePlayerPrefix(User)
     */
    public static @NotNull String getOnlinePlayerPrefix(@NotNull UUID uuid){
        Optional<User> optUser = getOnlineUser(uuid);
        return optUser.map(LuckPermsManager::getOnlinePlayerPrefix).orElse("");
    }

    /**
     * Get the group's prefix. If the prefix is null,
     * it will be set to ""
     *
     * @param group Group to get prefix for
     * @return Group prefix String
     */
    public static @NotNull String getGroupPrefix(@NotNull Group group) {
        String prefixString = group.getCachedData().getMetaData().getPrefix();
        return Objects.requireNonNullElse(prefixString, "");
    }

    /**
     * @param uuid UUID of the Player
     * @return User from the given UUID or empty
     */
    public static Optional<User> getOnlineUser(@NotNull UUID uuid) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        return Optional.ofNullable(luckPerms.getUserManager().getUser(uuid));
    }
}

