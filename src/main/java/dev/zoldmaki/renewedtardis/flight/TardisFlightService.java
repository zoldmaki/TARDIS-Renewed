package dev.zoldmaki.renewedtardis.flight;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.block.BlockTardisExterior;
import dev.zoldmaki.renewedtardis.config.TardisConfig;
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

/** Server-authoritative timed takeoff and landing state machine. */
public final class TardisFlightService {
    public enum LandingMode {
        EXACT,
        NEAREST_SAFE,
        DEPARTURE
    }

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

        EnumFacing departureFacing = exteriorState.getValue(BlockTardisExterior.FACING);
        boolean locked = ((TileEntityTardisExterior) tile).isLocked();
        long startedAt = System.currentTimeMillis();
        long durationMillis = calculateFlightDurationMillis(exteriorPosition, destination);
        long readyAt = startedAt + durationMillis;

        TardisWorldData data = TardisWorldData.get(server);
        if (!data.beginFlight(record.getTardisId(), locked, startedAt, readyAt, departureFacing)) {
            send(pilot, TextFormatting.RED, "The flight state could not be started.");
            return false;
        }

        exteriorWorld.playSound(null, exteriorPos, ModSounds.TAKEOFF, SoundCategory.BLOCKS, 1.0F, 1.0F);
        boolean removed = BlockTardisExterior.removeStructure(exteriorWorld, exteriorPos);
        if (
            !removed
                || exteriorWorld.getBlockState(exteriorPos).getBlock() == ModBlocks.TARDIS_EXTERIOR
                || exteriorWorld.getBlockState(exteriorPos.up()).getBlock() == ModBlocks.TARDIS_EXTERIOR_TOP
        ) {
            data.abortFlight(record.getTardisId());
            send(pilot, TextFormatting.RED, "The exterior could not dematerialize; takeoff was canceled.");
            return false;
        }

