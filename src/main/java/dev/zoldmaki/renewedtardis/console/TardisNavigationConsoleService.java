package dev.zoldmaki.renewedtardis.console;

import dev.zoldmaki.renewedtardis.block.BlockTardisExterior;
import dev.zoldmaki.renewedtardis.config.TardisConfig;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.dimension.InteriorAddress;
import dev.zoldmaki.renewedtardis.dimension.TardisDimensionManager;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisConsolePanel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

/** Functional navigation-console routing for the physical placeholder panel. */
public final class TardisNavigationConsoleService {
    private static final int MAX_HORIZONTAL_COORDINATE = 29999984;
    private static final int MAX_NAVIGATION_MAGNITUDE = 99999999;
    private static final int LOCAL_RANDOM_RADIUS = 9999;

    private TardisNavigationConsoleService() {
    }

    public static boolean handles(ConsoleControlId control) {
        return control == ConsoleControlId.NAV_AXIS
            || control == ConsoleControlId.NAV_SCALE_RANGE
            || control == ConsoleControlId.NAV_SIGN
            || control == ConsoleControlId.NAV_DIGIT_1
            || control == ConsoleControlId.NAV_DIGIT_2
            || control == ConsoleControlId.NAV_DIGIT_3
            || control == ConsoleControlId.NAV_DIGIT_4
            || control == ConsoleControlId.NAV_RESET
            || control == ConsoleControlId.NAV_PREVIOUS
            || control == ConsoleControlId.NAV_RANDOM
            || control == ConsoleControlId.NAV_WAYPOINT_WRITE_READ
            || control == ConsoleControlId.NAV_WAYPOINT_CURRENT_DESTINATION
            || control.isWaypointButton()
            || control == ConsoleControlId.NAV_ROTOR_SCREEN;
    }

    /**
     * Named paper and dyes customize a waypoint without triggering its guarded
     * read/write action. Returns true only when the held item was handled.
     */
    public static boolean tryCustomizeWaypoint(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record,
        ConsoleControlId control,
        EnumHand hand
    ) {
        if (player == null || data == null || record == null || control == null
            || !control.isWaypointButton()) {
            return false;
        }
        ItemStack held = player.getHeldItem(hand == null ? EnumHand.MAIN_HAND : hand);
        if (held.isEmpty()) {
            return false;
        }

        int slot = control.getWaypointSlotIndex();
        TardisConsoleState state = record.getConsoleState();
        if (held.getItem() == Items.PAPER && held.hasDisplayName()) {
            String requested = held.getDisplayName();
            boolean changed = data.setConsoleWaypointName(record.getTardisId(), slot, requested);
            if (changed) {
                syncWaypointPanel(player, record);
                consumeOne(player, held);
                send(
                    player,
                    TextFormatting.GREEN,
                    "Waypoint " + (slot + 1) + " renamed to \"" + state.getWaypointName(slot) + "\"."
                );
            } else {
                send(
                    player,
                    TextFormatting.YELLOW,
                    "Waypoint " + (slot + 1) + " already uses that name."
                );
            }
            return true;
        }

        if (held.getItem() == Items.DYE) {
            int dyeDamage = Math.max(0, Math.min(15, held.getMetadata()));
            boolean changed = data.setConsoleWaypointColor(record.getTardisId(), slot, dyeDamage);
            EnumDyeColor color = EnumDyeColor.byDyeDamage(dyeDamage);
            if (changed) {
                syncWaypointPanel(player, record);
                consumeOne(player, held);
                send(
                    player,
                    TextFormatting.GREEN,
                    "Waypoint \"" + state.getWaypointName(slot) + "\" recolored "
                        + color.getName().replace('_', ' ') + "."
                );
            } else {
                send(
                    player,
                    TextFormatting.YELLOW,
                    "Waypoint \"" + state.getWaypointName(slot) + "\" already uses that color."
                );
            }
            return true;
        }
        return false;
    }

