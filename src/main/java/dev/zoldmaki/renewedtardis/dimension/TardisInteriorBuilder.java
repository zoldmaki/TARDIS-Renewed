package dev.zoldmaki.renewedtardis.dimension;

import dev.zoldmaki.renewedtardis.block.BlockTardisConsoleControl;
import dev.zoldmaki.renewedtardis.console.ConsolePanel;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import dev.zoldmaki.renewedtardis.interior.InteriorRoomManager;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisConsoleControl;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisConsolePanel;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisEngineCore;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisFlightConsole;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisInteriorDoor;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisTransmatPad;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

/** Builds the first deliberately simple and replaceable TARDIS room. */
public final class TardisInteriorBuilder {
    private static final int HALF_SIZE = 5;
    private static final int ROOM_HEIGHT = 6;

    private TardisInteriorBuilder() {
    }

    public static void ensureGenerated(WorldServer world, TardisWorldData data, TardisRecord record) {
        DimensionPosition address = data.getOrCreateInterior(record);
        BlockPos center = address.getPosition();

        if (!record.isInteriorGenerated()) {
            buildRoom(world, center);
            data.setInteriorGenerated(record.getTardisId(), true);
        }

        ensureAndBindDoor(world, InteriorAddress.getDoorPosition(center), record);
        ensureAndBindLegacyConsole(world, InteriorAddress.getFlightConsolePosition(center), record);
        for (ConsolePanel panel : ConsolePanel.values()) {
            ensureAndBindConsolePanel(
                world,
                InteriorAddress.getConsolePanelPosition(center, panel),
                panel,
                record
            );
        }
        ensureAndBindControl(
            world,
            InteriorAddress.getTakeoffControlPosition(center),
            ModBlocks.TARDIS_TAKEOFF_CONTROL,
            record
        );
        ensureAndBindControl(
            world,
            InteriorAddress.getLandingControlPosition(center),
            ModBlocks.TARDIS_LANDING_CONTROL,
            record
        );
        ensureAndBindControl(
            world,
            InteriorAddress.getDestinationControlPosition(center),
            ModBlocks.TARDIS_DESTINATION_CONTROL,
            record
        );
        ensureAndBindControl(
            world,
            InteriorAddress.getFacingControlPosition(center),
            ModBlocks.TARDIS_FACING_CONTROL,
            record
        );
        ensureAndBindControl(
            world,
            InteriorAddress.getStatusControlPosition(center),
            ModBlocks.TARDIS_STATUS_CONTROL,
            record
        );
        ensureAndBindEngineCore(world, InteriorAddress.getEngineCorePosition(center), record);
        ensureAndBindTransmat(world, data, center, record);
        InteriorRoomManager.ensureGeneratedRooms(world, data, record);
    }

