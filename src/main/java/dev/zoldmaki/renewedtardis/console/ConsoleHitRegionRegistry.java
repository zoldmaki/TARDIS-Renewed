package dev.zoldmaki.renewedtardis.console;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.util.EnumFacing;

/**
 * Placeholder hit maps for the four generated panel blocks.
 *
 * <p>The navigation face uses a denser sketch-inspired layout so all 25
 * waypoint slots are directly testable. The final model may replace these
 * rectangles with model-specific boxes, but it must continue routing to the
 * same {@link ConsoleControlId} values.</p>
 */
public final class ConsoleHitRegionRegistry {
    private static final int GRID_SIZE = 4;
    private static final Map<ConsolePanel, List<ConsoleHitRegion>> REGIONS;

    static {
        EnumMap<ConsolePanel, List<ConsoleHitRegion>> regions = new EnumMap<>(ConsolePanel.class);

        regions.put(ConsolePanel.WEST_NAVIGATION, navigationRegions());
        regions.put(ConsolePanel.SOUTH_MAIN_FLIGHT, mainFlightRegions());
        regions.put(
            ConsolePanel.EAST_UTILITY,
            grid(
                ConsolePanel.EAST_UTILITY,
                new ConsoleControlId[][] {
                    {ConsoleControlId.UTILITY_INTERNAL_TIME, ConsoleControlId.UTILITY_EVENT_BUTTON_1, ConsoleControlId.UTILITY_SHELL_PREVIOUS, ConsoleControlId.UTILITY_SHELL_NEXT},
                    {ConsoleControlId.UTILITY_EXTERNAL_SCANNER, ConsoleControlId.UTILITY_EXTERNAL_SCANNER, ConsoleControlId.UTILITY_SHELL_SCREEN, ConsoleControlId.UTILITY_SHELL_SCREEN},
                    {ConsoleControlId.UTILITY_DOOR_LOCK, ConsoleControlId.UTILITY_ABSOLUTE_RELATIVE, ConsoleControlId.UTILITY_SHELL_APPLY, ConsoleControlId.UTILITY_EVENT_SWITCH_1},
                    {ConsoleControlId.UTILITY_SPATIAL_LINK, ConsoleControlId.UTILITY_SPATIAL_LINK, ConsoleControlId.UTILITY_EVENT_BUTTON_2, ConsoleControlId.UTILITY_EVENT_SWITCH_2}
                }
            )
        );
        regions.put(
            ConsolePanel.NORTH_ROOMS,
            grid(
                ConsolePanel.NORTH_ROOMS,
                new ConsoleControlId[][] {
                    {ConsoleControlId.ROOMS_DELETE_ALL, ConsoleControlId.ROOMS_EVENT_SWITCH_1, ConsoleControlId.ROOMS_COUNT_GAUGE, ConsoleControlId.ROOMS_COUNT_GAUGE},
                    {ConsoleControlId.ROOMS_SONIC_SLOT, ConsoleControlId.ROOMS_SONIC_GENERATE, ConsoleControlId.ROOMS_TYPE_PREVIOUS, ConsoleControlId.ROOMS_TYPE_NEXT},
                    {ConsoleControlId.ROOMS_TYPE_SCREEN, ConsoleControlId.ROOMS_TYPE_SCREEN, ConsoleControlId.ROOMS_SCHEMA_PREVIOUS, ConsoleControlId.ROOMS_SCHEMA_NEXT},
                    {ConsoleControlId.ROOMS_HELMIC_REGULATOR, ConsoleControlId.ROOMS_HELMIC_REGULATOR, ConsoleControlId.ROOMS_SCHEMA_SCREEN, ConsoleControlId.ROOMS_SCHEMA_SCREEN}
                }
            )
        );

        REGIONS = Collections.unmodifiableMap(regions);
    }

    private ConsoleHitRegionRegistry() {
    }

    public static List<ConsoleHitRegion> getRegions(ConsolePanel panel) {
        List<ConsoleHitRegion> regions = REGIONS.get(panel);
        return regions == null ? Collections.<ConsoleHitRegion>emptyList() : regions;
    }

    @Nullable
    public static ConsoleControlId resolve(
        ConsolePanel panel,
        float hitX,
        float hitY,
        float hitZ
    ) {
        if (panel == null) {
            return null;
        }

        double[] local = toLocalTopCoordinates(panel.getOutwardFacing(), hitX, hitZ);
        double u = clampUnit(local[0]);
        double v = clampUnit(local[1]);
        for (ConsoleHitRegion region : getRegions(panel)) {
            if (region.contains(u, v)) {
                return region.getControl();
            }
        }
        return null;
    }

