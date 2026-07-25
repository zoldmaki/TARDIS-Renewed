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
        event.getRegistry().register(ModBlocks.CHRONOSTEEL_BLOCK);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
            ModItems.CHRONOSTEEL_INGOT,
            ModItems.TARDIS_KEY
        );

        ItemBlock chronosteelBlockItem = new ItemBlock(ModBlocks.CHRONOSTEEL_BLOCK);
        chronosteelBlockItem.setRegistryName(ModBlocks.CHRONOSTEEL_BLOCK.getRegistryName());
        event.getRegistry().register(chronosteelBlockItem);
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(ModSounds.ALL);
    }
}
