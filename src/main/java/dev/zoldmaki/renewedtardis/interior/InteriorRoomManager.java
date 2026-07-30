package dev.zoldmaki.renewedtardis.interior;

import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisRoomConnection;
import dev.zoldmaki.renewedtardis.data.TardisRoomRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.dimension.InteriorAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

/** Builds, removes, connects, and locates rooms inside one shared-dimension cell. */
public final class InteriorRoomManager {
    public static final int MAIN_ROOM_ID = 0;
    public static final int ROOM_GRID_SPACING = 18;
    public static final int MAX_GRID_DISTANCE = 40;

    private static final int MAIN_HALF_WIDTH = 5;
    private static final int MAIN_HALF_DEPTH = 5;
    private static final int CORRIDOR_HALF_WIDTH = 2;
    private static final int CORRIDOR_HEIGHT = 4;

    private InteriorRoomManager() {
    }

    public static void ensureGeneratedRooms(WorldServer world, TardisWorldData data, TardisRecord record) {
        for (TardisRoomRecord room : record.getRooms()) {
            if (!room.isGenerated()) {
                buildRoomAndConnector(world, record, room);
                data.setRoomGenerated(record.getTardisId(), room.getRoomId(), true);
            }
        }
        for (TardisRoomConnection connection : record.getRoomConnections()) {
            if (!connection.isGenerated()) {
                buildSecondaryConnection(world, record, connection);
                data.setRoomConnectionGenerated(
                    record.getTardisId(),
                    connection.getFirstRoomId(),
                    connection.getSecondRoomId(),
                    true
                );
            }
        }
    }

    public static BlockPos getRoomCenter(TardisRecord record, int roomId) {
        BlockPos mainCenter = requireMainCenter(record);
        if (roomId == MAIN_ROOM_ID) {
            return mainCenter;
        }
        TardisRoomRecord room = requireRoom(record, roomId);
        return getRoomCenter(mainCenter, room.getGridX(), room.getGridZ());
    }

    public static BlockPos getRoomTeleportPosition(TardisRecord record, int roomId) {
        return getRoomCenter(record, roomId).up();
    }

    public static BlockPos getRoomCenter(BlockPos mainCenter, int gridX, int gridZ) {
        return mainCenter.add(gridX * ROOM_GRID_SPACING, 0, gridZ * ROOM_GRID_SPACING);
    }

    public static int getTargetGridX(TardisRecord record, int parentRoomId, EnumFacing direction) {
        return getRoomGridX(record, parentRoomId) + direction.getXOffset();
    }

    public static int getTargetGridZ(TardisRecord record, int parentRoomId, EnumFacing direction) {
        return getRoomGridZ(record, parentRoomId) + direction.getZOffset();
    }

    public static boolean isGridWithinCell(int gridX, int gridZ) {
        return Math.abs(gridX) <= MAX_GRID_DISTANCE && Math.abs(gridZ) <= MAX_GRID_DISTANCE;
    }

    public static boolean roomsExist(TardisRecord record, int firstRoomId, int secondRoomId) {
        return roomExists(record, firstRoomId) && roomExists(record, secondRoomId);
    }

    public static boolean areRoomsAdjacent(TardisRecord record, int firstRoomId, int secondRoomId) {
        return getConnectionDirection(record, firstRoomId, secondRoomId) != null;
    }

    @Nullable
    public static EnumFacing getConnectionDirection(TardisRecord record, int firstRoomId, int secondRoomId) {
        if (!roomsExist(record, firstRoomId, secondRoomId) || firstRoomId == secondRoomId) {
            return null;
        }
        int dx = getRoomGridX(record, secondRoomId) - getRoomGridX(record, firstRoomId);
        int dz = getRoomGridZ(record, secondRoomId) - getRoomGridZ(record, firstRoomId);
        if (dx == 1 && dz == 0) {
            return EnumFacing.EAST;
        }
        if (dx == -1 && dz == 0) {
            return EnumFacing.WEST;
        }
        if (dx == 0 && dz == 1) {
            return EnumFacing.SOUTH;
        }
        if (dx == 0 && dz == -1) {
            return EnumFacing.NORTH;
        }
        return null;
    }

