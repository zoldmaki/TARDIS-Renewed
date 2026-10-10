package dev.zoldmaki.renewedtardis.tile;

import dev.zoldmaki.renewedtardis.console.TardisConsoleState;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;

/** Persistent TARDIS UUID link and client-visible waypoint-name cache for one console face. */
public final class TileEntityTardisConsolePanel extends TileEntity {
    private static final String TAG_TARDIS_ID = "TardisId";
    private static final String TAG_WAYPOINT_NAME_PREFIX = "WaypointName";
    private static final String TAG_WAYPOINT_COLORS = "WaypointColors";

    @Nullable
    private UUID tardisId;
    private final String[] waypointNames = new String[TardisConsoleState.WAYPOINT_COUNT];
    private final int[] waypointColors = new int[TardisConsoleState.WAYPOINT_COUNT];

    public TileEntityTardisConsolePanel() {
        for (int index = 0; index < waypointColors.length; index++) {
            waypointColors[index] = -1;
        }
    }

    @Nullable
    public UUID getTardisId() {
        return tardisId;
    }

    public void bind(UUID tardisId) {
        if (tardisId == null || tardisId.equals(this.tardisId)) {
            return;
        }
        this.tardisId = tardisId;
        markChanged();
    }

    public void syncWaypointMetadata(TardisConsoleState state) {
        if (state == null) {
            return;
        }
        boolean changed = false;
        for (int slot = 0; slot < TardisConsoleState.WAYPOINT_COUNT; slot++) {
            String nextName = state.getWaypointName(slot);
            int nextColor = state.getWaypointColor(slot);
            String currentName = waypointNames[slot];
            if (currentName == null ? nextName != null : !currentName.equals(nextName)) {
                waypointNames[slot] = nextName;
                changed = true;
            }
            if (waypointColors[slot] != nextColor) {
                waypointColors[slot] = nextColor;
                changed = true;
            }
        }
        if (changed) {
            markChanged();
        }
    }

    public String getWaypointName(int slot) {
        if (slot < 0 || slot >= waypointNames.length) {
            return "?";
        }
        String name = waypointNames[slot];
        return name == null || name.isEmpty() ? Integer.toString(slot + 1) : name;
    }

    public int getWaypointColor(int slot) {
        return slot < 0 || slot >= waypointColors.length ? -1 : waypointColors[slot];
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (tardisId != null) {
            compound.setString(TAG_TARDIS_ID, tardisId.toString());
        }
        for (int slot = 0; slot < waypointNames.length; slot++) {
            String name = waypointNames[slot];
            if (name != null && !name.isEmpty()) {
                compound.setString(TAG_WAYPOINT_NAME_PREFIX + slot, name);
            }
        }
        compound.setIntArray(TAG_WAYPOINT_COLORS, waypointColors);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        tardisId = readUuid(compound, TAG_TARDIS_ID);
        for (int slot = 0; slot < waypointNames.length; slot++) {
            String key = TAG_WAYPOINT_NAME_PREFIX + slot;
            waypointNames[slot] = compound.hasKey(key, Constants.NBT.TAG_STRING)
                ? compound.getString(key)
                : Integer.toString(slot + 1);
            waypointColors[slot] = -1;
        }
        int[] colors = compound.getIntArray(TAG_WAYPOINT_COLORS);
        for (int slot = 0; slot < waypointColors.length && slot < colors.length; slot++) {
            waypointColors[slot] = colors[slot] < 0 ? -1 : Math.min(15, colors[slot]);
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager networkManager, SPacketUpdateTileEntity packet) {
        if (packet != null) {
            readFromNBT(packet.getNbtCompound());
        }
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
