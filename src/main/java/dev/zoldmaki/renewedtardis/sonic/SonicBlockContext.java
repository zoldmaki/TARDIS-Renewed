package dev.zoldmaki.renewedtardis.sonic;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

/** Immutable server-side context supplied to sonic block actions. */
public final class SonicBlockContext {
    private final WorldServer world;
    private final EntityPlayerMP player;
    private final ItemStack stack;
    private final SonicMode mode;
    private final BlockPos position;
    private final EnumFacing side;
    private final EnumHand hand;
    private final IBlockState state;

    public SonicBlockContext(
        WorldServer world,
        EntityPlayerMP player,
        ItemStack stack,
        SonicMode mode,
        BlockPos position,
        EnumFacing side,
        EnumHand hand
    ) {
        this.world = world;
        this.player = player;
        this.stack = stack;
        this.mode = mode;
        this.position = position.toImmutable();
        this.side = side;
        this.hand = hand;
        this.state = world.getBlockState(position);
    }

    public WorldServer getWorld() {
        return world;
    }

    public EntityPlayerMP getPlayer() {
        return player;
    }

    public ItemStack getStack() {
        return stack;
    }

    public SonicMode getMode() {
        return mode;
    }

    public BlockPos getPosition() {
        return position;
    }

    public EnumFacing getSide() {
        return side;
    }

    public EnumHand getHand() {
        return hand;
    }

    public IBlockState getState() {
        return state;
    }
}
