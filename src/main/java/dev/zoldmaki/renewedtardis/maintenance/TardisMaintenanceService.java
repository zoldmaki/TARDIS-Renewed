package dev.zoldmaki.renewedtardis.maintenance;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.block.BlockTardisExterior;
import dev.zoldmaki.renewedtardis.config.TardisConfig;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisRoomConnection;
import dev.zoldmaki.renewedtardis.data.TardisRoomRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.dimension.InteriorAddress;
import dev.zoldmaki.renewedtardis.dimension.TardisDimensionManager;
import dev.zoldmaki.renewedtardis.dimension.TardisInteriorBuilder;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisExterior;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

/** Read-only diagnostics and narrowly scoped operator recovery helpers. */
public final class TardisMaintenanceService {
    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }

    public static final class Finding {
        private final Severity severity;
        private final String message;

        private Finding(Severity severity, String message) {
            this.severity = severity;
            this.message = message;
        }

        public Severity getSeverity() {
            return severity;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class DiagnosticReport {
        private final List<Finding> findings = new ArrayList<>();
        private int warnings;
        private int errors;

        private void add(Severity severity, String message) {
            findings.add(new Finding(severity, message));
            if (severity == Severity.WARNING) {
                warnings++;
            } else if (severity == Severity.ERROR) {
                errors++;
            }
        }

        public List<Finding> getFindings() {
            return Collections.unmodifiableList(findings);
        }

        public int getWarningCount() {
            return warnings;
        }

        public int getErrorCount() {
            return errors;
        }

        public boolean isHealthy() {
            return warnings == 0 && errors == 0;
        }
    }

    public static final class RepairResult {
        private final boolean success;
        private final boolean changed;
        private final String message;
        private final String details;

        private RepairResult(boolean success, boolean changed, String message, String details) {
            this.success = success;
            this.changed = changed;
            this.message = message;
            this.details = details == null ? "" : details;
        }

        public static RepairResult success(boolean changed, String message) {
            return new RepairResult(true, changed, message, "");
        }

        public static RepairResult failure(String message, String details) {
            return new RepairResult(false, false, message, details);
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isChanged() {
            return changed;
        }

        public String getMessage() {
            return message;
        }

        public String getDetails() {
            return details;
        }
    }

    private TardisMaintenanceService() {
    }

    /**
     * Performs record-only checks by default. Deep mode may load the saved
     * exterior dimension and chunk, so it is intentionally operator-only at the
     * command layer.
     */
    public static DiagnosticReport inspect(MinecraftServer server, TardisRecord record, boolean deep) {
        DiagnosticReport report = new DiagnosticReport();
        report.add(
            Severity.INFO,
            "TARDIS " + record.getTardisId() + " owned by " + safeName(record.getOwnerName())
        );

        inspectIdentity(record, report);
        inspectInterior(record, report);
        inspectFlight(record, report);
        inspectEnergyAndUpgrades(record, report);
        inspectRooms(record, report);
        inspectExterior(server, record, deep, report);

        if (report.isHealthy()) {
            report.add(Severity.INFO, "No consistency problems were detected.");
        }
        return report;
    }

    private static void inspectIdentity(TardisRecord record, DiagnosticReport report) {
        if (record.getOwnerName() == null || record.getOwnerName().trim().isEmpty()) {
            report.add(Severity.WARNING, "The saved owner name is empty; UUID ownership is still authoritative.");
        }
        if (record.getCrew().containsKey(record.getOwnerId())) {
            report.add(Severity.ERROR, "The owner UUID is also present in the crew list.");
        }
    }

    private static void inspectInterior(TardisRecord record, DiagnosticReport report) {
        if (!record.hasInterior()) {
            report.add(Severity.INFO, "No interior cell has been allocated yet.");
            return;
        }

        DimensionPosition interior = record.getInteriorPosition();
        if (interior == null) {
            report.add(Severity.ERROR, "The record claims to have an interior but has no interior position.");
            return;
        }
        if (record.getInteriorDimension() != interior.getDimension()) {
            report.add(Severity.ERROR, "Interior dimension fields disagree inside the save record.");
        }
        if (interior.getDimension() != TardisDimensionManager.getDimensionId()) {
            report.add(
                Severity.ERROR,
                "Interior is assigned to dimension " + interior.getDimension()
                    + " while the running config uses " + TardisDimensionManager.getDimensionId() + "."
            );
        }
        BlockPos expected = InteriorAddress.getCenterForIndex(record.getInteriorIndex());
        if (!expected.equals(interior.getPosition())) {
            report.add(
                Severity.ERROR,
                "Interior cell " + record.getInteriorIndex() + " should be centered at " + expected
                    + " but the record points to " + interior.getPosition() + "."
            );
        }
    }

    private static void inspectFlight(TardisRecord record, DiagnosticReport report) {
        if (record.isInFlight()) {
            if (record.getExteriorPosition() != null) {
                report.add(Severity.ERROR, "An in-flight TARDIS still has a landed exterior position.");
            }
            if (record.getFlightDeparture() == null) {
                report.add(Severity.ERROR, "The active flight has no saved departure position.");
            }
            if (record.isCoordinatedDriveMode() && !record.hasFlightRoute()) {
                report.add(Severity.ERROR, "Coordinated flight has no complete active route state.");
            }
            if (record.hasCoordinatedTravel() && record.getFlightRouteStart() == null) {
                report.add(Severity.ERROR, "Travel history exists without a reconstructable route start.");
            }
            if (record.isCoordinatedDriveMode()
                && record.getFlightReadyAtMillis() > 0L
                && record.getFlightReadyAtMillis() < record.getFlightStartedAtMillis()) {
                report.add(Severity.ERROR, "The active coordinated ready timestamp is earlier than takeoff.");
            }
        } else {
            if (record.getFlightDeparture() != null) {
                report.add(Severity.WARNING, "A landed TARDIS retains stale departure data.");
            }
            if (record.getFlightResolvedDestination() != null) {
                report.add(Severity.WARNING, "A landed TARDIS retains a stale resolved flight target.");
            }
            if (record.getFlightStartedAtMillis() != 0L || record.getFlightReadyAtMillis() != 0L) {
                report.add(Severity.WARNING, "A landed TARDIS retains stale flight timing data.");
            }
        }
    }

    private static void inspectEnergyAndUpgrades(TardisRecord record, DiagnosticReport report) {
        if (record.getArtronEnergy() < 0L || record.getArtronEnergy() > record.getArtronCapacity()) {
            report.add(Severity.ERROR, "Artron energy is outside the record's effective capacity.");
        }
        if (record.getInstalledUpgradeCount() > TardisConfig.getUpgradeSlotCount()) {
            report.add(
                Severity.WARNING,
                "Installed upgrades exceed the current configured slot count; existing upgrades were not deleted."
            );
        }
    }

    private static void inspectRooms(TardisRecord record, DiagnosticReport report) {
        Set<Integer> ids = new HashSet<>();
        Set<String> positions = new HashSet<>();
        for (TardisRoomRecord room : record.getRooms()) {
            if (!ids.add(Integer.valueOf(room.getRoomId()))) {
                report.add(Severity.ERROR, "Duplicate expandable room ID " + room.getRoomId() + ".");
            }
            String gridKey = room.getGridX() + ":" + room.getGridZ();
            if (!positions.add(gridKey)) {
                report.add(Severity.ERROR, "Multiple expandable rooms occupy grid position " + gridKey + ".");
            }
            if (room.getParentRoomId() != 0 && record.getRoom(room.getParentRoomId()) == null) {
                report.add(
                    Severity.ERROR,
                    "Room " + room.getRoomId() + " references missing parent room " + room.getParentRoomId() + "."
                );
            }
            if (room.getConnectionFacing() == null || room.getConnectionFacing().getAxis() == EnumFacing.Axis.Y) {
                report.add(Severity.ERROR, "Room " + room.getRoomId() + " has an invalid connection facing.");
            }
        }

        Set<String> connectionKeys = new HashSet<>();
        for (TardisRoomConnection connection : record.getRoomConnections()) {
            int first = connection.getFirstRoomId();
            int second = connection.getSecondRoomId();
            if ((first != 0 && record.getRoom(first) == null) || (second != 0 && record.getRoom(second) == null)) {
                report.add(Severity.ERROR, "A secondary corridor references a missing room: " + first + "-" + second + ".");
            }
            String key = Math.min(first, second) + ":" + Math.max(first, second);
            if (!connectionKeys.add(key)) {
                report.add(Severity.WARNING, "Duplicate secondary corridor record " + key + ".");
            }
        }
    }

    private static void inspectExterior(
        MinecraftServer server,
        TardisRecord record,
        boolean deep,
        DiagnosticReport report
    ) {
        DimensionPosition exterior = record.getExteriorPosition();
        if (record.isInFlight()) {
            if (exterior == null) {
                report.add(Severity.INFO, "Exterior is correctly dematerialized during flight.");
            }
            return;
        }
        if (exterior == null) {
            report.add(Severity.WARNING, "The landed TARDIS has no saved exterior position.");
            return;
        }
        if (!deep) {
            report.add(
                Severity.INFO,
                "Exterior saved at " + exterior + ". Use '/tr doctor deep' as an operator to verify world blocks."
            );
            return;
        }

        WorldServer world = getOrLoadWorld(server, exterior.getDimension());
        if (world == null) {
            report.add(Severity.ERROR, "Exterior dimension " + exterior.getDimension() + " is unavailable.");
            return;
        }
        BlockPos pos = exterior.getPosition();
        world.getChunkProvider().provideChunk(pos.getX() >> 4, pos.getZ() >> 4);
        IBlockState lower = world.getBlockState(pos);
        IBlockState upper = world.getBlockState(pos.up());
        if (lower.getBlock() != ModBlocks.TARDIS_EXTERIOR) {
            report.add(Severity.ERROR, "The saved exterior lower block is missing at " + exterior + ".");
            return;
        }
        if (upper.getBlock() != ModBlocks.TARDIS_EXTERIOR_TOP) {
            report.add(Severity.ERROR, "The exterior upper block is missing at " + pos.up() + ".");
        }
        if (lower.getValue(BlockTardisExterior.SHELL) != record.getExteriorShell()) {
            report.add(Severity.WARNING, "The placed shell does not match the saved shell selection.");
        }

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityTardisExterior)) {
            report.add(Severity.ERROR, "The exterior lower block has no TARDIS tile entity.");
            return;
        }
        TileEntityTardisExterior exteriorTile = (TileEntityTardisExterior) tile;
        if (!record.getTardisId().equals(exteriorTile.getTardisId())) {
            report.add(Severity.ERROR, "The exterior tile is bound to a different or missing TARDIS UUID.");
        }
        if (!record.getOwnerId().equals(exteriorTile.getOwnerId())) {
            report.add(Severity.WARNING, "The exterior tile has stale owner metadata.");
        }
        if (record.isExteriorLocked() != exteriorTile.isLocked()) {
            report.add(Severity.WARNING, "The exterior tile lock state differs from the persistent record.");
        }
    }

    public static RepairResult repairExterior(
        MinecraftServer server,
        TardisWorldData data,
        TardisRecord record,
        boolean force
    ) {
        if (record.isInFlight()) {
            return RepairResult.failure(
                "The exterior cannot be repaired while the TARDIS is in flight.",
                "Use /tr land departure force or finish the flight first."
            );
        }
        DimensionPosition exterior = record.getExteriorPosition();
        if (exterior == null) {
            return RepairResult.failure(
                "No saved exterior position exists.",
                "Use /tr exterior to place the TARDIS near an operator."
            );
        }
        if (exterior.getDimension() == TardisDimensionManager.getDimensionId()) {
            return RepairResult.failure(
                "The saved exterior points into the TARDIS interior dimension.",
                "Use /tr exterior from a normal dimension to relocate it."
            );
        }

        WorldServer world = getOrLoadWorld(server, exterior.getDimension());
        if (world == null) {
            return RepairResult.failure(
                "Exterior dimension " + exterior.getDimension() + " is unavailable.",
                "The dimension is not registered or could not be loaded."
            );
        }
        BlockPos pos = exterior.getPosition();
        world.getChunkProvider().provideChunk(pos.getX() >> 4, pos.getZ() >> 4);

        IBlockState current = world.getBlockState(pos);
        IBlockState desired;
        boolean changed = false;
        boolean recreatedLower = current.getBlock() != ModBlocks.TARDIS_EXTERIOR;
        if (!recreatedLower) {
            desired = current.withProperty(BlockTardisExterior.SHELL, record.getExteriorShell());
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityTardisExterior) {
                UUID boundId = ((TileEntityTardisExterior) tile).getTardisId();
                if (boundId != null && !record.getTardisId().equals(boundId) && !force) {
                    return RepairResult.failure(
                        "The saved position is occupied by another bound TARDIS exterior.",
                        "Use the force option only after verifying the saved coordinates."
                    );
                }
            }
            BlockTardisExterior.StructurePlacementResult synchronization =
                BlockTardisExterior.placeStructureDetailed(world, pos, desired, force);
            if (!synchronization.isSuccess()) {
                return RepairResult.failure(synchronization.getReason(), synchronization.getDetails());
            }
            changed = true;
        } else {
            desired = ModBlocks.TARDIS_EXTERIOR.getDefaultState()
                .withProperty(BlockTardisExterior.FACING, EnumFacing.NORTH)
                .withProperty(BlockTardisExterior.SHELL, record.getExteriorShell());
            BlockTardisExterior.StructurePlacementResult placement =
                BlockTardisExterior.placeStructureDetailed(world, pos, desired, force);
            if (!placement.isSuccess()) {
                return RepairResult.failure(placement.getReason(), placement.getDetails());
            }
            changed = true;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityTardisExterior)) {
            return RepairResult.failure(
                "The exterior block was restored but its tile entity is still missing.",
                "position=" + exterior
            );
        }
        ((TileEntityTardisExterior) tile).bind(record);
        data.setExteriorPosition(record.getTardisId(), exterior);
        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);

        logRepair(record, "exterior", force);
        return RepairResult.success(
            changed,
            "Exterior structure and binding were synchronized at " + exterior + "."
                + (recreatedLower ? " The missing lower block was recreated facing north." : "")
        );
    }

    public static RepairResult repairInterior(
        MinecraftServer server,
        TardisWorldData data,
        TardisRecord record
    ) {
        DimensionPosition interior = data.getOrCreateInterior(record);
        if (interior.getDimension() != TardisDimensionManager.getDimensionId()) {
            return RepairResult.failure(
                "The saved interior dimension does not match the running configuration.",
                "saved=" + interior.getDimension() + ", configured=" + TardisDimensionManager.getDimensionId()
            );
        }
        try {
            WorldServer world = TardisDimensionManager.getOrLoadWorld(server);
            BlockPos center = interior.getPosition();
            world.getChunkProvider().provideChunk(center.getX() >> 4, center.getZ() >> 4);
            TardisInteriorBuilder.ensureGenerated(world, data, record);
            logRepair(record, "interior", false);
            return RepairResult.success(
                true,
                "Interior door, four physical console panels, compatibility controls, engine core, transmat, registered rooms, and bindings were verified."
            );
        } catch (RuntimeException exception) {
            if (RenewedTardisMod.logger != null) {
                RenewedTardisMod.logger.error(
                    "Failed to repair interior for TARDIS {}.",
                    record.getTardisId(),
                    exception
                );
            }
            return RepairResult.failure(
                "The interior repair failed. Check the server log.",
                exception.getClass().getSimpleName() + ": " + String.valueOf(exception.getMessage())
            );
        }
    }

    @Nullable
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

    private static void logRepair(TardisRecord record, String target, boolean force) {
        if (TardisConfig.isSuccessfulRepairLoggingEnabled() && RenewedTardisMod.logger != null) {
            RenewedTardisMod.logger.info(
                "TARDIS maintenance repaired {} for {} (owner={}, force={}).",
                target,
                record.getTardisId(),
                record.getOwnerName(),
                force
            );
        }
    }

    private static String safeName(String name) {
        return name == null || name.trim().isEmpty() ? "<unknown>" : name;
    }
}
