package dev.zoldmaki.renewedtardis.console;

import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants;

/**
 * Persistent per-TARDIS physical-console state.
 *
 * <p>Control values are stored under stable string IDs. Unknown IDs are kept
 * when loading so a record does not destroy state written by a newer build.</p>
 *
 * <p>The four physical navigation levers are banked by selected axis and by
 * local/global range. This mirrors the real console: changing X/Y/Z or scale
 * range reveals the remembered values for that selection rather than reusing
 * one shared set of four digits.</p>
 */
public final class TardisConsoleState {
    public static final int WAYPOINT_COUNT = 25;
    public static final int MAX_WAYPOINT_NAME_LENGTH = 32;

    private static final String TAG_STATES = "States";
    private static final String TAG_ID = "Id";
    private static final String TAG_VALUE = "Value";
    private static final String TAG_SEQUENCE = "TakeoffSequence";
    private static final String TAG_NAV_DIMENSION = "NavigationDimension";
    private static final String TAG_PREVIOUS_MATERIALIZED = "PreviousMaterialized";
    private static final String TAG_PREVIOUS_MATERIALIZED_FACING = "PreviousMaterializedFacing";
    private static final String TAG_WAYPOINTS = "Waypoints";
    private static final String TAG_WAYPOINT_SLOT = "Slot";
    private static final String TAG_WAYPOINT_DATA = "Data";
    private static final String TAG_WAYPOINT_NAME = "Name";
    private static final String TAG_WAYPOINT_COLOR = "Color";

    private static final String NAV_BANK_PREFIX = "nav.bank.";
    private static final String NAV_SIGN_PREFIX = "nav.sign.";

    private final Map<String, Integer> states;
    private final TardisWaypoint[] waypoints;
    private final String[] waypointNames;
    private final int[] waypointColors;
    private int takeoffSequenceProgress;
    private Integer navigationDimensionId;
    @Nullable
    private DimensionPosition previousMaterializedPosition;
    private EnumFacing previousMaterializedFacing;

    public TardisConsoleState() {
        this(
            Collections.<String, Integer>emptyMap(),
            0,
            null,
            null,
            EnumFacing.NORTH,
            null,
            null,
            null
        );
    }

    private TardisConsoleState(
        Map<String, Integer> states,
        int takeoffSequenceProgress,
        Integer navigationDimensionId,
        @Nullable DimensionPosition previousMaterializedPosition,
        EnumFacing previousMaterializedFacing,
        @Nullable TardisWaypoint[] waypoints,
        @Nullable String[] waypointNames,
        @Nullable int[] waypointColors
    ) {
        this.states = new LinkedHashMap<>();
        if (states != null) {
            for (Map.Entry<String, Integer> entry : states.entrySet()) {
                String id = entry.getKey();
                Integer value = entry.getValue();
                if (id == null || id.trim().isEmpty() || value == null || value < 0) {
                    continue;
                }
                ConsoleControlId known = ConsoleControlId.fromId(id);
                String savedId = known == null ? id : known.getId();
                int savedValue = known == null ? value : known.clampState(value);
                if (known != null && savedValue == 0) {
                    continue;
                }
                this.states.put(savedId, savedValue);
            }
        }
        this.takeoffSequenceProgress = clampSequence(takeoffSequenceProgress);
        this.navigationDimensionId = navigationDimensionId;
        this.previousMaterializedPosition = previousMaterializedPosition;
        this.previousMaterializedFacing = normalizeHorizontal(previousMaterializedFacing);
        this.waypoints = new TardisWaypoint[WAYPOINT_COUNT];
        this.waypointNames = new String[WAYPOINT_COUNT];
        this.waypointColors = new int[WAYPOINT_COUNT];
        for (int index = 0; index < WAYPOINT_COUNT; index++) {
            this.waypointColors[index] = -1;
            if (waypoints != null && index < waypoints.length) {
                this.waypoints[index] = waypoints[index];
            }
            if (waypointNames != null && index < waypointNames.length) {
                String name = sanitizeWaypointName(waypointNames[index]);
                this.waypointNames[index] = name.isEmpty() || defaultWaypointName(index).equals(name) ? null : name;
            }
            if (waypointColors != null && index < waypointColors.length) {
                this.waypointColors[index] = clampWaypointColor(waypointColors[index]);
            }
        }
        migrateSharedNavigationControls();
    }

