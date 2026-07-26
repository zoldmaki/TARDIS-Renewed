package dev.zoldmaki.renewedtardis.data;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants;

/**
 * Persistent identity, ownership, exterior, interior, and early flight state
 * for one TARDIS.
 */
public final class TardisRecord {
    public static final int UNASSIGNED_DIMENSION = Integer.MIN_VALUE;
    public static final int UNASSIGNED_INTERIOR_INDEX = -1;

    private final UUID tardisId;
    private final UUID ownerId;
    private String ownerName;
    private final long createdAt;

    private int interiorDimension;
    private int interiorIndex;
    @Nullable
    private DimensionPosition interiorPosition;
    private boolean interiorGenerated;

    @Nullable
    private DimensionPosition exteriorPosition;
    private boolean exteriorLocked;

    private boolean inFlight;
    @Nullable
    private DimensionPosition flightDeparture;
    @Nullable
    private DimensionPosition destinationPosition;
    private EnumFacing destinationFacing;

    public TardisRecord(UUID tardisId, UUID ownerId, String ownerName, long createdAt) {
        this(
            tardisId,
            ownerId,
            ownerName,
            createdAt,
            UNASSIGNED_DIMENSION,
            UNASSIGNED_INTERIOR_INDEX,
            null,
            false,
            null,
            true,
            false,
            null,
            null,
            EnumFacing.NORTH
        );
    }

