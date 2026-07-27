package dev.zoldmaki.renewedtardis.network;

import dev.zoldmaki.renewedtardis.init.ModBlocks;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Requests activation of one of the mod's travel blocks. The server validates
 * the position, block type, distance, and player state before invoking it.
 */
public final class MessageActivateTardisBlock implements IMessage {
    private BlockPos pos;
    private int handIndex;
    private int faceIndex;
    private float hitX;
    private float hitY;
    private float hitZ;

    public MessageActivateTardisBlock() {
    }

    public MessageActivateTardisBlock(BlockPos pos, EnumHand hand, EnumFacing face, Vec3d hitVec) {
        this.pos = pos;
        this.handIndex = hand.ordinal();
        this.faceIndex = face == null ? EnumFacing.UP.getIndex() : face.getIndex();
        this.hitX = relativeHit(hitVec == null ? 0.5D : hitVec.x - pos.getX());
        this.hitY = relativeHit(hitVec == null ? 0.5D : hitVec.y - pos.getY());
        this.hitZ = relativeHit(hitVec == null ? 0.5D : hitVec.z - pos.getZ());
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        pos = BlockPos.fromLong(buffer.readLong());
        handIndex = buffer.readUnsignedByte();
        faceIndex = buffer.readUnsignedByte();
        hitX = sanitizeHit(buffer.readFloat());
        hitY = sanitizeHit(buffer.readFloat());
        hitZ = sanitizeHit(buffer.readFloat());
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(pos.toLong());
        buffer.writeByte(handIndex);
        buffer.writeByte(faceIndex);
        buffer.writeFloat(hitX);
        buffer.writeFloat(hitY);
        buffer.writeFloat(hitZ);
    }

    private void handle(EntityPlayerMP player) {
        if (player == null || !player.isEntityAlive() || player.isSpectator() || pos == null) {
            return;
        }

        WorldServer world = player.getServerWorld();
        if (!world.isBlockLoaded(pos)) {
            return;
        }

        double distanceSq = player.getDistanceSq(
            pos.getX() + 0.5D,
            pos.getY() + 0.5D,
            pos.getZ() + 0.5D
        );
        if (distanceSq > 36.0D) {
            return;
        }

        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!ModBlocks.isTardisInteractionBlock(block)) {
            return;
        }

        EnumHand hand = handIndex == EnumHand.OFF_HAND.ordinal()
            ? EnumHand.OFF_HAND
            : EnumHand.MAIN_HAND;
        EnumFacing face = faceIndex >= 0 && faceIndex < EnumFacing.values().length
            ? EnumFacing.byIndex(faceIndex)
            : EnumFacing.UP;
        Vec3d absoluteHit = new Vec3d(
            pos.getX() + hitX,
            pos.getY() + hitY,
            pos.getZ() + hitZ
        );

        PlayerInteractEvent.RightClickBlock event = ForgeHooks.onRightClickBlock(
            player,
            hand,
            pos,
            face,
            absoluteHit
        );
        if (event.isCanceled() || event.getUseBlock() == Event.Result.DENY) {
            return;
        }

        block.onBlockActivated(world, pos, state, player, hand, face, hitX, hitY, hitZ);
    }

    private static float relativeHit(double value) {
        return sanitizeHit((float) value);
    }

    private static float sanitizeHit(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0.5F;
        }
        if (value < 0.0F) {
            return 0.0F;
        }
        if (value > 1.0F) {
            return 1.0F;
        }
        return value;
    }

    public static final class Handler implements IMessageHandler<MessageActivateTardisBlock, IMessage> {
        @Override
        public IMessage onMessage(MessageActivateTardisBlock message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> message.handle(player));
            return null;
        }
    }
}
