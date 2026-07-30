package dev.zoldmaki.renewedtardis.block;

import dev.zoldmaki.renewedtardis.RenewedTardisCreativeTab;
import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.access.TardisAccessService;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.damage.TardisDamageService;
import dev.zoldmaki.renewedtardis.flight.TardisFlightService;
import dev.zoldmaki.renewedtardis.energy.TardisEnergyService;
import dev.zoldmaki.renewedtardis.interaction.TardisInteractionGuard;
import dev.zoldmaki.renewedtardis.upgrade.TardisUpgradeService;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisConsoleControl;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Reusable prototype console control. Each registered block selects one small,
 * server-authoritative action while sharing the same persistent TARDIS link.
 */
public final class BlockTardisConsoleControl extends Block {
    public enum ControlType {
        TAKEOFF,
        LANDING,
        DESTINATION,
        FACING,
        STATUS
    }

    private final ControlType controlType;

    public BlockTardisConsoleControl(String registryName, ControlType controlType) {
        super(Material.IRON);
        this.controlType = controlType;
        setRegistryName(RenewedTardisMod.MOD_ID, registryName);
        setTranslationKey(RenewedTardisMod.MOD_ID + "." + registryName);
        setCreativeTab(RenewedTardisCreativeTab.INSTANCE);
        setBlockUnbreakable();
        setResistance(6000000.0F);
        setSoundType(SoundType.METAL);
    }

    public ControlType getControlType() {
        return controlType;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityTardisConsoleControl();
    }

    @Override
    public boolean onBlockActivated(
        World world,
        BlockPos pos,
        IBlockState state,
        EntityPlayer player,
        EnumHand hand,
        EnumFacing clickedFace,
        float hitX,
        float hitY,
        float hitZ
    ) {
        if (world.isRemote) {
            return true;
        }
        if (!TardisInteractionGuard.tryBegin(player, world, pos)) {
            return true;
        }
        if (!(player instanceof EntityPlayerMP) || world.getMinecraftServer() == null) {
            return false;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityTardisConsoleControl)) {
            return false;
        }

        UUID tardisId = ((TileEntityTardisConsoleControl) tile).getTardisId();
        if (tardisId == null) {
            send(player, TextFormatting.RED, "This control has lost its TARDIS link.");
            return true;
        }

        MinecraftServer server = world.getMinecraftServer();
        TardisWorldData data = TardisWorldData.get(server);
        TardisRecord record = data.getById(tardisId);
        if (record == null) {
            send(player, TextFormatting.RED, "This control belongs to a missing TARDIS record.");
            return true;
        }

        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        boolean statusOnly = controlType == ControlType.STATUS
            || (controlType == ControlType.TAKEOFF && player.isSneaking());
        if (!statusOnly && !TardisAccessService.canUseConsole(serverPlayer, record)) {
            send(player, TextFormatting.RED, "You do not have console permission for this TARDIS.");
            return true;
        }

