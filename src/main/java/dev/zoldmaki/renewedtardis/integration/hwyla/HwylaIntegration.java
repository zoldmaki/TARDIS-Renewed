package dev.zoldmaki.renewedtardis.integration.hwyla;

import dev.zoldmaki.renewedtardis.block.BlockGravityLift;
import dev.zoldmaki.renewedtardis.block.BlockTardisConsoleControl;
import dev.zoldmaki.renewedtardis.block.BlockTardisEngineCore;
import dev.zoldmaki.renewedtardis.block.BlockTardisExterior;
import dev.zoldmaki.renewedtardis.block.BlockTardisFlightConsole;
import dev.zoldmaki.renewedtardis.block.BlockTardisInteriorDoor;
import dev.zoldmaki.renewedtardis.block.BlockTardisTransmatPad;
import dev.zoldmaki.renewedtardis.damage.TardisDamageService;
import dev.zoldmaki.renewedtardis.damage.TardisSubsystem;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.energy.TardisEnergyService;
import dev.zoldmaki.renewedtardis.tile.TileEntityGravityLift;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisConsoleControl;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisEngineCore;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisExterior;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisFlightConsole;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisInteriorDoor;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisTransmatPad;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/** Optional server-authoritative HWYLA/Waila tooltips for TARDIS machinery. */
public final class HwylaIntegration implements IWailaDataProvider {
    private static final String TAG_BOUND = "TRBound";
    private static final String TAG_KIND = "TRKind";
    private static final String TAG_OWNER = "TROwner";
    private static final String TAG_STATE = "TRState";
    private static final String TAG_PROGRESS = "TRProgress";
    private static final String TAG_LOCKED = "TRLocked";
    private static final String TAG_SHELL = "TRShell";
    private static final String TAG_ACCESS = "TRAccess";
    private static final String TAG_ENERGY = "TREnergy";
    private static final String TAG_CAPACITY = "TRCapacity";
    private static final String TAG_LOWEST_SYSTEM = "TRLowestSystem";
    private static final String TAG_LOWEST_HEALTH = "TRLowestHealth";
    private static final String TAG_LIFT_UP = "TRLiftUp";

    private static final HwylaIntegration INSTANCE = new HwylaIntegration();

    private HwylaIntegration() {
    }

    /** Called reflectively by Waila/HWYLA after the string-based IMC message. */
    public static void register(IWailaRegistrar registrar) {
        registrar.registerBodyProvider(INSTANCE, BlockTardisExterior.class);
        registrar.registerBodyProvider(INSTANCE, BlockTardisInteriorDoor.class);
        registrar.registerBodyProvider(INSTANCE, BlockTardisFlightConsole.class);
        registrar.registerBodyProvider(INSTANCE, BlockTardisConsoleControl.class);
        registrar.registerBodyProvider(INSTANCE, BlockTardisEngineCore.class);
        registrar.registerBodyProvider(INSTANCE, BlockTardisTransmatPad.class);
        registrar.registerBodyProvider(INSTANCE, BlockGravityLift.class);

        registrar.registerNBTProvider(INSTANCE, TileEntityTardisExterior.class);
        registrar.registerNBTProvider(INSTANCE, TileEntityTardisInteriorDoor.class);
        registrar.registerNBTProvider(INSTANCE, TileEntityTardisFlightConsole.class);
        registrar.registerNBTProvider(INSTANCE, TileEntityTardisConsoleControl.class);
        registrar.registerNBTProvider(INSTANCE, TileEntityTardisEngineCore.class);
        registrar.registerNBTProvider(INSTANCE, TileEntityTardisTransmatPad.class);
        registrar.registerNBTProvider(INSTANCE, TileEntityGravityLift.class);
    }

