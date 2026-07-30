package dev.zoldmaki.renewedtardis.sonic;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/** Immutable server-side context supplied to sonic entity actions. */
public final class SonicEntityContext {
    private final EntityPlayerMP player;
    private final ItemStack stack;
    private final SonicMode mode;
    private final Entity target;

    public SonicEntityContext(EntityPlayerMP player, ItemStack stack, SonicMode mode, Entity target) {
        this.player = player;
        this.stack = stack;
        this.mode = mode;
        this.target = target;
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

    public Entity getTarget() {
        return target;
    }
}
