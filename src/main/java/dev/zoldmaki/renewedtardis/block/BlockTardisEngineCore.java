package dev.zoldmaki.renewedtardis.block;

import dev.zoldmaki.renewedtardis.RenewedTardisCreativeTab;
import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.access.TardisAccessService;
import dev.zoldmaki.renewedtardis.config.TardisConfig;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.damage.TardisDamageService;
import dev.zoldmaki.renewedtardis.damage.TardisSubsystem;
import dev.zoldmaki.renewedtardis.energy.TardisEnergyService;
import dev.zoldmaki.renewedtardis.interaction.TardisInteractionGuard;
import dev.zoldmaki.renewedtardis.init.ModItems;
import dev.zoldmaki.renewedtardis.item.ItemTardisUpgrade;
import dev.zoldmaki.renewedtardis.upgrade.TardisUpgradeService;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisEngineCore;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/** Temporary physical interface for the persistent Artron-energy system. */
public final class BlockTardisEngineCore extends Block {
    public BlockTardisEngineCore() {
        super(Material.IRON);
        setRegistryName(RenewedTardisMod.MOD_ID, "tardis_engine_core");
        setTranslationKey(RenewedTardisMod.MOD_ID + ".tardis_engine_core");
        setCreativeTab(RenewedTardisCreativeTab.INSTANCE);
        setBlockUnbreakable();
        setResistance(6000000.0F);
        setLightLevel(0.75F);
        setSoundType(SoundType.METAL);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityTardisEngineCore();
    }

    @Override
    public boolean onBlockActivated(
        World world,
        BlockPos pos,
        IBlockState state,
        EntityPlayer player,
        EnumHand hand,
        EnumFacing face,
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
        if (world.getMinecraftServer() == null) {
            return false;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityTardisEngineCore)) {
            return false;
        }
        UUID tardisId = ((TileEntityTardisEngineCore) tile).getTardisId();
        if (tardisId == null) {
            send(player, TextFormatting.RED, "This engine core has lost its TARDIS link.");
            return true;
        }

        TardisWorldData data = TardisWorldData.get(world.getMinecraftServer());
        TardisRecord record = data.getById(tardisId);
        if (record == null) {
            send(player, TextFormatting.RED, "This engine core belongs to a missing TARDIS record.");
            return true;
        }

        ItemStack held = player.getHeldItem(hand);
        if (!held.isEmpty() && held.getItem() instanceof ItemTardisUpgrade) {
            if (!TardisAccessService.canMaintain(player, record)) {
                send(player, TextFormatting.RED, "You do not have maintenance permission for this TARDIS.");
                return true;
            }
            ItemTardisUpgrade item = (ItemTardisUpgrade) held.getItem();
            TardisUpgradeService.InstallResult result = TardisUpgradeService.install(
                data,
                record,
                item.getUpgradeType()
            );
            if (result.isSuccess() && !player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
            send(
                player,
                result.isSuccess() ? TextFormatting.GREEN : TextFormatting.RED,
                result.getMessage() + " Current upgrades: " + TardisUpgradeService.describe(record) + "."
            );
            return true;
        }
        if (!held.isEmpty() && held.getItem() == ModItems.CHRONOSTEEL_INGOT) {
            if (!TardisAccessService.canMaintain(player, record)) {
                send(player, TextFormatting.RED, "You do not have maintenance permission for this TARDIS.");
                return true;
            }
            TardisSubsystem subsystem = TardisDamageService.findMostDamaged(record);
            int before = record.getSubsystemHealth(subsystem);
            if (before >= TardisDamageService.MAX_HEALTH) {
                send(player, TextFormatting.GREEN, "All TARDIS subsystems are already at full health.");
                return true;
            }
            int repaired = data.repairSubsystem(
                record.getTardisId(),
                subsystem,
                TardisConfig.getRepairPerChronosteel()
            );
            if (repaired > 0 && !player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
            send(
                player,
                TextFormatting.GREEN,
                "Chronosteel repaired " + subsystem.getDisplayName() + " by " + repaired
                    + "% (" + record.getSubsystemHealth(subsystem) + "% remaining health)."
            );
            return true;
        }

        TardisEnergyService.refresh(data, record);
        String regen = record.getArtronEnergy() >= record.getArtronCapacity()
            ? "fully charged"
            : "regenerating " + TardisUpgradeService.getRegenerationPerSecond(record) + " AU/s";
        send(
            player,
            TextFormatting.AQUA,
            "Engine core: " + TardisEnergyService.describe(record) + "; " + regen
                + "; systems: " + TardisDamageService.describe(record)
                + "; upgrades: " + TardisUpgradeService.describe(record)
                + "; access: " + record.getAccessMode().getName()
                + " (" + record.getCrew().size() + " crew)."
        );

        if (player.isSneaking()) {
            DimensionPosition departure = record.isInFlight()
                ? record.getFlightDeparture()
                : record.getExteriorPosition();
            DimensionPosition destination = record.getDestinationPosition();
            String flightCost = departure == null || destination == null
                ? "unavailable until exterior and destination are set"
                : TardisEnergyService.format(TardisEnergyService.calculateFlightCost(record, departure, destination)) + " AU";
            send(
                player,
                TextFormatting.GRAY,
                "Current flight cost: " + flightCost + "; one Chronosteel repairs "
                    + TardisConfig.getRepairPerChronosteel() + "% of the most damaged subsystem; room limit "
                    + TardisUpgradeService.getRoomLimit(record) + "; installed upgrades: "
                    + TardisUpgradeService.describe(record) + "."
            );
        }
        return true;
    }

    private static void send(EntityPlayer player, TextFormatting color, String text) {
        player.sendStatusMessage(
            new TextComponentString(color + "[TARDIS] " + TextFormatting.RESET + text),
            false
        );
    }
}