    public static boolean interact(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record,
        ConsoleControlId control
    ) {
        if (!handles(control)) {
            return false;
        }

        TardisConsoleState state = record.getConsoleState();
        if (control == ConsoleControlId.NAV_RESET) {
            return resetToExterior(player, data, record);
        }
        if (control == ConsoleControlId.NAV_PREVIOUS) {
            return loadPreviousMaterialized(player, data, record);
        }
        if (control == ConsoleControlId.NAV_RANDOM) {
            return randomizeDestination(player, data, record);
        }
        if (control == ConsoleControlId.NAV_WAYPOINT_WRITE_READ) {
            int next = state.getControlState(control) == 0 ? 1 : 0;
            data.setConsoleControlState(record.getTardisId(), control, next);
            send(
                player,
                TextFormatting.AQUA,
                next == 0
                    ? "Waypoint grid changed to read mode. Sneak-right-click a saved slot to load it."
                    : "Waypoint grid changed to write mode. Sneak-right-click a slot to save into it."
            );
            return true;
        }
        if (control == ConsoleControlId.NAV_WAYPOINT_CURRENT_DESTINATION) {
            int next = state.getControlState(control) == 0 ? 1 : 0;
            data.setConsoleControlState(record.getTardisId(), control, next);
            send(
                player,
                TextFormatting.AQUA,
                next == 0
                    ? "Waypoint write source: current materialized exterior."
                    : "Waypoint write source: entered destination."
            );
            return true;
        }
        if (control.isWaypointButton()) {
            return interactWaypoint(player, data, record, control.getWaypointSlotIndex());
        }
        if (control == ConsoleControlId.NAV_ROTOR_SCREEN) {
            send(player, TextFormatting.AQUA, describeSelection(record));
            return true;
        }

        int axis = state.getControlState(ConsoleControlId.NAV_AXIS);
        if (control == ConsoleControlId.NAV_DIGIT_4 && axis == 1) {
            return cycleDimension(player, data, record, player.isSneaking());
        }

        int previous = state.getControlState(control);
        int next = nextState(control, previous, player.isSneaking());
        data.setConsoleControlState(record.getTardisId(), control, next);

        if (control == ConsoleControlId.NAV_AXIS) {
            send(player, TextFormatting.AQUA, describeSelection(record));
            return true;
        }
        if (control == ConsoleControlId.NAV_SCALE_RANGE) {
            if (state.getControlState(ConsoleControlId.NAV_AXIS) == 1) {
                send(
                    player,
                    TextFormatting.AQUA,
                    "Y always uses the 1 / 10 / 100 levers; the local/global switch has no numeric effect on Y. "
                        + describeDigits(record)
                );
            } else {
                String range = next == 0 ? "local (1–1,000)" : "global (10,000–10,000,000)";
                send(player, TextFormatting.AQUA, "Navigation scale bank: " + range + ". " + describeDigits(record));
            }
            return true;
        }

        String failure = synchronizeDestination(data, record);
        if (!failure.isEmpty()) {
            data.setConsoleControlState(record.getTardisId(), control, previous);
            send(player, TextFormatting.RED, failure);
            return true;
        }

        send(player, TextFormatting.AQUA, describeSelection(record));
        return true;
    }

    /** Re-encodes the physical lever banks without changing the actual target. */
    public static boolean setRelativeMode(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record,
        int nextMode
    ) {
        DimensionPosition exterior = navigationReferencePosition(record);
        if (exterior == null) {
            send(player, TextFormatting.RED, "Relative coordinates need a materialized or reconstructed travel position.");
            return true;
        }

        DimensionPosition target = record.getDestinationPosition();
        if (target == null) {
            target = exterior;
        }
        int oldMode = record.getConsoleState().getControlState(ConsoleControlId.UTILITY_ABSOLUTE_RELATIVE);
        data.setConsoleControlState(
            record.getTardisId(),
            ConsoleControlId.UTILITY_ABSOLUTE_RELATIVE,
            nextMode
        );
        String failure = encodeTarget(record, target, nextMode != 0);
        if (!failure.isEmpty()) {
            data.setConsoleControlState(
                record.getTardisId(),
                ConsoleControlId.UTILITY_ABSOLUTE_RELATIVE,
                oldMode
            );
            send(player, TextFormatting.RED, failure);
            return true;
        }
        data.setDestination(record.getTardisId(), target, record.getDestinationFacing());
        send(
            player,
            TextFormatting.AQUA,
            nextMode == 0
                ? "Navigation readout changed to absolute coordinates. " + describeSelection(record)
                : "Navigation readout changed to relative coordinates. " + describeSelection(record)
        );
        return true;
    }

