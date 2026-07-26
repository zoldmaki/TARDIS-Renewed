package dev.zoldmaki.renewedtardis.teleport;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

/** Places an entity at an exact position without creating or searching portals. */
final class ExactTeleporter extends Teleporter {
    private final BlockPos target;
    private final float yaw;
    private final float pitch;

    ExactTeleporter(WorldServer world, BlockPos target, float yaw, float pitch) {
        super(world);
        this.target = target;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public void placeEntity(net.minecraft.world.World world, Entity entity, float suggestedYaw) {
        place(entity);
    }

    @Override
    public void placeInPortal(Entity entity, float rotationYaw) {
        place(entity);
    }

    @Override
    public boolean placeInExistingPortal(Entity entity, float rotationYaw) {
        place(entity);
        return true;
    }

    @Override
    public boolean makePortal(Entity entity) {
        return true;
    }

    private void place(Entity entity) {
        entity.motionX = 0.0D;
        entity.motionY = 0.0D;
        entity.motionZ = 0.0D;
        entity.fallDistance = 0.0F;
        entity.setLocationAndAngles(
            target.getX() + 0.5D,
            target.getY(),
            target.getZ() + 0.5D,
            yaw,
            pitch
        );
    }
}
