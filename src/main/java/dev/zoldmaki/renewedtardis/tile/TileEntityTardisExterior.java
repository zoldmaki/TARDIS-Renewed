package dev.zoldmaki.renewedtardis.tile;

import dev.zoldmaki.renewedtardis.block.BlockTardisExterior;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;

/** Persistent link between one placed exterior block and one TARDIS record. */
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
        locked = record.isExteriorLocked();
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
    public void onLoad() {
        super.onLoad();
        if (world == null || world.isRemote) {
            return;
        }

        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != ModBlocks.TARDIS_EXTERIOR) {
            return;
        }

        TardisRecord record = null;
        if (tardisId != null && world.getMinecraftServer() != null) {
            record = TardisWorldData.get(world.getMinecraftServer()).getById(tardisId);
        }
        if (record != null) {
            state = state.withProperty(BlockTardisExterior.SHELL, record.getExteriorShell());
        }
        BlockTardisExterior.synchronizeStructure(world, pos, state);
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
