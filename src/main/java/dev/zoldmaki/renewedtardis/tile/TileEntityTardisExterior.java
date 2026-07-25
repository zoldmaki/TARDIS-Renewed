package dev.zoldmaki.renewedtardis.tile;

import dev.zoldmaki.renewedtardis.data.TardisRecord;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;

/**
 * Persistent link between one placed exterior block and one TARDIS record.
 *
 * The Phase 3 exterior is deliberately a one-block prototype. The stored link
 * and lock state are intended to survive when the visual exterior is replaced
 * by a proper multi-block model later.
 */
public final class TileEntityTardisExterior extends TileEntity {
    private static final String TAG_TARDIS_ID = "TardisId";
    private static final String TAG_OWNER_ID = "OwnerId";
    private static final String TAG_OWNER_NAME = "OwnerName";
    private static final String TAG_LOCKED = "Locked";

    @Nullable
    private UUID tardisId;
    @Nullable
    private UUID ownerId;
    private String ownerName = "";
    private boolean locked = true;

    public boolean isBound() {
        return tardisId != null;
    }

    @Nullable
    public UUID getTardisId() {
        return tardisId;
    }

    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public boolean isLocked() {
        return locked;
    }

    public void bind(TardisRecord record) {
        tardisId = record.getTardisId();
        ownerId = record.getOwnerId();
        ownerName = record.getOwnerName();
        locked = true;
        markChanged();
    }

    public void setLocked(boolean locked) {
        if (this.locked == locked) {
            return;
        }
        this.locked = locked;
        markChanged();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        if (tardisId != null) {
            compound.setString(TAG_TARDIS_ID, tardisId.toString());
        }
        if (ownerId != null) {
            compound.setString(TAG_OWNER_ID, ownerId.toString());
        }
        compound.setString(TAG_OWNER_NAME, ownerName);
        compound.setBoolean(TAG_LOCKED, locked);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        tardisId = readUuid(compound, TAG_TARDIS_ID);
        ownerId = readUuid(compound, TAG_OWNER_ID);
        ownerName = compound.getString(TAG_OWNER_NAME);
        locked = !compound.hasKey(TAG_LOCKED, Constants.NBT.TAG_BYTE) || compound.getBoolean(TAG_LOCKED);
    }

    private void markChanged() {
        markDirty();
        if (world != null) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    @Nullable
    private static UUID readUuid(NBTTagCompound compound, String key) {
        if (!compound.hasKey(key, Constants.NBT.TAG_STRING)) {
            return null;
        }

        try {
            return UUID.fromString(compound.getString(key));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
