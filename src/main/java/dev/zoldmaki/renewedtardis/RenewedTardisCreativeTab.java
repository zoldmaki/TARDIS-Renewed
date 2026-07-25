package dev.zoldmaki.renewedtardis;

import dev.zoldmaki.renewedtardis.init.ModItems;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public final class RenewedTardisCreativeTab extends CreativeTabs {
    public static final RenewedTardisCreativeTab INSTANCE = new RenewedTardisCreativeTab();

    private RenewedTardisCreativeTab() {
        super(RenewedTardisMod.MOD_ID);
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(ModItems.CHRONOSTEEL_INGOT);
    }
}