        pilot.getServerWorld().playSound(null, controlPos, ModSounds.TAKEOFF, SoundCategory.BLOCKS, 0.9F, 1.0F);
        send(
            pilot,
            TextFormatting.AQUA,
            "The TARDIS enters flight. Estimated travel time: " + formatDuration(durationMillis) + "."
        );
        return true;
    }

    public static boolean land(EntityPlayerMP pilot, TardisRecord record, BlockPos controlPos) {
        return land(pilot, record, controlPos, LandingMode.EXACT, false);
    }

    public static boolean land(
        EntityPlayerMP pilot,
        TardisRecord record,
        BlockPos controlPos,
        LandingMode mode,
        boolean forceLanding
    ) {
        MinecraftServer server = pilot.getServer();
        if (server == null) {
            return false;
        }
        if (!record.isInFlight()) {
            send(pilot, TextFormatting.YELLOW, "This TARDIS is already landed.");
            return false;
        }

        long now = System.currentTimeMillis();
        if (!forceLanding && !record.isFlightReady(now)) {
            send(
                pilot,
                TextFormatting.YELLOW,
                "The TARDIS is still travelling: " + record.getFlightProgressPercent(now) + "% complete, "
                    + formatDuration(record.getFlightRemainingMillis(now)) + " remaining."
            );
            return false;
        }

        DimensionPosition requestedPosition;
        EnumFacing requestedFacing;
        if (mode == LandingMode.DEPARTURE) {
            requestedPosition = record.getFlightDeparture();
            requestedFacing = record.getFlightDepartureFacing();
            if (requestedPosition == null) {
                send(pilot, TextFormatting.RED, "The original departure position is unavailable.");
                return false;
            }
        } else {
            requestedPosition = record.getDestinationPosition();
            requestedFacing = record.getDestinationFacing();
            if (requestedPosition == null) {
                send(pilot, TextFormatting.RED, "The in-flight TARDIS has no destination. Set one with /tr destination.");
                return false;
            }
        }

        if (requestedPosition.getDimension() == TardisDimensionManager.getDimensionId()) {
            send(pilot, TextFormatting.RED, "The exterior cannot land inside the TARDIS interior dimension.");
            return false;
        }

        WorldServer targetWorld = getOrLoadWorld(server, requestedPosition.getDimension());
        if (targetWorld == null) {
            send(pilot, TextFormatting.RED, "The landing dimension could not be loaded. The TARDIS remains in flight.");
            return false;
        }

        BlockPos requestedTarget = requestedPosition.getPosition();
        cleanupOrphanedExteriorTop(targetWorld, requestedTarget);

        BlockPos actualTarget = requestedTarget;
        if (forceLanding) {
            if (requestedTarget.getY() < 0 || requestedTarget.getY() > 254) {
                send(
                    pilot,
                    TextFormatting.RED,
                    "Even a forced landing needs its lower block between Y=0 and Y=254."
                );
                return false;
            }
        } else if (mode == LandingMode.NEAREST_SAFE || mode == LandingMode.DEPARTURE) {
            actualTarget = findNearestSafeLanding(
                targetWorld,
                requestedTarget,
                TardisConfig.getSafeLandingSearchRadius()
            );
            if (actualTarget == null) {
                send(
                    pilot,
                    TextFormatting.RED,
                    "No safe landing space was found within " + TardisConfig.getSafeLandingSearchRadius()
                        + " blocks. The TARDIS remains in flight."
                );
                return false;
            }
        } else {
            if (requestedTarget.getY() < 1 || requestedTarget.getY() > 254) {
                send(pilot, TextFormatting.RED, "The destination height is outside the safe build range.");
                return false;
            }
            if (!isLandingSpaceClear(targetWorld, requestedTarget)) {
                send(
                    pilot,
                    TextFormatting.RED,
                    "The exact destination is obstructed. Use /tr land nearest, /tr land departure, or an operator force landing."
                );
                return false;
            }
        }

        IBlockState exteriorState = ModBlocks.TARDIS_EXTERIOR
            .getDefaultState()
            .withProperty(BlockTardisExterior.FACING, requestedFacing)
            .withProperty(BlockTardisExterior.SHELL, record.getExteriorShell());
        BlockTardisExterior.StructurePlacementResult placement =
            BlockTardisExterior.placeStructureDetailed(
                targetWorld,
                actualTarget,
                exteriorState,
                forceLanding
            );
        if (!placement.isSuccess()) {
            if (RenewedTardisMod.logger != null) {
                RenewedTardisMod.logger.error(
                    "TARDIS {} materialization failed: {}. {}",
                    record.getTardisId(),
                    placement.getReason(),
                    placement.getDetails()
                );
            }
            send(
                pilot,
                TextFormatting.RED,
                "Materialization failed: " + placement.getReason() + ". The TARDIS remains in flight."
            );
            send(pilot, TextFormatting.GRAY, "Debug: " + placement.getDetails());
            return false;
        }

        TileEntity tile = targetWorld.getTileEntity(actualTarget);
        if (!(tile instanceof TileEntityTardisExterior)) {
            BlockTardisExterior.removeStructure(targetWorld, actualTarget);
            send(pilot, TextFormatting.RED, "The exterior tile entity failed to form. The TARDIS remains in flight.");
            return false;
        }

        TileEntityTardisExterior exterior = (TileEntityTardisExterior) tile;
        exterior.bind(record);
        exterior.setLocked(record.isExteriorLocked());

        TardisWorldData data = TardisWorldData.get(server);
        DimensionPosition landedPosition = new DimensionPosition(requestedPosition.getDimension(), actualTarget);
        if (!data.completeFlight(record.getTardisId(), landedPosition)) {
            BlockTardisExterior.removeStructure(targetWorld, actualTarget);
            send(pilot, TextFormatting.RED, "The landing state could not be saved. The TARDIS remains in flight.");
            return false;
        }

        targetWorld.playSound(null, actualTarget, ModSounds.LANDING, SoundCategory.BLOCKS, 1.0F, 1.0F);
        pilot.getServerWorld().playSound(
            null,
            controlPos,
            ModSounds.LANDING_INTERIOR,
            SoundCategory.BLOCKS,
            0.9F,
            1.0F
        );

        String recoveryNote = actualTarget.equals(requestedTarget)
            ? ""
            : " (nearest safe position to " + requestedPosition.toString() + ")";
        send(
            pilot,
            TextFormatting.GREEN,
            "The TARDIS materializes at " + landedPosition.toString() + recoveryNote + "."
        );
        return true;
    }

    public static long calculateFlightDurationMillis(
        DimensionPosition departure,
        DimensionPosition destination
    ) {
        long seconds = TardisConfig.getMinimumFlightSeconds();
        if (departure.getDimension() == destination.getDimension()) {
            BlockPos from = departure.getPosition();
            BlockPos to = destination.getPosition();
            double dx = (double) to.getX() - from.getX();
            double dz = (double) to.getZ() - from.getZ();
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            seconds += (long) Math.floor(horizontalDistance / TardisConfig.getBlocksPerExtraSecond());
        } else {
            seconds += TardisConfig.getCrossDimensionPenaltySeconds();
        }

        seconds = Math.max(TardisConfig.getMinimumFlightSeconds(), seconds);
        seconds = Math.min(TardisConfig.getMaximumFlightSeconds(), seconds);
        return seconds * 1000L;
    }

    public static String describeFlightProgress(TardisRecord record, long nowMillis) {
        if (!record.isInFlight()) {
            return "LANDED";
        }
        if (record.isFlightReady(nowMillis)) {
            return "READY TO LAND (100%)";
        }
        return record.getFlightProgressPercent(nowMillis) + "% complete, "
            + formatDuration(record.getFlightRemainingMillis(nowMillis)) + " remaining";
    }

    public static String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, (millis + 999L) / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes <= 0L) {
            return seconds + "s";
        }
        return minutes + "m " + seconds + "s";
    }

    private static BlockPos findNearestSafeLanding(WorldServer world, BlockPos center, int radius) {
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (ring > 0 && Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        continue;
                    }
                    for (int verticalDistance = 0; verticalDistance <= 8; verticalDistance++) {
                        if (verticalDistance == 0) {
                            BlockPos candidate = center.add(dx, 0, dz);
                            if (isSafeLandingSpot(world, candidate)) {
                                return candidate;
                            }
                        } else {
                            BlockPos above = center.add(dx, verticalDistance, dz);
                            if (isSafeLandingSpot(world, above)) {
                                return above;
                            }
                            BlockPos below = center.add(dx, -verticalDistance, dz);
                            if (isSafeLandingSpot(world, below)) {
                                return below;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafeLandingSpot(WorldServer world, BlockPos target) {
        if (target.getY() < 1 || target.getY() > 254 || !isLandingSpaceClear(world, target)) {
            return false;
        }
        IBlockState support = world.getBlockState(target.down());
        return support.getMaterial().isSolid() && support.isFullCube();
    }

    private static boolean isLandingSpaceClear(WorldServer world, BlockPos target) {
        return isReplaceableForLanding(world, target) && isReplaceableForLanding(world, target.up());
    }

    private static boolean isReplaceableForLanding(WorldServer world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return world.isAirBlock(pos) || state.getMaterial().isReplaceable();
    }

    private static void cleanupOrphanedExteriorTop(WorldServer world, BlockPos target) {
        IBlockState lower = world.getBlockState(target);
        if (
            lower.getBlock() == ModBlocks.TARDIS_EXTERIOR_TOP
                && world.getBlockState(target.down()).getBlock() != ModBlocks.TARDIS_EXTERIOR
        ) {
            world.setBlockToAir(target);
        }

        IBlockState upper = world.getBlockState(target.up());
        if (
            upper.getBlock() == ModBlocks.TARDIS_EXTERIOR_TOP
                && world.getBlockState(target).getBlock() != ModBlocks.TARDIS_EXTERIOR
        ) {
            world.setBlockToAir(target.up());
        }
    }

    private static boolean prepareForcedLandingSpace(WorldServer world, BlockPos target) {
        clearBlockForForcedLanding(world, target);
        clearBlockForForcedLanding(world, target.up());
        return world.isAirBlock(target) && world.isAirBlock(target.up());
    }

    private static void clearBlockForForcedLanding(WorldServer world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (
            state.getBlock() == ModBlocks.TARDIS_EXTERIOR
                || state.getBlock() == ModBlocks.TARDIS_EXTERIOR_TOP
        ) {
            BlockTardisExterior.removeStructure(world, pos);
        } else if (!world.isAirBlock(pos)) {
            world.setBlockToAir(pos);
        }
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
