package dev.zoldmaki.renewedtardis.data;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Persistent secondary corridor between two already-existing adjacent rooms.
 * The original parent connection of an expandable room remains implicit in its
 * {@link TardisRoomRecord}; this class stores only extra graph edges.
 */
public final class TardisRoomConnection {
    private final int firstRoomId;
    private final int secondRoomId;
    private boolean generated;

    public TardisRoomConnection(int firstRoomId, int secondRoomId) {
        this(firstRoomId, secondRoomId, false);
    }

    private TardisRoomConnection(int firstRoomId, int secondRoomId, boolean generated) {
        if (firstRoomId < 0 || secondRoomId < 0) {
            throw new IllegalArgumentException("Room connection IDs cannot be negative");
        }
        if (firstRoomId == secondRoomId) {
            throw new IllegalArgumentException("A room cannot connect to itself");
        }
        this.firstRoomId = Math.min(firstRoomId, secondRoomId);
        this.secondRoomId = Math.max(firstRoomId, secondRoomId);
        this.generated = generated;
    }

    public int getFirstRoomId() {
        return firstRoomId;
    }

    public int getSecondRoomId() {
        return secondRoomId;
    }

    public boolean isGenerated() {
        return generated;
    }

    void setGenerated(boolean generated) {
        this.generated = generated;
    }

    public boolean connects(int roomId) {
        return firstRoomId == roomId || secondRoomId == roomId;
    }

    public boolean matches(int firstId, int secondId) {
        int low = Math.min(firstId, secondId);
        int high = Math.max(firstId, secondId);
        return firstRoomId == low && secondRoomId == high;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("FirstRoomId", firstRoomId);
        tag.setInteger("SecondRoomId", secondRoomId);
        tag.setBoolean("Generated", generated);
        return tag;
    }

    public static TardisRoomConnection readFromNBT(NBTTagCompound tag) {
        return new TardisRoomConnection(
            tag.getInteger("FirstRoomId"),
            tag.getInteger("SecondRoomId"),
            tag.getBoolean("Generated")
        );
    }
}
