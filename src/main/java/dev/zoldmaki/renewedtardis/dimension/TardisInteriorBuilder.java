package dev.zoldmaki.renewedtardis.dimension;

import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisFlightConsole;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisInteriorDoor;
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
        ensureAndBindConsole(world, InteriorAddress.getFlightConsolePosition(center), record);
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

    private static void ensureAndBindConsole(WorldServer world, BlockPos consolePos, TardisRecord record) {
        if (world.getBlockState(consolePos).getBlock() != ModBlocks.TARDIS_FLIGHT_CONSOLE) {
            world.setBlockState(consolePos, ModBlocks.TARDIS_FLIGHT_CONSOLE.getDefaultState(), 3);
        }

        TileEntity tile = world.getTileEntity(consolePos);
        if (!(tile instanceof TileEntityTardisFlightConsole)) {
            throw new IllegalStateException("The generated TARDIS flight console has no tile entity");
        }
        ((TileEntityTardisFlightConsole) tile).bind(record.getTardisId());
    }
}