    public static boolean isPrimaryConnection(TardisRecord record, int firstRoomId, int secondRoomId) {
        if (firstRoomId == secondRoomId) {
            return false;
        }
        TardisRoomRecord first = firstRoomId == MAIN_ROOM_ID ? null : record.getRoom(firstRoomId);
        TardisRoomRecord second = secondRoomId == MAIN_ROOM_ID ? null : record.getRoom(secondRoomId);
        return (first != null && first.getParentRoomId() == secondRoomId)
            || (second != null && second.getParentRoomId() == firstRoomId);
    }

    public static boolean hasAnyConnection(TardisRecord record, int firstRoomId, int secondRoomId) {
        return isPrimaryConnection(record, firstRoomId, secondRoomId)
            || record.getRoomConnection(firstRoomId, secondRoomId) != null;
    }

    public static int countChildren(TardisRecord record, int roomId) {
        int count = 0;
        for (TardisRoomRecord room : record.getRooms()) {
            if (room.getParentRoomId() == roomId) {
                count++;
            }
        }
        return count;
    }

    public static int countSecondaryConnections(TardisRecord record, int roomId) {
        return record.countSecondaryConnections(roomId);
    }

    public static List<EntityPlayerMP> getPlayersInRemovalArea(
        WorldServer world,
        TardisRecord record,
        TardisRoomRecord room
    ) {
        List<EntityPlayerMP> players = world.getEntitiesWithinAABB(
            EntityPlayerMP.class,
            getRemovalBounds(record, room)
        );
        return players.isEmpty() ? Collections.<EntityPlayerMP>emptyList() : new ArrayList<>(players);
    }

    public static List<EntityPlayerMP> getPlayersInConnectionArea(
        WorldServer world,
        TardisRecord record,
        int firstRoomId,
        int secondRoomId
    ) {
        List<EntityPlayerMP> players = world.getEntitiesWithinAABB(
            EntityPlayerMP.class,
            getConnectionBounds(record, firstRoomId, secondRoomId)
        );
        return players.isEmpty() ? Collections.<EntityPlayerMP>emptyList() : new ArrayList<>(players);
    }

    public static void movePlayersToMainRoom(List<EntityPlayerMP> players, TardisRecord record) {
        BlockPos entry = InteriorAddress.getEntryPosition(requireMainCenter(record));
        for (EntityPlayerMP player : players) {
            player.setPositionAndUpdate(entry.getX() + 0.5D, entry.getY(), entry.getZ() + 0.5D);
            player.fallDistance = 0.0F;
        }
    }

    public static void removeRoomStructure(WorldServer world, TardisRecord record, TardisRoomRecord room) {
        BlockPos mainCenter = requireMainCenter(record);
        BlockPos childCenter = getRoomCenter(mainCenter, room.getGridX(), room.getGridZ());
        InteriorRoomSchema childSchema = requireSchema(room);
        BlockPos parentCenter = getRoomCenter(record, room.getParentRoomId());
        EnumFacing direction = room.getConnectionFacing();
        int parentExtent = getRoomExtent(record, room.getParentRoomId(), direction);
        int childExtent = childSchema.getExtent(direction.getOpposite());

        clearRoom(world, childCenter, childSchema);
        clearCorridor(world, parentCenter, childCenter, direction, parentExtent, childExtent);
        sealDoorway(
            world,
            parentCenter,
            direction,
            parentExtent,
            getRoomWall(record, room.getParentRoomId())
        );
    }

    public static void buildSecondaryConnection(
        WorldServer world,
        TardisRecord record,
        TardisRoomConnection connection
    ) {
        buildSecondaryConnection(
            world,
            record,
            connection.getFirstRoomId(),
            connection.getSecondRoomId()
        );
    }

    public static void buildSecondaryConnection(
        WorldServer world,
        TardisRecord record,
        int firstRoomId,
        int secondRoomId
    ) {
        EnumFacing direction = requireConnectionDirection(record, firstRoomId, secondRoomId);
        BlockPos firstCenter = getRoomCenter(record, firstRoomId);
        BlockPos secondCenter = getRoomCenter(record, secondRoomId);
        buildCorridor(
            world,
            firstCenter,
            secondCenter,
            direction,
            getRoomExtent(record, firstRoomId, direction),
            getRoomExtent(record, secondRoomId, direction.getOpposite())
        );
    }