    public int getControlState(ConsoleControlId control) {
        if (control == null) {
            return 0;
        }
        int digitIndex = navigationDigitIndex(control);
        if (digitIndex >= 0) {
            int axis = getRawControlState(ConsoleControlId.NAV_AXIS);
            int range = getRawControlState(ConsoleControlId.NAV_SCALE_RANGE);
            return getNavigationDigit(axis, range, digitIndex);
        }
        if (control == ConsoleControlId.NAV_SIGN) {
            return getNavigationSign(getRawControlState(ConsoleControlId.NAV_AXIS));
        }
        return getRawControlState(control);
    }

    public Map<String, Integer> getSavedStates() {
        return Collections.unmodifiableMap(states);
    }

    public int getTakeoffSequenceProgress() {
        return takeoffSequenceProgress;
    }

    public boolean hasNavigationDimension() {
        return navigationDimensionId != null;
    }

    public int getNavigationDimensionId(int fallback) {
        return navigationDimensionId == null ? fallback : navigationDimensionId;
    }

    public boolean setNavigationDimensionId(int dimensionId) {
        if (navigationDimensionId != null && navigationDimensionId == dimensionId) {
            return false;
        }
        navigationDimensionId = dimensionId;
        return true;
    }

    @Nullable
    public DimensionPosition getPreviousMaterializedPosition() {
        return previousMaterializedPosition;
    }

    public EnumFacing getPreviousMaterializedFacing() {
        return previousMaterializedFacing;
    }

    public boolean setPreviousMaterializedPosition(
        @Nullable DimensionPosition position,
        EnumFacing facing
    ) {
        EnumFacing normalizedFacing = normalizeHorizontal(facing);
        if (samePosition(previousMaterializedPosition, position)
            && previousMaterializedFacing == normalizedFacing) {
            return false;
        }
        previousMaterializedPosition = position;
        previousMaterializedFacing = normalizedFacing;
        return true;
    }

    @Nullable
    public TardisWaypoint getWaypoint(int slotIndex) {
        return isWaypointSlot(slotIndex) ? waypoints[slotIndex] : null;
    }

    public boolean setWaypoint(int slotIndex, @Nullable TardisWaypoint waypoint) {
        if (!isWaypointSlot(slotIndex) || sameWaypoint(waypoints[slotIndex], waypoint)) {
            return false;
        }
        waypoints[slotIndex] = waypoint;
        return true;
    }

    public String getWaypointName(int slotIndex) {
        if (!isWaypointSlot(slotIndex)) {
            return "?";
        }
        String custom = waypointNames[slotIndex];
        return custom == null || custom.isEmpty() ? defaultWaypointName(slotIndex) : custom;
    }

    public boolean setWaypointName(int slotIndex, @Nullable String name) {
        if (!isWaypointSlot(slotIndex)) {
            return false;
        }
        String sanitized = sanitizeWaypointName(name);
        String stored = sanitized.isEmpty() || defaultWaypointName(slotIndex).equals(sanitized)
            ? null
            : sanitized;
        String current = waypointNames[slotIndex];
        if (current == null ? stored == null : current.equals(stored)) {
            return false;
        }
        waypointNames[slotIndex] = stored;
        return true;
    }

    /** Returns dye damage 0-15, or -1 when the slot uses its default appearance. */
    public int getWaypointColor(int slotIndex) {
        return isWaypointSlot(slotIndex) ? waypointColors[slotIndex] : -1;
    }

    public boolean setWaypointColor(int slotIndex, int dyeDamage) {
        if (!isWaypointSlot(slotIndex)) {
            return false;
        }
        int clamped = clampWaypointColor(dyeDamage);
        if (waypointColors[slotIndex] == clamped) {
            return false;
        }
        waypointColors[slotIndex] = clamped;
        return true;
    }

    public int getNavigationDigit(int axis, int range, int digitIndex) {
        String key = navigationDigitKey(axis, range, digitIndex);
        Integer value = states.get(key);
        return value == null ? 0 : clampDigit(value);
    }

