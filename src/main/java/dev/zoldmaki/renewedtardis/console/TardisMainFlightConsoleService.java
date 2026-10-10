package dev.zoldmaki.renewedtardis.console;

import dev.zoldmaki.renewedtardis.damage.TardisDamageService;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.energy.TardisEnergyService;
import dev.zoldmaki.renewedtardis.flight.FlightSpeed;
import dev.zoldmaki.renewedtardis.flight.TardisFlightService;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/** Server-authoritative actions and readouts for the south Main Flight panel. */
public final class TardisMainFlightConsoleService {
    private static final int PROGRESS_LIGHT_COUNT = 7;

    private TardisMainFlightConsoleService() {
    }

    public static boolean handles(ConsoleControlId control) {
        if (control == null) {
            return false;
        }
        switch (control) {
            case MAIN_SPEED:
            case MAIN_LANDING_MODE:
            case MAIN_LANDING_PAD:
            case MAIN_DIRECTION:
            case MAIN_DRIVE_MODE:
            case MAIN_SHIELD_GAUGE:
            case MAIN_ARTRON_GAUGE:
            case MAIN_XP_GAUGE:
            case MAIN_PROGRESS_1:
            case MAIN_PROGRESS_2:
            case MAIN_PROGRESS_3:
            case MAIN_PROGRESS_4:
            case MAIN_PROGRESS_5:
            case MAIN_PROGRESS_6:
            case MAIN_PROGRESS_7:
                return true;
            default:
                return false;
        }
    }

    public static boolean isReadoutControl(ConsoleControlId control) {
        if (control == null) {
            return false;
        }
        switch (control) {
            case MAIN_SHIELD_GAUGE:
            case MAIN_ARTRON_GAUGE:
            case MAIN_XP_GAUGE:
            case MAIN_PROGRESS_1:
            case MAIN_PROGRESS_2:
            case MAIN_PROGRESS_3:
            case MAIN_PROGRESS_4:
            case MAIN_PROGRESS_5:
            case MAIN_PROGRESS_6:
            case MAIN_PROGRESS_7:
                return true;
            default:
                return false;
        }
    }

    public static boolean interact(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record,
        ConsoleControlId control
    ) {
        if (player == null || data == null || record == null || control == null) {
            return false;
        }
        switch (control) {
            case MAIN_SPEED:
                return adjustSpeed(player, data, record);
            case MAIN_LANDING_MODE:
                return adjustLandingMode(player, data, record);
            case MAIN_LANDING_PAD:
                return toggleLandingPads(player, data, record);
            case MAIN_DIRECTION:
                return adjustDirection(player, data, record);
            case MAIN_DRIVE_MODE:
                return TardisFlightService.toggleDriveMode(player, record);
            case MAIN_SHIELD_GAUGE:
                return readShieldGauge(player, record);
            case MAIN_ARTRON_GAUGE:
                return readArtronGauge(player, data, record);
            case MAIN_XP_GAUGE:
                return readExperienceGauge(player);
            case MAIN_PROGRESS_1:
            case MAIN_PROGRESS_2:
            case MAIN_PROGRESS_3:
            case MAIN_PROGRESS_4:
            case MAIN_PROGRESS_5:
            case MAIN_PROGRESS_6:
            case MAIN_PROGRESS_7:
                return readProgress(player, record, control);
            default:
                return false;
        }
    }

    public static boolean land(
        EntityPlayerMP player,
        TardisRecord record,
        BlockPos controlPos
    ) {
        int selected = record.getConsoleState().getControlState(ConsoleControlId.MAIN_LANDING_MODE);
        TardisFlightService.LandingMode mode;
        switch (selected) {
            case 1:
                mode = TardisFlightService.LandingMode.SURFACE;
                break;
            case 2:
                mode = TardisFlightService.LandingMode.NEAREST_SOLID;
                break;
            case 0:
            default:
                mode = TardisFlightService.LandingMode.EXACT;
                break;
        }
        if (record.getConsoleState().getControlState(ConsoleControlId.MAIN_LANDING_PAD) != 0) {
            send(
                player,
                TextFormatting.YELLOW,
                "Landing-pad search is selected, but landing pads are not implemented yet; using "
                    + describeLandingMode(selected) + "."
            );
        }
        if (record.isInFlight() && !record.isCoordinatedDriveMode()) {
            return TardisFlightService.landAtTravelledPosition(player, record, controlPos, mode);
        }
        if (
            record.isInFlight()
                && player.isSneaking()
                && !record.isFlightReady(System.currentTimeMillis())
        ) {
            return TardisFlightService.landAtTravelledPosition(player, record, controlPos, mode);
        }
        return TardisFlightService.land(player, record, controlPos, mode, false);
    }

    private static boolean adjustSpeed(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record
    ) {
        if (record.isInFlight()) {
            send(player, TextFormatting.YELLOW, "Flight speed is locked after dematerialization.");
            return true;
        }
        int previous = record.getFlightSpeed().getConsoleState();
        int next = clampLever(previous + (player.isSneaking() ? -1 : 1), FlightSpeed.values().length);
        FlightSpeed speed = FlightSpeed.fromConsoleState(next);
        data.setFlightSpeed(record.getTardisId(), speed);
        send(
            player,
            TextFormatting.AQUA,
            "Flight speed: " + speed.getName() + " (" + speed.getDurationPercent() + "% of normal-profile duration)."
        );
        return true;
    }