    @Override
    public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return ItemStack.EMPTY;
    }

    @Override
    public List<String> getWailaHead(
        ItemStack itemStack,
        List<String> currentTip,
        IWailaDataAccessor accessor,
        IWailaConfigHandler config
    ) {
        return currentTip;
    }

    @Override
    public List<String> getWailaBody(
        ItemStack itemStack,
        List<String> currentTip,
        IWailaDataAccessor accessor,
        IWailaConfigHandler config
    ) {
        NBTTagCompound tag = accessor == null ? null : accessor.getNBTData();
        if (tag == null) {
            return currentTip;
        }

        String kind = tag.getString(TAG_KIND);
        if ("gravity_lift".equals(kind)) {
            currentTip.add(
                TextFormatting.AQUA + "Direction: " + TextFormatting.WHITE
                    + (tag.getBoolean(TAG_LIFT_UP) ? "up — press jump" : "down — hold sneak")
            );
            currentTip.add(TextFormatting.DARK_GRAY + "Right-click to reverse direction");
            return currentTip;
        }

        if (!tag.getBoolean(TAG_BOUND)) {
            currentTip.add(TextFormatting.YELLOW + "Unbound TARDIS component");
            return currentTip;
        }

        currentTip.add(
            TextFormatting.AQUA + "Owner: " + TextFormatting.WHITE + tag.getString(TAG_OWNER)
        );
        currentTip.add(
            TextFormatting.AQUA + "State: " + TextFormatting.WHITE + tag.getString(TAG_STATE)
        );
        if ("in flight".equals(tag.getString(TAG_STATE))) {
            currentTip.add(
                TextFormatting.AQUA + "Flight: " + TextFormatting.WHITE
                    + tag.getInteger(TAG_PROGRESS) + "%"
            );
        }

        if ("exterior".equals(kind)) {
            currentTip.add(
                TextFormatting.AQUA + "Door: " + TextFormatting.WHITE
                    + (tag.getBoolean(TAG_LOCKED) ? "locked" : "unlocked")
                    + TextFormatting.DARK_GRAY + " • " + tag.getString(TAG_ACCESS)
            );
            currentTip.add(
                TextFormatting.AQUA + "Shell: " + TextFormatting.WHITE + tag.getString(TAG_SHELL)
            );
        } else if ("engine_core".equals(kind)) {
            currentTip.add(
                TextFormatting.AQUA + "Artron: " + TextFormatting.WHITE
                    + tag.getLong(TAG_ENERGY) + " / " + tag.getLong(TAG_CAPACITY) + " AU"
            );
            currentTip.add(
                TextFormatting.AQUA + "Lowest system: " + TextFormatting.WHITE
                    + tag.getString(TAG_LOWEST_SYSTEM) + " " + tag.getInteger(TAG_LOWEST_HEALTH) + "%"
            );
        } else if ("transmat".equals(kind)) {
            currentTip.add(
                TextFormatting.AQUA + "Transmat: " + TextFormatting.WHITE
                    + ("landed".equals(tag.getString(TAG_STATE)) ? "ready" : "offline during flight")
            );
        }
        return currentTip;
    }

    @Override
    public List<String> getWailaTail(
        ItemStack itemStack,
        List<String> currentTip,
        IWailaDataAccessor accessor,
        IWailaConfigHandler config
    ) {
        return currentTip;
    }

    @Override
    public NBTTagCompound getNBTData(
        EntityPlayerMP player,
        TileEntity tileEntity,
        NBTTagCompound tag,
        World world,
        BlockPos pos
    ) {
        NBTTagCompound result = tag == null ? new NBTTagCompound() : tag;
        result.setString(TAG_KIND, getKind(tileEntity));

        if (tileEntity instanceof TileEntityGravityLift) {
            IBlockState state = world.getBlockState(pos);
            result.setBoolean(
                TAG_LIFT_UP,
                state.getPropertyKeys().contains(dev.zoldmaki.renewedtardis.block.BlockGravityLift.UP)
                    && state.getValue(dev.zoldmaki.renewedtardis.block.BlockGravityLift.UP)
            );
            return result;
        }

        UUID tardisId = getTardisId(tileEntity);
        MinecraftServer server = world == null ? null : world.getMinecraftServer();
        TardisRecord record = tardisId == null || server == null
            ? null
            : TardisWorldData.get(server).getById(tardisId);
        result.setBoolean(TAG_BOUND, record != null);
        if (record == null) {
            return result;
        }

        result.setString(TAG_OWNER, record.getOwnerName());
        result.setString(TAG_STATE, record.isInFlight() ? "in flight" : "landed");
        result.setInteger(TAG_PROGRESS, record.getFlightProgressPercent(System.currentTimeMillis()));

        if (tileEntity instanceof TileEntityTardisExterior) {
            result.setBoolean(TAG_LOCKED, record.isExteriorLocked());
            result.setString(TAG_SHELL, record.getExteriorShell().getDisplayName());
            result.setString(TAG_ACCESS, record.getAccessMode().getDisplayName());
        } else if (tileEntity instanceof TileEntityTardisEngineCore) {
            TardisEnergyService.refresh(TardisWorldData.get(server), record);
            result.setLong(TAG_ENERGY, record.getArtronEnergy());
            result.setLong(TAG_CAPACITY, record.getArtronCapacity());
            TardisSubsystem lowest = TardisDamageService.findMostDamaged(record);
            result.setString(TAG_LOWEST_SYSTEM, lowest == null ? "None" : lowest.getDisplayName());
            result.setInteger(TAG_LOWEST_HEALTH, lowest == null ? 100 : record.getSubsystemHealth(lowest));
        }
        return result;
    }

    private static String getKind(TileEntity tileEntity) {
        if (tileEntity instanceof TileEntityTardisExterior) {
            return "exterior";
        }
        if (tileEntity instanceof TileEntityTardisInteriorDoor) {
            return "interior_door";
        }
        if (tileEntity instanceof TileEntityTardisFlightConsole) {
            return "flight_console";
        }
        if (tileEntity instanceof TileEntityTardisConsoleControl) {
            return "console_control";
        }
        if (tileEntity instanceof TileEntityTardisEngineCore) {
            return "engine_core";
        }
        if (tileEntity instanceof TileEntityTardisTransmatPad) {
            return "transmat";
        }
        if (tileEntity instanceof TileEntityGravityLift) {
            return "gravity_lift";
        }
        return "component";
    }

    @Nullable
    private static UUID getTardisId(TileEntity tileEntity) {
        if (tileEntity instanceof TileEntityTardisExterior) {
            return ((TileEntityTardisExterior) tileEntity).getTardisId();
        }
        if (tileEntity instanceof TileEntityTardisInteriorDoor) {
            return ((TileEntityTardisInteriorDoor) tileEntity).getTardisId();
        }
        if (tileEntity instanceof TileEntityTardisFlightConsole) {
            return ((TileEntityTardisFlightConsole) tileEntity).getTardisId();
        }
        if (tileEntity instanceof TileEntityTardisConsoleControl) {
            return ((TileEntityTardisConsoleControl) tileEntity).getTardisId();
        }
        if (tileEntity instanceof TileEntityTardisEngineCore) {
            return ((TileEntityTardisEngineCore) tileEntity).getTardisId();
        }
        if (tileEntity instanceof TileEntityTardisTransmatPad) {
            return ((TileEntityTardisTransmatPad) tileEntity).getTardisId();
        }
        return null;
    }
}
