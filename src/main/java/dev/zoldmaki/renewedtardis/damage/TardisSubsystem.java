package dev.zoldmaki.renewedtardis.damage;

import java.util.Locale;
import javax.annotation.Nullable;

/** Persistent TARDIS systems that can be damaged and repaired. */
public enum TardisSubsystem {
    ENGINES("engines", "Engines"),
    NAVIGATION("navigation", "Navigation"),
    STABILIZERS("stabilizers", "Stabilizers"),
    DEMATERIALIZATION("dematerialization", "Dematerialization matrix");

    private final String name;
    private final String displayName;

    TardisSubsystem(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Nullable
    public static TardisSubsystem fromName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT).replace('-', '_');
        for (TardisSubsystem subsystem : values()) {
            if (subsystem.name.equals(normalized) || subsystem.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return subsystem;
            }
        }
        if ("engine".equals(normalized)) {
            return ENGINES;
        }
        if ("nav".equals(normalized)) {
            return NAVIGATION;
        }
        if ("stabilizer".equals(normalized) || "stabilisers".equals(normalized) || "stabiliser".equals(normalized)) {
            return STABILIZERS;
        }
        if ("demat".equals(normalized) || "matrix".equals(normalized)) {
            return DEMATERIALIZATION;
        }
        return null;
    }
}
