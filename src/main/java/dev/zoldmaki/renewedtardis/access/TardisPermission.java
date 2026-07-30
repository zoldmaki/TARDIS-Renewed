package dev.zoldmaki.renewedtardis.access;

import java.util.Locale;
import javax.annotation.Nullable;

/** Individual privileges that may be granted to a TARDIS crew member. */
public enum TardisPermission {
    DOOR("door", "door access"),
    KEY("key", "key use"),
    CONSOLE("console", "console control"),
    MAINTENANCE("maintenance", "maintenance");

    private final String name;
    private final String displayName;

    TardisPermission(String name, String displayName) {
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
    public static TardisPermission fromName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (TardisPermission permission : values()) {
            if (permission.name.equals(normalized)) {
                return permission;
            }
        }
        return null;
    }
}