    private static boolean resetToExterior(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record
    ) {
        DimensionPosition exterior = navigationReferencePosition(record);
        if (exterior == null) {
            send(player, TextFormatting.RED, "This TARDIS has no materialized or reconstructed travel position to copy.");
            return true;
        }
        boolean relative = isRelative(record);
        String failure = encodeTarget(record, exterior, relative);
        if (!failure.isEmpty()) {
            send(player, TextFormatting.RED, failure);
            return true;
        }
        data.setDestination(record.getTardisId(), exterior, record.getDestinationFacing());
        send(
            player,
            TextFormatting.GOLD,
            "Navigation reset to the exterior position. A valid destination is now prepared: "
                + exterior.toString() + "."
        );
        return true;
    }

    private static boolean loadPreviousMaterialized(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record
    ) {
        TardisConsoleState state = record.getConsoleState();
        DimensionPosition previous = state.getPreviousMaterializedPosition();
        if (previous == null) {
            send(
                player,
                TextFormatting.YELLOW,
                "No previous materialized position has been recorded yet. Complete a flight first."
            );
            return true;
        }
        String failure = prepareTarget(
            data,
            record,
            previous,
            state.getPreviousMaterializedFacing()
        );
        if (!failure.isEmpty()) {
            send(player, TextFormatting.RED, failure);
            return true;
        }
        send(
            player,
            TextFormatting.GOLD,
            "Loaded the previous materialized position: " + previous.toString() + "."
        );
        return true;
    }

    private static boolean interactWaypoint(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record,
        int slot
    ) {
        TardisConsoleState state = record.getConsoleState();
        if (slot < 0 || slot >= TardisConsoleState.WAYPOINT_COUNT) {
            send(player, TextFormatting.RED, "That waypoint slot is outside the physical 5x5 grid.");
            return true;
        }
        return state.getControlState(ConsoleControlId.NAV_WAYPOINT_WRITE_READ) == 0
            ? loadWaypoint(player, data, record, slot)
            : writeWaypoint(player, data, record, slot);
    }

    private static boolean writeWaypoint(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record,
        int slot
    ) {
        TardisConsoleState state = record.getConsoleState();
        boolean saveCurrent = state.getControlState(
            ConsoleControlId.NAV_WAYPOINT_CURRENT_DESTINATION
        ) == 0;

        DimensionPosition position;
        EnumFacing facing;
        String source;
        if (saveCurrent) {
            position = navigationReferencePosition(record);
            if (position == null) {
                send(
                    player,
                    TextFormatting.RED,
                    "Current-position waypoint writing needs a materialized or reconstructed travel position."
                );
                return true;
            }
            facing = resolveExteriorFacing(player, record);
            source = "current exterior";
        } else {
            position = record.getDestinationPosition();
            if (position == null) {
                send(
                    player,
                    TextFormatting.RED,
                    "Destination waypoint writing needs an entered destination."
                );
                return true;
            }
            facing = record.getDestinationFacing();
            source = "entered destination";
        }

        int landingMode = state.getControlState(ConsoleControlId.MAIN_LANDING_MODE);
        boolean landingPad = state.getControlState(ConsoleControlId.MAIN_LANDING_PAD) != 0;
        TardisWaypoint waypoint = new TardisWaypoint(
            position,
            facing,
            landingMode,
            landingPad
        );
        boolean changed = data.setConsoleWaypoint(record.getTardisId(), slot, waypoint);
        String name = state.getWaypointName(slot);
        send(
            player,
            changed ? TextFormatting.GREEN : TextFormatting.YELLOW,
            "Waypoint \"" + name + "\" " + (changed ? "saved" : "already matches")
                + " the " + source + ": " + position.toString()
                + ", facing " + facing.getName()
                + ", landing " + describeLandingMode(landingMode)
                + (landingPad ? ", landing pads enabled." : ", landing pads ignored.")
        );
        return true;
    }

