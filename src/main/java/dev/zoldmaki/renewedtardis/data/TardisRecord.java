package dev.zoldmaki.renewedtardis.data;

import dev.zoldmaki.renewedtardis.exterior.ExteriorShell;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants;

/**
 * Persistent identity, ownership, exterior, interior, and flight state for one
 * TARDIS.
 */
public final class TardisRecord {
    public static final int UNASSIGNED_DIMENSION = Integer.MIN_VALUE;
    public static final int UNASSIGNED_INTERIOR_INDEX = -1;

    private final UUID tardisId;
    private UUID ownerId;
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
    private ExteriorShell exteriorShell;

    private boolean inFlight;
    @Nullable
    private DimensionPosition flightDeparture;
    private EnumFacing flightDepartureFacing;
    @Nullable
    private DimensionPosition destinationPosition;
    private EnumFacing destinationFacing;
    private long flightStartedAtMillis;
    private long flightReadyAtMillis;

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
            ExteriorShell.POLICE_BOX,
            false,
            null,
            EnumFacing.NORTH,
            null,
            EnumFacing.NORTH,
            0L,
            0L
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
        ExteriorShell exteriorShell,
        boolean inFlight,
        @Nullable DimensionPosition flightDeparture,
        EnumFacing flightDepartureFacing,
        @Nullable DimensionPosition destinationPosition,
        EnumFacing destinationFacing,
        long flightStartedAtMillis,
        long flightReadyAtMillis
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
        this.exteriorShell = exteriorShell == null ? ExteriorShell.PROTOTYPE : exteriorShell;
        this.inFlight = inFlight;
        this.flightDeparture = flightDeparture;
        this.flightDepartureFacing = normalizeFacing(flightDepartureFacing);
        this.destinationPosition = destinationPosition;
        this.destinationFacing = normalizeFacing(destinationFacing);
        this.flightStartedAtMillis = Math.max(0L, flightStartedAtMillis);
        this.flightReadyAtMillis = Math.max(0L, flightReadyAtMillis);
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

