package dev.zoldmaki.renewedtardis.item;

import dev.zoldmaki.renewedtardis.RenewedTardisCreativeTab;
import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

/** Lightweight in-game reference manual; a full illustrated GUI can replace it later. */
public final class ItemTardisManual extends Item {
    private static final String TAG_PAGE = "Page";

    private static final String[] TITLES = {
        "Getting Started",
        "Keys and Ownership",
        "Flight",
        "Artron Energy and Damage",
        "Rooms and Corridors",
        "Crew Access",
        "Sonic Screwdriver",
        "Transmat and Gravity Lift",
        "Optional Integrations",
        "Diagnostics and Recovery"
    };

    private static final String[][] PAGES = {
        {
            "Create or bind a TARDIS with an unbound key, then place its exterior with /tr exterior during development.",
            "The exterior, interior, owner, shell, destination, and systems are stored persistently."
        },
        {
            "A bound key controls only its linked TARDIS and immediately respects crew permission changes.",
            "Operators can recover ownership with /tr transfer and inspect another record with /tr as <owner>."
        },
        {
            "Set a destination, take off, wait for the timer, then land. Speed, accuracy, damage, and upgrades affect travel.",
            "Forced landing is an operator recovery tool and may delete obstructing blocks."
        },
        {
            "Flights, rooms, upgrades, repairs, and transmats use Artron energy. The engine core reports charge and systems.",
            "Chronosteel repairs the most damaged subsystem when used on the engine core."
        },
        {
            "Expandable rooms occupy a stable grid. Add rooms from a parent, then connect adjacent rooms with /tr room connect.",
            "Secondary corridors can be removed without changing room IDs or the original parent tree."
        },
        {
            "Door, key, console, and maintenance rights are granted separately through crew roles or individual permissions.",
            "The interior exit remains usable so visitors cannot be trapped."
        },
        {
            "Sneak-right-click air to cycle sonic modes. Link first, then locate, reconfigure, scan, transmat, or recall.",
            "A revoked crew member's linked sonic stops working immediately."
        },
        {
            "Use Transmat mode within 250 blocks of the landed exterior, or activate the interior transmat pad.",
            "Place gravity-lift levels on the same X/Z with a clear shaft. Jump on an up level or hold sneak on a down level; right-click reverses it."
        },
        {
            "HWYLA/Waila can show live TARDIS component data. ComputerCraft can read TARDIS state from a modem beside the engine core.",
            "Both hooks are optional, configurable, and visible through /tr integrations. Computer access is read-only in this phase."
        },
        {
            "Use /tr doctor for a safe record check. Operators can use /tr doctor deep to verify the saved exterior blocks and tile binding.",
            "Operators can repair core interior components or the saved exterior with /tr repair. The force option is destructive and should be a last resort."
        }
    };

    public ItemTardisManual() {
        setRegistryName(RenewedTardisMod.MOD_ID, "tardis_manual");
        setTranslationKey(RenewedTardisMod.MOD_ID + ".tardis_manual");
        setCreativeTab(RenewedTardisCreativeTab.INSTANCE);
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        int page = getPage(stack);
        showPage(player, page);
        int direction = player.isSneaking() ? -1 : 1;
        setPage(stack, Math.floorMod(page + direction, TITLES.length));
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void addInformation(
        ItemStack stack,
        @Nullable World world,
        List<String> tooltip,
        ITooltipFlag flag
    ) {
        int page = getPage(stack);
        tooltip.add(TextFormatting.AQUA + "Next page: " + TITLES[page]);
        tooltip.add(TextFormatting.DARK_GRAY + "Right-click to read and advance");
        tooltip.add(TextFormatting.DARK_GRAY + "Sneak-right-click to read and go back");
    }

    private static void showPage(EntityPlayer player, int page) {
        player.sendStatusMessage(
            new TextComponentString(
                TextFormatting.GOLD + "[TARDIS MANUAL " + (page + 1) + "/" + TITLES.length + "] "
                    + TextFormatting.AQUA + TITLES[page]
            ),
            false
        );
        for (String line : PAGES[page]) {
            player.sendMessage(new TextComponentString(TextFormatting.GRAY + line));
        }
    }

    private static int getPage(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_PAGE, Constants.NBT.TAG_INT)) {
            return 0;
        }
        return Math.floorMod(tag.getInteger(TAG_PAGE), TITLES.length);
    }

    private static void setPage(ItemStack stack, int page) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setInteger(TAG_PAGE, Math.floorMod(page, TITLES.length));
    }
}
