package dev.zoldmaki.renewedtardis.console;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.util.EnumFacing;

/**
 * Stable logical identities for the four physical console faces.
 *
 * <p>The orientation is defined from the console-room entrance: south faces
 * the entrance, navigation is west/left, utility is east/right, and rooms are
 * north/rear.</p>
 */
public enum ConsolePanel {
    SOUTH_MAIN_FLIGHT("south", "Main Flight / Systems", EnumFacing.SOUTH),
    WEST_NAVIGATION("west", "Navigation", EnumFacing.WEST),
    NORTH_ROOMS("north", "Schematics / Rooms", EnumFacing.NORTH),
    EAST_UTILITY("east", "Utility", EnumFacing.EAST);

    private final String id;
    private final String displayName;
    private final EnumFacing outwardFacing;

    ConsolePanel(String id, String displayName, EnumFacing outwardFacing) {
        this.id = id;
        this.displayName = displayName;
        this.outwardFacing = outwardFacing;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public EnumFacing getOutwardFacing() {
        return outwardFacing;
    }

    @Nullable
    public static ConsolePanel fromFacing(EnumFacing facing) {
        if (facing == null || facing.getAxis() == EnumFacing.Axis.Y) {
            return null;
        }
        for (ConsolePanel panel : values()) {
            if (panel.outwardFacing == facing) {
                return panel;
            }
        }
        return null;
    }

    @Nullable
    public static ConsolePanel fromId(String id) {
        if (id == null) {
            return null;
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (ConsolePanel panel : values()) {
            if (panel.id.equals(normalized)) {
                return panel;
            }
        }
        return null;
    }
}
