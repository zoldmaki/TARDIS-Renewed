package dev.zoldmaki.renewedtardis.access;

import java.util.Locale;
import javax.annotation.Nullable;

/** Controls who may enter an unlocked exterior. */
public enum TardisAccessMode {
    OWNER("owner", "owner only"),
    CREW("crew", "approved crew"),
    PUBLIC("public", "any player");

    private final String name;
    private final String displayName;

    TardisAccessMode(String name, String displayName) {
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
    public static TardisAccessMode fromName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("owner_only".equals(normalized) || "private".equals(normalized)) {
            normalized = "owner";
        }
        for (TardisAccessMode mode : values()) {
            if (mode.name.equals(normalized)) {
                return mode;
            }
        }
        return null;
    }
}
