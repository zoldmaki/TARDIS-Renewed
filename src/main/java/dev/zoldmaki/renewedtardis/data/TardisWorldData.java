package dev.zoldmaki.renewedtardis.data;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.access.TardisAccessMode;
import dev.zoldmaki.renewedtardis.access.TardisPermission;
import dev.zoldmaki.renewedtardis.config.TardisConfig;
import dev.zoldmaki.renewedtardis.dimension.InteriorAddress;
import dev.zoldmaki.renewedtardis.damage.TardisSubsystem;
import dev.zoldmaki.renewedtardis.dimension.TardisDimensionManager;
import dev.zoldmaki.renewedtardis.exterior.ExteriorShell;
import dev.zoldmaki.renewedtardis.flight.FlightAccuracy;
import dev.zoldmaki.renewedtardis.flight.FlightSpeed;
import dev.zoldmaki.renewedtardis.upgrade.TardisUpgradeService;
import dev.zoldmaki.renewedtardis.upgrade.TardisUpgradeType;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

/**
 * Native 1.12.2 replacement for the original Darkcore-backed ownership data.
 * The data is stored in the overworld's MapStorage and survives normal saves,
 * server restarts, and unloading the shared interior dimension.
 */
public final class TardisWorldData extends WorldSavedData {
    public static final String DATA_NAME = RenewedTardisMod.MOD_ID + "_tardis_registry";
    private static final int DATA_VERSION = 12;

    private final Map<UUID, TardisRecord> records = new LinkedHashMap<>();
    private int nextInteriorIndex;

    public TardisWorldData() {
        this(DATA_NAME);
    }

    public TardisWorldData(String name) {
        super(name);
    }

    public static TardisWorldData get(MinecraftServer server) {
        WorldServer overworld = server.getWorld(0);
        if (overworld == null) {
            throw new IllegalStateException("Cannot access TARDIS data before the overworld exists");
        }

        MapStorage storage = overworld.getMapStorage();
        TardisWorldData data = (TardisWorldData) storage.getOrLoadData(TardisWorldData.class, DATA_NAME);

        if (data == null) {
            data = new TardisWorldData(DATA_NAME);
            storage.setData(DATA_NAME, data);
            data.markDirty();
        }

        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        records.clear();

        int version = nbt.getInteger("DataVersion");
        if (version > DATA_VERSION && RenewedTardisMod.logger != null) {
            RenewedTardisMod.logger.warn(
                "TARDIS registry data version {} is newer than supported version {}.",
                version,
                DATA_VERSION
            );
        }

        int highestInteriorIndex = -1;
        NBTTagList recordList = nbt.getTagList("Records", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < recordList.tagCount(); i++) {
            try {
                TardisRecord record = TardisRecord.readFromNBT(recordList.getCompoundTagAt(i));
                records.put(record.getTardisId(), record);
                highestInteriorIndex = Math.max(highestInteriorIndex, record.getInteriorIndex());
            } catch (RuntimeException exception) {
                if (RenewedTardisMod.logger != null) {
                    RenewedTardisMod.logger.error("Skipped an invalid TARDIS registry entry while loading.", exception);
                }
            }
        }

        int savedNextIndex = nbt.hasKey("NextInteriorIndex", Constants.NBT.TAG_INT)
            ? nbt.getInteger("NextInteriorIndex")
            : 0;
        nextInteriorIndex = Math.max(savedNextIndex, highestInteriorIndex + 1);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("DataVersion", DATA_VERSION);
        nbt.setInteger("NextInteriorIndex", nextInteriorIndex);

        NBTTagList recordList = new NBTTagList();
        for (TardisRecord record : records.values()) {
            recordList.appendTag(record.writeToNBT());
        }
        nbt.setTag("Records", recordList);

        return nbt;
    }

    @Nullable
    public TardisRecord getById(UUID tardisId) {
        return records.get(tardisId);
    }

    @Nullable
    public TardisRecord getByOwner(UUID ownerId) {
        for (TardisRecord record : records.values()) {
            if (record.getOwnerId().equals(ownerId)) {
                return record;
            }
        }
        return null;
    }

    /** Finds a record by its last saved owner name, case-insensitively. */
    @Nullable
    public TardisRecord getByOwnerName(String ownerName) {
        if (ownerName == null) {
            return null;
        }
        for (TardisRecord record : records.values()) {
            if (record.getOwnerName().equalsIgnoreCase(ownerName)) {
                return record;
            }
        }
        return null;
    }