    public boolean setNavigationDigit(int axis, int range, int digitIndex, int value) {
        String key = navigationDigitKey(axis, range, digitIndex);
        int clamped = clampDigit(value);
        Integer current = states.get(key);
        if ((current == null ? 0 : clampDigit(current)) == clamped) {
            return false;
        }
        if (clamped == 0) {
            states.remove(key);
        } else {
            states.put(key, clamped);
        }
        return true;
    }

    public int getNavigationSign(int axis) {
        Integer value = states.get(navigationSignKey(axis));
        return value == null ? 0 : Math.max(0, Math.min(1, value));
    }

    public boolean setNavigationSign(int axis, int value) {
        String key = navigationSignKey(axis);
        int clamped = Math.max(0, Math.min(1, value));
        Integer current = states.get(key);
        if ((current == null ? 0 : Math.max(0, Math.min(1, current))) == clamped) {
            return false;
        }
        if (clamped == 0) {
            states.remove(key);
        } else {
            states.put(key, clamped);
        }
        return true;
    }

    public boolean setControlState(ConsoleControlId control, int state) {
        if (control == null) {
            return false;
        }
        int digitIndex = navigationDigitIndex(control);
        if (digitIndex >= 0) {
            int axis = getRawControlState(ConsoleControlId.NAV_AXIS);
            int range = getRawControlState(ConsoleControlId.NAV_SCALE_RANGE);
            return setNavigationDigit(axis, range, digitIndex, state);
        }
        if (control == ConsoleControlId.NAV_SIGN) {
            return setNavigationSign(getRawControlState(ConsoleControlId.NAV_AXIS), state);
        }
        return setRawControlState(control, state);
    }

    public boolean setTakeoffSequenceProgress(int progress) {
        int clamped = clampSequence(progress);
        if (takeoffSequenceProgress == clamped) {
            return false;
        }
        takeoffSequenceProgress = clamped;
        return true;
    }

    public boolean resetTakeoffSequence() {
        return setTakeoffSequenceProgress(0);
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList stateList = new NBTTagList();
        for (Map.Entry<String, Integer> entry : states.entrySet()) {
            NBTTagCompound stateTag = new NBTTagCompound();
            stateTag.setString(TAG_ID, entry.getKey());
            stateTag.setInteger(TAG_VALUE, entry.getValue());
            stateList.appendTag(stateTag);
        }
        tag.setTag(TAG_STATES, stateList);
        tag.setInteger(TAG_SEQUENCE, takeoffSequenceProgress);
        if (navigationDimensionId != null) {
            tag.setInteger(TAG_NAV_DIMENSION, navigationDimensionId);
        }
        if (previousMaterializedPosition != null) {
            tag.setTag(TAG_PREVIOUS_MATERIALIZED, previousMaterializedPosition.writeToNBT());
            tag.setInteger(
                TAG_PREVIOUS_MATERIALIZED_FACING,
                previousMaterializedFacing.getHorizontalIndex()
            );
        }

        NBTTagList waypointList = new NBTTagList();
        for (int slot = 0; slot < WAYPOINT_COUNT; slot++) {
            TardisWaypoint waypoint = waypoints[slot];
            String customName = waypointNames[slot];
            int color = waypointColors[slot];
            if (waypoint == null && customName == null && color < 0) {
                continue;
            }
            NBTTagCompound waypointTag = new NBTTagCompound();
            waypointTag.setInteger(TAG_WAYPOINT_SLOT, slot);
            if (waypoint != null) {
                waypointTag.setTag(TAG_WAYPOINT_DATA, waypoint.writeToNBT());
            }
            if (customName != null) {
                waypointTag.setString(TAG_WAYPOINT_NAME, customName);
            }
            if (color >= 0) {
                waypointTag.setInteger(TAG_WAYPOINT_COLOR, color);
            }
            waypointList.appendTag(waypointTag);
        }
        if (waypointList.tagCount() > 0) {
            tag.setTag(TAG_WAYPOINTS, waypointList);
        }
        return tag;
    }