        switch (controlType) {
            case TAKEOFF:
                handleTakeoff(serverPlayer, record, pos);
                break;
            case LANDING:
                handleLanding(serverPlayer, record, pos);
                break;
            case DESTINATION:
                handleDestination(serverPlayer, data, record, clickedFace);
                break;
            case FACING:
                handleFacing(serverPlayer, data, record);
                break;
            case STATUS:
                showStatus(serverPlayer, record);
                break;
            default:
                send(player, TextFormatting.RED, "Unknown console control type.");
                break;
        }
        return true;
    }

    private static void handleTakeoff(EntityPlayerMP player, TardisRecord record, BlockPos controlPos) {
        if (player.isSneaking()) {
            showStatus(player, record);
            return;
        }
        TardisFlightService.takeOff(player, record, controlPos);
    }

    private static void handleLanding(EntityPlayerMP player, TardisRecord record, BlockPos controlPos) {
        TardisFlightService.LandingMode mode = player.isSneaking()
            ? TardisFlightService.LandingMode.NEAREST_SAFE
            : TardisFlightService.LandingMode.EXACT;
        TardisFlightService.land(player, record, controlPos, mode, false);
    }

    private static void handleDestination(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record,
        EnumFacing clickedFace
    ) {
        DimensionPosition destination = record.getDestinationPosition();
        if (destination == null) {
            send(
                player,
                TextFormatting.YELLOW,
                "No destination is set. Use the bound key outside or /tr destination first."
            );
            return;
        }

        EnumFacing direction = clickedFace == null ? EnumFacing.UP : clickedFace;
        int distance = player.isSneaking() ? 10 : 1;
        BlockPos moved = destination.getPosition().offset(direction, distance);
        if (moved.getY() < 1 || moved.getY() > 254) {
            send(player, TextFormatting.RED, "That adjustment would leave the safe build height.");
            return;
        }

        MinecraftServer server = player.getServer();
        WorldServer destinationWorld = server == null ? null : server.getWorld(destination.getDimension());
        if (
            destinationWorld != null
                && (!destinationWorld.isAirBlock(moved) || !destinationWorld.isAirBlock(moved.up()))
        ) {
            send(player, TextFormatting.RED, "The adjusted destination needs two clear blocks.");
            return;
        }

        data.setDestination(
            record.getTardisId(),
            new DimensionPosition(destination.getDimension(), moved),
            record.getDestinationFacing()
        );
        send(
            player,
            TextFormatting.GOLD,
            "Destination moved " + distance + " block" + (distance == 1 ? "" : "s")
                + " " + direction.getName() + " to dim " + destination.getDimension() + " @ "
                + moved.getX() + ", " + moved.getY() + ", " + moved.getZ() + "."
        );
    }

    private static void handleFacing(
        EntityPlayerMP player,
        TardisWorldData data,
        TardisRecord record
    ) {
        DimensionPosition destination = record.getDestinationPosition();
        if (destination == null) {
            send(player, TextFormatting.YELLOW, "Set a destination before changing its exterior facing.");
            return;
        }

        EnumFacing current = record.getDestinationFacing();
        EnumFacing next = player.isSneaking() ? current.rotateYCCW() : current.rotateY();
        data.setDestination(record.getTardisId(), destination, next);
        send(player, TextFormatting.GOLD, "Destination facing set to " + next.getName() + ".");
    }

    private static void showStatus(EntityPlayer player, TardisRecord record) {
        long now = System.currentTimeMillis();
        DimensionPosition destination = record.getDestinationPosition();
        DimensionPosition exterior = record.getExteriorPosition();

        send(
            player,
            TextFormatting.AQUA,
            "Flight: " + TardisFlightService.describeFlightProgress(record, now)
                + "; " + TardisFlightService.describeFlightProfile(record)
                + "; exterior: " + (exterior == null ? "not placed" : exterior.toString())
                + (record.isExteriorLocked() ? " (locked)" : " (unlocked)")
                + "; shell: " + record.getExteriorShell().getName()
                + "; access: " + record.getAccessMode().getName()
                + " (" + record.getCrew().size() + " crew)."
        );
        send(
            player,
            TextFormatting.AQUA,
            "Destination: " + (destination == null
                ? "not set."
                : destination.toString() + " facing " + record.getDestinationFacing().getName() + ".")
        );
        if (record.isInFlight() && record.getFlightResolvedDestination() != null) {
            send(
                player,
                TextFormatting.AQUA,
                "Resolved landing target: " + record.getFlightResolvedDestination().toString() + "."
            );
        }
        if (player.world.getMinecraftServer() != null) {
            TardisWorldData data = TardisWorldData.get(player.world.getMinecraftServer());
            TardisEnergyService.refresh(data, record);
            send(player, TextFormatting.AQUA, "Artron energy: " + TardisEnergyService.describe(record)
                + "; systems: " + TardisDamageService.describe(record)
                + "; upgrades: " + TardisUpgradeService.describe(record) + ".");
        }
    }

    private static void send(EntityPlayer player, TextFormatting color, String text) {
        player.sendStatusMessage(
            new TextComponentString(color + "[TARDIS] " + TextFormatting.RESET + text),
            false
        );
    }
}
