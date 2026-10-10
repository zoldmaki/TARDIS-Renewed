package dev.zoldmaki.renewedtardis.console;

import dev.zoldmaki.renewedtardis.access.TardisAccessService;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.flight.TardisFlightService;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/** Server-authoritative router for physical console-panel interactions. */
public final class TardisConsoleInteractionService {
    private TardisConsoleInteractionService() {
    }

    public static boolean interact(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record,
        ConsolePanel panel,
        ConsoleControlId control,
        BlockPos controlPos,
        EnumHand hand
    ) {
        if (player == null || data == null || record == null || panel == null || control == null) {
            return false;
        }
        if (control.getPanel() != panel) {
            send(player, TextFormatting.RED, "That hit region is registered on the wrong console panel.");
            return true;
        }
        if (!TardisAccessService.canUseConsole(player, record)) {
            send(player, TextFormatting.RED, "You do not have console permission for this TARDIS.");
            return true;
        }

        if (TardisNavigationConsoleService.tryCustomizeWaypoint(
            player,
            data,
            record,
            control,
            hand
        )) {
            return true;
        }

        if (control == ConsoleControlId.NAV_SIGN
            && record.getConsoleState().getControlState(ConsoleControlId.NAV_AXIS) == 1
            && record.getConsoleState().getControlState(ConsoleControlId.UTILITY_ABSOLUTE_RELATIVE) == 0) {
            send(player, TextFormatting.YELLOW, "The sign control is disabled for absolute Y coordinates.");
            return true;
        }

        if (control.isSneakRequired() && !player.isSneaking()) {
            send(
                player,
                TextFormatting.YELLOW,
                control.getDisplayName() + " requires sneak-right-click as a safety action."
            );
            return true;
        }

        if (control.isTakeoffSequenceControl()) {
            return handleTakeoffSequence(player, data, record, control, controlPos);
        }

        // Touching any flight-affecting control invalidates a preloaded takeoff,
        // even when the control is already at its end state.
        if (control.resetsTakeoffSequence()) {
            data.resetTakeoffSequence(record.getTardisId());
        }

        if (TardisNavigationConsoleService.handles(control)) {
            return TardisNavigationConsoleService.interact(player, data, record, control);
        }

        if (control == ConsoleControlId.UTILITY_ABSOLUTE_RELATIVE) {
            int previous = record.getConsoleState().getControlState(control);
            int next = previous == 0 ? 1 : 0;
            return TardisNavigationConsoleService.setRelativeMode(player, data, record, next);
        }

        if (TardisMainFlightConsoleService.handles(control)) {
            return TardisMainFlightConsoleService.interact(player, data, record, control);
        }

        if (TardisUtilityConsoleService.handles(control)) {
            return TardisUtilityConsoleService.interact(player, data, record, control);
        }

        if (control.getKind().isReadOnly()) {
            send(
                player,
                TextFormatting.AQUA,
                panel.getDisplayName() + " — " + control.getDisplayName() + " (read-only placeholder)."
            );
            return true;
        }

        int stateCount = control.getStateCount();
        if (stateCount <= 1) {
            send(
                player,
                TextFormatting.AQUA,
                panel.getDisplayName() + " — " + control.getDisplayName()
                    + " routed successfully; its gameplay action is reserved for the next console step."
            );
            return true;
        }

        int previous = record.getConsoleState().getControlState(control);
        int next = calculateNextState(control, previous, player.isSneaking());
        data.setConsoleControlState(record.getTardisId(), control, next);
        send(
            player,
            TextFormatting.AQUA,
            panel.getDisplayName() + " — " + control.getDisplayName() + ": " + describeState(control, next)
        );
        return true;
    }

