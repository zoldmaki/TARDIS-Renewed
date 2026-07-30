package dev.zoldmaki.renewedtardis.item;

import dev.zoldmaki.renewedtardis.RenewedTardisCreativeTab;
import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.config.TardisConfig;
import dev.zoldmaki.renewedtardis.upgrade.TardisUpgradeType;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/** Craftable component installed by right-clicking the linked engine core. */
public final class ItemTardisUpgrade extends Item {
    private final TardisUpgradeType type;

    public ItemTardisUpgrade(TardisUpgradeType type) {
        this.type = type;
        setRegistryName(RenewedTardisMod.MOD_ID, type.getItemName());
        setTranslationKey(RenewedTardisMod.MOD_ID + "." + type.getItemName());
        setCreativeTab(RenewedTardisCreativeTab.INSTANCE);
        setMaxStackSize(16);
    }

    public TardisUpgradeType getUpgradeType() {
        return type;
    }

    @Override
    public void addInformation(
        ItemStack stack,
        @Nullable World world,
        List<String> tooltip,
        ITooltipFlag flag
    ) {
        tooltip.add(TextFormatting.GRAY + effectDescription());
        tooltip.add(TextFormatting.DARK_GRAY + "Install at the TARDIS engine core");
        tooltip.add(TextFormatting.DARK_GRAY + "Per-type limit: " + maximumCount());
    }

    private int maximumCount() {
        switch (type) {
            case SPEED:
                return TardisConfig.getMaximumSpeedUpgrades();
            case CAPACITY:
                return TardisConfig.getMaximumCapacityUpgrades();
            case REGENERATION:
                return TardisConfig.getMaximumRegenerationUpgrades();
            case ROOM:
                return TardisConfig.getMaximumRoomUpgrades();
            case PROTECTION:
                return TardisConfig.getMaximumProtectionUpgrades();
            default:
                return 0;
        }
    }

    private String effectDescription() {
        switch (type) {
            case SPEED:
                return "Reduces flight time by " + TardisConfig.getSpeedDurationReductionPercentPerUpgrade() + "%";
            case CAPACITY:
                return "Adds " + TardisConfig.getCapacityBonusPerUpgrade() + " AU capacity";
            case REGENERATION:
                return "Adds " + TardisConfig.getRegenerationBonusPerUpgrade() + " AU/s regeneration";
            case ROOM:
                return "Adds " + TardisConfig.getRoomLimitBonusPerUpgrade() + " room slots and reduces room cost";
            case PROTECTION:
                return "Reduces flight wear by " + TardisConfig.getProtectionWearReductionPercentPerUpgrade() + "%";
            default:
                return "TARDIS upgrade component";
        }
    }
}