    private static boolean loadWaypoint(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record,
        int slot
    ) {
        TardisConsoleState state = record.getConsoleState();
        TardisWaypoint waypoint = state.getWaypoint(slot);
        String name = state.getWaypointName(slot);
        if (waypoint == null) {
            send(player, TextFormatting.YELLOW, "Waypoint \"" + name + "\" is empty.");
            return true;
        }

        String failure = prepareTarget(
            data,
            record,
            waypoint.getPosition(),
            waypoint.getFacing()
        );
        if (!failure.isEmpty()) {
            send(
                player,
                TextFormatting.RED,
                "Waypoint \"" + name + "\" could not be loaded: " + failure
            );
            return true;
        }
        data.setConsoleControlState(
            record.getTardisId(),
            ConsoleControlId.MAIN_LANDING_MODE,
            waypoint.getLandingMode()
        );
        data.setConsoleControlState(
            record.getTardisId(),
            ConsoleControlId.MAIN_LANDING_PAD,
            waypoint.usesLandingPad() ? 1 : 0
        );
        send(
            player,
            TextFormatting.GOLD,
            "Loaded waypoint \"" + name + "\": " + waypoint.getPosition().toString()
                + ", facing " + waypoint.getFacing().getName()
                + ", landing " + describeLandingMode(waypoint.getLandingMode())
                + (waypoint.usesLandingPad() ? ", landing pads enabled." : ", landing pads ignored.")
        );
        return true;
    }

    private static EnumFacing resolveExteriorFacing(
        EntityPlayerMP player,
        TardisRecord record
    ) {
        DimensionPosition exterior = record.getExteriorPosition();
        if (player != null && exterior != null && player.getServerWorld().getMinecraftServer() != null) {
            WorldServer world = player.getServerWorld().getMinecraftServer().getWorld(
                exterior.getDimension()
            );
            if (world != null) {
                net.minecraft.block.state.IBlockState blockState = world.getBlockState(
                    exterior.getPosition()
                );
                if (blockState.getBlock() == ModBlocks.TARDIS_EXTERIOR
                    && blockState.getPropertyKeys().contains(BlockTardisExterior.FACING)) {
                    EnumFacing facing = blockState.getValue(BlockTardisExterior.FACING);
                    if (facing != null && facing.getAxis().isHorizontal()) {
                        return facing;
                    }
                }
            }
        }
        return directionFacing(
            record.getConsoleState().getControlState(ConsoleControlId.MAIN_DIRECTION)
        );
    }

    private static String describeLandingMode(int mode) {
        switch (Math.max(0, Math.min(2, mode))) {
            case 1:
                return "surface";
            case 2:
                return "nearest solid";
            default:
                return "exact / midair";
        }
    }

    /** Re-encodes the physical navigation banks after an interrupted landing. */
    public static void synchronizeAfterInterruptedLanding(TardisRecord record) {
        if (record == null || record.getDestinationPosition() == null) {
            return;
        }
        encodeTarget(record, record.getDestinationPosition(), isRelative(record));
        record.getConsoleState().setControlState(
            ConsoleControlId.MAIN_DIRECTION,
            directionState(record.getDestinationFacing())
        );
    }