    public static void removeSecondaryConnectionStructure(
        WorldServer world,
        TardisRecord record,
        int firstRoomId,
        int secondRoomId
    ) {
        EnumFacing direction = requireConnectionDirection(record, firstRoomId, secondRoomId);
        BlockPos firstCenter = getRoomCenter(record, firstRoomId);
        BlockPos secondCenter = getRoomCenter(record, secondRoomId);
        int firstExtent = getRoomExtent(record, firstRoomId, direction);
        int secondExtent = getRoomExtent(record, secondRoomId, direction.getOpposite());

        clearCorridor(world, firstCenter, secondCenter, direction, firstExtent, secondExtent);
        sealDoorway(world, firstCenter, direction, firstExtent, getRoomWall(record, firstRoomId));
        sealDoorway(
            world,
            secondCenter,
            direction.getOpposite(),
            secondExtent,
            getRoomWall(record, secondRoomId)
        );
    }

    public static List<String> getSchemaNames() {
        List<String> names = new ArrayList<>();
        for (InteriorRoomSchema schema : InteriorRoomSchema.valuesList()) {
            names.add(schema.getName());
        }
        return names;
    }

    private static void buildRoomAndConnector(WorldServer world, TardisRecord record, TardisRoomRecord room) {
        BlockPos mainCenter = requireMainCenter(record);
        BlockPos childCenter = getRoomCenter(mainCenter, room.getGridX(), room.getGridZ());
        InteriorRoomSchema childSchema = requireSchema(room);
        BlockPos parentCenter = getRoomCenter(record, room.getParentRoomId());
        EnumFacing direction = room.getConnectionFacing();
        int parentExtent = getRoomExtent(record, room.getParentRoomId(), direction);
        int childExtent = childSchema.getExtent(direction.getOpposite());

        buildRoom(world, childCenter, childSchema);
        buildCorridor(world, parentCenter, childCenter, direction, parentExtent, childExtent);
    }

