package dev.zoldmaki.renewedtardis.flight;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.block.BlockTardisExterior;
import dev.zoldmaki.renewedtardis.config.TardisConfig;
import dev.zoldmaki.renewedtardis.console.ConsoleControlId;
import dev.zoldmaki.renewedtardis.console.TardisNavigationConsoleService;
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
import javax.annotation.Nullable;
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
        SURFACE,
        NEAREST_SOLID,
        /** Legacy command mode retained for compatibility with /tr land nearest. */
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

        boolean coordinated = record.isCoordinatedDriveMode();
        DimensionPosition destination = record.getDestinationPosition();
        if (coordinated && destination == null) {
            send(
                pilot,
                TextFormatting.YELLOW,
                "No destination is set for coordinated flight. On the Navigation panel, sneak-right-click Reset to copy the exterior position, select Uncoordinated drive, or use the bound key or /tr destination."
            );
            return false;
        }
        if (coordinated && destination.getDimension() == TardisDimensionManager.getDimensionId()) {
            send(pilot, TextFormatting.RED, "The exterior cannot land inside the TARDIS interior dimension.");
            return false;
        }

        DimensionPosition exteriorPosition = record.getExteriorPosition();
        if (exteriorPosition == null) {
            send(pilot, TextFormatting.RED, "This TARDIS has no placed exterior to dematerialize.");
            return false;
        }

        if (coordinated) {
            String restriction = TardisConfig.describeFlightRestriction(
                exteriorPosition.getDimension(),
                destination.getDimension()
            );
            if (!restriction.isEmpty()) {
                send(pilot, TextFormatting.RED, restriction);
                return false;
            }
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
        DimensionPosition resolvedDestination = coordinated
            ? resolveFlightTarget(server, record, destination, startedAt)
            : null;
        long durationMillis = coordinated
            ? calculateEffectiveFlightDurationMillis(record, exteriorPosition, destination)
            : 0L;
        DimensionPosition energyTarget = coordinated ? destination : exteriorPosition;
        long energyCost = TardisEnergyService.calculateFlightCost(record, exteriorPosition, energyTarget);
        DimensionPosition eventTarget = coordinated && resolvedDestination != null
            ? resolvedDestination
            : exteriorPosition;

        TardisFlightEvent.Takeoff takeoffEvent = new TardisFlightEvent.Takeoff(
            pilot,
            record,
            exteriorPosition,
            eventTarget,
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
            departureFacing,
            coordinated ? destination : null,
            resolvedDestination,
            durationMillis,
            coordinated
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

        TardisDamageService.applyTakeoffWear(data, record, exteriorPosition, energyTarget);
        data.setConsoleControlState(
            record.getTardisId(),
            ConsoleControlId.MAIN_QUANTUM_HANDBRAKE,
            1
        );
        data.resetTakeoffSequence(record.getTardisId());
        pilot.getServerWorld().playSound(null, controlPos, ModSounds.TAKEOFF, SoundCategory.BLOCKS, 0.9F, 1.0F);

        if (!coordinated) {
            send(
                pilot,
                TextFormatting.AQUA,
                "The TARDIS enters uncoordinated vortex flight. No destination is required and no travel occurs until Coordinated drive is selected; landing now returns to the departure position. Artron cost: "
                    + TardisEnergyService.format(energyCost) + " AU; remaining: "
                    + TardisEnergyService.format(record.getArtronEnergy()) + " AU; systems: "
                    + TardisDamageService.describe(record) + "."
            );
            return true;
        }

        String drift = resolvedDestination.equals(destination)
            ? ""
            : "; resolved landing target: " + resolvedDestination.toString();
        send(
            pilot,
            TextFormatting.AQUA,
            "The TARDIS enters coordinated flight. Estimated travel time: " + formatDuration(durationMillis)
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
        if (
            record != null
                && record.isInFlight()
                && !record.isCoordinatedDriveMode()
                && !forceLanding
                && mode != LandingMode.DEPARTURE
        ) {
            return landAtTravelledPosition(pilot, record, controlPos, mode);
        }
        return landInternal(
            pilot,
            record,
            controlPos,
            mode,
            forceLanding,
            forceLanding,
            null,
            null,
            false
        );
    }

    /**
     * Lands at the conceptual position already reached by coordinated travel.
     * This is used by uncoordinated handbrake landings and sneak-handbrake
     * emergency landings. It bypasses the travel timer but never force-replaces
     * obstructing blocks.
     */
    public static boolean landAtTravelledPosition(
        EntityPlayerMP pilot,
        TardisRecord record,
        BlockPos controlPos,
        LandingMode mode
    ) {
        if (pilot == null || record == null || !record.isInFlight()) {
            return false;
        }
        long nowMillis = System.currentTimeMillis();
        DimensionPosition current = !record.isCoordinatedDriveMode()
            ? record.getFlightPausedPosition()
            : null;
        if (current == null) {
            current = record.getCurrentFlightPosition(nowMillis);
        }
        if (current == null) {
            current = record.getFlightDeparture();
        }
        if (current == null) {
            send(pilot, TextFormatting.RED, "The travelled position could not be reconstructed.");
            return false;
        }
        EnumFacing facing = record.hasCoordinatedTravel()
            ? randomHorizontalFacing(record, nowMillis)
            : record.getFlightDepartureFacing();
        return landInternal(
            pilot,
            record,
            controlPos,
            mode,
            true,
            false,
            current,
            facing,
            true
        );
    }

    private static boolean landInternal(
        EntityPlayerMP pilot,
        TardisRecord record,
        BlockPos controlPos,
        LandingMode mode,
        boolean bypassTravelTimer,
        boolean forcePlacement,
        @Nullable DimensionPosition positionOverride,
        @Nullable EnumFacing facingOverride,
        boolean interruptedTravel
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
        if (!bypassTravelTimer && !record.isFlightReady(now)) {
            send(
                pilot,
                TextFormatting.YELLOW,
                "The TARDIS is still travelling: " + record.getFlightProgressPercent(now) + "% complete, "
                    + formatDuration(record.getFlightRemainingMillis(now))
                    + " remaining. Sneak-right-click the handbrake for an early landing."
            );
            return false;
        }

        DimensionPosition flightDeparture = record.getFlightDeparture();
        DimensionPosition requestedPosition;
        EnumFacing requestedFacing;
        if (positionOverride != null) {
            requestedPosition = positionOverride;
            requestedFacing = facingOverride == null ? EnumFacing.NORTH : facingOverride;
        } else if (mode == LandingMode.DEPARTURE) {
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
        if (!forcePlacement && flightDeparture != null) {
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
        if (forcePlacement) {
            if (requestedTarget.getY() < 0 || requestedTarget.getY() > 254) {
                send(
                    pilot,
                    TextFormatting.RED,
                    "Even a forced landing needs its lower block between Y=0 and Y=254."
                );
                return false;
            }
        } else if (mode == LandingMode.SURFACE) {
            actualTarget = findSurfaceLanding(targetWorld, requestedTarget);
            if (actualTarget == null) {
                send(
                    pilot,
                    TextFormatting.RED,
                    "No clear surface landing was found at the selected X/Z. The TARDIS remains in flight."
                );
                return false;
            }
        } else if (mode == LandingMode.NEAREST_SOLID) {
            actualTarget = findNearestSolidLanding(
                targetWorld,
                requestedTarget,
                TardisConfig.getSafeLandingSearchRadius()
            );
            if (actualTarget == null) {
                send(
                    pilot,
                    TextFormatting.RED,
                    "No solid-supported landing was found near the selected Y within "
                        + TardisConfig.getSafeLandingSearchRadius()
                        + " horizontal blocks. The TARDIS remains in flight."
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
            if (requestedTarget.getY() < 0 || requestedTarget.getY() > 254) {
                send(pilot, TextFormatting.RED, "The destination lower block must be between Y=0 and Y=254.");
                return false;
            }
            if (!isLandingSpaceClear(targetWorld, requestedTarget)) {
                send(
                    pilot,
                    TextFormatting.RED,
                    "The exact destination is obstructed. Select Surface or Nearest Solid, use /tr land nearest, or use an operator force landing."
                );
                return false;
            }
        }

        cleanupOrphanedExteriorTop(targetWorld, actualTarget);
        DimensionPosition actualPosition = new DimensionPosition(requestedPosition.getDimension(), actualTarget);
        TardisFlightEvent.Landing landingEvent = new TardisFlightEvent.Landing(
            pilot,
            record,
            flightDeparture,
            requestedPosition,
            actualTarget,
            mode,
            bypassTravelTimer || forcePlacement
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
                forcePlacement
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
        if (!data.completeFlight(
            record.getTardisId(),
            actualPosition,
            requestedFacing,
            interruptedTravel
        )) {
            BlockTardisExterior.removeStructure(targetWorld, actualTarget);
            send(pilot, TextFormatting.RED, "The landing state could not be saved. The TARDIS remains in flight.");
            return false;
        }
        if (interruptedTravel) {
            TardisNavigationConsoleService.synchronizeAfterInterruptedLanding(record);
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

        String recoveryNote;
        if (actualTarget.equals(requestedTarget)) {
            recoveryNote = "";
        } else if (mode == LandingMode.SURFACE) {
            recoveryNote = " (surface position for target X/Z "
                + requestedTarget.getX() + "/" + requestedTarget.getZ() + ")";
        } else if (mode == LandingMode.NEAREST_SOLID) {
            recoveryNote = " (nearest solid-supported position to selected Y="
                + requestedTarget.getY() + ")";
        } else {
            recoveryNote = " (nearest safe position to " + requestedPosition.toString() + ")";
        }
        String occupantNote = capturedPlayers == 0
            ? ""
            : " " + capturedPlayers + " player" + (capturedPlayers == 1 ? " was" : "s were")
                + " moved safely into the interior.";
        String interruptionNote = interruptedTravel
            ? " The interrupted route was cleared and Navigation now targets this materialized position."
            : "";
        send(
            pilot,
            TextFormatting.GREEN,
            "The TARDIS materializes at " + actualPosition.toString() + recoveryNote + "."
                + interruptionNote + occupantNote
        );
        return true;
    }

    public static boolean toggleDriveMode(EntityPlayerMP pilot, TardisRecord record) {
        if (pilot == null || record == null || pilot.getServer() == null) {
            return false;
        }
        TardisWorldData data = TardisWorldData.get(pilot.getServer());
        if (!record.isInFlight()) {
            int next = record.isCoordinatedDriveMode() ? 1 : 0;
            data.setConsoleControlState(record.getTardisId(), ConsoleControlId.MAIN_DRIVE_MODE, next);
            send(
                pilot,
                TextFormatting.AQUA,
                next == 0
                    ? "Drive mode prepared: Coordinated. Takeoff requires a destination and begins travel immediately."
                    : "Drive mode prepared: Uncoordinated. Takeoff needs no destination and remains in the vortex until Coordinated is selected."
            );
            return true;
        }

        long nowMillis = System.currentTimeMillis();
        if (record.isCoordinatedDriveMode()) {
            if (!data.pauseCoordinatedFlight(record.getTardisId(), nowMillis)) {
                send(pilot, TextFormatting.RED, "The coordinated route could not be paused.");
                return true;
            }
            send(
                pilot,
                TextFormatting.GOLD,
                record.hasCoordinatedTravel()
                    ? "Drive mode: Uncoordinated. Travel is paused at "
                        + record.getFlightProgressPercent(nowMillis)
                        + "% at " + record.getFlightPausedPosition()
                        + "; using the handbrake now lands at that frozen position."
                    : "Drive mode: Uncoordinated. No coordinated travel has occurred; using the handbrake returns to departure."
            );
            return true;
        }

        DimensionPosition destination = record.getDestinationPosition();
        if (destination == null) {
            send(
                pilot,
                TextFormatting.YELLOW,
                "Coordinated drive needs a destination. The TARDIS remains in uncoordinated vortex flight."
            );
            return true;
        }
        if (destination.getDimension() == TardisDimensionManager.getDimensionId()) {
            send(
                pilot,
                TextFormatting.RED,
                "The exterior cannot travel into the TARDIS interior dimension. The drive remains uncoordinated."
            );
            return true;
        }

        DimensionPosition current = record.getCurrentFlightPosition(nowMillis);
        if (current == null) {
            current = record.getFlightDeparture();
        }
        if (current == null) {
            send(pilot, TextFormatting.RED, "The current travelled position could not be reconstructed.");
            return true;
        }
        String restriction = TardisConfig.describeFlightRestriction(
            current.getDimension(),
            destination.getDimension()
        );
        if (!restriction.isEmpty()) {
            send(pilot, TextFormatting.RED, restriction + " The TARDIS remains uncoordinated.");
            return true;
        }

        boolean resumeExistingRoute = record.hasFlightRoute() && record.isFlightRouteFor(destination);
        if (resumeExistingRoute) {
            if (!data.resumeCoordinatedFlight(record.getTardisId(), nowMillis)) {
                send(pilot, TextFormatting.RED, "The paused coordinated route could not be resumed.");
                return true;
            }
            send(
                pilot,
                TextFormatting.AQUA,
                "Drive mode: Coordinated. Travel resumed toward " + destination.toString() + "; "
                    + formatDuration(record.getFlightRemainingMillis(nowMillis)) + " remaining."
            );
            return true;
        }

        DimensionPosition resolved = resolveFlightTarget(
            pilot.getServer(),
            record,
            destination,
            nowMillis
        );
        long durationMillis = calculateEffectiveFlightDurationMillis(record, current, destination);
        long routeEnergyCost = calculateRouteActivationCost(record, current, destination);
        long availableEnergy = TardisEnergyService.refresh(data, record);
        if (availableEnergy < routeEnergyCost) {
            send(
                pilot,
                TextFormatting.RED,
                "Insufficient Artron energy to engage the new coordinated route: "
                    + TardisEnergyService.format(routeEnergyCost) + " AU needed, "
                    + TardisEnergyService.format(availableEnergy) + " AU available. The drive remains uncoordinated."
            );
            return true;
        }
        if (routeEnergyCost > 0L && !TardisEnergyService.consume(data, record, routeEnergyCost)) {
            send(pilot, TextFormatting.RED, "The coordinated-route energy cost could not be reserved.");
            return true;
        }
        if (!data.setCoordinatedFlightRoute(
            record.getTardisId(),
            current,
            destination,
            resolved,
            durationMillis,
            nowMillis
        )) {
            if (routeEnergyCost > 0L) {
                TardisEnergyService.refund(data, record, routeEnergyCost);
            }
            send(
                pilot,
                TextFormatting.RED,
                "The coordinated route could not be created. Its Artron cost was refunded."
            );
            return true;
        }

        String drift = resolved == destination
            ? ""
            : "; resolved landing target: " + resolved.toString();
        send(
            pilot,
            TextFormatting.AQUA,
            "Drive mode: Coordinated. A new route began toward " + destination.toString()
                + drift + "; " + formatDuration(durationMillis) + " remaining; Artron cost: "
                + TardisEnergyService.format(routeEnergyCost) + " AU."
        );
        return true;
    }

    private static long calculateRouteActivationCost(
        TardisRecord record,
        DimensionPosition current,
        DimensionPosition destination
    ) {
        long fullCost = TardisEnergyService.calculateFlightCost(record, current, destination);
        if (record.hasCoordinatedTravel()) {
            return fullCost;
        }
        // Uncoordinated takeoff already paid the stationary/base flight cost.
        long alreadyPaidBase = TardisEnergyService.calculateFlightCost(record, current, current);
        return Math.max(0L, fullCost - alreadyPaidBase);
    }

    public static long calculateEffectiveFlightDurationMillis(
        TardisRecord record,
        DimensionPosition departure,
        DimensionPosition destination
    ) {
        return TardisUpgradeService.applyFlightDuration(
            record,
            TardisDamageService.applyDurationPenalty(
                record,
                calculateFlightDurationMillis(departure, destination, record.getFlightSpeed())
            )
        );
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
        if (!record.isCoordinatedDriveMode()) {
            if (!record.hasCoordinatedTravel()) {
                return "UNCOORDINATED VORTEX; no coordinated travel";
            }
            return "UNCOORDINATED; travel paused at "
                + record.getFlightProgressPercent(nowMillis) + "%";
        }
        if (!record.hasFlightRoute()) {
            return "COORDINATED; no destination route";
        }
        if (record.isFlightReady(nowMillis)) {
            return "READY TO LAND (100%)";
        }
        return record.getFlightProgressPercent(nowMillis) + "% complete, "
            + formatDuration(record.getFlightRemainingMillis(nowMillis)) + " remaining";
    }

    public static String describeFlightProfile(TardisRecord record) {
        return "drive=" + (record.isCoordinatedDriveMode() ? "coordinated" : "uncoordinated")
            + ", speed=" + record.getFlightSpeed().getName()
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


    private static BlockPos findSurfaceLanding(WorldServer world, BlockPos requested) {
        int x = requested.getX();
        int z = requested.getZ();
        for (int y = 254; y >= 1; y--) {
            BlockPos candidate = new BlockPos(x, y, z);
            if (isSafeLandingSpot(world, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Finds a solid-supported position by prioritizing vertical distance from
     * the selected Y, then horizontal distance. This allows caves and elevated
     * platforms to be selected without silently forcing the top surface.
     */
    private static BlockPos findNearestSolidLanding(WorldServer world, BlockPos center, int radius) {
        int safeRadius = Math.max(0, radius);
        for (int verticalDistance = 0; verticalDistance <= 254; verticalDistance++) {
            for (int ring = 0; ring <= safeRadius; ring++) {
                for (int dx = -ring; dx <= ring; dx++) {
                    for (int dz = -ring; dz <= ring; dz++) {
                        if (ring > 0 && Math.abs(dx) != ring && Math.abs(dz) != ring) {
                            continue;
                        }
                        if (verticalDistance == 0) {
                            BlockPos candidate = center.add(dx, 0, dz);
                            if (isSafeLandingSpot(world, candidate)) {
                                return candidate;
                            }
                            continue;
                        }
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
        return null;
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

    private static EnumFacing randomHorizontalFacing(TardisRecord record, long nowMillis) {
        long seed = nowMillis
            ^ record.getTardisId().getMostSignificantBits()
            ^ record.getTardisId().getLeastSignificantBits()
            ^ record.getFlightStartedAtMillis();
        return EnumFacing.byHorizontalIndex(new Random(seed).nextInt(4));
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
