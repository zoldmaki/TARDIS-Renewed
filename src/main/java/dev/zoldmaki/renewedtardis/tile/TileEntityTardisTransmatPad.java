package dev.zoldmaki.renewedtardis.tile;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;

/** Persistent link from a transmat pad to one TARDIS record. */
public final class TileEntityTardisTransmatPad extends TileEntity {
    private static final String TAG_TARDIS_ID = "TardisId";

    @Nullable
    private UUID tardisId;

    @Nullable
    public UUID getTardisId() {
        return tardisId;
    }

    public void bind(UUID tardisId) {
        if (tardisId == null) {
            throw new IllegalArgumentException("tardisId cannot be null");
        }
        if (tardisId.equals(this.tardisId)) {
            return;
        }
        this.tardisId = tardisId;
        markDirty();
        if (world != null && !world.isRemote) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
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
        tardisId = null;
        if (compound.hasKey(TAG_TARDIS_ID, Constants.NBT.TAG_STRING)) {
            try {
                tardisId = UUID.fromString(compound.getString(TAG_TARDIS_ID));
            } catch (IllegalArgumentException ignored) {
                tardisId = null;
            }
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        readFromNBT(packet.getNbtCompound());
    }
}