    private static boolean randomizeDestination(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record
    ) {
        TardisConsoleState state = record.getConsoleState();
        DimensionPosition reference = referencePosition(record);
        boolean local = state.getControlState(ConsoleControlId.NAV_SCALE_RANGE) == 0;
        if (local && reference == null) {
            send(
                player,
                TextFormatting.RED,
                "Local randomization needs a materialized exterior or recorded flight departure."
            );
            return true;
        }

        int dimension = local
            ? reference.getDimension()
            : state.getNavigationDimensionId(defaultDimension(record));
        String dimensionFailure = validateDestinationDimension(record, dimension);
        if (!dimensionFailure.isEmpty()) {
            send(player, TextFormatting.RED, dimensionFailure);
            return true;
        }

        int x;
        int z;
        if (local) {
            BlockPos base = reference.getPosition();
            x = randomBetween(
                Math.max(-MAX_HORIZONTAL_COORDINATE, base.getX() - LOCAL_RANDOM_RADIUS),
                Math.min(MAX_HORIZONTAL_COORDINATE, base.getX() + LOCAL_RANDOM_RADIUS)
            );
            z = randomBetween(
                Math.max(-MAX_HORIZONTAL_COORDINATE, base.getZ() - LOCAL_RANDOM_RADIUS),
                Math.min(MAX_HORIZONTAL_COORDINATE, base.getZ() + LOCAL_RANDOM_RADIUS)
            );
            if (x == base.getX() && z == base.getZ()) {
                x = x < MAX_HORIZONTAL_COORDINATE ? x + 1 : x - 1;
            }
        } else {
            x = randomBetween(-MAX_HORIZONTAL_COORDINATE, MAX_HORIZONTAL_COORDINATE);
            z = randomBetween(-MAX_HORIZONTAL_COORDINATE, MAX_HORIZONTAL_COORDINATE);
        }

        DimensionPosition existing = record.getDestinationPosition();
        int y = existing != null
            ? existing.getPosition().getY()
            : reference != null ? reference.getPosition().getY() : 64;
        DimensionPosition target = new DimensionPosition(dimension, new BlockPos(x, y, z));
        String failure = prepareTarget(data, record, target, record.getDestinationFacing());
        if (!failure.isEmpty()) {
            send(player, TextFormatting.RED, failure);
            return true;
        }
        send(
            player,
            TextFormatting.GOLD,
            (local ? "Local" : "Global") + " random destination prepared: " + target.toString() + "."
        );
        return true;
    }

    private static boolean cycleDimension(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record,
        boolean backwards
    ) {
        TardisConsoleState state = record.getConsoleState();
        int current = state.getNavigationDimensionId(defaultDimension(record));
        List<Integer> dimensions = availableDimensions(record, current);
        if (dimensions.isEmpty()) {
            send(player, TextFormatting.RED, "No targetable registered dimensions are available.");
            return true;
        }
        int index = dimensions.indexOf(current);
        if (index < 0) {
            index = 0;
        } else {
            index = (index + (backwards ? -1 : 1)) % dimensions.size();
            if (index < 0) {
                index += dimensions.size();
            }
        }
        int next = dimensions.get(index);
        data.setConsoleNavigationDimension(record.getTardisId(), next);
        String failure = synchronizeDestination(data, record);
        if (!failure.isEmpty()) {
            data.setConsoleNavigationDimension(record.getTardisId(), current);
            send(player, TextFormatting.RED, failure);
            return true;
        }
        send(
            player,
            TextFormatting.AQUA,
            "Navigation dimension: " + describeDimension(next) + " ("
                + (index + 1) + " / " + dimensions.size() + ")."
        );
        return true;
    }

    private static String prepareTarget(
        TardisWorldData data,
        TardisRecord record,
        DimensionPosition target,
        EnumFacing facing
    ) {
        if (target == null) {
            return "No target position was supplied.";
        }
        String dimensionFailure = validateDestinationDimension(record, target.getDimension());
        if (!dimensionFailure.isEmpty()) {
            return dimensionFailure;
        }
        String failure = encodeTarget(record, target, isRelative(record));
        if (!failure.isEmpty()) {
            return failure;
        }
        data.setConsoleControlState(
            record.getTardisId(),
            ConsoleControlId.MAIN_DIRECTION,
            directionState(facing)
        );
        data.setDestination(record.getTardisId(), target, facing);
        return "";
    }

