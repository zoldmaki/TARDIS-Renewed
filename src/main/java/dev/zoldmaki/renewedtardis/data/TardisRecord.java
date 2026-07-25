package dev.zoldmaki.renewedtardis.data;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

/**
 * Persistent identity and ownership information for one TARDIS.
 *
 * Phase 2 deliberately stores more than it currently uses so the dimension
 * and exterior milestones can attach their state without changing identity.
 */
public final class TardisRecord {
    public static final int UNASSIGNED_DIMENSION = Integer.MIN_VALUE;

    private final UUID tardisId;
    private final UUID ownerId;
    private String ownerName;
    private final long createdAt;
    private int interiorDimension;
    @Nullable
    private DimensionPosition exteriorPosition;

    public TardisRecord(UUID tardisId, UUID ownerId, String ownerName, long createdAt) {
        this(tardisId, ownerId, ownerName, createdAt, UNASSIGNED_DIMENSION, null);
    }

    private TardisRecord(
        UUID tardisId,
        UUID ownerId,
        String ownerName,
        long createdAt,
        int interiorDimension,
        @Nullable DimensionPosition exteriorPosition
    ) {
        this.tardisId = tardisId;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.createdAt = createdAt;
        this.interiorDimension = interiorDimension;
        this.exteriorPosition = exteriorPosition;
    }

    public UUID getTardisId() {
        return tardisId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean hasInteriorDimension() {
        return interiorDimension != UNASSIGNED_DIMENSION;
    }

    public int getInteriorDimension() {
        return interiorDimension;
    }

    void setInteriorDimension(int interiorDimension) {
        this.interiorDimension = interiorDimension;
    }

    @Nullable
    public DimensionPosition getExteriorPosition() {
        return exteriorPosition;
    }

    void setExteriorPosition(@Nullable DimensionPosition exteriorPosition) {
        this.exteriorPosition = exteriorPosition;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("TardisId", tardisId.toString());
        tag.setString("OwnerId", ownerId.toString());
        tag.setString("OwnerName", ownerName);
        tag.setLong("CreatedAt", createdAt);

        if (hasInteriorDimension()) {
            tag.setInteger("InteriorDimension", interiorDimension);
        }

        if (exteriorPosition != null) {
            tag.setTag("Exterior", exteriorPosition.writeToNBT());
        }

        return tag;
    }

    public static TardisRecord readFromNBT(NBTTagCompound tag) {
        UUID tardisId = UUID.fromString(tag.getString("TardisId"));
        UUID ownerId = UUID.fromString(tag.getString("OwnerId"));
        String ownerName = tag.getString("OwnerName");
        long createdAt = tag.getLong("CreatedAt");

        int interiorDimension = tag.hasKey("InteriorDimension", Constants.NBT.TAG_INT)
            ? tag.getInteger("InteriorDimension")
            : UNASSIGNED_DIMENSION;

        DimensionPosition exteriorPosition = tag.hasKey("Exterior", Constants.NBT.TAG_COMPOUND)
            ? DimensionPosition.readFromNBT(tag.getCompoundTag("Exterior"))
            : null;

        return new TardisRecord(
            tardisId,
            ownerId,
            ownerName,
            createdAt,
            interiorDimension,
            exteriorPosition
        );
    }
}