    private static boolean handleTakeoffSequence(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record,
        ConsoleControlId control,
        BlockPos controlPos
    ) {
        int step = control.getTakeoffSequenceStep();
        int progress = record.getConsoleState().getTakeoffSequenceProgress();

        if (step == 4 && record.isInFlight()) {
            boolean landed = TardisMainFlightConsoleService.land(player, record, controlPos);
            if (landed) {
                data.setConsoleControlState(record.getTardisId(), control, 0);
            }
            return true;
        }

        int expected = progress + 1;
        if (step != expected) {
            if (step == 1) {
                data.setTakeoffSequenceProgress(record.getTardisId(), 1);
                send(player, TextFormatting.GOLD, "Takeoff sequence restarted: 1/4 — Temporal Primer.");
            } else {
                data.resetTakeoffSequence(record.getTardisId());
                send(
                    player,
                    TextFormatting.YELLOW,
                    "Incorrect takeoff order. The four-control sequence was reset."
                );
            }
            return true;
        }

        if (step < 4) {
            data.setTakeoffSequenceProgress(record.getTardisId(), step);
            send(
                player,
                TextFormatting.GOLD,
                "Takeoff sequence " + step + "/4 — " + control.getDisplayName() + "."
            );
            return true;
        }

        boolean tookOff = TardisFlightService.takeOff(player, record, controlPos);
        if (tookOff) {
            data.setConsoleControlState(record.getTardisId(), control, 1);
        } else {
            send(
                player,
                TextFormatting.YELLOW,
                "The sequence remains prepared because takeoff did not complete."
            );
        }
        return true;
    }

    private static int calculateNextState(
        ConsoleControlId control,
        int previous,
        boolean sneaking
    ) {
        int count = control.getStateCount();
        if (control.getKind() == ConsoleControlKind.PULL_SWITCH) {
            return previous == 0 ? 1 : 0;
        }

        int direction = sneaking ? -1 : 1;
        if (control.getKind() == ConsoleControlKind.DIAL
            || control.getKind() == ConsoleControlKind.BUTTON
            || control.getKind() == ConsoleControlKind.SCREEN) {
            int wrapped = (previous + direction) % count;
            return wrapped < 0 ? wrapped + count : wrapped;
        }
        return control.clampState(previous + direction);
    }

    private static String describeState(ConsoleControlId control, int state) {
        switch (control) {
            case NAV_AXIS:
                return new String[] {"X", "Y", "Z"}[control.clampState(state)];
            case NAV_SCALE_RANGE:
                return state == 0 ? "local" : "global";
            case NAV_SIGN:
                return state == 0 ? "positive" : "negative";
            case NAV_WAYPOINT_WRITE_READ:
                return state == 0 ? "read" : "write";
            case NAV_WAYPOINT_CURRENT_DESTINATION:
                return state == 0 ? "current exterior" : "entered destination";
            case MAIN_SPEED:
                return dev.zoldmaki.renewedtardis.flight.FlightSpeed.fromConsoleState(state).getName();
            case MAIN_LANDING_MODE:
                return new String[] {"exact / midair", "surface", "nearest solid"}[control.clampState(state)];
            case MAIN_LANDING_PAD:
                return state == 0 ? "ignore landing pads" : "use landing pads";
            case MAIN_DIRECTION:
                return new String[] {"north", "east", "south", "west"}[control.clampState(state)];
            case MAIN_DRIVE_MODE:
                return state == 0 ? "coordinated" : "uncoordinated";
            case UTILITY_DOOR_LOCK:
                return state == 0 ? "unlocked placeholder" : "locked placeholder";
            case UTILITY_INTERNAL_TIME:
                return new String[] {"simulated Overworld", "day", "night"}[control.clampState(state)];
            case UTILITY_ABSOLUTE_RELATIVE:
                return state == 0 ? "absolute" : "relative";
            case UTILITY_EXTERNAL_SCANNER:
                return "page " + (control.clampState(state) + 1) + " / " + control.getStateCount();
            default:
                return "state " + (control.clampState(state) + 1) + " / " + control.getStateCount();
        }
    }

    private static void send(EntityPlayerMP player, TextFormatting color, String text) {
        player.sendStatusMessage(
            new TextComponentString(color + "[TARDIS] " + TextFormatting.RESET + text),
            false
        );
    }
}