    void transferOwnership(UUID ownerId, String ownerName) {
        if (ownerId == null) {
            throw new IllegalArgumentException("ownerId cannot be null");
        }
        this.ownerId = ownerId;
        this.ownerName = ownerName == null ? "" : ownerName;
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

    public ExteriorShell getExteriorShell() {
        return exteriorShell;
    }

    void setExteriorShell(ExteriorShell exteriorShell) {
        this.exteriorShell = exteriorShell == null ? ExteriorShell.PROTOTYPE : exteriorShell;
    }

    public boolean isInFlight() {
        return inFlight;
    }

    @Nullable
    public DimensionPosition getFlightDeparture() {
        return flightDeparture;
    }

    public EnumFacing getFlightDepartureFacing() {
        return flightDepartureFacing;
    }

    @Nullable
    public DimensionPosition getDestinationPosition() {
        return destinationPosition;
    }

    public EnumFacing getDestinationFacing() {
        return destinationFacing;
    }

    public long getFlightStartedAtMillis() {
        return flightStartedAtMillis;
    }

    public long getFlightReadyAtMillis() {
        return flightReadyAtMillis;
    }

    public long getFlightDurationMillis() {
        if (!inFlight || flightReadyAtMillis <= flightStartedAtMillis) {
            return 0L;
        }
        return flightReadyAtMillis - flightStartedAtMillis;
    }

    public long getFlightRemainingMillis(long nowMillis) {
        if (!inFlight || flightReadyAtMillis <= 0L) {
            return 0L;
        }
        return Math.max(0L, flightReadyAtMillis - nowMillis);
    }

    public boolean isFlightReady(long nowMillis) {
        return inFlight && (flightReadyAtMillis <= 0L || nowMillis >= flightReadyAtMillis);
    }

    public int getFlightProgressPercent(long nowMillis) {
        if (!inFlight) {
            return 0;
        }
        long duration = getFlightDurationMillis();
        if (duration <= 0L) {
            return 100;
        }
        long elapsed = Math.max(0L, Math.min(duration, nowMillis - flightStartedAtMillis));
        return (int) Math.min(100L, (elapsed * 100L) / duration);
    }

    void setDestination(@Nullable DimensionPosition destinationPosition, EnumFacing destinationFacing) {
        this.destinationPosition = destinationPosition;
        this.destinationFacing = normalizeFacing(destinationFacing);
    }

    void beginFlight(long startedAtMillis, long readyAtMillis, EnumFacing departureFacing) {
        if (inFlight) {
            return;
        }
        flightDeparture = exteriorPosition;
        flightDepartureFacing = normalizeFacing(departureFacing);
        exteriorPosition = null;
        inFlight = true;
        flightStartedAtMillis = Math.max(0L, startedAtMillis);
        flightReadyAtMillis = Math.max(flightStartedAtMillis, readyAtMillis);
    }

    void abortFlight() {
        if (!inFlight) {
            return;
        }
        exteriorPosition = flightDeparture;
        flightDeparture = null;
        flightDepartureFacing = EnumFacing.NORTH;
        inFlight = false;
        clearFlightTiming();
    }

    void completeFlight(DimensionPosition landedPosition) {
        exteriorPosition = landedPosition;
        flightDeparture = null;
        flightDepartureFacing = EnumFacing.NORTH;
        destinationPosition = null;
        destinationFacing = EnumFacing.NORTH;
        inFlight = false;
        clearFlightTiming();
    }

    private void clearFlightTiming() {
        flightStartedAtMillis = 0L;
        flightReadyAtMillis = 0L;
    }

    private static EnumFacing normalizeFacing(@Nullable EnumFacing facing) {
        return facing == null || facing.getAxis() == EnumFacing.Axis.Y ? EnumFacing.NORTH : facing;
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
        tag.setString("ExteriorShell", exteriorShell.getName());

        tag.setBoolean("InFlight", inFlight);
        if (flightDeparture != null) {
            tag.setTag("FlightDeparture", flightDeparture.writeToNBT());
            tag.setInteger("FlightDepartureFacing", flightDepartureFacing.getHorizontalIndex());
        }
        if (destinationPosition != null) {
            tag.setTag("Destination", destinationPosition.writeToNBT());
            tag.setInteger("DestinationFacing", destinationFacing.getHorizontalIndex());
        }
        if (inFlight) {
            tag.setLong("FlightStartedAtMillis", flightStartedAtMillis);
            tag.setLong("FlightReadyAtMillis", flightReadyAtMillis);
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
        ExteriorShell exteriorShell = tag.hasKey("ExteriorShell", Constants.NBT.TAG_STRING)
            ? ExteriorShell.fromName(tag.getString("ExteriorShell"))
            : ExteriorShell.PROTOTYPE;
        if (exteriorShell == null) {
            exteriorShell = ExteriorShell.PROTOTYPE;
        }

        boolean inFlight = tag.getBoolean("InFlight");
        DimensionPosition flightDeparture = tag.hasKey("FlightDeparture", Constants.NBT.TAG_COMPOUND)
            ? DimensionPosition.readFromNBT(tag.getCompoundTag("FlightDeparture"))
            : null;
        EnumFacing flightDepartureFacing = tag.hasKey("FlightDepartureFacing", Constants.NBT.TAG_INT)
            ? EnumFacing.byHorizontalIndex(tag.getInteger("FlightDepartureFacing"))
            : EnumFacing.NORTH;
        DimensionPosition destinationPosition = tag.hasKey("Destination", Constants.NBT.TAG_COMPOUND)
            ? DimensionPosition.readFromNBT(tag.getCompoundTag("Destination"))
            : null;
        EnumFacing destinationFacing = tag.hasKey("DestinationFacing", Constants.NBT.TAG_INT)
            ? EnumFacing.byHorizontalIndex(tag.getInteger("DestinationFacing"))
            : EnumFacing.NORTH;
        long flightStartedAtMillis = tag.hasKey("FlightStartedAtMillis", Constants.NBT.TAG_LONG)
            ? tag.getLong("FlightStartedAtMillis")
            : 0L;
        long flightReadyAtMillis = tag.hasKey("FlightReadyAtMillis", Constants.NBT.TAG_LONG)
            ? tag.getLong("FlightReadyAtMillis")
            : 0L;

        // A corrupted/incomplete in-flight record must never silently erase a
        // still-present exterior from older data.
        if (inFlight && flightDeparture == null && exteriorPosition != null) {
            flightDeparture = exteriorPosition;
            exteriorPosition = null;
        }

        // Phase 6-7 saves had no timing fields. Treat those already airborne
        // TARDISes as immediately ready rather than trapping them in flight.
        if (inFlight && flightReadyAtMillis < flightStartedAtMillis) {
            flightReadyAtMillis = flightStartedAtMillis;
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
            exteriorShell,
            inFlight,
            flightDeparture,
            flightDepartureFacing,
            destinationPosition,
            destinationFacing,
            flightStartedAtMillis,
            flightReadyAtMillis
        );
    }
}
