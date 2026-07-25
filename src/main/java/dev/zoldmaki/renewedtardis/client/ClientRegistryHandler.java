package dev.zoldmaki.renewedtardis.client;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import dev.zoldmaki.renewedtardis.init.ModItems;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = RenewedTardisMod.MOD_ID, value = Side.CLIENT)
public final class ClientRegistryHandler {
    private ClientRegistryHandler() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(
            ModItems.CHRONOSTEEL_INGOT,
            0,
            new ModelResourceLocation(ModItems.CHRONOSTEEL_INGOT.getRegistryName(), "inventory")
        );

        ModelLoader.setCustomModelResourceLocation(
            ModItems.TARDIS_KEY,
            0,
            new ModelResourceLocation(ModItems.TARDIS_KEY.getRegistryName(), "inventory")
        );

        Item blockItem = Item.getItemFromBlock(ModBlocks.CHRONOSTEEL_BLOCK);
        ModelLoader.setCustomModelResourceLocation(
            blockItem,
            0,
            new ModelResourceLocation(ModBlocks.CHRONOSTEEL_BLOCK.getRegistryName(), "inventory")
        );
    }
}
