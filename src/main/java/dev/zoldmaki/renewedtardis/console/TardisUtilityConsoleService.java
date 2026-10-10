package dev.zoldmaki.renewedtardis.console;

import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.exterior.ExteriorShell;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;

/** Server-authoritative gameplay for the east Utility panel. */
public final class TardisUtilityConsoleService {
    private TardisUtilityConsoleService() {
    }

    public static boolean handles(ConsoleControlId control) {
        switch (control) {
            case UTILITY_EXTERNAL_SCANNER:
            case UTILITY_DOOR_LOCK:
            case UTILITY_INTERNAL_TIME:
            case UTILITY_SHELL_SCREEN:
            case UTILITY_SHELL_PREVIOUS:
            case UTILITY_SHELL_NEXT:
            case UTILITY_SHELL_APPLY:
                return true;
            default:
                return false;
        }
    }

    public static boolean isReadoutControl(ConsoleControlId control) {
        return control == ConsoleControlId.UTILITY_EXTERNAL_SCANNER
            || control == ConsoleControlId.UTILITY_SHELL_SCREEN;
    }

    public static boolean interact(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record,
        ConsoleControlId control
    ) {
        switch (control) {
            case UTILITY_EXTERNAL_SCANNER:
                cycleScanner(player, data, record);
                return true;
            case UTILITY_DOOR_LOCK:
                toggleDoor(player, data, record);
                return true;
            case UTILITY_INTERNAL_TIME:
                cycleInternalTime(player, data, record);
                return true;
            case UTILITY_SHELL_PREVIOUS:
                selectShell(player, data, record, -1);
                return true;
            case UTILITY_SHELL_NEXT:
                selectShell(player, data, record, 1);
                return true;
            case UTILITY_SHELL_SCREEN:
                showShell(player, record);
                return true;
            case UTILITY_SHELL_APPLY:
                applyShell(player, data, record);
                return true;
            default:
                return false;
        }
    }

    private static void cycleScanner(EntityPlayerMP player, TardisWorldData data, TardisRecord record) {
        int previous = record.getConsoleState().getControlState(ConsoleControlId.UTILITY_EXTERNAL_SCANNER);
        int page = (previous + 1) % ConsoleControlId.UTILITY_EXTERNAL_SCANNER.getStateCount();
        data.setConsoleControlState(record.getTardisId(), ConsoleControlId.UTILITY_EXTERNAL_SCANNER, page);
        send(player, TextFormatting.AQUA, scannerPage(player, record, page));
    }

    private static String scannerPage(EntityPlayerMP player, TardisRecord record, int page) {
        DimensionPosition exterior = record.getExteriorPosition();
        if (record.isInFlight() || exterior == null) {
            return "External scanner " + (page + 1) + "/6: exterior dematerialized.";
        }
        MinecraftServer server = player.getServer();
        WorldServer world = server == null ? null : server.getWorld(exterior.getDimension());
        BlockPos pos = exterior.getPosition();
        if (world == null) {
            return "External scanner: dimension " + exterior.getDimension() + " is unavailable.";
        }

        switch (page) {
            case 0:
                long time = world.getWorldTime() % 24000L;
                String phase = time < 12000L ? "Day" : "Night";
                return "Dim " + exterior.getDimension() + " — " + phase + " (" + time + ")";
            case 1:
                return "X " + pos.getX() + "  Y " + pos.getY() + "  Z " + pos.getZ();
            case 2:
                return "Biome: " + world.getBiome(pos).getBiomeName();
            case 3:
                return doorScan(world, pos, record.getDestinationFacing());
            case 4:
                return liquidScan(world, pos);
            case 5:
            default:
                return "Door " + (record.isExteriorLocked() ? "locked" : "unlocked")
                    + " — Shell: " + record.getExteriorShell().getDisplayName();
        }
    }

    private static String doorScan(WorldServer world, BlockPos exteriorPos, EnumFacing facing) {
        EnumFacing front = horizontal(facing);
        boolean frontClear = isPassable(world, exteriorPos.offset(front))
            && isPassable(world, exteriorPos.offset(front).up());
        if (frontClear) {
            return "Door: clear " + shortName(front);
        }
        for (EnumFacing direction : EnumFacing.HORIZONTALS) {
            if (isPassable(world, exteriorPos.offset(direction))
                && isPassable(world, exteriorPos.offset(direction).up())) {
                return "Door: blocked — safe " + shortName(direction);
            }
        }
        return "Door: blocked — no safe side";
    }

