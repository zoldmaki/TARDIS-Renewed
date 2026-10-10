package dev.zoldmaki.renewedtardis.data;

import dev.zoldmaki.renewedtardis.config.TardisConfig;
import dev.zoldmaki.renewedtardis.console.ConsoleControlId;
import dev.zoldmaki.renewedtardis.console.TardisConsoleState;
import dev.zoldmaki.renewedtardis.access.TardisAccessMode;
import dev.zoldmaki.renewedtardis.access.TardisCrewMember;
import dev.zoldmaki.renewedtardis.access.TardisPermission;
import dev.zoldmaki.renewedtardis.exterior.ExteriorShell;
import dev.zoldmaki.renewedtardis.damage.TardisDamageService;
import dev.zoldmaki.renewedtardis.damage.TardisSubsystem;
import dev.zoldmaki.renewedtardis.flight.FlightAccuracy;
import dev.zoldmaki.renewedtardis.flight.FlightSpeed;
import dev.zoldmaki.renewedtardis.flight.FlightTravelMath;
import dev.zoldmaki.renewedtardis.upgrade.TardisUpgradeType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private TardisAccessMode accessMode;
    private final Map<UUID, TardisCrewMember> crew;

    private int interiorDimension;
    private int interiorIndex;
    @Nullable
    private DimensionPosition interiorPosition;
    private boolean interiorGenerated;
    private final List<TardisRoomRecord> rooms;
    private int nextRoomId;
    private final List<TardisRoomConnection> roomConnections;

    @Nullable
    private DimensionPosition transmatPosition;

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
    private FlightSpeed flightSpeed;
    private FlightAccuracy flightAccuracy;
    @Nullable
    private DimensionPosition flightResolvedDestination;
    @Nullable
    private DimensionPosition flightRouteStart;
    @Nullable
    private DimensionPosition flightRouteDestination;
    private long flightStartedAtMillis;
    private long flightReadyAtMillis;
    private long flightRouteDurationMillis;
    private long flightRouteElapsedMillis;
    private long flightRouteSegmentStartedAtMillis;
    private boolean flightCoordinatedEverActive;
    @Nullable
    private DimensionPosition flightPausedPosition;

    private final EnumMap<TardisUpgradeType, Integer> upgrades;

    private long artronEnergy;
    private long artronBaseCapacity;
    private long artronLastUpdateMillis;

    private int engineHealth;
    private int navigationHealth;
    private int stabilizerHealth;
    private int dematerializationHealth;

    private final TardisConsoleState consoleState;

    public TardisRecord(UUID tardisId, UUID ownerId, String ownerName, long createdAt) {
        this(
            tardisId,
            ownerId,
            ownerName,
            createdAt,
            TardisAccessMode.PUBLIC,
            Collections.<UUID, TardisCrewMember>emptyMap(),
            UNASSIGNED_DIMENSION,
            UNASSIGNED_INTERIOR_INDEX,
            null,
            false,
            Collections.<TardisRoomRecord>emptyList(),
            1,
            Collections.<TardisRoomConnection>emptyList(),
            null,
            null,
            true,
            ExteriorShell.POLICE_BOX,
            false,
            null,
            EnumFacing.NORTH,
            null,
            EnumFacing.NORTH,
            FlightSpeed.NORMAL,
            FlightAccuracy.PRECISE,
            null,
            null,
            null,
            0L,
            0L,
            0L,
            0L,
            0L,
            false,
            null,
            Collections.<TardisUpgradeType, Integer>emptyMap(),
            TardisConfig.getStartingArtronEnergy(),
            TardisConfig.getDefaultArtronCapacity(),
            System.currentTimeMillis(),
            TardisDamageService.MAX_HEALTH,
            TardisDamageService.MAX_HEALTH,
            TardisDamageService.MAX_HEALTH,
            TardisDamageService.MAX_HEALTH,
            new TardisConsoleState()
        );
    }

    private TardisRecord(
        UUID tardisId,
        UUID ownerId,
        String ownerName,
        long createdAt,
        TardisAccessMode accessMode,
        Map<UUID, TardisCrewMember> crew,
        int interiorDimension,
        int interiorIndex,
        @Nullable DimensionPosition interiorPosition,
        boolean interiorGenerated,
        List<TardisRoomRecord> rooms,
        int nextRoomId,
        List<TardisRoomConnection> roomConnections,
        @Nullable DimensionPosition transmatPosition,
        @Nullable DimensionPosition exteriorPosition,
        boolean exteriorLocked,
        ExteriorShell exteriorShell,
        boolean inFlight,
        @Nullable DimensionPosition flightDeparture,
        EnumFacing flightDepartureFacing,
        @Nullable DimensionPosition destinationPosition,
        EnumFacing destinationFacing,
        FlightSpeed flightSpeed,
        FlightAccuracy flightAccuracy,
        @Nullable DimensionPosition flightResolvedDestination,
        @Nullable DimensionPosition flightRouteStart,
        @Nullable DimensionPosition flightRouteDestination,
        long flightStartedAtMillis,
        long flightReadyAtMillis,
        long flightRouteDurationMillis,
        long flightRouteElapsedMillis,
        long flightRouteSegmentStartedAtMillis,
        boolean flightCoordinatedEverActive,
        @Nullable DimensionPosition flightPausedPosition,
        Map<TardisUpgradeType, Integer> upgrades,
        long artronEnergy,
        long artronBaseCapacity,
        long artronLastUpdateMillis,
        int engineHealth,
        int navigationHealth,
        int stabilizerHealth,
        int dematerializationHealth,
        TardisConsoleState consoleState
    ) {
        this.tardisId = tardisId;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.createdAt = createdAt;
        this.accessMode = accessMode == null ? TardisAccessMode.PUBLIC : accessMode;
        this.crew = new LinkedHashMap<>();
        if (crew != null) {
            for (Map.Entry<UUID, TardisCrewMember> entry : crew.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && !entry.getKey().equals(ownerId)) {
                    this.crew.put(entry.getKey(), entry.getValue());
                }
            }
        }
        this.interiorDimension = interiorDimension;
        this.interiorIndex = interiorIndex;
        this.interiorPosition = interiorPosition;
        this.interiorGenerated = interiorGenerated;
        this.rooms = new ArrayList<>(rooms == null ? Collections.<TardisRoomRecord>emptyList() : rooms);
        int highestRoomId = 0;
        for (TardisRoomRecord room : this.rooms) {
            highestRoomId = Math.max(highestRoomId, room.getRoomId());
        }
        this.nextRoomId = Math.max(Math.max(1, nextRoomId), highestRoomId + 1);
        this.roomConnections = new ArrayList<>();
        if (roomConnections != null) {
            for (TardisRoomConnection connection : roomConnections) {
                if (connection == null) {
                    continue;
                }
                int firstId = connection.getFirstRoomId();
                int secondId = connection.getSecondRoomId();
                if ((firstId != 0 && getRoom(firstId) == null) || (secondId != 0 && getRoom(secondId) == null)) {
                    continue;
                }
                if (getRoomConnection(firstId, secondId) == null) {
                    this.roomConnections.add(connection);
                }
            }
        }
        this.transmatPosition = transmatPosition;
        this.exteriorPosition = exteriorPosition;
        this.exteriorLocked = exteriorLocked;
        this.exteriorShell = exteriorShell == null ? ExteriorShell.PROTOTYPE : exteriorShell;
        this.inFlight = inFlight;
        this.flightDeparture = flightDeparture;
        this.flightDepartureFacing = normalizeFacing(flightDepartureFacing);
        this.destinationPosition = destinationPosition;
        this.destinationFacing = normalizeFacing(destinationFacing);
        this.flightSpeed = flightSpeed == null ? FlightSpeed.NORMAL : flightSpeed;
        this.flightAccuracy = flightAccuracy == null ? FlightAccuracy.PRECISE : flightAccuracy;
        this.flightResolvedDestination = flightResolvedDestination;
        this.flightRouteStart = flightRouteStart;
        this.flightRouteDestination = flightRouteDestination;
        this.flightStartedAtMillis = Math.max(0L, flightStartedAtMillis);
        this.flightReadyAtMillis = Math.max(0L, flightReadyAtMillis);
        this.flightRouteDurationMillis = Math.max(0L, flightRouteDurationMillis);
        this.flightRouteElapsedMillis = FlightTravelMath.clampElapsed(
            flightRouteElapsedMillis,
            this.flightRouteDurationMillis
        );
        this.flightRouteSegmentStartedAtMillis = Math.max(0L, flightRouteSegmentStartedAtMillis);
        this.flightCoordinatedEverActive = flightCoordinatedEverActive;
        this.flightPausedPosition = flightPausedPosition;
        this.upgrades = new EnumMap<>(TardisUpgradeType.class);
        if (upgrades != null) {
            for (Map.Entry<TardisUpgradeType, Integer> entry : upgrades.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0) {
                    this.upgrades.put(entry.getKey(), entry.getValue());
                }
            }
        }
        this.artronBaseCapacity = Math.max(1L, artronBaseCapacity);
        this.artronEnergy = Math.max(0L, Math.min(getArtronCapacity(), artronEnergy));
        this.artronLastUpdateMillis = Math.max(0L, artronLastUpdateMillis);
        this.engineHealth = clampSubsystemHealth(engineHealth);
        this.navigationHealth = clampSubsystemHealth(navigationHealth);
        this.stabilizerHealth = clampSubsystemHealth(stabilizerHealth);
        this.dematerializationHealth = clampSubsystemHealth(dematerializationHealth);
        this.consoleState = consoleState == null ? new TardisConsoleState() : consoleState;
        synchronizePhysicalFlightControls();
    }

    private void synchronizePhysicalFlightControls() {
        consoleState.setControlState(
            ConsoleControlId.MAIN_SPEED,
            flightSpeed.getConsoleState()
        );
        consoleState.setControlState(
            ConsoleControlId.MAIN_QUANTUM_HANDBRAKE,
            inFlight ? 1 : 0
        );
        if (destinationPosition != null) {
            consoleState.setControlState(
                ConsoleControlId.MAIN_DIRECTION,
                horizontalFacingState(destinationFacing)
            );
        }
    }

    private static int horizontalFacingState(EnumFacing facing) {
        EnumFacing horizontal = normalizeFacing(facing);
        switch (horizontal) {
            case EAST:
                return 1;
            case SOUTH:
                return 2;
            case WEST:
                return 3;
            case NORTH:
            default:
                return 0;
        }
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
        crew.remove(ownerId);
    }

    public TardisAccessMode getAccessMode() {
        return accessMode;
    }

    void setAccessMode(TardisAccessMode accessMode) {
        this.accessMode = accessMode == null ? TardisAccessMode.PUBLIC : accessMode;
    }

    public Map<UUID, TardisCrewMember> getCrew() {
        return Collections.unmodifiableMap(crew);
    }

    @Nullable
    public TardisCrewMember getCrewMember(UUID playerId) {
        return playerId == null ? null : crew.get(playerId);
    }

    @Nullable
    public TardisCrewMember getCrewMemberByName(String playerName) {
        if (playerName == null) {
            return null;
        }
        for (TardisCrewMember member : crew.values()) {
            if (member.getPlayerName().equalsIgnoreCase(playerName)) {
                return member;
            }
        }
        return null;
    }

    public boolean hasCrewPermission(UUID playerId, TardisPermission permission) {
        TardisCrewMember member = getCrewMember(playerId);
        return member != null && member.hasPermission(permission);
    }

    boolean addOrUpdateCrewMember(UUID playerId, String playerName, Set<TardisPermission> permissions) {
        if (playerId == null || playerId.equals(ownerId)) {
            return false;
        }
        TardisCrewMember existing = crew.get(playerId);
        if (existing == null) {
            crew.put(playerId, new TardisCrewMember(playerId, playerName, permissions));
            return true;
        }
        boolean changed = false;
        String safeName = playerName == null ? "" : playerName;
        if (!existing.getPlayerName().equals(safeName)) {
            existing.setPlayerName(safeName);
            changed = true;
        }
        return existing.setPermissions(permissions) || changed;
    }

    boolean removeCrewMember(UUID playerId) {
        return playerId != null && crew.remove(playerId) != null;
    }

    boolean grantCrewPermission(UUID playerId, TardisPermission permission) {
        TardisCrewMember member = getCrewMember(playerId);
        return member != null && member.grant(permission);
    }

    boolean revokeCrewPermission(UUID playerId, TardisPermission permission) {
        TardisCrewMember member = getCrewMember(playerId);
        return member != null && member.revoke(permission);
    }

    boolean clearCrew() {
        if (crew.isEmpty()) {
            return false;
        }
        crew.clear();
        return true;
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

    public List<TardisRoomRecord> getRooms() {
        return Collections.unmodifiableList(rooms);
    }

    @Nullable
    public TardisRoomRecord getRoom(int roomId) {
        for (TardisRoomRecord room : rooms) {
            if (room.getRoomId() == roomId) {
                return room;
            }
        }
        return null;
    }

    @Nullable
    public TardisRoomRecord getRoomAtGrid(int gridX, int gridZ) {
        for (TardisRoomRecord room : rooms) {
            if (room.getGridX() == gridX && room.getGridZ() == gridZ) {
                return room;
            }
        }
        return null;
    }

    TardisRoomRecord addRoom(
        String schemaName,
        int gridX,
        int gridZ,
        int parentRoomId,
        EnumFacing connectionFacing
    ) {
        TardisRoomRecord room = new TardisRoomRecord(
            nextRoomId++,
            schemaName,
            gridX,
            gridZ,
            parentRoomId,
            connectionFacing
        );
        rooms.add(room);
        return room;
    }

    boolean removeRoom(int roomId) {
        for (int index = 0; index < rooms.size(); index++) {
            if (rooms.get(index).getRoomId() == roomId) {
                rooms.remove(index);
                for (int connectionIndex = roomConnections.size() - 1; connectionIndex >= 0; connectionIndex--) {
                    if (roomConnections.get(connectionIndex).connects(roomId)) {
                        roomConnections.remove(connectionIndex);
                    }
                }
                return true;
            }
        }
        return false;
    }

    boolean setRoomGenerated(int roomId, boolean generated) {
        TardisRoomRecord room = getRoom(roomId);
        if (room == null) {
            return false;
        }
        room.setGenerated(generated);
        return true;
    }

    public List<TardisRoomConnection> getRoomConnections() {
        return Collections.unmodifiableList(roomConnections);
    }

    @Nullable
    public TardisRoomConnection getRoomConnection(int firstRoomId, int secondRoomId) {
        for (TardisRoomConnection connection : roomConnections) {
            if (connection.matches(firstRoomId, secondRoomId)) {
                return connection;
            }
        }
        return null;
    }

    TardisRoomConnection addRoomConnection(int firstRoomId, int secondRoomId) {
        TardisRoomConnection existing = getRoomConnection(firstRoomId, secondRoomId);
        if (existing != null) {
            return existing;
        }
        TardisRoomConnection connection = new TardisRoomConnection(firstRoomId, secondRoomId);
        roomConnections.add(connection);
        return connection;
    }

    boolean removeRoomConnection(int firstRoomId, int secondRoomId) {
        for (int index = 0; index < roomConnections.size(); index++) {
            if (roomConnections.get(index).matches(firstRoomId, secondRoomId)) {
                roomConnections.remove(index);
                return true;
            }
        }
        return false;
    }

    boolean setRoomConnectionGenerated(int firstRoomId, int secondRoomId, boolean generated) {
        TardisRoomConnection connection = getRoomConnection(firstRoomId, secondRoomId);
        if (connection == null) {
            return false;
        }
        connection.setGenerated(generated);
        return true;
    }

    public int countSecondaryConnections(int roomId) {
        int count = 0;
        for (TardisRoomConnection connection : roomConnections) {
            if (connection.connects(roomId)) {
                count++;
            }
        }
        return count;
    }

    @Nullable
    public DimensionPosition getTransmatPosition() {
        return transmatPosition;
    }

    void setTransmatPosition(@Nullable DimensionPosition transmatPosition) {
        this.transmatPosition = transmatPosition;
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

    public FlightSpeed getFlightSpeed() {
        return flightSpeed;
    }

    public FlightAccuracy getFlightAccuracy() {
        return flightAccuracy;
    }

    @Nullable
    public DimensionPosition getFlightResolvedDestination() {
        return flightResolvedDestination;
    }

    @Nullable
    public DimensionPosition getFlightRouteStart() {
        return flightRouteStart;
    }

    @Nullable
    public DimensionPosition getFlightRouteDestination() {
        return flightRouteDestination;
    }

    public boolean isFlightRouteFor(@Nullable DimensionPosition destination) {
        return samePosition(flightRouteDestination, destination);
    }

    public long getFlightStartedAtMillis() {
        return flightStartedAtMillis;
    }

    /** Absolute ready timestamp while coordinated travel is active, otherwise zero. */
    public long getFlightReadyAtMillis() {
        return flightReadyAtMillis;
    }

    public long getFlightDurationMillis() {
        return inFlight && flightCoordinatedEverActive ? flightRouteDurationMillis : 0L;
    }

    public long getFlightElapsedMillis(long nowMillis) {
        if (!inFlight || !flightCoordinatedEverActive) {
            return 0L;
        }
        long elapsed = flightRouteElapsedMillis;
        if (flightRouteSegmentStartedAtMillis > 0L) {
            elapsed += Math.max(0L, nowMillis - flightRouteSegmentStartedAtMillis);
        }
        return FlightTravelMath.clampElapsed(elapsed, flightRouteDurationMillis);
    }

    public long getFlightRemainingMillis(long nowMillis) {
        if (!inFlight || !flightCoordinatedEverActive || flightResolvedDestination == null) {
            return 0L;
        }
        return FlightTravelMath.remainingMillis(
            getFlightElapsedMillis(nowMillis),
            flightRouteDurationMillis
        );
    }

    public boolean isFlightReady(long nowMillis) {
        return inFlight
            && flightCoordinatedEverActive
            && flightResolvedDestination != null
            && getFlightRemainingMillis(nowMillis) == 0L;
    }

    public int getFlightProgressPercent(long nowMillis) {
        if (!inFlight || !flightCoordinatedEverActive || flightResolvedDestination == null) {
            return 0;
        }
        return FlightTravelMath.progressPercent(
            getFlightElapsedMillis(nowMillis),
            flightRouteDurationMillis
        );
    }

    public boolean isCoordinatedDriveMode() {
        return consoleState.getControlState(ConsoleControlId.MAIN_DRIVE_MODE) == 0;
    }

    public boolean hasCoordinatedTravel() {
        return inFlight && flightCoordinatedEverActive;
    }

    public boolean hasFlightRoute() {
        return inFlight
            && flightRouteStart != null
            && flightRouteDestination != null
            && flightResolvedDestination != null;
    }

    @Nullable
    public DimensionPosition getFlightPausedPosition() {
        return flightPausedPosition;
    }

    /**
     * Returns the conceptual exterior position reached by coordinated travel.
     * Minecraft dimensions have no continuous shared coordinate space, so a
     * cross-dimensional route remains at its route start until it completes.
     */
    @Nullable
    public DimensionPosition getCurrentFlightPosition(long nowMillis) {
        if (!inFlight) {
            return exteriorPosition;
        }
        if (flightRouteSegmentStartedAtMillis <= 0L && flightPausedPosition != null) {
            return flightPausedPosition;
        }
        return calculateRoutePosition(nowMillis);
    }

    @Nullable
    private DimensionPosition calculateRoutePosition(long nowMillis) {
        DimensionPosition fallback = flightRouteStart != null ? flightRouteStart : flightDeparture;
        if (!flightCoordinatedEverActive || fallback == null || flightResolvedDestination == null) {
            return fallback;
        }
        long elapsed = getFlightElapsedMillis(nowMillis);
        long duration = flightRouteDurationMillis;
        if (duration <= 0L || elapsed >= duration) {
            return flightResolvedDestination;
        }
        if (fallback.getDimension() != flightResolvedDestination.getDimension()) {
            return fallback;
        }
        net.minecraft.util.math.BlockPos from = fallback.getPosition();
        net.minecraft.util.math.BlockPos to = flightResolvedDestination.getPosition();
        return new DimensionPosition(
            fallback.getDimension(),
            new net.minecraft.util.math.BlockPos(
                FlightTravelMath.interpolateCoordinate(from.getX(), to.getX(), elapsed, duration),
                FlightTravelMath.interpolateCoordinate(from.getY(), to.getY(), elapsed, duration),
                FlightTravelMath.interpolateCoordinate(from.getZ(), to.getZ(), elapsed, duration)
            )
        );
    }

    void setDestination(@Nullable DimensionPosition destinationPosition, EnumFacing destinationFacing) {
        this.destinationPosition = destinationPosition;
        this.destinationFacing = normalizeFacing(destinationFacing);
    }

    void setFlightSpeed(FlightSpeed flightSpeed) {
        this.flightSpeed = flightSpeed == null ? FlightSpeed.NORMAL : flightSpeed;
    }

    void setFlightAccuracy(FlightAccuracy flightAccuracy) {
        this.flightAccuracy = flightAccuracy == null ? FlightAccuracy.PRECISE : flightAccuracy;
    }

    void beginFlight(
        long startedAtMillis,
        EnumFacing departureFacing,
        @Nullable DimensionPosition routeDestination,
        @Nullable DimensionPosition resolvedDestination,
        long routeDurationMillis,
        boolean coordinated
    ) {
        if (inFlight) {
            return;
        }
        flightDeparture = exteriorPosition;
        flightDepartureFacing = normalizeFacing(departureFacing);
        exteriorPosition = null;
        inFlight = true;
        flightStartedAtMillis = Math.max(0L, startedAtMillis);
        flightRouteStart = flightDeparture;
        flightRouteDestination = coordinated ? routeDestination : null;
        flightResolvedDestination = coordinated ? resolvedDestination : null;
        flightRouteDurationMillis = coordinated ? Math.max(0L, routeDurationMillis) : 0L;
        flightRouteElapsedMillis = 0L;
        flightRouteSegmentStartedAtMillis = coordinated ? flightStartedAtMillis : 0L;
        flightCoordinatedEverActive = coordinated;
        flightPausedPosition = coordinated ? null : flightDeparture;
        flightReadyAtMillis = coordinated
            ? flightStartedAtMillis + flightRouteDurationMillis
            : 0L;
    }

    boolean pauseFlightRoute(long nowMillis) {
        if (!inFlight || !flightCoordinatedEverActive || !hasFlightRoute()) {
            flightReadyAtMillis = 0L;
            return false;
        }
        DimensionPosition reached = calculateRoutePosition(nowMillis);
        if (reached == null) {
            return false;
        }
        flightRouteElapsedMillis = getFlightElapsedMillis(nowMillis);
        flightRouteSegmentStartedAtMillis = 0L;
        flightReadyAtMillis = 0L;
        flightPausedPosition = reached;
        return true;
    }

    boolean resumeFlightRoute(long nowMillis) {
        if (
            !inFlight
                || flightResolvedDestination == null
                || flightRouteStart == null
                || flightRouteDestination == null
        ) {
            return false;
        }
        if (flightRouteSegmentStartedAtMillis > 0L) {
            flightPausedPosition = null;
            return true;
        }
        flightCoordinatedEverActive = true;
        flightPausedPosition = null;
        flightRouteSegmentStartedAtMillis = Math.max(1L, nowMillis);
        flightReadyAtMillis = flightRouteSegmentStartedAtMillis
            + FlightTravelMath.remainingMillis(flightRouteElapsedMillis, flightRouteDurationMillis);
        return true;
    }

    void setFlightRoute(
        DimensionPosition routeStart,
        DimensionPosition routeDestination,
        DimensionPosition resolvedDestination,
        long routeDurationMillis,
        long nowMillis,
        boolean active
    ) {
        flightRouteStart = routeStart;
        flightRouteDestination = routeDestination;
        flightResolvedDestination = resolvedDestination;
        flightRouteDurationMillis = Math.max(0L, routeDurationMillis);
        flightRouteElapsedMillis = 0L;
        flightPausedPosition = active ? null : routeStart;
        if (active) {
            flightCoordinatedEverActive = true;
            flightRouteSegmentStartedAtMillis = Math.max(1L, nowMillis);
            flightReadyAtMillis = flightRouteSegmentStartedAtMillis + flightRouteDurationMillis;
        } else {
            flightRouteSegmentStartedAtMillis = 0L;
            flightReadyAtMillis = 0L;
        }
    }

    void abortFlight() {
        if (!inFlight) {
            return;
        }
        exteriorPosition = flightDeparture;
        flightDeparture = null;
        flightDepartureFacing = EnumFacing.NORTH;
        inFlight = false;
        clearFlightState();
    }

    void completeFlight(
        DimensionPosition landedPosition,
        EnumFacing landedFacing,
        boolean keepLandedPositionAsDestination
    ) {
        if (flightDeparture != null) {
            consoleState.setPreviousMaterializedPosition(
                flightDeparture,
                flightDepartureFacing
            );
        }
        exteriorPosition = landedPosition;
        flightDeparture = null;
        flightDepartureFacing = EnumFacing.NORTH;
        if (keepLandedPositionAsDestination) {
            destinationPosition = landedPosition;
            destinationFacing = normalizeFacing(landedFacing);
        } else {
            destinationPosition = null;
            destinationFacing = EnumFacing.NORTH;
        }
        inFlight = false;
        clearFlightState();
    }

    public int getUpgradeCount(TardisUpgradeType type) {
        if (type == null) {
            return 0;
        }
        Integer count = upgrades.get(type);
        return count == null ? 0 : Math.max(0, count);
    }

    public int getInstalledUpgradeCount() {
        int total = 0;
        for (Integer count : upgrades.values()) {
            if (count != null && count > 0) {
                total += count;
            }
        }
        return total;
    }

    public Map<TardisUpgradeType, Integer> getUpgrades() {
        return Collections.unmodifiableMap(upgrades);
    }

    boolean setUpgradeCount(TardisUpgradeType type, int count) {
        if (type == null || count < 0) {
            return false;
        }
        int current = getUpgradeCount(type);
        if (current == count) {
            return false;
        }
        if (count == 0) {
            upgrades.remove(type);
        } else {
            upgrades.put(type, count);
        }
        artronEnergy = Math.min(artronEnergy, getArtronCapacity());
        return true;
    }

    public long getArtronEnergy() {
        return artronEnergy;
    }

    public long getBaseArtronCapacity() {
        return artronBaseCapacity;
    }

    public long getArtronCapacity() {
        long bonus = (long) getUpgradeCount(TardisUpgradeType.CAPACITY)
            * Math.max(0, TardisConfig.getCapacityBonusPerUpgrade());
        if (bonus > Long.MAX_VALUE - artronBaseCapacity) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, artronBaseCapacity + bonus);
    }

    public long getArtronLastUpdateMillis() {
        return artronLastUpdateMillis;
    }

    /** Applies lazy real-time regeneration while preserving sub-second remainder. */
    boolean refreshArtronEnergy(long nowMillis, long regenerationPerSecond) {
        long safeNow = Math.max(0L, nowMillis);
        if (artronLastUpdateMillis <= 0L || safeNow < artronLastUpdateMillis) {
            artronLastUpdateMillis = safeNow;
            return true;
        }
        if (artronEnergy >= getArtronCapacity()) {
            boolean changed = artronEnergy != getArtronCapacity() || artronLastUpdateMillis != safeNow;
            artronEnergy = getArtronCapacity();
            artronLastUpdateMillis = safeNow;
            return changed;
        }
        if (regenerationPerSecond <= 0L) {
            if (artronLastUpdateMillis != safeNow) {
                artronLastUpdateMillis = safeNow;
                return true;
            }
            return false;
        }

        long elapsedMillis = safeNow - artronLastUpdateMillis;
        long elapsedSeconds = elapsedMillis / 1000L;
        if (elapsedSeconds <= 0L) {
            return false;
        }

        long possibleGain;
        if (elapsedSeconds > Long.MAX_VALUE / regenerationPerSecond) {
            possibleGain = Long.MAX_VALUE;
        } else {
            possibleGain = elapsedSeconds * regenerationPerSecond;
        }
        long missing = getArtronCapacity() - artronEnergy;
        long gained = Math.min(missing, possibleGain);
        artronEnergy += gained;
        if (artronEnergy >= getArtronCapacity()) {
            artronEnergy = getArtronCapacity();
            artronLastUpdateMillis = safeNow;
        } else {
            artronLastUpdateMillis += elapsedSeconds * 1000L;
        }
        return gained > 0L || elapsedSeconds > 0L;
    }

    boolean consumeArtronEnergy(long amount) {
        long safeAmount = Math.max(0L, amount);
        if (artronEnergy < safeAmount) {
            return false;
        }
        artronEnergy -= safeAmount;
        return true;
    }

    long addArtronEnergy(long amount) {
        long safeAmount = Math.max(0L, amount);
        long previous = artronEnergy;
        if (safeAmount > getArtronCapacity() - artronEnergy) {
            artronEnergy = getArtronCapacity();
        } else {
            artronEnergy += safeAmount;
        }
        return artronEnergy - previous;
    }

    void setArtronEnergy(long amount, long nowMillis) {
        artronEnergy = Math.max(0L, Math.min(getArtronCapacity(), amount));
        artronLastUpdateMillis = Math.max(0L, nowMillis);
    }

    void setArtronCapacity(long capacity, long nowMillis) {
        artronBaseCapacity = Math.max(1L, capacity);
        artronEnergy = Math.min(artronEnergy, getArtronCapacity());
        artronLastUpdateMillis = Math.max(0L, nowMillis);
    }


    public TardisConsoleState getConsoleState() {
        return consoleState;
    }

    public int getSubsystemHealth(TardisSubsystem subsystem) {
        if (subsystem == null) {
            return 0;
        }
        switch (subsystem) {
            case ENGINES:
                return engineHealth;
            case NAVIGATION:
                return navigationHealth;
            case STABILIZERS:
                return stabilizerHealth;
            case DEMATERIALIZATION:
                return dematerializationHealth;
            default:
                return 0;
        }
    }

    boolean setSubsystemHealth(TardisSubsystem subsystem, int health) {
        if (subsystem == null) {
            return false;
        }
        int clamped = clampSubsystemHealth(health);
        if (getSubsystemHealth(subsystem) == clamped) {
            return false;
        }
        switch (subsystem) {
            case ENGINES:
                engineHealth = clamped;
                break;
            case NAVIGATION:
                navigationHealth = clamped;
                break;
            case STABILIZERS:
                stabilizerHealth = clamped;
                break;
            case DEMATERIALIZATION:
                dematerializationHealth = clamped;
                break;
            default:
                return false;
        }
        return true;
    }

    int damageSubsystem(TardisSubsystem subsystem, int amount) {
        int previous = getSubsystemHealth(subsystem);
        setSubsystemHealth(subsystem, previous - Math.max(0, amount));
        return previous - getSubsystemHealth(subsystem);
    }

    int repairSubsystem(TardisSubsystem subsystem, int amount) {
        int previous = getSubsystemHealth(subsystem);
        setSubsystemHealth(subsystem, previous + Math.max(0, amount));
        return getSubsystemHealth(subsystem) - previous;
    }

    private static int clampSubsystemHealth(int health) {
        return Math.max(0, Math.min(TardisDamageService.MAX_HEALTH, health));
    }

    private void clearFlightState() {
        flightResolvedDestination = null;
        flightRouteStart = null;
        flightRouteDestination = null;
        flightStartedAtMillis = 0L;
        flightReadyAtMillis = 0L;
        flightRouteDurationMillis = 0L;
        flightRouteElapsedMillis = 0L;
        flightRouteSegmentStartedAtMillis = 0L;
        flightCoordinatedEverActive = false;
        flightPausedPosition = null;
    }

    private static boolean samePosition(
        @Nullable DimensionPosition first,
        @Nullable DimensionPosition second
    ) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.getDimension() == second.getDimension()
            && first.getPosition().equals(second.getPosition());
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
        tag.setString("AccessMode", accessMode.getName());
        net.minecraft.nbt.NBTTagList crewList = new net.minecraft.nbt.NBTTagList();
        for (TardisCrewMember member : crew.values()) {
            crewList.appendTag(member.writeToNBT());
        }
        tag.setTag("Crew", crewList);

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
        tag.setInteger("NextRoomId", nextRoomId);
        net.minecraft.nbt.NBTTagList roomList = new net.minecraft.nbt.NBTTagList();
        for (TardisRoomRecord room : rooms) {
            roomList.appendTag(room.writeToNBT());
        }
        tag.setTag("Rooms", roomList);
        net.minecraft.nbt.NBTTagList connectionList = new net.minecraft.nbt.NBTTagList();
        for (TardisRoomConnection connection : roomConnections) {
            connectionList.appendTag(connection.writeToNBT());
        }
        tag.setTag("RoomConnections", connectionList);

        if (transmatPosition != null) {
            tag.setTag("TransmatPosition", transmatPosition.writeToNBT());
        }

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
        tag.setString("FlightSpeed", flightSpeed.getName());
        tag.setString("FlightAccuracy", flightAccuracy.getName());
        if (inFlight && flightResolvedDestination != null) {
            tag.setTag("FlightResolvedDestination", flightResolvedDestination.writeToNBT());
        }
        if (inFlight && flightRouteStart != null) {
            tag.setTag("FlightRouteStart", flightRouteStart.writeToNBT());
        }
        if (inFlight && flightRouteDestination != null) {
            tag.setTag("FlightRouteDestination", flightRouteDestination.writeToNBT());
        }
        if (inFlight && flightPausedPosition != null) {
            tag.setTag("FlightPausedPosition", flightPausedPosition.writeToNBT());
        }
        if (inFlight) {
            tag.setLong("FlightStartedAtMillis", flightStartedAtMillis);
            tag.setLong("FlightReadyAtMillis", flightReadyAtMillis);
            tag.setLong("FlightRouteDurationMillis", flightRouteDurationMillis);
            tag.setLong("FlightRouteElapsedMillis", flightRouteElapsedMillis);
            tag.setLong("FlightRouteSegmentStartedAtMillis", flightRouteSegmentStartedAtMillis);
            tag.setBoolean("FlightCoordinatedEverActive", flightCoordinatedEverActive);
        }

        NBTTagCompound upgradesTag = new NBTTagCompound();
        for (TardisUpgradeType type : TardisUpgradeType.values()) {
            int count = getUpgradeCount(type);
            if (count > 0) {
                upgradesTag.setInteger(type.getName(), count);
            }
        }
        tag.setTag("Upgrades", upgradesTag);

        tag.setLong("ArtronEnergy", artronEnergy);
        tag.setLong("ArtronBaseCapacity", artronBaseCapacity);
        tag.setLong("ArtronCapacity", getArtronCapacity());
        tag.setLong("ArtronLastUpdateMillis", artronLastUpdateMillis);

        NBTTagCompound systemsTag = new NBTTagCompound();
        systemsTag.setInteger("Engines", engineHealth);
        systemsTag.setInteger("Navigation", navigationHealth);
        systemsTag.setInteger("Stabilizers", stabilizerHealth);
        systemsTag.setInteger("Dematerialization", dematerializationHealth);
        tag.setTag("SubsystemHealth", systemsTag);

        tag.setTag("ConsoleState", consoleState.writeToNBT());

        return tag;
    }

    public static TardisRecord readFromNBT(NBTTagCompound tag) {
        UUID tardisId = UUID.fromString(tag.getString("TardisId"));
        UUID ownerId = UUID.fromString(tag.getString("OwnerId"));
        String ownerName = tag.getString("OwnerName");
        long createdAt = tag.getLong("CreatedAt");
        TardisAccessMode accessMode = tag.hasKey("AccessMode", Constants.NBT.TAG_STRING)
            ? TardisAccessMode.fromName(tag.getString("AccessMode"))
            : TardisAccessMode.PUBLIC;
        if (accessMode == null) {
            accessMode = TardisAccessMode.PUBLIC;
        }
        Map<UUID, TardisCrewMember> crew = new LinkedHashMap<>();
        if (tag.hasKey("Crew", Constants.NBT.TAG_LIST)) {
            net.minecraft.nbt.NBTTagList crewList = tag.getTagList("Crew", Constants.NBT.TAG_COMPOUND);
            for (int index = 0; index < crewList.tagCount(); index++) {
                try {
                    TardisCrewMember member = TardisCrewMember.readFromNBT(crewList.getCompoundTagAt(index));
                    if (!member.getPlayerId().equals(ownerId)) {
                        crew.put(member.getPlayerId(), member);
                    }
                } catch (RuntimeException ignored) {
                    // Keep the TARDIS record loadable if one crew entry is damaged.
                }
            }
        }

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
        List<TardisRoomRecord> rooms = new ArrayList<>();
        if (tag.hasKey("Rooms", Constants.NBT.TAG_LIST)) {
            net.minecraft.nbt.NBTTagList roomList = tag.getTagList("Rooms", Constants.NBT.TAG_COMPOUND);
            for (int index = 0; index < roomList.tagCount(); index++) {
                try {
                    rooms.add(TardisRoomRecord.readFromNBT(roomList.getCompoundTagAt(index)));
                } catch (RuntimeException ignored) {
                    // Keep the owning TARDIS record loadable if one room entry is damaged.
                }
            }
        }
        int nextRoomId = tag.hasKey("NextRoomId", Constants.NBT.TAG_INT)
            ? tag.getInteger("NextRoomId")
            : 1;
        List<TardisRoomConnection> roomConnections = new ArrayList<>();
        if (tag.hasKey("RoomConnections", Constants.NBT.TAG_LIST)) {
            net.minecraft.nbt.NBTTagList connectionList = tag.getTagList(
                "RoomConnections",
                Constants.NBT.TAG_COMPOUND
            );
            for (int index = 0; index < connectionList.tagCount(); index++) {
                try {
                    roomConnections.add(
                        TardisRoomConnection.readFromNBT(connectionList.getCompoundTagAt(index))
                    );
                } catch (RuntimeException ignored) {
                    // Keep the owning TARDIS record loadable if one connection is damaged.
                }
            }
        }
        DimensionPosition transmatPosition = tag.hasKey("TransmatPosition", Constants.NBT.TAG_COMPOUND)
            ? DimensionPosition.readFromNBT(tag.getCompoundTag("TransmatPosition"))
            : null;

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
        FlightSpeed flightSpeed = tag.hasKey("FlightSpeed", Constants.NBT.TAG_STRING)
            ? FlightSpeed.fromName(tag.getString("FlightSpeed"))
            : FlightSpeed.NORMAL;
        if (flightSpeed == null) {
            flightSpeed = FlightSpeed.NORMAL;
        }
        FlightAccuracy flightAccuracy = tag.hasKey("FlightAccuracy", Constants.NBT.TAG_STRING)
            ? FlightAccuracy.fromName(tag.getString("FlightAccuracy"))
            : FlightAccuracy.PRECISE;
        if (flightAccuracy == null) {
            flightAccuracy = FlightAccuracy.PRECISE;
        }
        DimensionPosition flightResolvedDestination = tag.hasKey("FlightResolvedDestination", Constants.NBT.TAG_COMPOUND)
            ? DimensionPosition.readFromNBT(tag.getCompoundTag("FlightResolvedDestination"))
            : null;
        long flightStartedAtMillis = tag.hasKey("FlightStartedAtMillis", Constants.NBT.TAG_LONG)
            ? tag.getLong("FlightStartedAtMillis")
            : 0L;
        long flightReadyAtMillis = tag.hasKey("FlightReadyAtMillis", Constants.NBT.TAG_LONG)
            ? tag.getLong("FlightReadyAtMillis")
            : 0L;
        boolean hasRouteState = tag.hasKey("FlightCoordinatedEverActive", Constants.NBT.TAG_BYTE)
            || tag.hasKey("FlightRouteDurationMillis", Constants.NBT.TAG_LONG)
            || tag.hasKey("FlightRouteStart", Constants.NBT.TAG_COMPOUND)
            || tag.hasKey("FlightRouteDestination", Constants.NBT.TAG_COMPOUND);
        DimensionPosition flightRouteStart = tag.hasKey("FlightRouteStart", Constants.NBT.TAG_COMPOUND)
            ? DimensionPosition.readFromNBT(tag.getCompoundTag("FlightRouteStart"))
            : null;
        DimensionPosition flightRouteDestination = tag.hasKey(
            "FlightRouteDestination",
            Constants.NBT.TAG_COMPOUND
        )
            ? DimensionPosition.readFromNBT(tag.getCompoundTag("FlightRouteDestination"))
            : null;
        long flightRouteDurationMillis = tag.hasKey("FlightRouteDurationMillis", Constants.NBT.TAG_LONG)
            ? Math.max(0L, tag.getLong("FlightRouteDurationMillis"))
            : 0L;
        long flightRouteElapsedMillis = tag.hasKey("FlightRouteElapsedMillis", Constants.NBT.TAG_LONG)
            ? Math.max(0L, tag.getLong("FlightRouteElapsedMillis"))
            : 0L;
        long flightRouteSegmentStartedAtMillis = tag.hasKey(
            "FlightRouteSegmentStartedAtMillis",
            Constants.NBT.TAG_LONG
        )
            ? Math.max(0L, tag.getLong("FlightRouteSegmentStartedAtMillis"))
            : 0L;
        boolean flightCoordinatedEverActive = tag.hasKey(
            "FlightCoordinatedEverActive",
            Constants.NBT.TAG_BYTE
        ) && tag.getBoolean("FlightCoordinatedEverActive");
        DimensionPosition flightPausedPosition = tag.hasKey(
            "FlightPausedPosition",
            Constants.NBT.TAG_COMPOUND
        )
            ? DimensionPosition.readFromNBT(tag.getCompoundTag("FlightPausedPosition"))
            : null;

        EnumMap<TardisUpgradeType, Integer> upgrades = new EnumMap<>(TardisUpgradeType.class);
        if (tag.hasKey("Upgrades", Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound upgradesTag = tag.getCompoundTag("Upgrades");
            for (TardisUpgradeType type : TardisUpgradeType.values()) {
                if (upgradesTag.hasKey(type.getName(), Constants.NBT.TAG_INT)) {
                    int count = Math.max(0, upgradesTag.getInteger(type.getName()));
                    if (count > 0) {
                        upgrades.put(type, count);
                    }
                }
            }
        }

        long artronBaseCapacity = tag.hasKey("ArtronBaseCapacity", Constants.NBT.TAG_LONG)
            ? tag.getLong("ArtronBaseCapacity")
            : tag.hasKey("ArtronCapacity", Constants.NBT.TAG_LONG)
                ? tag.getLong("ArtronCapacity")
                : TardisConfig.getDefaultArtronCapacity();
        artronBaseCapacity = Math.max(1L, artronBaseCapacity);
        long artronEnergy = tag.hasKey("ArtronEnergy", Constants.NBT.TAG_LONG)
            ? tag.getLong("ArtronEnergy")
            : artronBaseCapacity;
        long artronLastUpdateMillis = tag.hasKey("ArtronLastUpdateMillis", Constants.NBT.TAG_LONG)
            ? tag.getLong("ArtronLastUpdateMillis")
            : System.currentTimeMillis();

        NBTTagCompound systemsTag = tag.hasKey("SubsystemHealth", Constants.NBT.TAG_COMPOUND)
            ? tag.getCompoundTag("SubsystemHealth")
            : new NBTTagCompound();
        int engineHealth = systemsTag.hasKey("Engines", Constants.NBT.TAG_INT)
            ? systemsTag.getInteger("Engines")
            : TardisDamageService.MAX_HEALTH;
        int navigationHealth = systemsTag.hasKey("Navigation", Constants.NBT.TAG_INT)
            ? systemsTag.getInteger("Navigation")
            : TardisDamageService.MAX_HEALTH;
        int stabilizerHealth = systemsTag.hasKey("Stabilizers", Constants.NBT.TAG_INT)
            ? systemsTag.getInteger("Stabilizers")
            : TardisDamageService.MAX_HEALTH;
        int dematerializationHealth = systemsTag.hasKey("Dematerialization", Constants.NBT.TAG_INT)
            ? systemsTag.getInteger("Dematerialization")
            : TardisDamageService.MAX_HEALTH;

        TardisConsoleState consoleState = tag.hasKey("ConsoleState", Constants.NBT.TAG_COMPOUND)
            ? TardisConsoleState.readFromNBT(tag.getCompoundTag("ConsoleState"))
            : new TardisConsoleState();
        // The flight record is authoritative over the visible handbrake state,
        // including migration from Phase 20 saves created before physical controls.
        consoleState.setControlState(
            ConsoleControlId.MAIN_QUANTUM_HANDBRAKE,
            inFlight ? 1 : 0
        );

        // A corrupted/incomplete in-flight record must never silently erase a
        // still-present exterior from older data.
        if (inFlight && flightDeparture == null && exteriorPosition != null) {
            flightDeparture = exteriorPosition;
            exteriorPosition = null;
        }

        if (inFlight && !hasRouteState) {
            // Phase 6-21F flights were always coordinated and used one real-time
            // interval. Convert that interval into the pause/resume route state.
            consoleState.setControlState(ConsoleControlId.MAIN_DRIVE_MODE, 0);
            if (flightReadyAtMillis < flightStartedAtMillis) {
                flightReadyAtMillis = flightStartedAtMillis;
            }
            if (flightResolvedDestination == null) {
                flightResolvedDestination = destinationPosition;
            }
            flightRouteStart = flightDeparture;
            flightRouteDestination = destinationPosition;
            flightRouteDurationMillis = Math.max(0L, flightReadyAtMillis - flightStartedAtMillis);
            flightRouteElapsedMillis = 0L;
            flightRouteSegmentStartedAtMillis = Math.max(1L, flightStartedAtMillis);
            flightCoordinatedEverActive = true;
        }
        if (inFlight && flightCoordinatedEverActive && flightRouteDestination == null) {
            // Early Phase 21G snapshots did not persist the requested route separately.
            flightRouteDestination = destinationPosition != null
                ? destinationPosition
                : flightResolvedDestination;
        }
        if (!inFlight) {
            flightRouteStart = null;
            flightRouteDestination = null;
            flightRouteDurationMillis = 0L;
            flightRouteElapsedMillis = 0L;
            flightRouteSegmentStartedAtMillis = 0L;
            flightCoordinatedEverActive = false;
            flightPausedPosition = null;
        } else if (consoleState.getControlState(ConsoleControlId.MAIN_DRIVE_MODE) != 0) {
            // Phase 21G saves did not persist a frozen position for paused or
            // purely uncoordinated flight. Reconstruct it once during load so
            // later landing cannot advance to the route target while paused.
            if (flightPausedPosition == null) {
                if (!flightCoordinatedEverActive) {
                    flightPausedPosition = flightDeparture;
                } else if (flightRouteStart != null && flightResolvedDestination != null) {
                    long elapsed = FlightTravelMath.clampElapsed(
                        flightRouteElapsedMillis,
                        flightRouteDurationMillis
                    );
                    if (
                        flightRouteStart.getDimension()
                            != flightResolvedDestination.getDimension()
                        || flightRouteDurationMillis <= 0L
                    ) {
                        flightPausedPosition = elapsed >= flightRouteDurationMillis
                            ? flightResolvedDestination
                            : flightRouteStart;
                    } else {
                        net.minecraft.util.math.BlockPos from = flightRouteStart.getPosition();
                        net.minecraft.util.math.BlockPos to = flightResolvedDestination.getPosition();
                        flightPausedPosition = new DimensionPosition(
                            flightRouteStart.getDimension(),
                            new net.minecraft.util.math.BlockPos(
                                FlightTravelMath.interpolateCoordinate(
                                    from.getX(),
                                    to.getX(),
                                    elapsed,
                                    flightRouteDurationMillis
                                ),
                                FlightTravelMath.interpolateCoordinate(
                                    from.getY(),
                                    to.getY(),
                                    elapsed,
                                    flightRouteDurationMillis
                                ),
                                FlightTravelMath.interpolateCoordinate(
                                    from.getZ(),
                                    to.getZ(),
                                    elapsed,
                                    flightRouteDurationMillis
                                )
                            )
                        );
                    }
                }
            }
        }

        return new TardisRecord(
            tardisId,
            ownerId,
            ownerName,
            createdAt,
            accessMode,
            crew,
            interiorDimension,
            interiorIndex,
            interiorPosition,
            interiorGenerated,
            rooms,
            nextRoomId,
            roomConnections,
            transmatPosition,
            exteriorPosition,
            exteriorLocked,
            exteriorShell,
            inFlight,
            flightDeparture,
            flightDepartureFacing,
            destinationPosition,
            destinationFacing,
            flightSpeed,
            flightAccuracy,
            flightResolvedDestination,
            flightRouteStart,
            flightRouteDestination,
            flightStartedAtMillis,
            flightReadyAtMillis,
            flightRouteDurationMillis,
            flightRouteElapsedMillis,
            flightRouteSegmentStartedAtMillis,
            flightCoordinatedEverActive,
            flightPausedPosition,
            upgrades,
            artronEnergy,
            artronBaseCapacity,
            artronLastUpdateMillis,
            engineHealth,
            navigationHealth,
            stabilizerHealth,
            dematerializationHealth,
            consoleState
        );
    }
}
