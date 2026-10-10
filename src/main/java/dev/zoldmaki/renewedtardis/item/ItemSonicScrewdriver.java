package dev.zoldmaki.renewedtardis.item;

import dev.zoldmaki.renewedtardis.RenewedTardisCreativeTab;
import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.access.TardisAccessService;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.dimension.TardisDimensionManager;
import dev.zoldmaki.renewedtardis.flight.TardisFlightService;
import dev.zoldmaki.renewedtardis.init.ModSounds;
import dev.zoldmaki.renewedtardis.sonic.SonicActionRegistry;
import dev.zoldmaki.renewedtardis.sonic.SonicActionResult;
import dev.zoldmaki.renewedtardis.sonic.SonicBlockContext;
import dev.zoldmaki.renewedtardis.sonic.SonicEntityContext;
import dev.zoldmaki.renewedtardis.sonic.SonicMode;
import dev.zoldmaki.renewedtardis.transmat.TardisTransmatService;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.FakePlayer;

/** First persistent, extensible sonic screwdriver implementation. */
public final class ItemSonicScrewdriver extends Item {
    private static final String TAG_ROOT = RenewedTardisMod.MOD_ID;
    private static final String TAG_MODE = "SonicMode";
    private static final String TAG_TARDIS_ID = "TardisId";
    private static final String TAG_OWNER_NAME = "OwnerName";