    public static TardisConsoleState readFromNBT(NBTTagCompound tag) {
        if (tag == null) {
            return new TardisConsoleState();
        }
        Map<String, Integer> states = new LinkedHashMap<>();
        if (tag.hasKey(TAG_STATES, Constants.NBT.TAG_LIST)) {
            NBTTagList stateList = tag.getTagList(TAG_STATES, Constants.NBT.TAG_COMPOUND);
            for (int index = 0; index < stateList.tagCount(); index++) {
                NBTTagCompound stateTag = stateList.getCompoundTagAt(index);
                if (!stateTag.hasKey(TAG_ID, Constants.NBT.TAG_STRING)
                    || !stateTag.hasKey(TAG_VALUE, Constants.NBT.TAG_INT)) {
                    continue;
                }
                String id = stateTag.getString(TAG_ID);
                if (!id.trim().isEmpty()) {
                    states.put(id, Math.max(0, stateTag.getInteger(TAG_VALUE)));
                }
            }
        }
        int sequence = tag.hasKey(TAG_SEQUENCE, Constants.NBT.TAG_INT)
            ? tag.getInteger(TAG_SEQUENCE)
            : 0;
        Integer dimension = tag.hasKey(TAG_NAV_DIMENSION, Constants.NBT.TAG_INT)
            ? tag.getInteger(TAG_NAV_DIMENSION)
            : null;
        DimensionPosition previousMaterialized = tag.hasKey(
            TAG_PREVIOUS_MATERIALIZED,
            Constants.NBT.TAG_COMPOUND
        )
            ? DimensionPosition.readFromNBT(tag.getCompoundTag(TAG_PREVIOUS_MATERIALIZED))
            : null;
        EnumFacing previousFacing = tag.hasKey(
            TAG_PREVIOUS_MATERIALIZED_FACING,
            Constants.NBT.TAG_INT
        )
            ? EnumFacing.byHorizontalIndex(tag.getInteger(TAG_PREVIOUS_MATERIALIZED_FACING))
            : EnumFacing.NORTH;

        TardisWaypoint[] waypoints = new TardisWaypoint[WAYPOINT_COUNT];
        String[] waypointNames = new String[WAYPOINT_COUNT];
        int[] waypointColors = new int[WAYPOINT_COUNT];
        for (int index = 0; index < WAYPOINT_COUNT; index++) {
            waypointColors[index] = -1;
        }
        if (tag.hasKey(TAG_WAYPOINTS, Constants.NBT.TAG_LIST)) {
            NBTTagList waypointList = tag.getTagList(TAG_WAYPOINTS, Constants.NBT.TAG_COMPOUND);
            for (int index = 0; index < waypointList.tagCount(); index++) {
                NBTTagCompound waypointTag = waypointList.getCompoundTagAt(index);
                int slot = waypointTag.getInteger(TAG_WAYPOINT_SLOT);
                if (!isWaypointSlot(slot)) {
                    continue;
                }
                if (waypointTag.hasKey(TAG_WAYPOINT_DATA, Constants.NBT.TAG_COMPOUND)) {
                    try {
                        waypoints[slot] = TardisWaypoint.readFromNBT(
                            waypointTag.getCompoundTag(TAG_WAYPOINT_DATA)
                        );
                    } catch (RuntimeException ignored) {
                        // Preserve the owning TARDIS even if one waypoint entry is damaged.
                    }
                }
                if (waypointTag.hasKey(TAG_WAYPOINT_NAME, Constants.NBT.TAG_STRING)) {
                    waypointNames[slot] = waypointTag.getString(TAG_WAYPOINT_NAME);
                }
                if (waypointTag.hasKey(TAG_WAYPOINT_COLOR, Constants.NBT.TAG_INT)) {
                    waypointColors[slot] = clampWaypointColor(
                        waypointTag.getInteger(TAG_WAYPOINT_COLOR)
                    );
                }
            }
        }

        return new TardisConsoleState(
            states,
            sequence,
            dimension,
            previousMaterialized,
            previousFacing,
            waypoints,
            waypointNames,
            waypointColors
        );
    }

    private int getRawControlState(ConsoleControlId control) {
        Integer value = states.get(control.getId());
        return value == null ? 0 : control.clampState(value);
    }

    private boolean setRawControlState(ConsoleControlId control, int state) {
        int clamped = control.clampState(state);
        int current = getRawControlState(control);
        if (current == clamped) {
            return false;
        }
        if (clamped == 0) {
            states.remove(control.getId());
        } else {
            states.put(control.getId(), clamped);
        }
        return true;
    }

