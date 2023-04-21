package net.trustgames.toolkit.config.chat;

import java.util.Arrays;
import java.util.Comparator;

public enum ChatLimitConfig {
    TITAN(0.1d, 10d),
    LORD(3d, 25d),
    KNIGHT(5d, 45d),
    PRIME(10d, 60d),
    DEFAULT(15d, 120d);

    public final double chatLimitSec;
    public final double chatLimitSameSec;

    ChatLimitConfig(double chatLimitSec, double chatLimitSameSec) {
        this.chatLimitSec = chatLimitSec;
        this.chatLimitSameSec = chatLimitSameSec;
    }

    private static ChatLimitConfig[] sortedValues;

    /**
     * @return Sorted array by the lowest limit
     */
    public static ChatLimitConfig[] getSorted() {
        if (sortedValues == null) {
            sortedValues = sortValues();
        }
        return sortedValues;
    }

    private static ChatLimitConfig[] sortValues() {
        ChatLimitConfig[] values = ChatLimitConfig.values();
        Arrays.sort(values, Comparator.comparingDouble(config -> config.chatLimitSec));
        return values;
    }
}