    private static void buildRoom(WorldServer world, BlockPos center, InteriorRoomSchema schema) {
        int minX = center.getX() - schema.getHalfWidth();
        int maxX = center.getX() + schema.getHalfWidth();
        int minZ = center.getZ() - schema.getHalfDepth();
        int maxZ = center.getZ() + schema.getHalfDepth();
        int floorY = center.getY();
        int ceilingY = floorY + schema.getHeight();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlockState(new BlockPos(x, floorY, z), schema.getFloor(), 3);
                world.setBlockState(new BlockPos(x, ceilingY, z), schema.getCeiling(), 3);
                for (int y = floorY + 1; y < ceilingY; y++) {
                    boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                    world.setBlockState(new BlockPos(x, y, z), edge ? schema.getWall() : Blocks.AIR.getDefaultState(), 3);
                }
            }
        }

        world.setBlockState(new BlockPos(center.getX(), ceilingY, center.getZ()), schema.getLight(), 3);
        world.setBlockState(new BlockPos(minX + 1, ceilingY, minZ + 1), schema.getLight(), 3);
        world.setBlockState(new BlockPos(maxX - 1, ceilingY, minZ + 1), schema.getLight(), 3);
        world.setBlockState(new BlockPos(minX + 1, ceilingY, maxZ - 1), schema.getLight(), 3);
        world.setBlockState(new BlockPos(maxX - 1, ceilingY, maxZ - 1), schema.getLight(), 3);
    }

    private static void clearRoom(WorldServer world, BlockPos center, InteriorRoomSchema schema) {
        for (int x = center.getX() - schema.getHalfWidth(); x <= center.getX() + schema.getHalfWidth(); x++) {
            for (int z = center.getZ() - schema.getHalfDepth(); z <= center.getZ() + schema.getHalfDepth(); z++) {
                for (int y = center.getY(); y <= center.getY() + schema.getHeight(); y++) {
                    world.setBlockToAir(new BlockPos(x, y, z));
                }
            }
        }
    }

    private static void buildCorridor(
        WorldServer world,
        BlockPos firstCenter,
        BlockPos secondCenter,
        EnumFacing direction,
        int firstExtent,
        int secondExtent
    ) {
        BlockPos start = firstCenter.offset(direction, firstExtent + 1);
        BlockPos end = secondCenter.offset(direction.getOpposite(), secondExtent + 1);
        int length = horizontalDistance(start, end);
        EnumFacing lateral = direction.rotateY();
        IBlockState floor = Blocks.QUARTZ_BLOCK.getDefaultState();
        IBlockState wall = Blocks.QUARTZ_BLOCK.getDefaultState();
        IBlockState light = Blocks.SEA_LANTERN.getDefaultState();

        for (int step = 0; step <= length; step++) {
            BlockPos line = start.offset(direction, step);
            for (int side = -CORRIDOR_HALF_WIDTH; side <= CORRIDOR_HALF_WIDTH; side++) {
                BlockPos base = line.offset(lateral, side);
                world.setBlockState(base, floor, 3);
                world.setBlockState(base.up(CORRIDOR_HEIGHT), step % 5 == 2 && side == 0 ? light : wall, 3);
                for (int y = 1; y < CORRIDOR_HEIGHT; y++) {
                    boolean sideWall = Math.abs(side) == CORRIDOR_HALF_WIDTH;
                    world.setBlockState(base.up(y), sideWall ? wall : Blocks.AIR.getDefaultState(), 3);
                }
            }
        }

        carveDoorway(world, firstCenter.offset(direction, firstExtent), direction);
        carveDoorway(world, secondCenter.offset(direction.getOpposite(), secondExtent), direction);
    }

    private static void clearCorridor(
        WorldServer world,
        BlockPos firstCenter,
        BlockPos secondCenter,
        EnumFacing direction,
        int firstExtent,
        int secondExtent
    ) {
        BlockPos start = firstCenter.offset(direction, firstExtent + 1);
        BlockPos end = secondCenter.offset(direction.getOpposite(), secondExtent + 1);
        int length = horizontalDistance(start, end);
        EnumFacing lateral = direction.rotateY();

        for (int step = 0; step <= length; step++) {
            BlockPos line = start.offset(direction, step);
            for (int side = -CORRIDOR_HALF_WIDTH; side <= CORRIDOR_HALF_WIDTH; side++) {
                BlockPos base = line.offset(lateral, side);
                for (int y = 0; y <= CORRIDOR_HEIGHT; y++) {
                    world.setBlockToAir(base.up(y));
                }
            }
        }
    }

    private static void carveDoorway(WorldServer world, BlockPos wallCenter, EnumFacing direction) {
        EnumFacing lateral = direction.rotateY();
        for (int side = -1; side <= 1; side++) {
            for (int y = 1; y <= 3; y++) {
                world.setBlockToAir(wallCenter.offset(lateral, side).up(y));
            }
        }
    }

    private static void sealDoorway(
        WorldServer world,
        BlockPos roomCenter,
        EnumFacing direction,
        int extent,
        IBlockState wall
    ) {
        BlockPos wallCenter = roomCenter.offset(direction, extent);
        EnumFacing lateral = direction.rotateY();
        for (int side = -1; side <= 1; side++) {
            for (int y = 1; y <= 3; y++) {
                world.setBlockState(wallCenter.offset(lateral, side).up(y), wall, 3);
            }
        }
    }

    private static AxisAlignedBB getRemovalBounds(TardisRecord record, TardisRoomRecord room) {
        BlockPos mainCenter = requireMainCenter(record);
        BlockPos childCenter = getRoomCenter(mainCenter, room.getGridX(), room.getGridZ());
        InteriorRoomSchema schema = requireSchema(room);
        BlockPos parentCenter = getRoomCenter(record, room.getParentRoomId());
        EnumFacing direction = room.getConnectionFacing();
        int parentExtent = getRoomExtent(record, room.getParentRoomId(), direction);
        BlockPos corridorStart = parentCenter.offset(direction, parentExtent + 1);

        int minX = Math.min(corridorStart.getX() - CORRIDOR_HALF_WIDTH, childCenter.getX() - schema.getHalfWidth()) - 1;
        int maxX = Math.max(corridorStart.getX() + CORRIDOR_HALF_WIDTH, childCenter.getX() + schema.getHalfWidth()) + 2;
        int minZ = Math.min(corridorStart.getZ() - CORRIDOR_HALF_WIDTH, childCenter.getZ() - schema.getHalfDepth()) - 1;
        int maxZ = Math.max(corridorStart.getZ() + CORRIDOR_HALF_WIDTH, childCenter.getZ() + schema.getHalfDepth()) + 2;
        return new AxisAlignedBB(minX, mainCenter.getY(), minZ, maxX, mainCenter.getY() + schema.getHeight() + 2, maxZ);
    }

    private static AxisAlignedBB getConnectionBounds(
        TardisRecord record,
        int firstRoomId,
        int secondRoomId
    ) {
        EnumFacing direction = requireConnectionDirection(record, firstRoomId, secondRoomId);
        BlockPos firstCenter = getRoomCenter(record, firstRoomId);
        BlockPos secondCenter = getRoomCenter(record, secondRoomId);
        int firstExtent = getRoomExtent(record, firstRoomId, direction);
        int secondExtent = getRoomExtent(record, secondRoomId, direction.getOpposite());
        BlockPos start = firstCenter.offset(direction, firstExtent + 1);
        BlockPos end = secondCenter.offset(direction.getOpposite(), secondExtent + 1);
        int minX = Math.min(start.getX(), end.getX()) - CORRIDOR_HALF_WIDTH - 1;
        int maxX = Math.max(start.getX(), end.getX()) + CORRIDOR_HALF_WIDTH + 2;
        int minZ = Math.min(start.getZ(), end.getZ()) - CORRIDOR_HALF_WIDTH - 1;
        int maxZ = Math.max(start.getZ(), end.getZ()) + CORRIDOR_HALF_WIDTH + 2;
        return new AxisAlignedBB(
            minX,
            firstCenter.getY(),
            minZ,
            maxX,
            firstCenter.getY() + CORRIDOR_HEIGHT + 2,
            maxZ
        );
    }

    private static boolean roomExists(TardisRecord record, int roomId) {
        return roomId == MAIN_ROOM_ID || record.getRoom(roomId) != null;
    }

    private static int getRoomGridX(TardisRecord record, int roomId) {
        return roomId == MAIN_ROOM_ID ? 0 : requireRoom(record, roomId).getGridX();
    }

    private static int getRoomGridZ(TardisRecord record, int roomId) {
        return roomId == MAIN_ROOM_ID ? 0 : requireRoom(record, roomId).getGridZ();
    }

    private static int getRoomExtent(TardisRecord record, int roomId, EnumFacing direction) {
        if (roomId == MAIN_ROOM_ID) {
            return direction.getAxis() == EnumFacing.Axis.X ? MAIN_HALF_WIDTH : MAIN_HALF_DEPTH;
        }
        return requireSchema(requireRoom(record, roomId)).getExtent(direction);
    }

    private static IBlockState getRoomWall(TardisRecord record, int roomId) {
        return roomId == MAIN_ROOM_ID
            ? Blocks.QUARTZ_BLOCK.getDefaultState()
            : requireSchema(requireRoom(record, roomId)).getWall();
    }

    private static int horizontalDistance(BlockPos first, BlockPos second) {
        return Math.abs(first.getX() - second.getX()) + Math.abs(first.getZ() - second.getZ());
    }

    private static BlockPos requireMainCenter(TardisRecord record) {
        if (record.getInteriorPosition() == null) {
            throw new IllegalStateException("The TARDIS interior has not been assigned yet");
        }
        return record.getInteriorPosition().getPosition();
    }

    private static TardisRoomRecord requireRoom(TardisRecord record, int roomId) {
        TardisRoomRecord room = record.getRoom(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Unknown room ID " + roomId);
        }
        return room;
    }

    private static EnumFacing requireConnectionDirection(
        TardisRecord record,
        int firstRoomId,
        int secondRoomId
    ) {
        EnumFacing direction = getConnectionDirection(record, firstRoomId, secondRoomId);
        if (direction == null) {
            throw new IllegalArgumentException(
                "Rooms " + firstRoomId + " and " + secondRoomId + " are not adjacent"
            );
        }
        return direction;
    }

    private static InteriorRoomSchema requireSchema(TardisRoomRecord room) {
        InteriorRoomSchema schema = InteriorRoomSchema.fromName(room.getSchemaName());
        if (schema == null) {
            throw new IllegalStateException(
                "Room " + room.getRoomId() + " uses unknown schema '" + room.getSchemaName() + "'"
            );
        }
        return schema;
    }
}
