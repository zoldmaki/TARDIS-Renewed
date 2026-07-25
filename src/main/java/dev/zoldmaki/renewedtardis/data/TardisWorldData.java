package dev.zoldmaki.renewedtardis.data;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

/**
 * Native 1.12.2 replacement for the original Darkcore-backed ownership data.
 * The data is stored in the overworld's MapStorage and therefore survives
 * normal saves, server restarts, and dimension unloads.
 */
public final class TardisWorldData extends WorldSavedData {
    public static final String DATA_NAME = RenewedTardisMod.MOD_ID + "_tardis_registry";
    private static final int DATA_VERSION = 1;

    private final Map<UUID, TardisRecord> records = new LinkedHashMap<>();

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

        NBTTagList recordList = nbt.getTagList("Records", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < recordList.tagCount(); i++) {
            try {
                TardisRecord record = TardisRecord.readFromNBT(recordList.getCompoundTagAt(i));
                records.put(record.getTardisId(), record);
            } catch (RuntimeException exception) {
                if (RenewedTardisMod.logger != null) {
                    RenewedTardisMod.logger.error("Skipped an invalid TARDIS registry entry while loading.", exception);
                }
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("DataVersion", DATA_VERSION);

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

    public boolean assignInteriorDimension(UUID tardisId, int dimensionId) {
        TardisRecord record = getById(tardisId);
        if (record == null) {
            return false;
        }
        record.setInteriorDimension(dimensionId);
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
