package dev.zoldmaki.renewedtardis.sonic;

import java.util.Locale;
import javax.annotation.Nullable;

/** Persistent operating modes for the first 1.12.2 sonic screwdriver. */
public enum SonicMode {
    LINK("link", "Link"),
    LOCATE("locate", "Locate"),
    RECONFIGURE("reconfigure", "Reconfigure"),
    SCHEMATIC("schematic", "Schematic Scan"),
    TRANSMAT("transmat", "Transmat"),
    RECALL("recall", "Recall");

    private final String name;
    private final String displayName;

    SonicMode(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public SonicMode next() {
        SonicMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    @Nullable
    public static SonicMode fromName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (SonicMode mode : values()) {
            if (mode.name.equals(normalized)) {
                return mode;
            }
        }
        return null;
    }
}