    private static List<ConsoleHitRegion> mainFlightRegions() {
        List<ConsoleHitRegion> result = new ArrayList<>();
        ConsolePanel panel = ConsolePanel.SOUTH_MAIN_FLIGHT;

        // Gauges and the two dedicated blue flight-event buttons.
        add(result, panel, ConsoleControlId.MAIN_SHIELD_GAUGE, 0.08D, 0.04D, 0.28D, 0.18D);
        add(result, panel, ConsoleControlId.MAIN_EVENT_BUTTON_1, 0.31D, 0.06D, 0.36D, 0.15D);
        add(result, panel, ConsoleControlId.MAIN_ARTRON_GAUGE, 0.39D, 0.04D, 0.59D, 0.18D);
        add(result, panel, ConsoleControlId.MAIN_EVENT_BUTTON_2, 0.62D, 0.06D, 0.67D, 0.15D);
        add(result, panel, ConsoleControlId.MAIN_XP_GAUGE, 0.70D, 0.04D, 0.90D, 0.18D);

        // Seven individual progress lamps around the radial speed lever.
        add(result, panel, ConsoleControlId.MAIN_PROGRESS_1, 0.29D, 0.34D, 0.34D, 0.40D);
        add(result, panel, ConsoleControlId.MAIN_PROGRESS_2, 0.32D, 0.27D, 0.37D, 0.33D);
        add(result, panel, ConsoleControlId.MAIN_PROGRESS_3, 0.37D, 0.21D, 0.42D, 0.27D);
        add(result, panel, ConsoleControlId.MAIN_PROGRESS_4, 0.44D, 0.19D, 0.49D, 0.25D);
        add(result, panel, ConsoleControlId.MAIN_PROGRESS_5, 0.51D, 0.22D, 0.56D, 0.28D);
        add(result, panel, ConsoleControlId.MAIN_PROGRESS_6, 0.29D, 0.76D, 0.34D, 0.82D);
        add(result, panel, ConsoleControlId.MAIN_PROGRESS_7, 0.37D, 0.81D, 0.42D, 0.87D);
        add(result, panel, ConsoleControlId.MAIN_SPEED, 0.35D, 0.36D, 0.56D, 0.74D);

        // The handbrake and the right-hand landing controls follow the sketch.
        add(result, panel, ConsoleControlId.MAIN_QUANTUM_HANDBRAKE, 0.07D, 0.52D, 0.28D, 0.78D);
        add(result, panel, ConsoleControlId.MAIN_LANDING_MODE, 0.60D, 0.27D, 0.69D, 0.39D);
        add(result, panel, ConsoleControlId.MAIN_LANDING_PAD, 0.60D, 0.44D, 0.69D, 0.56D);
        add(result, panel, ConsoleControlId.MAIN_DRIVE_MODE, 0.60D, 0.61D, 0.69D, 0.73D);
        add(result, panel, ConsoleControlId.MAIN_EVENT_SWITCH_1, 0.73D, 0.24D, 0.91D, 0.36D);
        add(result, panel, ConsoleControlId.MAIN_DIRECTION, 0.73D, 0.49D, 0.93D, 0.82D);

        return Collections.unmodifiableList(result);
    }

