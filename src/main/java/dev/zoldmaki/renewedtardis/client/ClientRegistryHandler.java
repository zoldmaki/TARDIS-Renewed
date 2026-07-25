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
        registerItemModel(ModItems.CHRONOSTEEL_INGOT);
        registerItemModel(ModItems.TARDIS_KEY);
        registerItemModel(Item.getItemFromBlock(ModBlocks.CHRONOSTEEL_BLOCK));
        registerItemModel(Item.getItemFromBlock(ModBlocks.TARDIS_EXTERIOR));
    }

    private static void registerItemModel(Item item) {
        ModelLoader.setCustomModelResourceLocation(
            item,
            0,
            new ModelResourceLocation(item.getRegistryName(), "inventory")
        );
    }
}
