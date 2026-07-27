package dev.zoldmaki.renewedtardis.exterior;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.util.IStringSerializable;

/**
 * Stable identifiers for exterior appearances. The record stores the string ID
 * so future shells can be added without changing the TARDIS identity.
 */
public enum ExteriorShell implements IStringSerializable {
    PROTOTYPE(0, "prototype", "Prototype Blue Box"),
    POLICE_BOX(1, "police_box", "Police Box");

    private final int metadata;
    private final String name;
    private final String displayName;

    ExteriorShell(int metadata, String name, String displayName) {
        this.metadata = metadata;
        this.name = name;
        this.displayName = displayName;
    }

    public int getMetadata() {
        return metadata;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ExteriorShell fromMetadata(int metadata) {
        for (ExteriorShell shell : values()) {
            if (shell.metadata == metadata) {
                return shell;
            }
        }
        return PROTOTYPE;
    }

    @Nullable
    public static ExteriorShell fromName(String token) {
        if (token == null) {
            return null;
        }

        String normalized = token.toLowerCase(Locale.ROOT).replace('-', '_');
        switch (normalized) {
            case "prototype":
            case "debug":
            case "blue_box":
                return PROTOTYPE;
            case "police":
            case "policebox":
            case "police_box":
            case "classic":
                return POLICE_BOX;
            default:
                return null;
        }
    }
}