    private TardisRecord(
        UUID tardisId,
        UUID ownerId,
        String ownerName,
        long createdAt,
        int interiorDimension,
        int interiorIndex,
        @Nullable DimensionPosition interiorPosition,
        boolean interiorGenerated,
        @Nullable DimensionPosition exteriorPosition,
        boolean exteriorLocked,
        boolean inFlight,
        @Nullable DimensionPosition flightDeparture,
        @Nullable DimensionPosition destinationPosition,
        EnumFacing destinationFacing
    ) {
        this.tardisId = tardisId;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.createdAt = createdAt;
        this.interiorDimension = interiorDimension;
        this.interiorIndex = interiorIndex;
        this.interiorPosition = interiorPosition;
        this.interiorGenerated = interiorGenerated;
        this.exteriorPosition = exteriorPosition;
        this.exteriorLocked = exteriorLocked;
        this.inFlight = inFlight;
        this.flightDeparture = flightDeparture;
        this.destinationPosition = destinationPosition;
        this.destinationFacing = destinationFacing == null || destinationFacing.getAxis() == EnumFacing.Axis.Y
            ? EnumFacing.NORTH
            : destinationFacing;
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

    public boolean hasInterior() {
        return hasInteriorDimension()
            && interiorIndex != UNASSIGNED_INTERIOR_INDEX
            && interiorPosition != null;
    }

    public int getInteriorIndex() {
        return interiorIndex;
    }

    @Nullable
    public DimensionPosition getInteriorPosition() {
        return interiorPosition;
    }

    public boolean isInteriorGenerated() {
        return interiorGenerated;
    }

    void assignInterior(int interiorIndex, DimensionPosition interiorPosition) {
        this.interiorDimension = interiorPosition.getDimension();
        this.interiorIndex = interiorIndex;
        this.interiorPosition = interiorPosition;
        this.interiorGenerated = false;
    }

    void setInteriorDimension(int interiorDimension) {
        this.interiorDimension = interiorDimension;
    }

    void setInteriorGenerated(boolean interiorGenerated) {
        this.interiorGenerated = interiorGenerated;
    }

    @Nullable
    public DimensionPosition getExteriorPosition() {
        return exteriorPosition;
    }

    void setExteriorPosition(@Nullable DimensionPosition exteriorPosition) {
        this.exteriorPosition = exteriorPosition;
    }

    public boolean isExteriorLocked() {
        return exteriorLocked;
    }

    void setExteriorLocked(boolean exteriorLocked) {
        this.exteriorLocked = exteriorLocked;
    }

    public boolean isInFlight() {
        return inFlight;
    }

    @Nullable
    public DimensionPosition getFlightDeparture() {
        return flightDeparture;
    }

    @Nullable
    public DimensionPosition getDestinationPosition() {
        return destinationPosition;
    }

    public EnumFacing getDestinationFacing() {
        return destinationFacing;
    }

    void setDestination(@Nullable DimensionPosition destinationPosition, EnumFacing destinationFacing) {
        this.destinationPosition = destinationPosition;
        this.destinationFacing = destinationFacing == null || destinationFacing.getAxis() == EnumFacing.Axis.Y
            ? EnumFacing.NORTH
            : destinationFacing;
    }

    void beginFlight() {
        if (inFlight) {
            return;
        }
        flightDeparture = exteriorPosition;
        exteriorPosition = null;
        inFlight = true;
    }

    void abortFlight() {
        if (!inFlight) {
            return;
        }
        exteriorPosition = flightDeparture;
        flightDeparture = null;
        inFlight = false;
    }

    void completeFlight(DimensionPosition landedPosition) {
        exteriorPosition = landedPosition;
        flightDeparture = null;
        destinationPosition = null;
        destinationFacing = EnumFacing.NORTH;
        inFlight = false;
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
        if (interiorIndex != UNASSIGNED_INTERIOR_INDEX) {
            tag.setInteger("InteriorIndex", interiorIndex);
        }
        if (interiorPosition != null) {
            tag.setTag("Interior", interiorPosition.writeToNBT());
        }
        tag.setBoolean("InteriorGenerated", interiorGenerated);

        if (exteriorPosition != null) {
            tag.setTag("Exterior", exteriorPosition.writeToNBT());
        }
        tag.setBoolean("ExteriorLocked", exteriorLocked);

        tag.setBoolean("InFlight", inFlight);
        if (flightDeparture != null) {
            tag.setTag("FlightDeparture", flightDeparture.writeToNBT());
        }
        if (destinationPosition != null) {
            tag.setTag("Destination", destinationPosition.writeToNBT());
            tag.setInteger("DestinationFacing", destinationFacing.getHorizontalIndex());
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
        int interiorIndex = tag.hasKey("InteriorIndex", Constants.NBT.TAG_INT)
            ? tag.getInteger("InteriorIndex")
            : UNASSIGNED_INTERIOR_INDEX;
        DimensionPosition interiorPosition = tag.hasKey("Interior", Constants.NBT.TAG_COMPOUND)
            ? DimensionPosition.readFromNBT(tag.getCompoundTag("Interior"))
            : null;
        boolean interiorGenerated = tag.getBoolean("InteriorGenerated");

        DimensionPosition exteriorPosition = tag.hasKey("Exterior", Constants.NBT.TAG_COMPOUND)
            ? DimensionPosition.readFromNBT(tag.getCompoundTag("Exterior"))
            : null;
        boolean exteriorLocked = !tag.hasKey("ExteriorLocked", Constants.NBT.TAG_BYTE)
            || tag.getBoolean("ExteriorLocked");

        boolean inFlight = tag.getBoolean("InFlight");
        DimensionPosition flightDeparture = tag.hasKey("FlightDeparture", Constants.NBT.TAG_COMPOUND)
            ? DimensionPosition.readFromNBT(tag.getCompoundTag("FlightDeparture"))
            : null;
        DimensionPosition destinationPosition = tag.hasKey("Destination", Constants.NBT.TAG_COMPOUND)
            ? DimensionPosition.readFromNBT(tag.getCompoundTag("Destination"))
            : null;
        EnumFacing destinationFacing = tag.hasKey("DestinationFacing", Constants.NBT.TAG_INT)
            ? EnumFacing.byHorizontalIndex(tag.getInteger("DestinationFacing"))
            : EnumFacing.NORTH;

        // A corrupted/incomplete in-flight record must never silently erase a
        // still-present exterior from older data.
        if (inFlight && flightDeparture == null && exteriorPosition != null) {
            flightDeparture = exteriorPosition;
            exteriorPosition = null;
        }

        return new TardisRecord(
            tardisId,
            ownerId,
            ownerName,
            createdAt,
            interiorDimension,
            interiorIndex,
            interiorPosition,
            interiorGenerated,
            exteriorPosition,
            exteriorLocked,
            inFlight,
            flightDeparture,
            destinationPosition,
            destinationFacing
        );
    }
}
