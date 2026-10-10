package dev.zoldmaki.renewedtardis.console;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Stable logical control IDs. Saved data and interaction routing use these IDs
 * rather than Blockbench/model-part names so the final console model can be
 * replaced without invalidating TARDIS records.
 */
public enum ConsoleControlId {
    // West / navigation
    NAV_AXIS("nav.axis", "X / Y / Z selector", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.SLIDER, 3, false, false, true, false, 0),
    NAV_SCALE_CONTROL("nav.scale_control", "Scale control", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 2, false, true, true, false, 0),
    NAV_SCALE_RANGE("nav.scale_range", "Local / global scale", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.PULL_SWITCH, 2, false, true, true, false, 0),
    NAV_SIGN("nav.sign", "Positive / negative", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.PULL_SWITCH, 2, false, true, true, false, 0),
    NAV_DIGIT_1("nav.digit_1", "Coordinate lever 1", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.LEVER, 10, false, false, true, false, 0),
    NAV_DIGIT_2("nav.digit_2", "Coordinate lever 2", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.LEVER, 10, false, false, true, false, 0),
    NAV_DIGIT_3("nav.digit_3", "Coordinate lever 3", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.LEVER, 10, false, false, true, false, 0),
    NAV_DIGIT_4("nav.digit_4", "Coordinate / dimension lever 4", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.LEVER, 10, false, false, true, false, 0),
    NAV_RESET("nav.reset", "Reset to exterior position", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_PREVIOUS("nav.previous", "Previous materialized position", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_RANDOM("nav.random", "Random destination", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_TEMPORAL_PRIMER("nav.temporal_primer", "Temporal Primer", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, false, false, false, false, 1),
    NAV_WAYPOINT_WRITE_READ("nav.waypoint_write_read", "Waypoint write / read", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.PULL_SWITCH, 2, false, true, false, false, 0),
    NAV_WAYPOINT_CURRENT_DESTINATION("nav.waypoint_current_destination", "Waypoint current / destination", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.PULL_SWITCH, 2, false, true, false, false, 0),
    NAV_WAYPOINT_01("nav.waypoint_01", "Waypoint 1", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_02("nav.waypoint_02", "Waypoint 2", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_03("nav.waypoint_03", "Waypoint 3", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_04("nav.waypoint_04", "Waypoint 4", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_05("nav.waypoint_05", "Waypoint 5", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_06("nav.waypoint_06", "Waypoint 6", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_07("nav.waypoint_07", "Waypoint 7", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_08("nav.waypoint_08", "Waypoint 8", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_09("nav.waypoint_09", "Waypoint 9", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_10("nav.waypoint_10", "Waypoint 10", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_11("nav.waypoint_11", "Waypoint 11", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_12("nav.waypoint_12", "Waypoint 12", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_13("nav.waypoint_13", "Waypoint 13", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_14("nav.waypoint_14", "Waypoint 14", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_15("nav.waypoint_15", "Waypoint 15", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_16("nav.waypoint_16", "Waypoint 16", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_17("nav.waypoint_17", "Waypoint 17", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_18("nav.waypoint_18", "Waypoint 18", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_19("nav.waypoint_19", "Waypoint 19", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_20("nav.waypoint_20", "Waypoint 20", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_21("nav.waypoint_21", "Waypoint 21", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_22("nav.waypoint_22", "Waypoint 22", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_23("nav.waypoint_23", "Waypoint 23", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_24("nav.waypoint_24", "Waypoint 24", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_WAYPOINT_25("nav.waypoint_25", "Waypoint 25", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, true, true, true, false, 0),
    NAV_ROTOR_SCREEN("nav.rotor_screen", "Navigation readout", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.SCREEN, 1, false, false, false, false, 0),
    NAV_EVENT_BUTTON_1("nav.event_button_1", "Navigation flight button 1", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, false, true, false, false, 0),
    NAV_EVENT_BUTTON_2("nav.event_button_2", "Navigation flight button 2", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, false, true, false, false, 0),
    NAV_EVENT_BUTTON_3("nav.event_button_3", "Navigation flight button 3", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.BUTTON, 1, false, true, false, false, 0),
    NAV_EVENT_SWITCH_1("nav.event_switch_1", "Navigation flight switch 1", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.PULL_SWITCH, 2, false, true, false, false, 0),
    NAV_EVENT_SWITCH_2("nav.event_switch_2", "Navigation flight switch 2", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.PULL_SWITCH, 2, false, true, false, false, 0),
    NAV_EVENT_SWITCH_3("nav.event_switch_3", "Navigation flight switch 3", ConsolePanel.WEST_NAVIGATION, ConsoleControlKind.PULL_SWITCH, 2, false, true, false, false, 0),

    // South / main flight
    MAIN_QUANTUM_HANDBRAKE("main.quantum_handbrake", "Quantum Handbrake", ConsolePanel.SOUTH_MAIN_FLIGHT, ConsoleControlKind.LEVER, 2, false, false, false, false, 4),
    MAIN_SPEED("main.speed", "Flight speed", ConsolePanel.SOUTH_MAIN_FLIGHT, ConsoleControlKind.LEVER, 3, false, false, true, false, 0),
    MAIN_LANDING_MODE("main.landing_mode", "Landing mode", ConsolePanel.SOUTH_MAIN_FLIGHT, ConsoleControlKind.LEVER, 3, false, false, true, false, 0),
    MAIN_LANDING_PAD("main.landing_pad", "Landing pads", ConsolePanel.SOUTH_MAIN_FLIGHT, ConsoleControlKind.PULL_SWITCH, 2, false, true, true, false, 0),
    MAIN_DIRECTION("main.direction", "Exterior direction", ConsolePanel.SOUTH_MAIN_FLIGHT, ConsoleControlKind.DIAL, 4, false, false, true, false, 0),
    MAIN_DRIVE_MODE("main.drive_mode", "Coordinated / uncoordinated flight", ConsolePanel.SOUTH_MAIN_FLIGHT, ConsoleControlKind.PULL_SWITCH, 2, false, true, true, false, 0),
    MAIN_SHIELD_GAUGE("main.shield_gauge", "Shield gauge", ConsolePanel.SOUTH_MAIN_FLIGHT, ConsoleControlKind.GAUGE, 1, false, false, false, false, 0),
    MAIN_ARTRON_GAUGE("main.artron_gauge", "Artron gauge", ConsolePanel.SOUTH_MAIN_FLIGHT, ConsoleControlKind.GAUGE, 1, false, false, false, false, 0),
    MAIN_XP_GAUGE("main.xp_gauge", "TARDIS experience gauge", ConsolePanel.SOUTH_MAIN_FLIGHT, ConsoleControlKind.GAUGE, 1, false, false, false, false, 0),
    MAIN_PROGRESS_1("main.progress_1", "Flight progress light 1", ConsolePanel.SOUTH_MAIN_FLIGHT, ConsoleControlKind.INDICATOR, 1, false, false, false, false, 0),
    MAIN_PROGRESS_2("main.progress_2", "Flight progress light 2", ConsolePanel.SOUTH_MAIN_FLIGHT, ConsoleControlKind.INDICATOR, 1, false, false, false, false, 0),
    MAIN_PROGRESS_3("main.progress_3", "Flight progress light 3", ConsolePanel.SOUTH_MAIN_FLIGHT, ConsoleControlKind.INDICATOR, 1, false, false, false, false, 0),
    MAIN_PROGRESS_4("main.progress_4", "Flight progress light 4", ConsolePanel.SOUTH_MAIN_FLIGHT, ConsoleControlKind.INDICATOR, 1, false, false, false, false, 0),
    MAIN_PROGRESS_5("main.progress_5", "Flight progress light 5", ConsolePanel.SOUTH_MAIN_FLIGHT, ConsoleControlKind.INDICATOR, 1, false, false, false, false, 0),
    MAIN_PROGRESS_6("main.progress_6", "Flight progress light 6", ConsolePanel.SOUTH_MAIN_FLIGHT, ConsoleControlKind.INDICATOR, 1, false, false, false, false, 0),
    MAIN_PROGRESS_7("main.progress_7", "Flight progress light 7", ConsolePanel.SOUTH_MAIN_FLIGHT, ConsoleControlKind.INDICATOR, 1, false, false, false, false, 0),
    MAIN_EVENT_BUTTON_1("main.event_button_1", "Main flight button 1", ConsolePanel.SOUTH_MAIN_FLIGHT, ConsoleControlKind.BUTTON, 1, false, true, false, false, 0),
    MAIN_EVENT_BUTTON_2("main.event_button_2", "Main flight button 2", ConsolePanel.SOUTH_MAIN_FLIGHT, ConsoleControlKind.BUTTON, 1, false, true, false, false, 0),
    MAIN_EVENT_SWITCH_1("main.event_switch_1", "Main flight switch 1", ConsolePanel.SOUTH_MAIN_FLIGHT, ConsoleControlKind.PULL_SWITCH, 2, false, true, false, false, 0),

    // East / utility
    UTILITY_EXTERNAL_SCANNER("utility.external_scanner", "External scanner", ConsolePanel.EAST_UTILITY, ConsoleControlKind.SCREEN, 6, false, false, false, false, 0),
    UTILITY_DOOR_LOCK("utility.door_lock", "Door lock", ConsolePanel.EAST_UTILITY, ConsoleControlKind.BUTTON, 2, false, true, false, false, 0),
    UTILITY_INTERNAL_TIME("utility.internal_time", "Internal time", ConsolePanel.EAST_UTILITY, ConsoleControlKind.BUTTON, 3, false, true, false, false, 0),
    UTILITY_SHELL_SCREEN("utility.shell_screen", "Shell selection", ConsolePanel.EAST_UTILITY, ConsoleControlKind.SCREEN, 2, false, false, false, false, 0),
    UTILITY_SHELL_PREVIOUS("utility.shell_previous", "Previous shell", ConsolePanel.EAST_UTILITY, ConsoleControlKind.BUTTON, 1, false, false, false, true, 0),
    UTILITY_SHELL_NEXT("utility.shell_next", "Next shell", ConsolePanel.EAST_UTILITY, ConsoleControlKind.BUTTON, 1, false, false, false, true, 0),
    UTILITY_SHELL_APPLY("utility.shell_apply", "Apply shell on materialization", ConsolePanel.EAST_UTILITY, ConsoleControlKind.BUTTON, 1, false, true, true, false, 0),
    UTILITY_ABSOLUTE_RELATIVE("utility.absolute_relative", "Absolute / relative coordinates", ConsolePanel.EAST_UTILITY, ConsoleControlKind.PULL_SWITCH, 2, false, true, true, false, 0),
    UTILITY_SPATIAL_LINK("utility.spatial_link", "Spatial Link", ConsolePanel.EAST_UTILITY, ConsoleControlKind.BUTTON, 1, false, false, false, false, 3),
    UTILITY_EVENT_BUTTON_1("utility.event_button_1", "Utility flight button 1", ConsolePanel.EAST_UTILITY, ConsoleControlKind.BUTTON, 1, false, true, false, false, 0),
    UTILITY_EVENT_BUTTON_2("utility.event_button_2", "Utility flight button 2", ConsolePanel.EAST_UTILITY, ConsoleControlKind.BUTTON, 1, false, true, false, false, 0),
    UTILITY_EVENT_SWITCH_1("utility.event_switch_1", "Utility flight switch 1", ConsolePanel.EAST_UTILITY, ConsoleControlKind.PULL_SWITCH, 2, false, true, false, false, 0),
    UTILITY_EVENT_SWITCH_2("utility.event_switch_2", "Utility flight switch 2", ConsolePanel.EAST_UTILITY, ConsoleControlKind.PULL_SWITCH, 2, false, true, false, false, 0),

    // North / schematics and rooms
    ROOMS_DELETE_ALL("rooms.delete_all", "Delete all expandable rooms", ConsolePanel.NORTH_ROOMS, ConsoleControlKind.BUTTON, 1, true, true, false, false, 0),
    ROOMS_SONIC_SLOT("rooms.sonic_slot", "Sonic screwdriver slot", ConsolePanel.NORTH_ROOMS, ConsoleControlKind.SLOT, 1, false, false, false, false, 0),
    ROOMS_SONIC_GENERATE("rooms.sonic_generate", "Generate / return sonic", ConsolePanel.NORTH_ROOMS, ConsoleControlKind.BUTTON, 1, false, true, false, false, 0),
    ROOMS_TYPE_PREVIOUS("rooms.type_previous", "Previous schema type", ConsolePanel.NORTH_ROOMS, ConsoleControlKind.BUTTON, 1, false, false, false, true, 0),
    ROOMS_TYPE_NEXT("rooms.type_next", "Next schema type", ConsolePanel.NORTH_ROOMS, ConsoleControlKind.BUTTON, 1, false, false, false, true, 0),
    ROOMS_SCHEMA_PREVIOUS("rooms.schema_previous", "Previous room", ConsolePanel.NORTH_ROOMS, ConsoleControlKind.BUTTON, 1, false, false, false, true, 0),
    ROOMS_SCHEMA_NEXT("rooms.schema_next", "Next room", ConsolePanel.NORTH_ROOMS, ConsoleControlKind.BUTTON, 1, false, false, false, true, 0),
    ROOMS_TYPE_SCREEN("rooms.type_screen", "Schema type screen", ConsolePanel.NORTH_ROOMS, ConsoleControlKind.SCREEN, 1, false, false, false, false, 0),
    ROOMS_SCHEMA_SCREEN("rooms.schema_screen", "Room selection screen", ConsolePanel.NORTH_ROOMS, ConsoleControlKind.SCREEN, 1, false, false, false, false, 0),
    ROOMS_COUNT_GAUGE("rooms.count_gauge", "Room count gauge", ConsolePanel.NORTH_ROOMS, ConsoleControlKind.GAUGE, 1, false, false, false, false, 0),
    ROOMS_HELMIC_REGULATOR("rooms.helmic_regulator", "Helmic Regulator", ConsolePanel.NORTH_ROOMS, ConsoleControlKind.BUTTON, 1, false, false, false, false, 2),
    ROOMS_EVENT_SWITCH_1("rooms.event_switch_1", "Room-panel flight switch 1", ConsolePanel.NORTH_ROOMS, ConsoleControlKind.PULL_SWITCH, 2, false, true, false, false, 0);

    private static final Map<String, ConsoleControlId> BY_ID;

    static {
        Map<String, ConsoleControlId> byId = new LinkedHashMap<>();
        for (ConsoleControlId control : values()) {
            ConsoleControlId previous = byId.put(control.id, control);
            if (previous != null) {
                throw new IllegalStateException("Duplicate console control ID: " + control.id);
            }
        }
        BY_ID = Collections.unmodifiableMap(byId);
    }

    private final String id;
    private final String displayName;
    private final ConsolePanel panel;
    private final ConsoleControlKind kind;
    private final int stateCount;
    private final boolean sneakRequired;
    private final boolean quickTimeEligible;
    private final boolean resetsTakeoffSequence;
    private final boolean selectionControl;
    private final int takeoffSequenceStep;

    ConsoleControlId(
        String id,
        String displayName,
        ConsolePanel panel,
        ConsoleControlKind kind,
        int stateCount,
        boolean sneakRequired,
        boolean quickTimeEligible,
        boolean resetsTakeoffSequence,
        boolean selectionControl,
        int takeoffSequenceStep
    ) {
        this.id = id;
        this.displayName = displayName;
        this.panel = panel;
        this.kind = kind;
        this.stateCount = Math.max(1, stateCount);
        this.sneakRequired = sneakRequired;
        this.quickTimeEligible = quickTimeEligible;
        this.resetsTakeoffSequence = resetsTakeoffSequence;
        this.selectionControl = selectionControl;
        this.takeoffSequenceStep = Math.max(0, Math.min(4, takeoffSequenceStep));
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ConsolePanel getPanel() {
        return panel;
    }

    public ConsoleControlKind getKind() {
        return kind;
    }

    public int getStateCount() {
        return stateCount;
    }

    public boolean isSneakRequired() {
        return sneakRequired;
    }

    public boolean isQuickTimeEligible() {
        return quickTimeEligible;
    }

    public boolean resetsTakeoffSequence() {
        return resetsTakeoffSequence;
    }

    public boolean isSelectionControl() {
        return selectionControl;
    }

    public int getTakeoffSequenceStep() {
        return takeoffSequenceStep;
    }

    public boolean isTakeoffSequenceControl() {
        return takeoffSequenceStep > 0;
    }

    public boolean isWaypointButton() {
        return ordinal() >= NAV_WAYPOINT_01.ordinal() && ordinal() <= NAV_WAYPOINT_25.ordinal();
    }

    /** Returns a zero-based waypoint slot index, or -1 for non-waypoint controls. */
    public int getWaypointSlotIndex() {
        return isWaypointButton() ? ordinal() - NAV_WAYPOINT_01.ordinal() : -1;
    }

    public int clampState(int state) {
        return Math.max(0, Math.min(stateCount - 1, state));
    }

    @Nullable
    public static ConsoleControlId fromId(String id) {
        if (id == null) {
            return null;
        }
        return BY_ID.get(id.trim().toLowerCase(Locale.ROOT));
    }

    public static Map<String, ConsoleControlId> byId() {
        return BY_ID;
    }
}
