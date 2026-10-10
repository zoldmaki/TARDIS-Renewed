package dev.zoldmaki.renewedtardis.flight;

import java.util.Locale;
import javax.annotation.Nullable;

/** Persistent prototype speed selection used by the physical radial lever. */
public enum FlightSpeed {
    SLOW("slow", 150, 0),
    NORMAL("normal", 100, 1),
    FAST("fast", 65, 2);

    private final String name;
    private final int durationPercent;
    private final int consoleState;

    FlightSpeed(String name, int durationPercent, int consoleState) {
        this.name = name;
        this.durationPercent = durationPercent;
        this.consoleState = consoleState;
    }

    public String getName() {
        return name;
    }

    public int getDurationPercent() {
        return durationPercent;
    }

    public int getConsoleState() {
        return consoleState;
    }

    public long applyToDuration(long baseDurationMillis) {
        long safeBase = Math.max(0L, baseDurationMillis);
        if (safeBase > Long.MAX_VALUE / durationPercent) {
            return Long.MAX_VALUE;
        }
        return Math.max(1000L, (safeBase * durationPercent + 99L) / 100L);
    }

    public static FlightSpeed fromConsoleState(int state) {
        int clamped = Math.max(0, Math.min(values().length - 1, state));
        for (FlightSpeed speed : values()) {
            if (speed.consoleState == clamped) {
                return speed;
            }
        }
        return NORMAL;
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
