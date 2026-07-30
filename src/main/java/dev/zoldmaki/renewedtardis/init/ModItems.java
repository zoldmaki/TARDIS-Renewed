package dev.zoldmaki.renewedtardis.init;

import dev.zoldmaki.renewedtardis.item.ItemChronosteelIngot;
import dev.zoldmaki.renewedtardis.item.ItemTardisKey;
import dev.zoldmaki.renewedtardis.item.ItemTardisManual;
import dev.zoldmaki.renewedtardis.item.ItemSonicScrewdriver;
import dev.zoldmaki.renewedtardis.item.ItemTardisUpgrade;
import dev.zoldmaki.renewedtardis.upgrade.TardisUpgradeType;
import javax.annotation.Nullable;

public final class ModItems {
    public static final ItemChronosteelIngot CHRONOSTEEL_INGOT = new ItemChronosteelIngot();
    public static final ItemTardisKey TARDIS_KEY = new ItemTardisKey();
    public static final ItemSonicScrewdriver SONIC_SCREWDRIVER = new ItemSonicScrewdriver();
    public static final ItemTardisManual TARDIS_MANUAL = new ItemTardisManual();

    public static final ItemTardisUpgrade SPEED_UPGRADE = new ItemTardisUpgrade(TardisUpgradeType.SPEED);
    public static final ItemTardisUpgrade ARTRON_CAPACITY_UPGRADE = new ItemTardisUpgrade(TardisUpgradeType.CAPACITY);
    public static final ItemTardisUpgrade ARTRON_REGENERATION_UPGRADE = new ItemTardisUpgrade(TardisUpgradeType.REGENERATION);
    public static final ItemTardisUpgrade ROOM_UPGRADE = new ItemTardisUpgrade(TardisUpgradeType.ROOM);
    public static final ItemTardisUpgrade PROTECTION_UPGRADE = new ItemTardisUpgrade(TardisUpgradeType.PROTECTION);

    private ModItems() {
    }

    @Nullable
    public static ItemTardisUpgrade getUpgradeItem(TardisUpgradeType type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case SPEED:
                return SPEED_UPGRADE;
            case CAPACITY:
                return ARTRON_CAPACITY_UPGRADE;
            case REGENERATION:
                return ARTRON_REGENERATION_UPGRADE;
            case ROOM:
                return ROOM_UPGRADE;
            case PROTECTION:
                return PROTECTION_UPGRADE;
            default:
                return null;
        }
    }
}
