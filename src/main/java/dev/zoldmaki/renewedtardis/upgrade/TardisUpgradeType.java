package dev.zoldmaki.renewedtardis.upgrade;

import java.util.Locale;
import javax.annotation.Nullable;

/** Stable identifiers for installable TARDIS upgrade components. */
public enum TardisUpgradeType {
    SPEED("speed", "Temporal Accelerator", "speed_upgrade"),
    CAPACITY("capacity", "Artron Capacitor", "artron_capacity_upgrade"),
    REGENERATION("regeneration", "Artron Regenerator", "artron_regeneration_upgrade"),
    ROOM("room", "Architectural Expansion", "room_upgrade"),
    PROTECTION("protection", "Temporal Shielding", "protection_upgrade");

    private final String name;
    private final String displayName;
    private final String itemName;

    TardisUpgradeType(String name, String displayName, String itemName) {
        this.name = name;
        this.displayName = displayName;
        this.itemName = itemName;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getItemName() {
        return itemName;
    }

    @Nullable
    public static TardisUpgradeType fromName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (TardisUpgradeType type : values()) {
            if (type.name.equals(normalized) || type.itemName.equals(normalized)) {
                return type;
            }
        }
        switch (normalized) {
            case "accelerator":
            case "flight_speed":
                return SPEED;
            case "energy":
            case "capacitor":
            case "artron_capacity":
                return CAPACITY;
            case "regen":
            case "energy_regen":
            case "artron_regen":
                return REGENERATION;
            case "rooms":
            case "architecture":
                return ROOM;
            case "shield":
            case "shields":
            case "armor":
                return PROTECTION;
            default:
                return null;
        }
    }
}
