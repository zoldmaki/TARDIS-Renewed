package dev.zoldmaki.renewedtardis.network;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.util.EnumActionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Sends TARDIS block interactions over the mod channel instead of relying on
 * vanilla's protected-block path. Only this mod's TARDIS interaction blocks are caught.
 */
@Mod.EventBusSubscriber(modid = RenewedTardisMod.MOD_ID, value = Side.CLIENT)
public final class ClientTardisInteractionHandler {
    private ClientTardisInteractionHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getWorld().isRemote) {
            return;
        }

        Block block = event.getWorld().getBlockState(event.getPos()).getBlock();
        if (!ModBlocks.isTardisInteractionBlock(block)) {
            return;
        }

        ModNetwork.CHANNEL.sendToServer(new MessageActivateTardisBlock(
            event.getPos(),
            event.getHand(),
            event.getFace(),
            event.getHitVec()
        ));

        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setCanceled(true);
    }
}
