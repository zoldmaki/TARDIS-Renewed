package dev.zoldmaki.renewedtardis.item;

import dev.zoldmaki.renewedtardis.RenewedTardisCreativeTab;
import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import net.minecraft.item.Item;

/**
 * Standalone 1.12.2 form of the Chronosteel metadata item from 0.995-350.
 */
public final class ItemChronosteelIngot extends Item {
    public ItemChronosteelIngot() {
        setRegistryName(RenewedTardisMod.MOD_ID, "chronosteel_ingot");
        setTranslationKey(RenewedTardisMod.MOD_ID + ".chronosteel_ingot");
        setCreativeTab(RenewedTardisCreativeTab.INSTANCE);
    }
}
