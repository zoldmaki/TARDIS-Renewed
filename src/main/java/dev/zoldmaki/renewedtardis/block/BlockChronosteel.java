package dev.zoldmaki.renewedtardis.block;

import dev.zoldmaki.renewedtardis.RenewedTardisCreativeTab;
import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

/**
 * The first legacy block ported from TARDIS Mod 0.995-350.
 *
 * The old CompressedBlock stored Dalekanium and Chronosteel as metadata
 * variants. The 1.12.2 port uses separate registry entries so future
 * versions are easier to maintain.
 */
public final class BlockChronosteel extends Block {
    public BlockChronosteel() {
        super(Material.IRON);
        setRegistryName(RenewedTardisMod.MOD_ID, "chronosteel_block");
        setTranslationKey(RenewedTardisMod.MOD_ID + ".chronosteel_block");
        setCreativeTab(RenewedTardisCreativeTab.INSTANCE);
        setHardness(5.0F);
        setResistance(10.0F);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 2);
    }
}