    /**
     * Transfers an existing record without changing its TARDIS UUID, interior,
     * exterior, destination, lock state, or flight state.
     */
    public boolean transferOwnership(UUID tardisId, UUID newOwnerId, String newOwnerName) {
        TardisRecord record = getById(tardisId);
        if (record == null || newOwnerId == null) {
            return false;
        }

        TardisRecord conflicting = getByOwner(newOwnerId);
        if (conflicting != null && !conflicting.getTardisId().equals(tardisId)) {
            return false;
        }

        record.transferOwnership(newOwnerId, newOwnerName);
        markDirty();
        return true;
    }

    public boolean setAccessMode(UUID tardisId, TardisAccessMode accessMode) {
        TardisRecord record = getById(tardisId);
        if (record == null || accessMode == null || record.getAccessMode() == accessMode) {
            return false;
        }
        record.setAccessMode(accessMode);
        markDirty();
        return true;
    }

    public boolean addOrUpdateCrewMember(
        UUID tardisId,
        UUID playerId,
        String playerName,
        Set<TardisPermission> permissions
    ) {
        TardisRecord record = getById(tardisId);
        if (record == null || playerId == null || playerId.equals(record.getOwnerId())) {
            return false;
        }
        boolean changed = record.addOrUpdateCrewMember(playerId, playerName, permissions);
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public boolean removeCrewMember(UUID tardisId, UUID playerId) {
        TardisRecord record = getById(tardisId);
        if (record == null || !record.removeCrewMember(playerId)) {
            return false;
        }
        markDirty();
        return true;
    }

    public boolean grantCrewPermission(UUID tardisId, UUID playerId, TardisPermission permission) {
        TardisRecord record = getById(tardisId);
        if (record == null || permission == null || !record.grantCrewPermission(playerId, permission)) {
            return false;
        }
        markDirty();
        return true;
    }

    public boolean revokeCrewPermission(UUID tardisId, UUID playerId, TardisPermission permission) {
        TardisRecord record = getById(tardisId);
        if (record == null || permission == null || !record.revokeCrewPermission(playerId, permission)) {
            return false;
        }
        markDirty();
        return true;
    }

    public boolean clearCrew(UUID tardisId) {
        TardisRecord record = getById(tardisId);
        if (record == null || !record.clearCrew()) {
            return false;
        }
        markDirty();
        return true;
    }

    public TardisRecord getOrCreateForOwner(UUID ownerId, String ownerName) {
        TardisRecord existing = getByOwner(ownerId);
        if (existing != null) {
            if (!existing.getOwnerName().equals(ownerName)) {
                existing.setOwnerName(ownerName);
                markDirty();
            }
            return existing;
        }

        TardisRecord created = new TardisRecord(
            UUID.randomUUID(),
            ownerId,
            ownerName,
            System.currentTimeMillis()
        );
        records.put(created.getTardisId(), created);
        markDirty();
        return created;
    }

    /** Assigns one isolated cell in the shared TARDIS interior dimension. */
    public DimensionPosition getOrCreateInterior(TardisRecord record) {
        DimensionPosition existing = record.getInteriorPosition();
        if (record.hasInterior() && existing != null) {
            return existing;
        }

        int index = nextInteriorIndex++;
        BlockPos center = InteriorAddress.getCenterForIndex(index);
        DimensionPosition assigned = new DimensionPosition(TardisDimensionManager.getDimensionId(), center);
        record.assignInterior(index, assigned);
        markDirty();
        return assigned;
    }

    public boolean setInteriorGenerated(UUID tardisId, boolean generated) {
        TardisRecord record = getById(tardisId);
        if (record == null) {
            return false;
        }
        record.setInteriorGenerated(generated);
        markDirty();
        return true;
    }

    @Nullable
    public TardisRoomRecord addRoom(
        UUID tardisId,
        String schemaName,
        int gridX,
        int gridZ,
        int parentRoomId,
        EnumFacing connectionFacing
    ) {
        TardisRecord record = getById(tardisId);
        if (record == null || record.getRoomAtGrid(gridX, gridZ) != null) {
            return null;
        }
        TardisRoomRecord room = record.addRoom(
            schemaName,
            gridX,
            gridZ,
            parentRoomId,
            connectionFacing
        );
        markDirty();
        return room;
    }

    public boolean setRoomGenerated(UUID tardisId, int roomId, boolean generated) {
        TardisRecord record = getById(tardisId);
        if (record == null || !record.setRoomGenerated(roomId, generated)) {
            return false;
        }
        markDirty();
        return true;
    }

    @Nullable
    public TardisRoomConnection addRoomConnection(UUID tardisId, int firstRoomId, int secondRoomId) {
        TardisRecord record = getById(tardisId);
        if (record == null || firstRoomId == secondRoomId) {
            return null;
        }
        if ((firstRoomId != 0 && record.getRoom(firstRoomId) == null)
            || (secondRoomId != 0 && record.getRoom(secondRoomId) == null)
            || record.getRoomConnection(firstRoomId, secondRoomId) != null) {
            return null;
        }
        TardisRoomConnection connection = record.addRoomConnection(firstRoomId, secondRoomId);
        markDirty();
        return connection;
    }

    public boolean setRoomConnectionGenerated(
        UUID tardisId,
        int firstRoomId,
        int secondRoomId,
        boolean generated
    ) {
        TardisRecord record = getById(tardisId);
        if (record == null || !record.setRoomConnectionGenerated(firstRoomId, secondRoomId, generated)) {
            return false;
        }
        markDirty();
        return true;
    }

    public boolean removeRoomConnection(UUID tardisId, int firstRoomId, int secondRoomId) {
        TardisRecord record = getById(tardisId);
        if (record == null || !record.removeRoomConnection(firstRoomId, secondRoomId)) {
            return false;
        }
        markDirty();
        return true;
    }

    public boolean removeRoom(UUID tardisId, int roomId) {
        TardisRecord record = getById(tardisId);
        if (record == null || !record.removeRoom(roomId)) {
            return false;
        }
        markDirty();
        return true;
    }

    /** Kept for early-save compatibility. */
    public boolean assignInteriorDimension(UUID tardisId, int dimensionId) {
        TardisRecord record = getById(tardisId);
        if (record == null) {
            return false;
        }
        record.setInteriorDimension(dimensionId);
        markDirty();
        return true;
    }

    public boolean setTransmatPosition(UUID tardisId, @Nullable DimensionPosition position) {
        TardisRecord record = getById(tardisId);
        if (record == null) {
            return false;
        }
        record.setTransmatPosition(position);
        markDirty();
        return true;
    }

    public boolean setExteriorPosition(UUID tardisId, @Nullable DimensionPosition position) {
        TardisRecord record = getById(tardisId);
        if (record == null) {
            return false;
        }
        record.setExteriorPosition(position);
        markDirty();
        return true;
    }

    public boolean setExteriorLocked(UUID tardisId, boolean locked) {
        TardisRecord record = getById(tardisId);
        if (record == null) {
            return false;
        }
        record.setExteriorLocked(locked);
        markDirty();
        return true;
    }

    public boolean setExteriorShell(UUID tardisId, ExteriorShell shell) {
        TardisRecord record = getById(tardisId);
        if (record == null || shell == null) {
            return false;
        }
        record.setExteriorShell(shell);
        markDirty();
        return true;
    }

    public boolean setDestination(UUID tardisId, DimensionPosition position, EnumFacing facing) {
        TardisRecord record = getById(tardisId);
        if (record == null) {
            return false;
        }
        record.setDestination(position, facing);
        markDirty();
        return true;
    }

    public boolean setFlightSpeed(UUID tardisId, FlightSpeed speed) {
        TardisRecord record = getById(tardisId);
        if (record == null || speed == null || record.isInFlight()) {
            return false;
        }
        record.setFlightSpeed(speed);
        markDirty();
        return true;
    }

    public boolean setFlightAccuracy(UUID tardisId, FlightAccuracy accuracy) {
        TardisRecord record = getById(tardisId);
        if (record == null || accuracy == null || record.isInFlight()) {
            return false;
        }
        record.setFlightAccuracy(accuracy);
        markDirty();
        return true;
    }

    public boolean clearDestination(UUID tardisId) {
        TardisRecord record = getById(tardisId);
        if (record == null) {
            return false;
        }
        record.setDestination(null, EnumFacing.NORTH);
        markDirty();
        return true;
    }

    public long refreshArtronEnergy(UUID tardisId, long nowMillis) {
        TardisRecord record = getById(tardisId);
        if (record == null) {
            return 0L;
        }
        if (record.refreshArtronEnergy(nowMillis, TardisUpgradeService.getRegenerationPerSecond(record))) {
            markDirty();
        }
        return record.getArtronEnergy();
    }

    public boolean consumeArtronEnergy(UUID tardisId, long amount, long nowMillis) {
        TardisRecord record = getById(tardisId);
        if (record == null) {
            return false;
        }
        record.refreshArtronEnergy(nowMillis, TardisUpgradeService.getRegenerationPerSecond(record));
        if (!record.consumeArtronEnergy(amount)) {
            markDirty();
            return false;
        }
        markDirty();
        return true;
    }

    public long addArtronEnergy(UUID tardisId, long amount, long nowMillis) {
        TardisRecord record = getById(tardisId);
        if (record == null) {
            return 0L;
        }
        record.refreshArtronEnergy(nowMillis, TardisUpgradeService.getRegenerationPerSecond(record));
        record.addArtronEnergy(amount);
        markDirty();
        return record.getArtronEnergy();
    }

    public boolean setArtronEnergy(UUID tardisId, long amount, long nowMillis) {
        TardisRecord record = getById(tardisId);
        if (record == null) {
            return false;
        }
        record.setArtronEnergy(amount, nowMillis);
        markDirty();
        return true;
    }

    public boolean setArtronCapacity(UUID tardisId, long capacity, long nowMillis) {
        TardisRecord record = getById(tardisId);
        if (record == null) {
            return false;
        }
        record.refreshArtronEnergy(nowMillis, TardisUpgradeService.getRegenerationPerSecond(record));
        record.setArtronCapacity(capacity, nowMillis);
        markDirty();
        return true;
    }

    public boolean setSubsystemHealth(UUID tardisId, TardisSubsystem subsystem, int health) {
        TardisRecord record = getById(tardisId);
        if (record == null || subsystem == null) {
            return false;
        }
        boolean changed = record.setSubsystemHealth(subsystem, health);
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public int damageSubsystem(UUID tardisId, TardisSubsystem subsystem, int amount) {
        TardisRecord record = getById(tardisId);
        if (record == null || subsystem == null) {
            return 0;
        }
        int changed = record.damageSubsystem(subsystem, amount);
        if (changed > 0) {
            markDirty();
        }
        return changed;
    }

    public int repairSubsystem(UUID tardisId, TardisSubsystem subsystem, int amount) {
        TardisRecord record = getById(tardisId);
        if (record == null || subsystem == null) {
            return 0;
        }
        int changed = record.repairSubsystem(subsystem, amount);
        if (changed > 0) {
            markDirty();
        }
        return changed;
    }

    public boolean repairAllSubsystems(UUID tardisId) {
        TardisRecord record = getById(tardisId);
        if (record == null) {
            return false;
        }
        boolean changed = false;
        for (TardisSubsystem subsystem : TardisSubsystem.values()) {
            changed |= record.setSubsystemHealth(subsystem, 100);
        }
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public boolean setUpgradeCount(UUID tardisId, TardisUpgradeType type, int count) {
        TardisRecord record = getById(tardisId);
        if (record == null || type == null || count < 0) {
            return false;
        }
        long nowMillis = System.currentTimeMillis();
        record.refreshArtronEnergy(nowMillis, TardisUpgradeService.getRegenerationPerSecond(record));
        boolean changed = record.setUpgradeCount(type, count);
        if (!changed) {
            return false;
        }
        markDirty();
        return true;
    }

    public boolean addUpgrade(UUID tardisId, TardisUpgradeType type) {
        TardisRecord record = getById(tardisId);
        return record != null && setUpgradeCount(tardisId, type, record.getUpgradeCount(type) + 1);
    }

    public boolean removeUpgrade(UUID tardisId, TardisUpgradeType type) {
        TardisRecord record = getById(tardisId);
        return record != null
            && record.getUpgradeCount(type) > 0
            && setUpgradeCount(tardisId, type, record.getUpgradeCount(type) - 1);
    }

    public boolean clearUpgrades(UUID tardisId) {
        TardisRecord record = getById(tardisId);
        if (record == null || record.getInstalledUpgradeCount() == 0) {
            return false;
        }
        record.refreshArtronEnergy(
            System.currentTimeMillis(),
            TardisUpgradeService.getRegenerationPerSecond(record)
        );
        boolean changed = false;
        for (TardisUpgradeType type : TardisUpgradeType.values()) {
            if (record.getUpgradeCount(type) > 0) {
                changed |= record.setUpgradeCount(type, 0);
            }
        }
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public boolean beginFlight(
        UUID tardisId,
        boolean exteriorLocked,
        long startedAtMillis,
        long readyAtMillis,
        EnumFacing departureFacing,
        DimensionPosition resolvedDestination
    ) {
        TardisRecord record = getById(tardisId);
        if (record == null || record.isInFlight() || record.getExteriorPosition() == null) {
            return false;
        }
        record.setExteriorLocked(exteriorLocked);
        record.beginFlight(startedAtMillis, readyAtMillis, departureFacing, resolvedDestination);
        markDirty();
        return true;
    }

    public boolean abortFlight(UUID tardisId) {
        TardisRecord record = getById(tardisId);
        if (record == null || !record.isInFlight()) {
            return false;
        }
        record.abortFlight();
        markDirty();
        return true;
    }

    public boolean completeFlight(UUID tardisId, DimensionPosition landedPosition) {
        TardisRecord record = getById(tardisId);
        if (record == null || !record.isInFlight()) {
            return false;
        }
        record.completeFlight(landedPosition);
        markDirty();
        return true;
    }

    public boolean remove(UUID tardisId) {
        if (records.remove(tardisId) != null) {
            markDirty();
            return true;
        }
        return false;
    }

    public int size() {
        return records.size();
    }

    public Collection<TardisRecord> getRecords() {
        return Collections.unmodifiableCollection(records.values());
    }
}