    private static boolean adjustLandingMode(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record
    ) {
        int previous = record.getConsoleState().getControlState(ConsoleControlId.MAIN_LANDING_MODE);
        int next = clampLever(previous + (player.isSneaking() ? -1 : 1), 3);
        data.setConsoleControlState(record.getTardisId(), ConsoleControlId.MAIN_LANDING_MODE, next);
        send(player, TextFormatting.AQUA, "Landing mode: " + describeLandingMode(next) + ".");
        return true;
    }

    private static boolean toggleLandingPads(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record
    ) {
        int previous = record.getConsoleState().getControlState(ConsoleControlId.MAIN_LANDING_PAD);
        int next = previous == 0 ? 1 : 0;
        data.setConsoleControlState(record.getTardisId(), ConsoleControlId.MAIN_LANDING_PAD, next);
        if (next == 0) {
            send(player, TextFormatting.AQUA, "Landing pads will be ignored.");
        } else {
            send(
                player,
                TextFormatting.YELLOW,
                "Landing-pad preference enabled. Until pad blocks are implemented, normal landing rules are used."
            );
        }
        return true;
    }

    private static boolean adjustDirection(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record
    ) {
        int previous = record.getConsoleState().getControlState(ConsoleControlId.MAIN_DIRECTION);
        int direction = player.isSneaking() ? -1 : 1;
        int next = wrap(previous + direction, 4);
        EnumFacing facing = horizontalFacing(next);
        data.setConsoleControlState(record.getTardisId(), ConsoleControlId.MAIN_DIRECTION, next);
        if (record.getDestinationPosition() != null) {
            data.setDestination(record.getTardisId(), record.getDestinationPosition(), facing);
        }
        send(player, TextFormatting.AQUA, "Exterior direction: " + facing.getName().toUpperCase(java.util.Locale.ROOT) + ".");
        return true;
    }

    private static boolean readShieldGauge(EntityPlayerMP player, TardisRecord record) {
        send(
            player,
            TextFormatting.YELLOW,
            "Shield reserve is not implemented yet. Current subsystem condition: "
                + TardisDamageService.describe(record) + "."
        );
        return true;
    }

    private static boolean readArtronGauge(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record
    ) {
        TardisEnergyService.refresh(data, record);
        send(
            player,
            TextFormatting.AQUA,
            "Artron energy: " + TardisEnergyService.describe(record)
                + "; regeneration: " + TardisEnergyService.format(TardisEnergyService.getRegenerationPerSecond(record))
                + " AU/s."
        );
        return true;
    }

    private static boolean readExperienceGauge(EntityPlayerMP player) {
        send(
            player,
            TextFormatting.YELLOW,
            "TARDIS experience is reserved for the later progression and upgrade-slot system; no TARDIS XP is awarded yet."
        );
        return true;
    }

    private static boolean readProgress(
        EntityPlayerMP player,
        TardisRecord record,
        ConsoleControlId control
    ) {
        long now = System.currentTimeMillis();
        int percent = record.isInFlight() ? record.getFlightProgressPercent(now) : 0;
        int lit = record.isInFlight() && percent > 0
            ? Math.min(PROGRESS_LIGHT_COUNT, Math.max(1, (percent * PROGRESS_LIGHT_COUNT + 99) / 100))
            : 0;
        int selected = progressLightIndex(control);
        String selectedState = selected <= lit ? "lit" : "dark";
        DimensionPosition travelled = record.getCurrentFlightPosition(now);
        String travelledText = record.isInFlight() && travelled != null
            ? "; reconstructed position " + travelled.toString()
            : "";
        send(
            player,
            TextFormatting.AQUA,
            "Flight progress: " + TardisFlightService.describeFlightProgress(record, now)
                + travelledText
                + "; lights " + lit + "/" + PROGRESS_LIGHT_COUNT
                + "; this indicator is " + selectedState + "."
        );
        return true;
    }

    public static String describeLandingMode(int state) {
        switch (Math.max(0, Math.min(2, state))) {
            case 1:
                return "surface (top valid solid support at target X/Z)";
            case 2:
                return "nearest solid (closest valid support to selected Y)";
            case 0:
            default:
                return "exact / midair";
        }
    }

    private static int progressLightIndex(ConsoleControlId control) {
        switch (control) {
            case MAIN_PROGRESS_1:
                return 1;
            case MAIN_PROGRESS_2:
                return 2;
            case MAIN_PROGRESS_3:
                return 3;
            case MAIN_PROGRESS_4:
                return 4;
            case MAIN_PROGRESS_5:
                return 5;
            case MAIN_PROGRESS_6:
                return 6;
            case MAIN_PROGRESS_7:
                return 7;
            default:
                return 0;
        }
    }

    private static EnumFacing horizontalFacing(int state) {
        switch (wrap(state, 4)) {
            case 1:
                return EnumFacing.EAST;
            case 2:
                return EnumFacing.SOUTH;
            case 3:
                return EnumFacing.WEST;
            case 0:
            default:
                return EnumFacing.NORTH;
        }
    }

    private static int clampLever(int value, int stateCount) {
        return Math.max(0, Math.min(Math.max(1, stateCount) - 1, value));
    }

    private static int wrap(int value, int stateCount) {
        int count = Math.max(1, stateCount);
        int wrapped = value % count;
        return wrapped < 0 ? wrapped + count : wrapped;
    }

    private static void send(EntityPlayerMP player, TextFormatting color, String text) {
        player.sendStatusMessage(
            new TextComponentString(color + "[TARDIS] " + TextFormatting.RESET + text),
            false
        );
    }
}