    private void migrateSharedNavigationControls() {
        int axis = getRawControlState(ConsoleControlId.NAV_AXIS);
        int range = getRawControlState(ConsoleControlId.NAV_SCALE_RANGE);
        ConsoleControlId[] digits = {
            ConsoleControlId.NAV_DIGIT_1,
            ConsoleControlId.NAV_DIGIT_2,
            ConsoleControlId.NAV_DIGIT_3,
            ConsoleControlId.NAV_DIGIT_4
        };
        for (int index = 0; index < digits.length; index++) {
            Integer oldValue = states.remove(digits[index].getId());
            if (oldValue == null || oldValue <= 0) {
                continue;
            }
            String bankKey = navigationDigitKey(axis, range, index);
            if (!states.containsKey(bankKey)) {
                states.put(bankKey, clampDigit(oldValue));
            }
        }
        Integer oldSign = states.remove(ConsoleControlId.NAV_SIGN.getId());
        if (oldSign != null && oldSign > 0) {
            String signKey = navigationSignKey(axis);
            if (!states.containsKey(signKey)) {
                states.put(signKey, 1);
            }
        }
    }

    private static int navigationDigitIndex(ConsoleControlId control) {
        switch (control) {
            case NAV_DIGIT_1:
                return 0;
            case NAV_DIGIT_2:
                return 1;
            case NAV_DIGIT_3:
                return 2;
            case NAV_DIGIT_4:
                return 3;
            default:
                return -1;
        }
    }

    private static String navigationDigitKey(int axis, int range, int digitIndex) {
        int safeAxis = Math.max(0, Math.min(2, axis));
        int safeRange = safeAxis == 1 ? 0 : Math.max(0, Math.min(1, range));
        int safeDigit = Math.max(0, Math.min(3, digitIndex));
        return NAV_BANK_PREFIX + axisName(safeAxis) + "." + rangeName(safeRange) + ".digit_" + (safeDigit + 1);
    }

    private static String navigationSignKey(int axis) {
        int safeAxis = Math.max(0, Math.min(2, axis));
        return NAV_SIGN_PREFIX + axisName(safeAxis);
    }

    private static String axisName(int axis) {
        return axis == 1 ? "y" : axis == 2 ? "z" : "x";
    }

    private static String rangeName(int range) {
        return range == 1 ? "global" : "local";
    }

    private static int clampDigit(int value) {
        return Math.max(0, Math.min(9, value));
    }

    private static int clampWaypointColor(int value) {
        return value < 0 ? -1 : Math.min(15, value);
    }

    private static boolean isWaypointSlot(int slotIndex) {
        return slotIndex >= 0 && slotIndex < WAYPOINT_COUNT;
    }

    private static String defaultWaypointName(int slotIndex) {
        return Integer.toString(slotIndex + 1);
    }

    private static String sanitizeWaypointName(@Nullable String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim();
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < trimmed.length() && result.length() < MAX_WAYPOINT_NAME_LENGTH; index++) {
            char character = trimmed.charAt(index);
            if (character >= 32 && character != 127) {
                result.append(character);
            }
        }
        return result.toString().trim();
    }

    private static boolean sameWaypoint(
        @Nullable TardisWaypoint first,
        @Nullable TardisWaypoint second
    ) {
        if (first == second) {
            return true;
        }
        return first != null
            && second != null
            && samePosition(first.getPosition(), second.getPosition())
            && first.getFacing() == second.getFacing()
            && first.getLandingMode() == second.getLandingMode()
            && first.usesLandingPad() == second.usesLandingPad();
    }

    private static boolean samePosition(
        @Nullable DimensionPosition first,
        @Nullable DimensionPosition second
    ) {
        if (first == second) {
            return true;
        }
        return first != null
            && second != null
            && first.getDimension() == second.getDimension()
            && first.getPosition().equals(second.getPosition());
    }

    private static EnumFacing normalizeHorizontal(EnumFacing facing) {
        return facing != null && facing.getAxis().isHorizontal() ? facing : EnumFacing.NORTH;
    }

    private static int clampSequence(int progress) {
        return Math.max(0, Math.min(3, progress));
    }
}
