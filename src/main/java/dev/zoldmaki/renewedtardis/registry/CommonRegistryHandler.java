package dev.zoldmaki.renewedtardis.registry;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import dev.zoldmaki.renewedtardis.init.ModItems;
import dev.zoldmaki.renewedtardis.init.ModSounds;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = RenewedTardisMod.MOD_ID)
public final class CommonRegistryHandler {
    private CommonRegistryHandler() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(
            ModBlocks.CHRONOSTEEL_BLOCK,
            ModBlocks.TARDIS_EXTERIOR,
            ModBlocks.TARDIS_EXTERIOR_TOP,
            ModBlocks.TARDIS_INTERIOR_DOOR,
            ModBlocks.TARDIS_ENGINE_CORE,
            ModBlocks.TARDIS_TRANSMAT_PAD,
            ModBlocks.GRAVITY_LIFT,
            ModBlocks.TARDIS_FLIGHT_CONSOLE,
            ModBlocks.TARDIS_TAKEOFF_CONTROL,
            ModBlocks.TARDIS_LANDING_CONTROL,
            ModBlocks.TARDIS_DESTINATION_CONTROL,
            ModBlocks.TARDIS_FACING_CONTROL,
            ModBlocks.TARDIS_STATUS_CONTROL
        );
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
            ModItems.CHRONOSTEEL_INGOT,
            ModItems.TARDIS_KEY,
            ModItems.SONIC_SCREWDRIVER,
            ModItems.TARDIS_MANUAL,
            ModItems.SPEED_UPGRADE,
            ModItems.ARTRON_CAPACITY_UPGRADE,
            ModItems.ARTRON_REGENERATION_UPGRADE,
            ModItems.ROOM_UPGRADE,
            ModItems.PROTECTION_UPGRADE,
            createItemBlock(ModBlocks.CHRONOSTEEL_BLOCK),
            createItemBlock(ModBlocks.TARDIS_EXTERIOR),
            createItemBlock(ModBlocks.TARDIS_ENGINE_CORE),
            createItemBlock(ModBlocks.TARDIS_TRANSMAT_PAD),
            createItemBlock(ModBlocks.GRAVITY_LIFT),
            createItemBlock(ModBlocks.TARDIS_FLIGHT_CONSOLE),
            createItemBlock(ModBlocks.TARDIS_TAKEOFF_CONTROL),
            createItemBlock(ModBlocks.TARDIS_LANDING_CONTROL),
            createItemBlock(ModBlocks.TARDIS_DESTINATION_CONTROL),
            createItemBlock(ModBlocks.TARDIS_FACING_CONTROL),
            createItemBlock(ModBlocks.TARDIS_STATUS_CONTROL)
        );
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(ModSounds.ALL);
    }

    private static ItemBlock createItemBlock(Block block) {
        ItemBlock itemBlock = new ItemBlock(block);
        itemBlock.setRegistryName(block.getRegistryName());
        return itemBlock;
    }
}