    private static String synchronizeDestination(TardisWorldData data, TardisRecord record) {
        TardisConsoleState state = record.getConsoleState();
        DimensionPosition exterior = navigationReferencePosition(record);
        boolean relative = isRelative(record);
        if (relative && exterior == null) {
            return "Relative coordinates need a materialized or reconstructed travel position.";
        }

        int x = decodeHorizontal(state, 0);
        int y = decodeY(state, relative);
        int z = decodeHorizontal(state, 2);
        if (relative) {
            BlockPos base = exterior.getPosition();
            x = safeAdd(base.getX(), x);
            y = safeAdd(base.getY(), y);
            z = safeAdd(base.getZ(), z);
        }
        if (Math.abs((long) x) > MAX_HORIZONTAL_COORDINATE
            || Math.abs((long) z) > MAX_HORIZONTAL_COORDINATE) {
            return "That target exceeds Minecraft's horizontal world limit.";
        }
        if (y < 0 || y > 255) {
            return "That target Y must remain between 0 and 255.";
        }

        int dimension = state.getNavigationDimensionId(defaultDimension(record));
        DimensionPosition target = new DimensionPosition(dimension, new BlockPos(x, y, z));
        data.setDestination(record.getTardisId(), target, record.getDestinationFacing());
        return "";
    }

    private static String encodeTarget(
        TardisRecord record,
        DimensionPosition target,
        boolean relative
    ) {
        DimensionPosition exterior = navigationReferencePosition(record);
        long x = target.getPosition().getX();
        long y = target.getPosition().getY();
        long z = target.getPosition().getZ();
        if (relative) {
            if (exterior == null) {
                return "Relative coordinates need a materialized or reconstructed travel position.";
            }
            x -= exterior.getPosition().getX();
            y -= exterior.getPosition().getY();
            z -= exterior.getPosition().getZ();
            if (Math.abs(x) > MAX_NAVIGATION_MAGNITUDE
                || Math.abs(z) > MAX_NAVIGATION_MAGNITUDE) {
                return "That relative horizontal offset does not fit the eight coordinate digits.";
            }
        } else if (Math.abs(x) > MAX_HORIZONTAL_COORDINATE
            || Math.abs(z) > MAX_HORIZONTAL_COORDINATE) {
            return "That target exceeds Minecraft's horizontal world limit.";
        }
        if (relative) {
            if (Math.abs(y) > 999L) {
                return "The relative Y offset must fit the three Y levers (-999 to 999).";
            }
        } else if (y < 0 || y > 255) {
            return "That target Y must remain between 0 and 255.";
        }

        TardisConsoleState state = record.getConsoleState();
        encodeHorizontal(state, 0, (int) x);
        encodeY(state, (int) y, relative);
        encodeHorizontal(state, 2, (int) z);
        state.setNavigationDimensionId(target.getDimension());
        return "";
    }

    private static void encodeHorizontal(TardisConsoleState state, int axis, int value) {
        long magnitude = Math.abs((long) value);
        state.setNavigationSign(axis, value < 0 ? 1 : 0);
        for (int digit = 0; digit < 4; digit++) {
            state.setNavigationDigit(axis, 0, digit, digitAt(magnitude, digit));
            state.setNavigationDigit(axis, 1, digit, digitAt(magnitude, digit + 4));
        }
    }

    private static void encodeY(TardisConsoleState state, int value, boolean signed) {
        long magnitude = Math.abs((long) value);
        state.setNavigationSign(1, signed && value < 0 ? 1 : 0);
        state.setNavigationDigit(1, 0, 0, digitAt(magnitude, 0));
        state.setNavigationDigit(1, 0, 1, digitAt(magnitude, 1));
        state.setNavigationDigit(1, 0, 2, digitAt(magnitude, 2));
        state.setNavigationDigit(1, 0, 3, 0);
    }

