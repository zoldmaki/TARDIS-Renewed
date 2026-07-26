package dev.zoldmaki.renewedtardis.flight;

import dev.zoldmaki.renewedtardis.block.BlockTardisExterior;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.dimension.TardisDimensionManager;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import dev.zoldmaki.renewedtardis.init.ModSounds;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisExterior;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

/** Server-authoritative prototype takeoff and landing state machine. */
public final class TardisFlightService {
    private TardisFlightService() {
    }

    public static boolean takeOff(EntityPlayerMP pilot, TardisRecord record, BlockPos controlPos) {
        MinecraftServer server = pilot.getServer();
        if (server == null) {
            return false;
        }
        if (record.isInFlight()) {
            send(pilot, TextFormatting.YELLOW, "This TARDIS is already in flight.");
            return false;
        }

        DimensionPosition destination = record.getDestinationPosition();
        if (destination == null) {
            send(
                pilot,
                TextFormatting.YELLOW,
                "No destination is set. Sneak-right-click a block with the bound key or use /tr destination."
            );
            return false;
        }
        if (destination.getDimension() == TardisDimensionManager.getDimensionId()) {
            send(pilot, TextFormatting.RED, "The exterior cannot land inside the TARDIS interior dimension.");
            return false;
        }

        DimensionPosition exteriorPosition = record.getExteriorPosition();
        if (exteriorPosition == null) {
            send(pilot, TextFormatting.RED, "This TARDIS has no placed exterior to dematerialize.");
            return false;
        }

        WorldServer exteriorWorld = getOrLoadWorld(server, exteriorPosition.getDimension());
        if (exteriorWorld == null) {
            send(pilot, TextFormatting.RED, "The exterior dimension could not be loaded.");
            return false;
        }

        BlockPos exteriorPos = exteriorPosition.getPosition();
        IBlockState exteriorState = exteriorWorld.getBlockState(exteriorPos);
        TileEntity tile = exteriorWorld.getTileEntity(exteriorPos);
        if (
            exteriorState.getBlock() != ModBlocks.TARDIS_EXTERIOR
                || !(tile instanceof TileEntityTardisExterior)
                || !record.getTardisId().equals(((TileEntityTardisExterior) tile).getTardisId())
        ) {
            send(pilot, TextFormatting.RED, "The saved exterior is missing or belongs to another TARDIS.");
            return false;
        }

        boolean locked = ((TileEntityTardisExterior) tile).isLocked();
        TardisWorldData data = TardisWorldData.get(server);
        if (!data.beginFlight(record.getTardisId(), locked)) {
            send(pilot, TextFormatting.RED, "The flight state could not be started.");
            return false;
        }

        exteriorWorld.playSound(null, exteriorPos, ModSounds.TAKEOFF, SoundCategory.BLOCKS, 1.0F, 1.0F);
        boolean removed = exteriorWorld.setBlockToAir(exteriorPos);
        if (!removed || exteriorWorld.getBlockState(exteriorPos).getBlock() == ModBlocks.TARDIS_EXTERIOR) {
            data.abortFlight(record.getTardisId());
            send(pilot, TextFormatting.RED, "The exterior could not dematerialize; takeoff was canceled.");
            return false;
        }

        pilot.getServerWorld().playSound(null, controlPos, ModSounds.TAKEOFF, SoundCategory.BLOCKS, 0.9F, 1.0F);
        send(pilot, TextFormatting.AQUA, "The TARDIS dematerializes and enters flight.");
        return true;
    }

    public static boolean land(EntityPlayerMP pilot, TardisRecord record, BlockPos controlPos) {
        MinecraftServer server = pilot.getServer();
        if (server == null) {
            return false;
        }
        if (!record.isInFlight()) {
            send(pilot, TextFormatting.YELLOW, "This TARDIS is already landed.");
            return false;
        }

        DimensionPosition destination = record.getDestinationPosition();
        if (destination == null) {
            send(pilot, TextFormatting.RED, "The in-flight TARDIS has no destination. Set one with /tr destination.");
            return false;
        }
        if (destination.getDimension() == TardisDimensionManager.getDimensionId()) {
            send(pilot, TextFormatting.RED, "The exterior cannot land inside the TARDIS interior dimension.");
            return false;
        }

        WorldServer targetWorld = getOrLoadWorld(server, destination.getDimension());
        if (targetWorld == null) {
            send(pilot, TextFormatting.RED, "The destination dimension could not be loaded. The TARDIS remains in flight.");
            return false;
        }

        BlockPos target = destination.getPosition();
        if (target.getY() < 1 || target.getY() > 254) {
            send(pilot, TextFormatting.RED, "The destination height is outside the safe build range.");
            return false;
        }
        if (!targetWorld.isAirBlock(target) || !targetWorld.isAirBlock(target.up())) {
            send(pilot, TextFormatting.RED, "The destination is obstructed. The TARDIS remains in flight.");
            return false;
        }

        EnumFacing facing = record.getDestinationFacing();
        IBlockState exteriorState = ModBlocks.TARDIS_EXTERIOR
            .getDefaultState()
            .withProperty(BlockTardisExterior.FACING, facing);
        if (!targetWorld.setBlockState(target, exteriorState, 3)) {
            send(pilot, TextFormatting.RED, "The exterior could not materialize. The TARDIS remains in flight.");
            return false;
        }

        TileEntity tile = targetWorld.getTileEntity(target);
        if (!(tile instanceof TileEntityTardisExterior)) {
            targetWorld.setBlockToAir(target);
            send(pilot, TextFormatting.RED, "The exterior tile entity failed to form. The TARDIS remains in flight.");
            return false;
        }

        TileEntityTardisExterior exterior = (TileEntityTardisExterior) tile;
        exterior.bind(record);
        exterior.setLocked(record.isExteriorLocked());

        TardisWorldData data = TardisWorldData.get(server);
        if (!data.completeFlight(record.getTardisId(), new DimensionPosition(destination.getDimension(), target))) {
            targetWorld.setBlockToAir(target);
            send(pilot, TextFormatting.RED, "The landing state could not be saved. The TARDIS remains in flight.");
            return false;
        }

        targetWorld.playSound(null, target, ModSounds.LANDING, SoundCategory.BLOCKS, 1.0F, 1.0F);
        pilot.getServerWorld().playSound(
            null,
            controlPos,
            ModSounds.LANDING_INTERIOR,
            SoundCategory.BLOCKS,
            0.9F,
            1.0F
        );
        send(
            pilot,
            TextFormatting.GREEN,
            "The TARDIS materializes at " + destination.toString() + "."
        );
        return true;
    }

    private static WorldServer getOrLoadWorld(MinecraftServer server, int dimension) {
        WorldServer world = server.getWorld(dimension);
        if (world != null) {
            return world;
        }
        if (!DimensionManager.isDimensionRegistered(dimension)) {
            return null;
        }

        DimensionManager.initDimension(dimension);
        return server.getWorld(dimension);
    }

    private static void send(EntityPlayerMP player, TextFormatting color, String text) {
        player.sendStatusMessage(
            new TextComponentString(color + "[TARDIS] " + TextFormatting.RESET + text),
            false
        );
    }
}
