package dev.zoldmaki.renewedtardis.flight;

import java.util.Locale;
import javax.annotation.Nullable;

/** Persistent prototype speed selection. Upgrade components can modify this later. */
public enum FlightSpeed {
    SLOW("slow", 150),
    NORMAL("normal", 100),
    FAST("fast", 65);

    private final String name;
    private final int durationPercent;

    FlightSpeed(String name, int durationPercent) {
        this.name = name;
        this.durationPercent = durationPercent;
    }

    public String getName() {
        return name;
    }

    public int getDurationPercent() {
        return durationPercent;
    }

    public long applyToDuration(long baseDurationMillis) {
        long safeBase = Math.max(0L, baseDurationMillis);
        if (safeBase > Long.MAX_VALUE / durationPercent) {
            return Long.MAX_VALUE;
        }
        return Math.max(1000L, (safeBase * durationPercent + 99L) / 100L);
    }

    @Nullable
    public static FlightSpeed fromName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (FlightSpeed speed : values()) {
            if (speed.name.equals(normalized)) {
                return speed;
            }
        }
        return null;
    }
}
