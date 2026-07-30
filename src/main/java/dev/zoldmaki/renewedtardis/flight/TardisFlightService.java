package dev.zoldmaki.renewedtardis.flight;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.block.BlockTardisExterior;
import dev.zoldmaki.renewedtardis.config.TardisConfig;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.damage.TardisDamageService;
import dev.zoldmaki.renewedtardis.dimension.TardisDimensionManager;
import dev.zoldmaki.renewedtardis.energy.TardisEnergyService;
import dev.zoldmaki.renewedtardis.event.TardisFlightEvent;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import dev.zoldmaki.renewedtardis.init.ModSounds;
import dev.zoldmaki.renewedtardis.teleport.TardisTeleportService;
import dev.zoldmaki.renewedtardis.upgrade.TardisUpgradeService;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisExterior;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;

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

        String restriction = TardisConfig.describeFlightRestriction(
            exteriorPosition.getDimension(),
            destination.getDimension()
        );
        if (!restriction.isEmpty()) {
            send(pilot, TextFormatting.RED, restriction);
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
        String systemFailure = TardisDamageService.getTakeoffFailure(record);
        if (!systemFailure.isEmpty()) {
            send(pilot, TextFormatting.RED, systemFailure + " Repair the damaged subsystem at the engine core.");
            return false;
        }
        long startedAt = System.currentTimeMillis();
        DimensionPosition resolvedDestination = resolveFlightTarget(server, record, destination, startedAt);
        long durationMillis = TardisUpgradeService.applyFlightDuration(
            record,
            TardisDamageService.applyDurationPenalty(
                record,
                calculateFlightDurationMillis(exteriorPosition, destination, record.getFlightSpeed())
            )
        );
        long readyAt = startedAt + durationMillis;
        long energyCost = TardisEnergyService.calculateFlightCost(record, exteriorPosition, destination);

        TardisFlightEvent.Takeoff takeoffEvent = new TardisFlightEvent.Takeoff(
            pilot,
            record,
            exteriorPosition,
            resolvedDestination,
            durationMillis
        );
        if (MinecraftForge.EVENT_BUS.post(takeoffEvent)) {
            send(pilot, TextFormatting.YELLOW, "Takeoff was prevented by another mod or server system.");
            return false;
        }

        TardisWorldData data = TardisWorldData.get(server);
        long availableEnergy = TardisEnergyService.refresh(data, record);
        if (availableEnergy < energyCost) {
            send(
                pilot,
                TextFormatting.RED,
                "Insufficient Artron energy: flight needs " + TardisEnergyService.format(energyCost)
                    + " AU, but only " + TardisEnergyService.format(availableEnergy) + " AU is available."
            );
            return false;
        }
        if (!TardisEnergyService.consume(data, record, energyCost)) {
            send(pilot, TextFormatting.RED, "The flight energy cost could not be reserved.");
            return false;
        }
        if (!data.beginFlight(
            record.getTardisId(),
            locked,
            startedAt,
            readyAt,
            departureFacing,
            resolvedDestination
        )) {
            TardisEnergyService.refund(data, record, energyCost);
            send(pilot, TextFormatting.RED, "The flight state could not be started. Its Artron cost was refunded.");
            return false;
        }

        exteriorWorld.playSound(null, exteriorPos, ModSounds.TAKEOFF, SoundCategory.BLOCKS, 1.0F, 1.0F);
        boolean removed;
        try {
            removed = BlockTardisExterior.removeStructure(exteriorWorld, exteriorPos);
        } catch (RuntimeException exception) {
            data.abortFlight(record.getTardisId());
            TardisEnergyService.refund(data, record, energyCost);
            if (RenewedTardisMod.logger != null) {
                RenewedTardisMod.logger.error(
                    "TARDIS {} threw an exception while dematerializing; flight and energy were rolled back.",
                    record.getTardisId(),
                    exception
                );
            }
            send(
                pilot,
                TextFormatting.RED,
                "The exterior failed while dematerializing; takeoff was canceled and its Artron cost was refunded."
            );
            return false;
        }
        if (
            !removed
                || exteriorWorld.getBlockState(exteriorPos).getBlock() == ModBlocks.TARDIS_EXTERIOR
                || exteriorWorld.getBlockState(exteriorPos.up()).getBlock() == ModBlocks.TARDIS_EXTERIOR_TOP
        ) {
            data.abortFlight(record.getTardisId());
            TardisEnergyService.refund(data, record, energyCost);
            send(
                pilot,
                TextFormatting.RED,
                "The exterior could not dematerialize; takeoff was canceled and its Artron cost was refunded."
            );
            return false;
        }

        TardisDamageService.applyTakeoffWear(data, record, exteriorPosition, destination);
        pilot.getServerWorld().playSound(null, controlPos, ModSounds.TAKEOFF, SoundCategory.BLOCKS, 0.9F, 1.0F);
        String drift = resolvedDestination.equals(destination)
            ? ""
            : "; resolved landing target: " + resolvedDestination.toString();
        send(
            pilot,
            TextFormatting.AQUA,
            "The TARDIS enters flight. Estimated travel time: " + formatDuration(durationMillis)
                + "; speed: " + record.getFlightSpeed().getName()
                + "; accuracy: " + record.getFlightAccuracy().getName()
                + drift
                + "; Artron cost: " + TardisEnergyService.format(energyCost) + " AU; remaining: "
                + TardisEnergyService.format(record.getArtronEnergy()) + " AU; systems: "
                + TardisDamageService.describe(record) + "."
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

        DimensionPosition flightDeparture = record.getFlightDeparture();
        DimensionPosition requestedPosition;
        EnumFacing requestedFacing;
        if (mode == LandingMode.DEPARTURE) {
            requestedPosition = flightDeparture;
            requestedFacing = record.getFlightDepartureFacing();
            if (requestedPosition == null) {
                send(pilot, TextFormatting.RED, "The original departure position is unavailable.");
                return false;
            }
        } else {
            requestedPosition = record.getFlightResolvedDestination();
            if (requestedPosition == null) {
                requestedPosition = record.getDestinationPosition();
            }
            requestedFacing = record.getDestinationFacing();
            if (requestedPosition == null) {
                send(pilot, TextFormatting.RED, "The in-flight TARDIS has no resolved destination.");
                return false;
            }
        }

        if (requestedPosition.getDimension() == TardisDimensionManager.getDimensionId()) {
            send(pilot, TextFormatting.RED, "The exterior cannot land inside the TARDIS interior dimension.");
            return false;
        }
        if (!forceLanding && flightDeparture != null) {
            String restriction = TardisConfig.describeFlightRestriction(
                flightDeparture.getDimension(),
                requestedPosition.getDimension()
            );
            if (!restriction.isEmpty()) {
                send(pilot, TextFormatting.RED, restriction + " The TARDIS remains in flight.");
                return false;
            }
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

        DimensionPosition actualPosition = new DimensionPosition(requestedPosition.getDimension(), actualTarget);
        TardisFlightEvent.Landing landingEvent = new TardisFlightEvent.Landing(
            pilot,
            record,
            flightDeparture,
            requestedPosition,
            actualTarget,
            mode,
            forceLanding
        );
        if (MinecraftForge.EVENT_BUS.post(landingEvent)) {
            send(pilot, TextFormatting.YELLOW, "Landing was prevented by another mod or server system.");
            return false;
        }

        List<EntityPlayerMP> materializationOccupants = findMaterializationOccupants(targetWorld, actualTarget);

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
        if (!data.completeFlight(record.getTardisId(), actualPosition)) {
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

        int capturedPlayers = moveMaterializationOccupants(
            materializationOccupants,
            record,
            targetWorld,
            actualTarget,
            requestedFacing
        );
        MinecraftForge.EVENT_BUS.post(
            new TardisFlightEvent.Landed(pilot, record, flightDeparture, actualPosition, capturedPlayers)
        );

        String recoveryNote = actualTarget.equals(requestedTarget)
            ? ""
            : " (nearest safe position to " + requestedPosition.toString() + ")";
        String occupantNote = capturedPlayers == 0
            ? ""
            : " " + capturedPlayers + " player" + (capturedPlayers == 1 ? " was" : "s were")
                + " moved safely into the interior.";
        send(
            pilot,
            TextFormatting.GREEN,
            "The TARDIS materializes at " + actualPosition.toString() + recoveryNote + "." + occupantNote
        );
        return true;
    }

    public static long calculateFlightDurationMillis(
        DimensionPosition departure,
        DimensionPosition destination
    ) {
        return calculateFlightDurationMillis(departure, destination, FlightSpeed.NORMAL);
    }

    public static long calculateFlightDurationMillis(
        DimensionPosition departure,
        DimensionPosition destination,
        FlightSpeed speed
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
        long normalDuration = seconds * 1000L;
        return (speed == null ? FlightSpeed.NORMAL : speed).applyToDuration(normalDuration);
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

    public static String describeFlightProfile(TardisRecord record) {
        return "speed=" + record.getFlightSpeed().getName()
            + ", accuracy=" + record.getFlightAccuracy().getName()
            + ", accelerator upgrades=" + record.getUpgradeCount(
                dev.zoldmaki.renewedtardis.upgrade.TardisUpgradeType.SPEED
            );
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

    private static DimensionPosition resolveFlightTarget(
        MinecraftServer server,
        TardisRecord record,
        DimensionPosition destination,
        long startedAtMillis
    ) {
        int radius = record.getFlightAccuracy().getLandingRadius()
            + TardisDamageService.getNavigationDriftBonus(record);
        if (radius <= 0) {
            return destination;
        }

        WorldServer world = getOrLoadWorld(server, destination.getDimension());
        if (world == null) {
            return destination;
        }

        BlockPos center = destination.getPosition();
        List<BlockPos> driftCandidates = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                BlockPos candidate = center.add(dx, 0, dz);
                if (candidate.getY() >= 1 && candidate.getY() <= 254 && isLandingSpaceClear(world, candidate)) {
                    driftCandidates.add(candidate);
                }
            }
        }
        if (driftCandidates.isEmpty()) {
            return destination;
        }

        long seed = startedAtMillis
            ^ record.getTardisId().getMostSignificantBits()
            ^ record.getTardisId().getLeastSignificantBits()
            ^ center.toLong();
        BlockPos resolved = driftCandidates.get(new Random(seed).nextInt(driftCandidates.size()));
        return new DimensionPosition(destination.getDimension(), resolved);
    }

    private static List<EntityPlayerMP> findMaterializationOccupants(WorldServer world, BlockPos target) {
        AxisAlignedBB volume = new AxisAlignedBB(
            target.getX() - 0.05D,
            target.getY() - 0.05D,
            target.getZ() - 0.05D,
            target.getX() + 1.05D,
            target.getY() + 2.05D,
            target.getZ() + 1.05D
        );
        List<EntityPlayerMP> players = world.getEntitiesWithinAABB(EntityPlayerMP.class, volume);
        List<EntityPlayerMP> valid = new ArrayList<>();
        for (EntityPlayerMP player : players) {
            if (player != null && !player.isDead && !player.isSpectator()) {
                valid.add(player);
            }
        }
        return valid;
    }

    private static int moveMaterializationOccupants(
        List<EntityPlayerMP> players,
        TardisRecord record,
        WorldServer exteriorWorld,
        BlockPos exteriorPos,
        EnumFacing exteriorFacing
    ) {
        int moved = 0;
        for (EntityPlayerMP player : players) {
            if (TardisTeleportService.moveMaterializationOccupantIntoInterior(player, record)) {
                moved++;
                continue;
            }

            BlockPos fallback = findSafePlayerFallback(exteriorWorld, exteriorPos, exteriorFacing);
            player.setPositionAndUpdate(
                fallback.getX() + 0.5D,
                fallback.getY(),
                fallback.getZ() + 0.5D
            );
            player.fallDistance = 0.0F;
        }
        return moved;
    }

    private static BlockPos findSafePlayerFallback(WorldServer world, BlockPos exterior, EnumFacing front) {
        BlockPos[] candidates = new BlockPos[] {
            exterior.offset(front),
            exterior.offset(front, 2),
            exterior.offset(front.rotateY()),
            exterior.offset(front.rotateYCCW()),
            exterior.up(2)
        };
        for (BlockPos candidate : candidates) {
            if (world.isAirBlock(candidate) && world.isAirBlock(candidate.up())) {
                return candidate;
            }
        }
        return exterior.up(2);
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