    private static List<ConsoleHitRegion> navigationRegions() {
        List<ConsoleHitRegion> result = new ArrayList<>();
        ConsolePanel panel = ConsolePanel.WEST_NAVIGATION;

        add(result, panel, ConsoleControlId.NAV_AXIS, 0.03D, 0.04D, 0.18D, 0.16D);
        add(result, panel, ConsoleControlId.NAV_SCALE_CONTROL, 0.20D, 0.04D, 0.31D, 0.16D);
        add(result, panel, ConsoleControlId.NAV_SCALE_RANGE, 0.33D, 0.04D, 0.44D, 0.16D);
        add(result, panel, ConsoleControlId.NAV_SIGN, 0.46D, 0.04D, 0.57D, 0.16D);

        add(result, panel, ConsoleControlId.NAV_DIGIT_1, 0.03D, 0.19D, 0.14D, 0.34D);
        add(result, panel, ConsoleControlId.NAV_DIGIT_2, 0.16D, 0.19D, 0.27D, 0.34D);
        add(result, panel, ConsoleControlId.NAV_DIGIT_3, 0.29D, 0.19D, 0.40D, 0.34D);
        add(result, panel, ConsoleControlId.NAV_DIGIT_4, 0.42D, 0.19D, 0.57D, 0.34D);

        add(result, panel, ConsoleControlId.NAV_RESET, 0.03D, 0.37D, 0.14D, 0.52D);
        add(result, panel, ConsoleControlId.NAV_PREVIOUS, 0.16D, 0.37D, 0.27D, 0.52D);
        add(result, panel, ConsoleControlId.NAV_RANDOM, 0.29D, 0.37D, 0.40D, 0.52D);
        add(result, panel, ConsoleControlId.NAV_TEMPORAL_PRIMER, 0.42D, 0.37D, 0.57D, 0.52D);

        add(result, panel, ConsoleControlId.NAV_WAYPOINT_WRITE_READ, 0.03D, 0.55D, 0.17D, 0.70D);
        add(result, panel, ConsoleControlId.NAV_WAYPOINT_CURRENT_DESTINATION, 0.19D, 0.55D, 0.33D, 0.70D);
        add(result, panel, ConsoleControlId.NAV_ROTOR_SCREEN, 0.35D, 0.55D, 0.57D, 0.70D);
        add(result, panel, ConsoleControlId.NAV_EVENT_BUTTON_1, 0.03D, 0.74D, 0.17D, 0.91D);
        add(result, panel, ConsoleControlId.NAV_EVENT_SWITCH_1, 0.19D, 0.74D, 0.33D, 0.91D);

        ConsoleControlId[] waypointControls = {
            ConsoleControlId.NAV_WAYPOINT_01,
            ConsoleControlId.NAV_WAYPOINT_02,
            ConsoleControlId.NAV_WAYPOINT_03,
            ConsoleControlId.NAV_WAYPOINT_04,
            ConsoleControlId.NAV_WAYPOINT_05,
            ConsoleControlId.NAV_WAYPOINT_06,
            ConsoleControlId.NAV_WAYPOINT_07,
            ConsoleControlId.NAV_WAYPOINT_08,
            ConsoleControlId.NAV_WAYPOINT_09,
            ConsoleControlId.NAV_WAYPOINT_10,
            ConsoleControlId.NAV_WAYPOINT_11,
            ConsoleControlId.NAV_WAYPOINT_12,
            ConsoleControlId.NAV_WAYPOINT_13,
            ConsoleControlId.NAV_WAYPOINT_14,
            ConsoleControlId.NAV_WAYPOINT_15,
            ConsoleControlId.NAV_WAYPOINT_16,
            ConsoleControlId.NAV_WAYPOINT_17,
            ConsoleControlId.NAV_WAYPOINT_18,
            ConsoleControlId.NAV_WAYPOINT_19,
            ConsoleControlId.NAV_WAYPOINT_20,
            ConsoleControlId.NAV_WAYPOINT_21,
            ConsoleControlId.NAV_WAYPOINT_22,
            ConsoleControlId.NAV_WAYPOINT_23,
            ConsoleControlId.NAV_WAYPOINT_24,
            ConsoleControlId.NAV_WAYPOINT_25
        };
        double gridMinU = 0.61D;
        double gridMaxU = 0.97D;
        double gridMinV = 0.06D;
        double gridMaxV = 0.94D;
        double cellWidth = (gridMaxU - gridMinU) / 5.0D;
        double cellHeight = (gridMaxV - gridMinV) / 5.0D;
        double gap = 0.006D;
        for (int slot = 0; slot < waypointControls.length; slot++) {
            int row = slot / 5;
            int column = slot % 5;
            double minU = gridMinU + column * cellWidth + gap;
            double maxU = gridMinU + (column + 1) * cellWidth - gap;
            double minV = gridMinV + row * cellHeight + gap;
            double maxV = gridMinV + (row + 1) * cellHeight - gap;
            add(result, panel, waypointControls[slot], minU, minV, maxU, maxV);
        }
        return Collections.unmodifiableList(result);
    }

    private static void add(
        List<ConsoleHitRegion> result,
        ConsolePanel panel,
        ConsoleControlId control,
        double minU,
        double minV,
        double maxU,
        double maxV
    ) {
        if (control.getPanel() != panel) {
            throw new IllegalArgumentException(
                "Control " + control.getId() + " was placed on the wrong panel hit map"
            );
        }
        result.add(new ConsoleHitRegion(minU, minV, maxU, maxV, control));
    }

    private static List<ConsoleHitRegion> grid(
        ConsolePanel panel,
        ConsoleControlId[][] controls
    ) {
        if (controls.length != GRID_SIZE) {
            throw new IllegalArgumentException("Console hit grid must contain exactly four rows");
        }
        List<ConsoleHitRegion> result = new ArrayList<>();
        double step = 1.0D / GRID_SIZE;
        for (int row = 0; row < GRID_SIZE; row++) {
            if (controls[row].length != GRID_SIZE) {
                throw new IllegalArgumentException("Console hit grid rows must contain exactly four columns");
            }
            for (int column = 0; column < GRID_SIZE; column++) {
                ConsoleControlId control = controls[row][column];
                if (control == null) {
                    continue;
                }
                add(
                    result,
                    panel,
                    control,
                    column * step,
                    row * step,
                    (column + 1) * step,
                    (row + 1) * step
                );
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Converts world-block X/Z hit fractions into left-to-right / front-to-back
     * coordinates from the player's side of the panel.
     */
    private static double[] toLocalTopCoordinates(EnumFacing outward, double hitX, double hitZ) {
        switch (outward) {
            case SOUTH:
                return new double[] {hitX, 1.0D - hitZ};
            case NORTH:
                return new double[] {1.0D - hitX, hitZ};
            case WEST:
                return new double[] {hitZ, hitX};
            case EAST:
                return new double[] {1.0D - hitZ, 1.0D - hitX};
            default:
                return new double[] {hitX, hitZ};
        }
    }

    private static double clampUnit(double value) {
        return Math.max(0.0D, Math.min(0.999999D, value));
    }
}
