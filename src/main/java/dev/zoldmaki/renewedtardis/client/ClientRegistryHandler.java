package dev.zoldmaki.renewedtardis.client;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import dev.zoldmaki.renewedtardis.init.ModItems;
import dev.zoldmaki.renewedtardis.config.TardisConfig;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
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
        registerItemModel(ModItems.SONIC_SCREWDRIVER);
        registerItemModel(ModItems.TARDIS_MANUAL);
        registerItemModel(ModItems.SPEED_UPGRADE);
        registerItemModel(ModItems.ARTRON_CAPACITY_UPGRADE);
        registerItemModel(ModItems.ARTRON_REGENERATION_UPGRADE);
        registerItemModel(ModItems.ROOM_UPGRADE);
        registerItemModel(ModItems.PROTECTION_UPGRADE);
        registerItemModel(Item.getItemFromBlock(ModBlocks.CHRONOSTEEL_BLOCK));
        registerItemModel(Item.getItemFromBlock(ModBlocks.TARDIS_EXTERIOR));
        registerItemModel(Item.getItemFromBlock(ModBlocks.TARDIS_ENGINE_CORE));
        registerItemModel(Item.getItemFromBlock(ModBlocks.TARDIS_TRANSMAT_PAD));
        registerItemModel(Item.getItemFromBlock(ModBlocks.GRAVITY_LIFT));
        registerItemModel(Item.getItemFromBlock(ModBlocks.TARDIS_FLIGHT_CONSOLE));
        registerItemModel(Item.getItemFromBlock(ModBlocks.TARDIS_TAKEOFF_CONTROL));
        registerItemModel(Item.getItemFromBlock(ModBlocks.TARDIS_LANDING_CONTROL));
        registerItemModel(Item.getItemFromBlock(ModBlocks.TARDIS_DESTINATION_CONTROL));
        registerItemModel(Item.getItemFromBlock(ModBlocks.TARDIS_FACING_CONTROL));
        registerItemModel(Item.getItemFromBlock(ModBlocks.TARDIS_STATUS_CONTROL));
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (RenewedTardisMod.MOD_ID.equals(event.getModID())) {
            TardisConfig.reload();
        }
    }

    private static void registerItemModel(Item item) {
        ModelLoader.setCustomModelResourceLocation(
            item,
            0,
            new ModelResourceLocation(item.getRegistryName(), "inventory")
        );
    }
}