    private static int decodeHorizontal(TardisConsoleState state, int axis) {
        long magnitude = 0L;
        long multiplier = 1L;
        for (int digit = 0; digit < 4; digit++) {
            magnitude += (long) state.getNavigationDigit(axis, 0, digit) * multiplier;
            multiplier *= 10L;
        }
        for (int digit = 0; digit < 4; digit++) {
            magnitude += (long) state.getNavigationDigit(axis, 1, digit) * multiplier;
            multiplier *= 10L;
        }
        long signed = state.getNavigationSign(axis) == 0 ? magnitude : -magnitude;
        if (signed > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (signed < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) signed;
    }

    private static int decodeY(TardisConsoleState state, boolean signed) {
        int magnitude = state.getNavigationDigit(1, 0, 0)
            + state.getNavigationDigit(1, 0, 1) * 10
            + state.getNavigationDigit(1, 0, 2) * 100;
        return signed && state.getNavigationSign(1) != 0 ? -magnitude : magnitude;
    }

    private static String describeSelection(TardisRecord record) {
        TardisConsoleState state = record.getConsoleState();
        int axis = state.getControlState(ConsoleControlId.NAV_AXIS);
        String axisName = axis == 1 ? "Y" : axis == 2 ? "Z" : "X";
        String range = state.getControlState(ConsoleControlId.NAV_SCALE_RANGE) == 0 ? "local" : "global";
        String destination = record.getDestinationPosition() == null
            ? "no destination"
            : record.getDestinationPosition().toString();
        String selection = axis == 1
            ? axisName + " selected — " + describeDigits(record)
            : axisName + " selected, " + range + " bank — " + describeDigits(record);
        return selection + "; target: " + destination + ".";
    }

    private static String describeDigits(TardisRecord record) {
        TardisConsoleState state = record.getConsoleState();
        int axis = state.getControlState(ConsoleControlId.NAV_AXIS);
        if (axis == 1) {
            int dimension = state.getNavigationDimensionId(defaultDimension(record));
            return "C1/C2/C3 = "
                + state.getNavigationDigit(1, 0, 0) + "/"
                + state.getNavigationDigit(1, 0, 1) + "/"
                + state.getNavigationDigit(1, 0, 2)
                + "; dimension = " + describeDimension(dimension);
        }
        int range = state.getControlState(ConsoleControlId.NAV_SCALE_RANGE);
        return "C1/C2/C3/C4 = "
            + state.getNavigationDigit(axis, range, 0) + "/"
            + state.getNavigationDigit(axis, range, 1) + "/"
            + state.getNavigationDigit(axis, range, 2) + "/"
            + state.getNavigationDigit(axis, range, 3);
    }

    private static List<Integer> availableDimensions(TardisRecord record, int current) {
        TreeSet<Integer> dimensions = new TreeSet<>();
        try {
            Map<?, ?> registered = DimensionManager.getRegisteredDimensions();
            for (Object group : registered.values()) {
                if (!(group instanceof Iterable<?>)) {
                    continue;
                }
                for (Object raw : (Iterable<?>) group) {
                    if (raw instanceof Number) {
                        dimensions.add(((Number) raw).intValue());
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // Fall through to the known/current dimensions below. The selector
            // remains usable even if another mod mutates the registry oddly.
        }
        dimensions.add(-1);
        dimensions.add(0);
        dimensions.add(1);
        dimensions.add(current);

        int departureDimension = defaultDepartureDimension(record);
        List<Integer> allowed = new ArrayList<>();
        for (Integer dimension : dimensions) {
            if (dimension == null || dimension == TardisDimensionManager.getDimensionId()) {
                continue;
            }
            if (dimension == current) {
                allowed.add(dimension);
                continue;
            }
            if (!DimensionManager.isDimensionRegistered(dimension)) {
                continue;
            }
            if (TardisConfig.isFlightRouteAllowed(departureDimension, dimension)) {
                allowed.add(dimension);
            }
        }
        Collections.sort(allowed);
        return allowed;
    }

    private static String validateDestinationDimension(TardisRecord record, int dimension) {
        if (dimension == TardisDimensionManager.getDimensionId()) {
            return "The shared TARDIS interior dimension cannot be selected as an exterior destination.";
        }
        if (!DimensionManager.isDimensionRegistered(dimension)) {
            return "Dimension " + dimension + " is not registered.";
        }
        return TardisConfig.describeFlightRestriction(defaultDepartureDimension(record), dimension);
    }

    private static int defaultDimension(TardisRecord record) {
        if (record.getDestinationPosition() != null) {
            return record.getDestinationPosition().getDimension();
        }
        DimensionPosition reference = referencePosition(record);
        return reference == null ? 0 : reference.getDimension();
    }

    private static int defaultDepartureDimension(TardisRecord record) {
        DimensionPosition reference = record.getExteriorPosition();
        if (reference == null) {
            reference = record.getFlightDeparture();
        }
        return reference == null ? defaultDimension(record) : reference.getDimension();
    }

    private static DimensionPosition referencePosition(TardisRecord record) {
        DimensionPosition current = navigationReferencePosition(record);
        return current == null ? record.getDestinationPosition() : current;
    }

    private static DimensionPosition navigationReferencePosition(TardisRecord record) {
        if (record.getExteriorPosition() != null) {
            return record.getExteriorPosition();
        }
        if (record.isInFlight()) {
            DimensionPosition current = record.getCurrentFlightPosition(System.currentTimeMillis());
            if (current != null) {
                return current;
            }
        }
        return record.getFlightDeparture();
    }

    private static boolean isRelative(TardisRecord record) {
        return record.getConsoleState().getControlState(
            ConsoleControlId.UTILITY_ABSOLUTE_RELATIVE
        ) != 0;
    }

    private static int nextState(ConsoleControlId control, int previous, boolean sneaking) {
        if (control.getKind() == ConsoleControlKind.PULL_SWITCH) {
            return previous == 0 ? 1 : 0;
        }
        int direction = sneaking ? -1 : 1;
        if (control == ConsoleControlId.NAV_AXIS) {
            int wrapped = (previous + direction) % control.getStateCount();
            return wrapped < 0 ? wrapped + control.getStateCount() : wrapped;
        }
        return control.clampState(previous + direction);
    }

    private static int directionState(EnumFacing facing) {
        if (facing == EnumFacing.EAST) {
            return 1;
        }
        if (facing == EnumFacing.SOUTH) {
            return 2;
        }
        if (facing == EnumFacing.WEST) {
            return 3;
        }
        return 0;
    }

    private static EnumFacing directionFacing(int state) {
        switch (Math.max(0, Math.min(3, state))) {
            case 1:
                return EnumFacing.EAST;
            case 2:
                return EnumFacing.SOUTH;
            case 3:
                return EnumFacing.WEST;
            default:
                return EnumFacing.NORTH;
        }
    }

    private static void syncWaypointPanel(EntityPlayerMP player, TardisRecord record) {
        if (player == null || record == null || record.getInteriorPosition() == null
            || player.getServerWorld().getMinecraftServer() == null) {
            return;
        }
        DimensionPosition interior = record.getInteriorPosition();
        WorldServer world = player.getServerWorld().getMinecraftServer().getWorld(
            interior.getDimension()
        );
        if (world == null) {
            return;
        }
        BlockPos panelPos = InteriorAddress.getConsolePanelPosition(
            interior.getPosition(),
            ConsolePanel.WEST_NAVIGATION
        );
        TileEntity tile = world.getTileEntity(panelPos);
        if (tile instanceof TileEntityTardisConsolePanel) {
            ((TileEntityTardisConsolePanel) tile).syncWaypointMetadata(record.getConsoleState());
        }
    }

    private static void consumeOne(EntityPlayerMP player, ItemStack stack) {
        if (player != null && stack != null && !stack.isEmpty()
            && !player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
    }

    private static String describeDimension(int dimension) {
        try {
            DimensionType type = DimensionManager.getProviderType(dimension);
            if (type != null && type.getName() != null && !type.getName().trim().isEmpty()) {
                return type.getName() + " (" + dimension + ")";
            }
        } catch (RuntimeException ignored) {
            // Use the stable numeric fallback below.
        }
        return "dimension " + dimension;
    }

    private static int randomBetween(int minimum, int maximum) {
        if (minimum >= maximum) {
            return minimum;
        }
        return ThreadLocalRandom.current().nextInt(minimum, maximum + 1);
    }

    private static int digitAt(long value, int decimalPlace) {
        long divisor = 1L;
        for (int index = 0; index < decimalPlace; index++) {
            divisor *= 10L;
        }
        return (int) ((value / divisor) % 10L);
    }

    private static int safeAdd(int first, int second) {
        long result = (long) first + second;
        if (result > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (result < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) result;
    }

    private static void send(EntityPlayerMP player, TextFormatting color, String text) {
        player.sendStatusMessage(
            new TextComponentString(color + "[TARDIS] " + TextFormatting.RESET + text),
            false
        );
    }
}