    public ItemSonicScrewdriver() {
        setRegistryName(RenewedTardisMod.MOD_ID, "sonic_screwdriver");
        setTranslationKey(RenewedTardisMod.MOD_ID + ".sonic_screwdriver");
        setCreativeTab(RenewedTardisCreativeTab.INSTANCE);
        setMaxStackSize(1);
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
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        if (!(player instanceof EntityPlayerMP) || player instanceof FakePlayer) {
            return EnumActionResult.PASS;
        }

        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        if (serverPlayer.getCooldownTracker().hasCooldown(this)) {
            return EnumActionResult.SUCCESS;
        }
        SonicBlockContext context = new SonicBlockContext(
            (WorldServer) world,
            serverPlayer,
            stack,
            getMode(stack),
            pos,
            facing,
            hand
        );
        SonicActionResult result = SonicActionRegistry.dispatchBlock(context);
        if (result == SonicActionResult.PASS) {
            send(serverPlayer, TextFormatting.GRAY, "No sonic action is registered for that block in "
                + getMode(stack).getDisplayName() + " mode.");
            return EnumActionResult.PASS;
        }
        if (result == SonicActionResult.SUCCESS) {
            activate(serverPlayer, pos, getMode(stack));
            return EnumActionResult.SUCCESS;
        }
        serverPlayer.getCooldownTracker().setCooldown(this, 6);
        return EnumActionResult.FAIL;
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
        if (serverPlayer.getCooldownTracker().hasCooldown(this)) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        if (player.isSneaking()) {
            cycleMode(serverPlayer, stack);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        SonicMode mode = getMode(stack);
        boolean success;
        switch (mode) {
            case LOCATE:
                success = locate(serverPlayer, stack);
                break;
            case RECALL:
                success = recall(serverPlayer, stack);
                break;
            case TRANSMAT:
                success = transmat(serverPlayer, stack);
                break;
            case LINK:
                send(serverPlayer, TextFormatting.YELLOW,
                    "Right-click a linked exterior, interior door, console, or engine core to bind the sonic.");
                success = false;
                break;
            case RECONFIGURE:
            case SCHEMATIC:
            default:
                send(serverPlayer, TextFormatting.YELLOW,
                    "Use this mode on a supported block"
                        + (mode == SonicMode.SCHEMATIC ? " or left-click an entity." : "."));
                success = false;
                break;
        }

        if (success) {
            activate(serverPlayer, serverPlayer.getPosition(), mode);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        serverPlayer.getCooldownTracker().setCooldown(this, 6);
        return new ActionResult<>(EnumActionResult.FAIL, stack);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        SonicMode mode = getMode(stack);
        if (mode != SonicMode.SCHEMATIC) {
            return false;
        }
        if (player.world.isRemote) {
            return true;
        }
        if (!(player instanceof EntityPlayerMP) || player instanceof FakePlayer) {
            return true;
        }

        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        if (serverPlayer.getCooldownTracker().hasCooldown(this)) {
            return true;
        }
        SonicActionResult result = SonicActionRegistry.dispatchEntity(
            new SonicEntityContext(serverPlayer, stack, mode, entity)
        );
        if (result == SonicActionResult.SUCCESS) {
            activate(serverPlayer, entity.getPosition(), mode);
        } else {
            serverPlayer.getCooldownTracker().setCooldown(this, 6);
        }
        return true;
    }

    @Override
    public void addInformation(
        ItemStack stack,
        @Nullable World world,
        List<String> tooltip,
        ITooltipFlag flag
    ) {
        tooltip.add(TextFormatting.AQUA + "Mode: " + getMode(stack).getDisplayName());
        UUID tardisId = getTardisId(stack);
        if (tardisId == null) {
            tooltip.add(TextFormatting.GRAY + "Unbound");
            tooltip.add(TextFormatting.DARK_GRAY + "Link mode: use on a TARDIS block");
        } else {
            String ownerName = getOwnerName(stack);
            tooltip.add(TextFormatting.GRAY + "Linked owner: "
                + (ownerName.isEmpty() ? "Unknown" : ownerName));
            tooltip.add(TextFormatting.DARK_GRAY + "TARDIS: " + tardisId);
        }
        tooltip.add(TextFormatting.DARK_GRAY + "Sneak-right-click air to change mode");
        tooltip.add(TextFormatting.DARK_GRAY + "Sneak-right-click a block to use it");
    }

    private boolean locate(EntityPlayerMP player, ItemStack stack) {
        TardisRecord record = getAccessibleRecord(player, stack);
        if (record == null) {
            return false;
        }
        bind(stack, record);

        if (record.isInFlight()) {
            long now = System.currentTimeMillis();
            send(
                player,
                TextFormatting.AQUA,
                "The linked TARDIS is in flight: "
                    + TardisFlightService.describeFlightProgress(record, now) + "."
            );
            return true;
        }

        DimensionPosition exterior = record.getExteriorPosition();
        if (exterior == null) {
            send(player, TextFormatting.YELLOW, "The linked TARDIS has no materialized exterior.");
            return false;
        }
        if (exterior.getDimension() == player.dimension) {
            BlockPos target = exterior.getPosition();
            double dx = player.posX - (target.getX() + 0.5D);
            double dy = player.posY - target.getY();
            double dz = player.posZ - (target.getZ() + 0.5D);
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            send(player, TextFormatting.AQUA,
                "The linked TARDIS is at " + exterior.toString() + " (about "
                    + Math.round(distance) + " blocks away).");
        } else {
            send(player, TextFormatting.AQUA, "The linked TARDIS is at " + exterior.toString() + ".");
        }
        return true;
    }

    private boolean transmat(EntityPlayerMP player, ItemStack stack) {
        TardisRecord record = getAccessibleRecord(player, stack);
        if (record == null) {
            return false;
        }
        boolean success = TardisTransmatService.transmat(player, record);
        if (success) {
            bind(stack, record);
        }
        return success;
    }

    private boolean recall(EntityPlayerMP player, ItemStack stack) {
        TardisRecord record = getAccessibleRecord(player, stack);
        if (record == null) {
            return false;
        }
        if (!TardisAccessService.canUseConsole(player, record)) {
            send(player, TextFormatting.RED, "Recall requires console permission for the linked TARDIS.");
            return false;
        }
        if (player.dimension == TardisDimensionManager.getDimensionId()) {
            send(player, TextFormatting.RED, "The exterior cannot be recalled into the TARDIS interior dimension.");
            return false;
        }
        if (record.isInFlight()) {
            send(player, TextFormatting.YELLOW, "The TARDIS is already in flight; its resolved target is unchanged.");
            return false;
        }

        BlockPos target = player.getPosition();
        if (target.getY() < 1 || target.getY() > 254
            || !player.getServerWorld().isAirBlock(target)
            || !player.getServerWorld().isAirBlock(target.up())) {
            send(player, TextFormatting.RED, "Recall needs two clear blocks at your current position.");
            return false;
        }

        TardisWorldData data = TardisWorldData.get(player.getServer());
        DimensionPosition previousDestination = record.getDestinationPosition();
        EnumFacing previousFacing = record.getDestinationFacing();
        data.setDestination(
            record.getTardisId(),
            new DimensionPosition(player.dimension, target),
            player.getHorizontalFacing()
        );
        if (!TardisFlightService.takeOff(player, record, player.getPosition())) {
            data.setDestination(record.getTardisId(), previousDestination, previousFacing);
            return false;
        }
        bind(stack, record);
        return true;
    }

    @Nullable
    private TardisRecord getAccessibleRecord(EntityPlayerMP player, ItemStack stack) {
        UUID tardisId = getTardisId(stack);
        if (tardisId == null) {
            send(player, TextFormatting.YELLOW, "This sonic is unbound. Use Link mode on a TARDIS block first.");
            return null;
        }
        TardisRecord record = TardisWorldData.get(player.getServer()).getById(tardisId);
        if (record == null) {
            send(player, TextFormatting.RED, "This sonic points to a missing TARDIS record.");
            return null;
        }
        if (!TardisAccessService.canUseSonic(player, record)) {
            send(player, TextFormatting.RED, "You no longer have permission to use this linked sonic.");
            return null;
        }
        return record;
    }

    private void cycleMode(EntityPlayerMP player, ItemStack stack) {
        SonicMode next = getMode(stack).next();
        setMode(stack, next);
        player.getCooldownTracker().setCooldown(this, 4);
        player.getServerWorld().playSound(
            null,
            player.getPosition(),
            ModSounds.SONIC,
            SoundCategory.PLAYERS,
            0.35F,
            getPitch(next)
        );
        send(player, TextFormatting.AQUA, "Mode: " + next.getDisplayName() + ".");
    }

    private void activate(EntityPlayerMP player, BlockPos pos, SonicMode mode) {
        player.getCooldownTracker().setCooldown(this, 10);
        player.getServerWorld().playSound(
            null,
            pos,
            ModSounds.SONIC,
            SoundCategory.PLAYERS,
            0.7F,
            getPitch(mode)
        );
    }

    private static float getPitch(SonicMode mode) {
        return 0.85F + mode.ordinal() * 0.08F;
    }

    private static void send(EntityPlayerMP player, TextFormatting color, String message) {
        player.sendStatusMessage(
            new TextComponentString(color + "[SONIC] " + TextFormatting.RESET + message),
            false
        );
    }

    public static SonicMode getMode(ItemStack stack) {
        NBTTagCompound tag = getModTag(stack);
        SonicMode mode = tag == null ? null : SonicMode.fromName(tag.getString(TAG_MODE));
        return mode == null ? SonicMode.LINK : mode;
    }

    public static void setMode(ItemStack stack, SonicMode mode) {
        getOrCreateModTag(stack).setString(TAG_MODE, (mode == null ? SonicMode.LINK : mode).getName());
    }

    @Nullable
    public static UUID getTardisId(ItemStack stack) {
        NBTTagCompound tag = getModTag(stack);
        if (tag == null || !tag.hasKey(TAG_TARDIS_ID, Constants.NBT.TAG_STRING)) {
            return null;
        }
        try {
            return UUID.fromString(tag.getString(TAG_TARDIS_ID));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static String getOwnerName(ItemStack stack) {
        NBTTagCompound tag = getModTag(stack);
        return tag == null ? "" : tag.getString(TAG_OWNER_NAME);
    }

    public static void bind(ItemStack stack, TardisRecord record) {
        NBTTagCompound tag = getOrCreateModTag(stack);
        tag.setString(TAG_TARDIS_ID, record.getTardisId().toString());
        tag.setString(TAG_OWNER_NAME, record.getOwnerName());
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
