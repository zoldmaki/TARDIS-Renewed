package dev.zoldmaki.renewedtardis.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants;

/** Persistent description of one expandable room inside a TARDIS cell. */
public final class TardisRoomRecord {
    private final int roomId;
    private final String schemaName;
    private final int gridX;
    private final int gridZ;
    private final int parentRoomId;
    private final EnumFacing connectionFacing;
    private boolean generated;

    public TardisRoomRecord(
        int roomId,
        String schemaName,
        int gridX,
        int gridZ,
        int parentRoomId,
        EnumFacing connectionFacing
    ) {
        this(roomId, schemaName, gridX, gridZ, parentRoomId, connectionFacing, false);
    }

    private TardisRoomRecord(
        int roomId,
        String schemaName,
        int gridX,
        int gridZ,
        int parentRoomId,
        EnumFacing connectionFacing,
        boolean generated
    ) {
        if (roomId <= 0) {
            throw new IllegalArgumentException("Expandable room IDs must be positive");
        }
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new IllegalArgumentException("Room schema name cannot be empty");
        }
        if (connectionFacing == null || connectionFacing.getAxis() == EnumFacing.Axis.Y) {
            throw new IllegalArgumentException("Room connection facing must be horizontal");
        }
        this.roomId = roomId;
        this.schemaName = schemaName;
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.parentRoomId = parentRoomId;
        this.connectionFacing = connectionFacing;
        this.generated = generated;
    }

    public int getRoomId() {
        return roomId;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridZ() {
        return gridZ;
    }

    public int getParentRoomId() {
        return parentRoomId;
    }

    public EnumFacing getConnectionFacing() {
        return connectionFacing;
    }

    public boolean isGenerated() {
        return generated;
    }

    void setGenerated(boolean generated) {
        this.generated = generated;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("RoomId", roomId);
        tag.setString("Schema", schemaName);
        tag.setInteger("GridX", gridX);
        tag.setInteger("GridZ", gridZ);
        tag.setInteger("ParentRoomId", parentRoomId);
        tag.setInteger("ConnectionFacing", connectionFacing.getHorizontalIndex());
        tag.setBoolean("Generated", generated);
        return tag;
    }

    public static TardisRoomRecord readFromNBT(NBTTagCompound tag) {
        int roomId = tag.getInteger("RoomId");
        String schemaName = tag.getString("Schema");
        int gridX = tag.getInteger("GridX");
        int gridZ = tag.getInteger("GridZ");
        int parentRoomId = tag.hasKey("ParentRoomId", Constants.NBT.TAG_INT)
            ? tag.getInteger("ParentRoomId")
            : 0;
        EnumFacing connectionFacing = tag.hasKey("ConnectionFacing", Constants.NBT.TAG_INT)
            ? EnumFacing.byHorizontalIndex(tag.getInteger("ConnectionFacing"))
            : EnumFacing.NORTH;
        boolean generated = tag.getBoolean("Generated");
        return new TardisRoomRecord(
            roomId,
            schemaName,
            gridX,
            gridZ,
            parentRoomId,
            connectionFacing,
            generated
        );
    }
}
