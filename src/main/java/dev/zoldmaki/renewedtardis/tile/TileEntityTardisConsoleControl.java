package dev.zoldmaki.renewedtardis.tile;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;

/** Persistent TARDIS UUID link shared by all dedicated prototype controls. */
public final class TileEntityTardisConsoleControl extends TileEntity {
    private static final String TAG_TARDIS_ID = "TardisId";

    @Nullable
    private UUID tardisId;

    @Nullable
    public UUID getTardisId() {
        return tardisId;
    }

    public void bind(UUID tardisId) {
        if (tardisId.equals(this.tardisId)) {
            return;
        }
        this.tardisId = tardisId;
        markChanged();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (tardisId != null) {
            compound.setString(TAG_TARDIS_ID, tardisId.toString());
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        tardisId = readUuid(compound, TAG_TARDIS_ID);
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
