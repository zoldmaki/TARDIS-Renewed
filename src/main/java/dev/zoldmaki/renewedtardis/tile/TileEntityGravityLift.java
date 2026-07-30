package dev.zoldmaki.renewedtardis.tile;

import dev.zoldmaki.renewedtardis.block.BlockGravityLift;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/** Server-authoritative controller for one paired gravity-lift level. */
public final class TileEntityGravityLift extends TileEntity implements ITickable {
    private static final int MAX_DISTANCE = 32;
    private static final double JUMP_TRIGGER_SPEED = 0.08D;
    private static final String COOLDOWN_TAG = "renewedtardisGravityLiftCooldown";

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }

        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != ModBlocks.GRAVITY_LIFT) {
            return;
        }

        boolean upwards = state.getValue(BlockGravityLift.UP);
        BlockPos targetPad = findTargetPad(upwards);
        if (targetPad == null || !hasArrivalSpace(targetPad)) {
            return;
        }

        transportTriggeredEntities(targetPad, upwards);
    }

    /**
     * Kept for compatibility with BlockGravityLift. Trips are now completed in
     * one server-authoritative phase jump, so there is no local active list to
     * cancel or collision state to restore.
     */
    public void clearActiveEntities() {
        // No persistent trip state is used by the deterministic transport path.
    }

    private void transportTriggeredEntities(BlockPos targetPad, boolean upwards) {
        AxisAlignedBB activation = new AxisAlignedBB(
            pos.getX() + 0.08D,
            pos.getY() + 0.94D,
            pos.getZ() + 0.08D,
            pos.getX() + 0.92D,
            pos.getY() + 2.35D,
            pos.getZ() + 0.92D
        );
        List<EntityLivingBase> candidates = world.getEntitiesWithinAABB(EntityLivingBase.class, activation);
        long now = world.getTotalWorldTime();

        for (EntityLivingBase entity : candidates) {
            if (entity.isDead || isFlyingPlayer(entity)) {
                continue;
            }
            if (entity.getEntityData().getLong(COOLDOWN_TAG) > now) {
                continue;
            }
            if (!shouldActivate(entity, upwards)) {
                continue;
            }

            phaseToDestination(entity, targetPad);
            entity.getEntityData().setLong(COOLDOWN_TAG, now + 20L);
        }
    }

    /**
     * Players deliberately trigger the configured direction: jump for an up
     * level and sneak for a down level. Non-player living entities continue to
     * activate automatically.
     */
    private static boolean shouldActivate(EntityLivingBase entity, boolean upwards) {
        if (!(entity instanceof EntityPlayer)) {
            return true;
        }
        EntityPlayer player = (EntityPlayer) entity;
        return upwards ? player.motionY > JUMP_TRIGGER_SPEED : player.isSneaking();
    }

    private BlockPos findTargetPad(boolean upwards) {
        int step = upwards ? 1 : -1;
        for (int distance = 2; distance <= MAX_DISTANCE; distance++) {
            BlockPos scan = pos.add(0, distance * step, 0);
            if (world.getBlockState(scan).getBlock() == ModBlocks.GRAVITY_LIFT) {
                return scan;
            }
            if (!world.isAirBlock(scan)) {
                return null;
            }
        }
        return null;
    }

    /** The two blocks above the destination level must be clear for arrival. */
    private boolean hasArrivalSpace(BlockPos targetPad) {
        return world.isAirBlock(targetPad.up()) && world.isAirBlock(targetPad.up(2));
    }

    /**
     * Vanilla client collision prediction does not synchronise Entity#noClip
     * reliably for ordinary players. Incremental movement therefore left the
     * client embedded in the solid level block. Moving directly to the safe
     * standing position above the paired pad bypasses both level collisions
     * and is confirmed to the client by setPositionAndUpdate.
     */
    private static void phaseToDestination(EntityLivingBase entity, BlockPos targetPad) {
        double x = targetPad.getX() + 0.5D;
        double y = targetPad.getY() + 1.02D;
        double z = targetPad.getZ() + 0.5D;

        entity.noClip = false;
        if (entity instanceof EntityPlayerMP) {
            ((EntityPlayerMP) entity).setPositionAndUpdate(x, y, z);
        } else {
            entity.setPosition(x, y, z);
        }

        entity.motionX = 0.0D;
        entity.motionY = 0.0D;
        entity.motionZ = 0.0D;
        entity.fallDistance = 0.0F;
        entity.velocityChanged = true;
    }

    private static boolean isFlyingPlayer(EntityLivingBase entity) {
        return entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.isFlying;
    }
}
