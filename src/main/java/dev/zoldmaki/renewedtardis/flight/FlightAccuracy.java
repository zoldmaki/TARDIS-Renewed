package dev.zoldmaki.renewedtardis.flight;

import java.util.Locale;
import javax.annotation.Nullable;

/** Persistent prototype navigation accuracy selection. */
public enum FlightAccuracy {
    PRECISE("precise", 0),
    NORMAL("normal", 2),
    RISKY("risky", 6);

    private final String name;
    private final int landingRadius;

    FlightAccuracy(String name, int landingRadius) {
        this.name = name;
        this.landingRadius = landingRadius;
    }

    public String getName() {
        return name;
    }

    public int getLandingRadius() {
        return landingRadius;
    }

    @Nullable
    public static FlightAccuracy fromName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (FlightAccuracy accuracy : values()) {
            if (accuracy.name.equals(normalized)) {
                return accuracy;
            }
        }
        return null;
    }
}