    private static String liquidScan(WorldServer world, BlockPos exteriorPos) {
        for (EnumFacing direction : EnumFacing.HORIZONTALS) {
            IBlockState state = world.getBlockState(exteriorPos.offset(direction));
            Material material = state.getMaterial();
            if (material.isLiquid() || state.getBlock() instanceof BlockLiquid) {
                String liquid = material == Material.WATER ? "Water"
                    : material == Material.LAVA ? "Lava" : state.getBlock().getLocalizedName();
                return "Liquid: " + liquid + " " + shortName(direction);
            }
        }
        IBlockState below = world.getBlockState(exteriorPos.down());
        if (below.getMaterial().isLiquid()) {
            return "Liquid: " + below.getBlock().getLocalizedName() + " below";
        }
        return "Liquid: none nearby";
    }

    private static boolean isPassable(WorldServer world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getMaterial().isReplaceable() || !state.getMaterial().blocksMovement();
    }

    private static void toggleDoor(EntityPlayerMP player, TardisWorldData data, TardisRecord record) {
        boolean locked = !record.isExteriorLocked();
        data.setExteriorLocked(record.getTardisId(), locked);
        data.setConsoleControlState(record.getTardisId(), ConsoleControlId.UTILITY_DOOR_LOCK, locked ? 1 : 0);
        send(player, locked ? TextFormatting.RED : TextFormatting.GREEN,
            "Exterior door " + (locked ? "locked." : "unlocked."));
    }

    private static void cycleInternalTime(EntityPlayerMP player, TardisWorldData data, TardisRecord record) {
        ConsoleControlId control = ConsoleControlId.UTILITY_INTERNAL_TIME;
        int next = (record.getConsoleState().getControlState(control) + 1) % control.getStateCount();
        data.setConsoleControlState(record.getTardisId(), control, next);
        String[] names = {"simulated Overworld", "permanent day", "permanent night"};
        send(player, TextFormatting.AQUA,
            "Interior time profile: " + names[next] + ". The profile is persistent; visual lighting follows in the interior-rendering pass.");
    }

    private static void selectShell(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record,
        int direction
    ) {
        ExteriorShell[] shells = ExteriorShell.values();
        ConsoleControlId screen = ConsoleControlId.UTILITY_SHELL_SCREEN;
        int selected = record.getConsoleState().getControlState(screen);
        int next = (selected + direction) % shells.length;
        if (next < 0) {
            next += shells.length;
        }
        data.setConsoleControlState(record.getTardisId(), screen, next);
        send(player, TextFormatting.AQUA,
            "Selected shell: " + shells[next].getDisplayName() + ". Use Apply to queue it.");
    }

    private static void showShell(EntityPlayerMP player, TardisRecord record) {
        ExteriorShell[] shells = ExteriorShell.values();
        int selected = Math.max(0, Math.min(shells.length - 1,
            record.getConsoleState().getControlState(ConsoleControlId.UTILITY_SHELL_SCREEN)));
        send(player, TextFormatting.AQUA,
            "Shell selection: " + shells[selected].getDisplayName()
                + " — active: " + record.getExteriorShell().getDisplayName());
    }

    private static void applyShell(EntityPlayerMP player, TardisWorldData data, TardisRecord record) {
        ExteriorShell[] shells = ExteriorShell.values();
        int selected = Math.max(0, Math.min(shells.length - 1,
            record.getConsoleState().getControlState(ConsoleControlId.UTILITY_SHELL_SCREEN)));
        ExteriorShell shell = shells[selected];
        data.setExteriorShell(record.getTardisId(), shell);
        send(player, TextFormatting.GREEN,
            shell.getDisplayName() + " queued for the next successful materialization.");
    }

    private static EnumFacing horizontal(EnumFacing facing) {
        return facing == null || facing.getAxis() == EnumFacing.Axis.Y ? EnumFacing.NORTH : facing;
    }

    private static String shortName(EnumFacing facing) {
        switch (horizontal(facing)) {
            case EAST:
                return "E";
            case SOUTH:
                return "S";
            case WEST:
                return "W";
            case NORTH:
            default:
                return "N";
        }
    }

    private static void send(EntityPlayerMP player, TextFormatting color, String text) {
        player.sendStatusMessage(
            new TextComponentString(color + "[TARDIS] " + TextFormatting.RESET + text),
            false
        );
    }
}
