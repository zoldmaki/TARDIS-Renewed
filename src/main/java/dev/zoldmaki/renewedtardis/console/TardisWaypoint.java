package dev.zoldmaki.renewedtardis.console;

import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants;

/** Immutable absolute destination package stored in one physical waypoint slot. */
public final class TardisWaypoint {
    private static final String TAG_POSITION = "Position";
    private static final String TAG_FACING = "Facing";
    private static final String TAG_LANDING_MODE = "LandingMode";
    private static final String TAG_LANDING_PAD = "LandingPad";

    private final DimensionPosition position;
    private final EnumFacing facing;
    private final int landingMode;
    private final boolean landingPad;

    public TardisWaypoint(
        DimensionPosition position,
        EnumFacing facing,
        int landingMode,
        boolean landingPad
    ) {
        if (position == null) {
            throw new IllegalArgumentException("Waypoint position cannot be null");
        }
        this.position = position;
        this.facing = normalizeHorizontal(facing);
        this.landingMode = clampLandingMode(landingMode);
        this.landingPad = landingPad;
    }

    public DimensionPosition getPosition() {
        return position;
    }

    public EnumFacing getFacing() {
        return facing;
    }

    public int getLandingMode() {
        return landingMode;
    }

    public boolean usesLandingPad() {
        return landingPad;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(TAG_POSITION, position.writeToNBT());
        tag.setInteger(TAG_FACING, facing.getHorizontalIndex());
        tag.setInteger(TAG_LANDING_MODE, landingMode);
        tag.setBoolean(TAG_LANDING_PAD, landingPad);
        return tag;
    }

    @Nullable
    public static TardisWaypoint readFromNBT(NBTTagCompound tag) {
        if (tag == null || !tag.hasKey(TAG_POSITION, Constants.NBT.TAG_COMPOUND)) {
            return null;
        }
        DimensionPosition position = DimensionPosition.readFromNBT(tag.getCompoundTag(TAG_POSITION));
        if (position == null) {
            return null;
        }
        EnumFacing facing = tag.hasKey(TAG_FACING, Constants.NBT.TAG_INT)
            ? EnumFacing.byHorizontalIndex(tag.getInteger(TAG_FACING))
            : EnumFacing.NORTH;
        int landingMode = tag.hasKey(TAG_LANDING_MODE, Constants.NBT.TAG_INT)
            ? tag.getInteger(TAG_LANDING_MODE)
            : 0;
        boolean landingPad = tag.getBoolean(TAG_LANDING_PAD);
        return new TardisWaypoint(position, facing, landingMode, landingPad);
    }

    private static EnumFacing normalizeHorizontal(EnumFacing facing) {
        return facing != null && facing.getAxis().isHorizontal() ? facing : EnumFacing.NORTH;
    }

    private static int clampLandingMode(int mode) {
        return Math.max(0, Math.min(2, mode));
    }
}
