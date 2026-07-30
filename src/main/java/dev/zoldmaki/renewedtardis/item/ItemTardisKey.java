package dev.zoldmaki.renewedtardis.item;

import dev.zoldmaki.renewedtardis.RenewedTardisCreativeTab;
import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.access.TardisAccessService;
import dev.zoldmaki.renewedtardis.config.TardisConfig;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.dimension.TardisDimensionManager;
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
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
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
    public EnumActionResult onItemUse(
        EntityPlayer player,
        World world,
        BlockPos pos,
        EnumHand hand,
        EnumFacing facing,
        float hitX,
        float hitY,
        float hitZ
    ) {
        if (!player.isSneaking()) {
            return EnumActionResult.PASS;
        }
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        if (!(player instanceof EntityPlayerMP) || player instanceof FakePlayer || world.getMinecraftServer() == null) {
            return EnumActionResult.FAIL;
        }
        if (world.provider.getDimension() == TardisDimensionManager.getDimensionId()) {
            player.sendStatusMessage(
                new TextComponentString(
                    TextFormatting.RED + "[TARDIS KEY] The exterior cannot land inside the TARDIS interior dimension."
                ),
                false
            );
            return EnumActionResult.FAIL;
        }

        ItemStack stack = player.getHeldItem(hand);
        UUID tardisId = getTardisId(stack);
        if (tardisId == null) {
            player.sendStatusMessage(
                new TextComponentString(
                    TextFormatting.YELLOW + "[TARDIS KEY] Bind this key before setting a destination."
                ),
                false
            );
            return EnumActionResult.FAIL;
        }

        TardisWorldData data = TardisWorldData.get(world.getMinecraftServer());
        TardisRecord record = data.getById(tardisId);
        if (record == null) {
            player.sendStatusMessage(
                new TextComponentString(
                    TextFormatting.RED + "[TARDIS KEY] This key points to a missing TARDIS record."
                ),
                false
            );
            return EnumActionResult.FAIL;
        }
        if (!TardisAccessService.canUseKey(player, record)) {
            player.sendStatusMessage(
                new TextComponentString(
                    TextFormatting.RED + "[TARDIS KEY] You do not have key permission for this TARDIS."
                ),
                false
            );
            return EnumActionResult.FAIL;
        }

        int destinationDimension = world.provider.getDimension();
        if (TardisConfig.isDestinationDimensionBlocked(destinationDimension)) {
            player.sendStatusMessage(
                new TextComponentString(
                    TextFormatting.RED + "[TARDIS KEY] Dimension " + destinationDimension
                        + " is blocked by the TARDIS flight config."
                ),
                false
            );
            return EnumActionResult.FAIL;
        }
        DimensionPosition departure = record.isInFlight()
            ? record.getFlightDeparture()
            : record.getExteriorPosition();
        if (departure != null) {
            String restriction = TardisConfig.describeFlightRestriction(
                departure.getDimension(),
                destinationDimension
            );
            if (!restriction.isEmpty()) {
                player.sendStatusMessage(
                    new TextComponentString(TextFormatting.RED + "[TARDIS KEY] " + restriction),
                    false
                );
                return EnumActionResult.FAIL;
            }
        }

        BlockPos target = pos.offset(facing);
        if (target.getY() < 1 || target.getY() > 254) {
            player.sendStatusMessage(
                new TextComponentString(TextFormatting.RED + "[TARDIS KEY] That landing height is unsafe."),
                false
            );
            return EnumActionResult.FAIL;
        }
        if (!world.isAirBlock(target) || !world.isAirBlock(target.up())) {
            player.sendStatusMessage(
                new TextComponentString(
                    TextFormatting.RED + "[TARDIS KEY] The destination needs two clear blocks."
                ),
                false
            );
            return EnumActionResult.FAIL;
        }

        EnumFacing exteriorFacing = player.getHorizontalFacing().getOpposite();
        data.setDestination(
            record.getTardisId(),
            new DimensionPosition(world.provider.getDimension(), target),
            exteriorFacing
        );
        player.sendStatusMessage(
            new TextComponentString(
                TextFormatting.GOLD + "[TARDIS KEY] " + TextFormatting.RESET
                    + "Destination set to dim " + world.provider.getDimension() + " @ "
                    + target.getX() + ", " + target.getY() + ", " + target.getZ() + "."
            ),
            false
        );
        return EnumActionResult.SUCCESS;
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
        tooltip.add(TextFormatting.DARK_GRAY + "Sneak-right-click a block to set destination");
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