    private static void buildRoom(WorldServer world, BlockPos center) {
        IBlockState floor = ModBlocks.CHRONOSTEEL_BLOCK.getDefaultState();
        IBlockState wall = Blocks.QUARTZ_BLOCK.getDefaultState();
        IBlockState air = Blocks.AIR.getDefaultState();
        IBlockState light = Blocks.SEA_LANTERN.getDefaultState();

        int minX = center.getX() - HALF_SIZE;
        int maxX = center.getX() + HALF_SIZE;
        int minZ = center.getZ() - HALF_SIZE;
        int maxZ = center.getZ() + HALF_SIZE;
        int floorY = center.getY();
        int ceilingY = floorY + ROOM_HEIGHT;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlockState(new BlockPos(x, floorY, z), floor, 3);
                world.setBlockState(new BlockPos(x, ceilingY, z), wall, 3);

                for (int y = floorY + 1; y < ceilingY; y++) {
                    boolean wallEdge = x == minX || x == maxX || z == minZ || z == maxZ;
                    world.setBlockState(new BlockPos(x, y, z), wallEdge ? wall : air, 3);
                }
            }
        }

        int lightY = ceilingY - 1;
        world.setBlockState(center.add(0, lightY - center.getY(), 0), light, 3);
        world.setBlockState(new BlockPos(minX + 1, lightY, minZ + 1), light, 3);
        world.setBlockState(new BlockPos(maxX - 1, lightY, minZ + 1), light, 3);
        world.setBlockState(new BlockPos(minX + 1, lightY, maxZ - 1), light, 3);
        world.setBlockState(new BlockPos(maxX - 1, lightY, maxZ - 1), light, 3);

        world.setBlockState(
            InteriorAddress.getDoorPosition(center),
            ModBlocks.TARDIS_INTERIOR_DOOR.getDefaultState(),
            3
        );
        world.setBlockState(
            InteriorAddress.getFlightConsolePosition(center),
            ModBlocks.TARDIS_FLIGHT_CONSOLE.getDefaultState(),
            3
        );
        world.setBlockState(
            InteriorAddress.getTakeoffControlPosition(center),
            ModBlocks.TARDIS_TAKEOFF_CONTROL.getDefaultState(),
            3
        );
        world.setBlockState(
            InteriorAddress.getLandingControlPosition(center),
            ModBlocks.TARDIS_LANDING_CONTROL.getDefaultState(),
            3
        );
        world.setBlockState(
            InteriorAddress.getDestinationControlPosition(center),
            ModBlocks.TARDIS_DESTINATION_CONTROL.getDefaultState(),
            3
        );
        world.setBlockState(
            InteriorAddress.getFacingControlPosition(center),
            ModBlocks.TARDIS_FACING_CONTROL.getDefaultState(),
            3
        );
        world.setBlockState(
            InteriorAddress.getStatusControlPosition(center),
            ModBlocks.TARDIS_STATUS_CONTROL.getDefaultState(),
            3
        );
        world.setBlockState(
            InteriorAddress.getEngineCorePosition(center),
            ModBlocks.TARDIS_ENGINE_CORE.getDefaultState(),
            3
        );
        world.setBlockState(
            InteriorAddress.getTransmatPosition(center),
            ModBlocks.TARDIS_TRANSMAT_PAD.getDefaultState(),
            3
        );
    }

    private static void ensureAndBindDoor(WorldServer world, BlockPos doorPos, TardisRecord record) {
        if (world.getBlockState(doorPos).getBlock() != ModBlocks.TARDIS_INTERIOR_DOOR) {
            world.setBlockState(doorPos, ModBlocks.TARDIS_INTERIOR_DOOR.getDefaultState(), 3);
        }

        TileEntity tile = world.getTileEntity(doorPos);
        if (!(tile instanceof TileEntityTardisInteriorDoor)) {
            throw new IllegalStateException("The generated TARDIS interior door has no tile entity");
        }
        ((TileEntityTardisInteriorDoor) tile).bind(record.getTardisId());
    }

    private static void ensureAndBindLegacyConsole(WorldServer world, BlockPos consolePos, TardisRecord record) {
        if (world.getBlockState(consolePos).getBlock() != ModBlocks.TARDIS_FLIGHT_CONSOLE) {
            world.setBlockState(consolePos, ModBlocks.TARDIS_FLIGHT_CONSOLE.getDefaultState(), 3);
        }

        TileEntity tile = world.getTileEntity(consolePos);
        if (!(tile instanceof TileEntityTardisFlightConsole)) {
            throw new IllegalStateException("The generated legacy TARDIS flight console has no tile entity");
        }
        ((TileEntityTardisFlightConsole) tile).bind(record.getTardisId());
    }

    private static void ensureAndBindConsolePanel(
        WorldServer world,
        BlockPos panelPos,
        ConsolePanel panel,
        TardisRecord record
    ) {
        IBlockState desired = ModBlocks.TARDIS_CONSOLE_PANEL.getDefaultState()
            .withProperty(dev.zoldmaki.renewedtardis.block.BlockTardisConsolePanel.FACING, panel.getOutwardFacing());
        if (world.getBlockState(panelPos).getBlock() != ModBlocks.TARDIS_CONSOLE_PANEL
            || !world.getBlockState(panelPos).equals(desired)) {
            world.setBlockState(panelPos, desired, 3);
        }

        TileEntity tile = world.getTileEntity(panelPos);
        if (!(tile instanceof TileEntityTardisConsolePanel)) {
            throw new IllegalStateException("A generated TARDIS console panel has no tile entity");
        }
        TileEntityTardisConsolePanel panelTile = (TileEntityTardisConsolePanel) tile;
        panelTile.bind(record.getTardisId());
        panelTile.syncWaypointMetadata(record.getConsoleState());
    }

    private static void ensureAndBindEngineCore(WorldServer world, BlockPos corePos, TardisRecord record) {
        if (world.getBlockState(corePos).getBlock() != ModBlocks.TARDIS_ENGINE_CORE) {
            world.setBlockState(corePos, ModBlocks.TARDIS_ENGINE_CORE.getDefaultState(), 3);
        }

        TileEntity tile = world.getTileEntity(corePos);
        if (!(tile instanceof TileEntityTardisEngineCore)) {
            throw new IllegalStateException("The generated TARDIS engine core has no tile entity");
        }
        ((TileEntityTardisEngineCore) tile).bind(record.getTardisId());
    }


    private static void ensureAndBindTransmat(
        WorldServer world,
        TardisWorldData data,
        BlockPos center,
        TardisRecord record
    ) {
        DimensionPosition saved = record.getTransmatPosition();
        BlockPos defaultPos = InteriorAddress.getTransmatPosition(center);
        BlockPos padPos = defaultPos;
        if (saved != null
            && saved.getDimension() == world.provider.getDimension()
            && world.getBlockState(saved.getPosition()).getBlock() == ModBlocks.TARDIS_TRANSMAT_PAD) {
            padPos = saved.getPosition();
        }

        if (world.getBlockState(padPos).getBlock() != ModBlocks.TARDIS_TRANSMAT_PAD) {
            world.setBlockState(padPos, ModBlocks.TARDIS_TRANSMAT_PAD.getDefaultState(), 3);
        }
        TileEntity tile = world.getTileEntity(padPos);
        if (!(tile instanceof TileEntityTardisTransmatPad)) {
            throw new IllegalStateException("The generated TARDIS transmat pad has no tile entity");
        }
        ((TileEntityTardisTransmatPad) tile).bind(record.getTardisId());
        if (saved == null
            || saved.getDimension() != world.provider.getDimension()
            || !saved.getPosition().equals(padPos)) {
            data.setTransmatPosition(
                record.getTardisId(),
                new DimensionPosition(world.provider.getDimension(), padPos)
            );
        }
    }

    private static void ensureAndBindControl(
        WorldServer world,
        BlockPos controlPos,
        BlockTardisConsoleControl control,
        TardisRecord record
    ) {
        if (world.getBlockState(controlPos).getBlock() != control) {
            world.setBlockState(controlPos, control.getDefaultState(), 3);
        }

        TileEntity tile = world.getTileEntity(controlPos);
        if (!(tile instanceof TileEntityTardisConsoleControl)) {
            throw new IllegalStateException("A generated TARDIS console control has no tile entity");
        }
        ((TileEntityTardisConsoleControl) tile).bind(record.getTardisId());
    }
}
