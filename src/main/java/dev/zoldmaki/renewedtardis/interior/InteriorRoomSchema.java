package dev.zoldmaki.renewedtardis.interior;

import dev.zoldmaki.renewedtardis.config.TardisConfig;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

/** Small built-in schema registry used by the first expandable-room system. */
public enum InteriorRoomSchema {
    BASIC(
        "basic",
        "Basic Room",
        4,
        4,
        5,
        Blocks.QUARTZ_BLOCK.getDefaultState(),
        Blocks.QUARTZ_BLOCK.getDefaultState(),
        Blocks.QUARTZ_BLOCK.getDefaultState(),
        Blocks.SEA_LANTERN.getDefaultState()
    ),
    WORKSHOP(
        "workshop",
        "Workshop",
        5,
        4,
        5,
        Blocks.IRON_BLOCK.getDefaultState(),
        Blocks.STONEBRICK.getDefaultState(),
        Blocks.STONEBRICK.getDefaultState(),
        Blocks.GLOWSTONE.getDefaultState()
    ),
    STORAGE(
        "storage",
        "Storage Room",
        4,
        5,
        5,
        Blocks.QUARTZ_BLOCK.getDefaultState(),
        Blocks.IRON_BLOCK.getDefaultState(),
        Blocks.IRON_BLOCK.getDefaultState(),
        Blocks.SEA_LANTERN.getDefaultState()
    );

    private static final List<InteriorRoomSchema> VALUES = Collections.unmodifiableList(Arrays.asList(values()));

    private final String name;
    private final String displayName;
    private final int halfWidth;
    private final int halfDepth;
    private final int height;
    private final IBlockState floor;
    private final IBlockState wall;
    private final IBlockState ceiling;
    private final IBlockState light;

    InteriorRoomSchema(
        String name,
        String displayName,
        int halfWidth,
        int halfDepth,
        int height,
        IBlockState floor,
        IBlockState wall,
        IBlockState ceiling,
        IBlockState light
    ) {
        this.name = name;
        this.displayName = displayName;
        this.halfWidth = halfWidth;
        this.halfDepth = halfDepth;
        this.height = height;
        this.floor = floor;
        this.wall = wall;
        this.ceiling = ceiling;
        this.light = light;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getHalfWidth() {
        return halfWidth;
    }

    public int getHalfDepth() {
        return halfDepth;
    }

    public int getHeight() {
        return height;
    }

    public IBlockState getFloor() {
        return floor;
    }

    public IBlockState getWall() {
        return wall;
    }

    public IBlockState getCeiling() {
        return ceiling;
    }

    public IBlockState getLight() {
        return light;
    }

    public int getEnergyCost() {
        switch (this) {
            case BASIC:
                return TardisConfig.getBasicRoomEnergyCost();
            case WORKSHOP:
                return TardisConfig.getWorkshopRoomEnergyCost();
            case STORAGE:
                return TardisConfig.getStorageRoomEnergyCost();
            default:
                return 0;
        }
    }

    public int getExtent(net.minecraft.util.EnumFacing direction) {
        return direction.getAxis() == net.minecraft.util.EnumFacing.Axis.X ? halfWidth : halfDepth;
    }

    public static List<InteriorRoomSchema> valuesList() {
        return VALUES;
    }

    @Nullable
    public static InteriorRoomSchema fromName(String name) {
        if (name == null) {
            return null;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (InteriorRoomSchema schema : values()) {
            if (schema.name.equals(normalized)) {
                return schema;
            }
        }
        return null;
    }
}
