package dev.zoldmaki.renewedtardis.item;

import dev.zoldmaki.renewedtardis.RenewedTardisCreativeTab;
import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
import net.minecraftforge.common.util.FakePlayer;

/**
 * First functional port of the original TARDIS Key.
 *
 * In Phase 2 the key creates or locates the owner's persistent TARDIS record
 * and binds itself to that record. Exterior summoning and flight controls are
 * intentionally deferred until those systems exist.
 */
public final class ItemTardisKey extends Item {
    private static final String TAG_ROOT = RenewedTardisMod.MOD_ID;
    private static final String TAG_TARDIS_ID = "TardisId";
    private static final String TAG_OWNER_ID = "OwnerId";
    private static final String TAG_OWNER_NAME = "OwnerName";

    public ItemTardisKey() {
        setRegistryName(RenewedTardisMod.MOD_ID, "tardis_key");
        setTranslationKey(RenewedTardisMod.MOD_ID + ".tardis_key");
        setCreativeTab(RenewedTardisCreativeTab.INSTANCE);
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        if (!(player instanceof EntityPlayerMP) || player instanceof FakePlayer) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }

        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        if (world.getMinecraftServer() == null) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        TardisWorldData data = TardisWorldData.get(world.getMinecraftServer());
        UUID boundId = getTardisId(stack);

        if (boundId == null) {
            boolean alreadyOwned = data.getByOwner(serverPlayer.getUniqueID()) != null;
            TardisRecord record = data.getOrCreateForOwner(serverPlayer.getUniqueID(), serverPlayer.getName());
            bind(stack, record);

            String message = alreadyOwned
                ? "The key binds itself to your TARDIS."
                : "The key feels warm. Your TARDIS record has been created.";
            serverPlayer.sendStatusMessage(
                new TextComponentString(TextFormatting.GOLD + "[TARDIS KEY] " + TextFormatting.RESET + message),
                false
            );
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        TardisRecord record = data.getById(boundId);
        if (record == null) {
            serverPlayer.sendStatusMessage(
                new TextComponentString(
                    TextFormatting.RED + "[TARDIS KEY] This key points to a missing TARDIS record."
                ),
                false
            );
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        bind(stack, record);

        String dimensionText = record.hasInteriorDimension()
            ? Integer.toString(record.getInteriorDimension())
            : "not assigned yet";
        serverPlayer.sendStatusMessage(
            new TextComponentString(
                TextFormatting.GOLD + "[TARDIS KEY] " + TextFormatting.RESET
                    + "Owner: " + record.getOwnerName() + ", interior dimension: " + dimensionText + "."
            ),
            false
        );
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void addInformation(
        ItemStack stack,
        @Nullable World world,
        List<String> tooltip,
        ITooltipFlag flag
    ) {
        UUID tardisId = getTardisId(stack);
        if (tardisId == null) {
            tooltip.add(TextFormatting.GRAY + "Unbound");
            tooltip.add(TextFormatting.DARK_GRAY + "Right-click to bind");
            return;
        }

        String ownerName = getOwnerName(stack);
        tooltip.add(TextFormatting.GRAY + "Owner: " + (ownerName.isEmpty() ? "Unknown" : ownerName));
        tooltip.add(TextFormatting.DARK_GRAY + "TARDIS: " + tardisId);
    }

    public static ItemStack createBoundKey(TardisRecord record) {
        ItemStack stack = new ItemStack(dev.zoldmaki.renewedtardis.init.ModItems.TARDIS_KEY);
        bind(stack, record);
        return stack;
    }

    public static void bind(ItemStack stack, TardisRecord record) {
        NBTTagCompound modTag = getOrCreateModTag(stack);
        modTag.setString(TAG_TARDIS_ID, record.getTardisId().toString());
        modTag.setString(TAG_OWNER_ID, record.getOwnerId().toString());
        modTag.setString(TAG_OWNER_NAME, record.getOwnerName());
    }

    @Nullable
    public static UUID getTardisId(ItemStack stack) {
        NBTTagCompound modTag = getModTag(stack);
        if (modTag == null || !modTag.hasKey(TAG_TARDIS_ID, Constants.NBT.TAG_STRING)) {
            return null;
        }

        try {
            return UUID.fromString(modTag.getString(TAG_TARDIS_ID));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static String getOwnerName(ItemStack stack) {
        NBTTagCompound modTag = getModTag(stack);
        return modTag == null ? "" : modTag.getString(TAG_OWNER_NAME);
    }

    private static NBTTagCompound getOrCreateModTag(ItemStack stack) {
        NBTTagCompound root = stack.getTagCompound();
        if (root == null) {
            root = new NBTTagCompound();
            stack.setTagCompound(root);
        }

        if (!root.hasKey(TAG_ROOT, Constants.NBT.TAG_COMPOUND)) {
            root.setTag(TAG_ROOT, new NBTTagCompound());
        }
        return root.getCompoundTag(TAG_ROOT);
    }

    @Nullable
    private static NBTTagCompound getModTag(ItemStack stack) {
        NBTTagCompound root = stack.getTagCompound();
        if (root == null || !root.hasKey(TAG_ROOT, Constants.NBT.TAG_COMPOUND)) {
            return null;
        }
        return root.getCompoundTag(TAG_ROOT);
    }
}
