package net.trustgames.toolkit.utils;

public final class LevelUtils {

    private LevelUtils() {}

    /**
     * Get the progress from 0.0 to 1.0 that
     * the player has towards the next level.
     * The given XP will be converted to current level
     * and then a percentage to reach the next threshold
     * will be calculated.
     *
     * @param xp XP to get the progress for
     * @return 0.0 to 1.0 progress to next level
     */
    public static float getProgress(int xp) {
        int currentLevelThreshold = getThreshold(getLevelByXp(xp));
        int nextLevelThreshold = getThreshold(getLevelByXp(xp) + 1);

        return (float) (xp - currentLevelThreshold) / (float) (nextLevelThreshold - currentLevelThreshold);
    }

    /**
     * Calculate the needed xp to reach the given level
     * using a formula.
     *
     * @param level Level to get the threshold for
     * @return Needed XP to reach the level
     */
    public static int getThreshold(int level) {
        if (level < 0){
            level = 0;
        }
        // return the experience required for the next level
        // the base amount of experience required for level 1
        int base = 100;
        // the exponent that determines the experience curve
        double exponent = 1.2;
        return (int) Math.round(base * Math.pow(level, exponent));
    }

    /**
     * Loops through all the levels and checks
     * if the threshold is smaller. If false, that's
     * the current level
     *
     * @param xp Amount of XP to check the level for
     * @return The level the XP reaches
     */
    public static int getLevelByXp(int xp) {
        int level = 1;
        while (xp >= getThreshold(level)) {
            level++;
        }
        return level - 1;
    }
}
