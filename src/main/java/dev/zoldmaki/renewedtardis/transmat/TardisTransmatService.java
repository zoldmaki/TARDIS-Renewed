package dev.zoldmaki.renewedtardis.transmat;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.access.TardisAccessService;
import dev.zoldmaki.renewedtardis.block.BlockTardisExterior;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.dimension.TardisDimensionManager;
import dev.zoldmaki.renewedtardis.dimension.TardisInteriorBuilder;
import dev.zoldmaki.renewedtardis.energy.TardisEnergyService;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import dev.zoldmaki.renewedtardis.init.ModSounds;
import dev.zoldmaki.renewedtardis.teleport.TardisTeleportService;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;

/** Server-authoritative bidirectional transmat between an interior pad and the landed exterior. */
public final class TardisTransmatService {
    public static final long ENERGY_COST = 50L;
    public static final double MAX_EXTERIOR_RANGE = 250.0D;
    public static final double MAX_INTERIOR_PAD_RANGE = 8.0D;

    private TardisTransmatService() {
    }

    public static boolean transmat(EntityPlayerMP player, TardisRecord record) {
        if (player == null || record == null || player.getServer() == null) {
            return false;
        }
        if (!TardisAccessService.canUseConsole(player, record)) {
            send(player, TextFormatting.RED, "Transmat use requires console permission.");
            playFailure(player);
            return false;
        }
        if (record.isInFlight()) {
            send(player, TextFormatting.YELLOW, "The transmat is unavailable while the TARDIS is in flight.");
            playFailure(player);
            return false;
        }

        MinecraftServer server = player.getServer();
        TardisWorldData data = TardisWorldData.get(server);
        DimensionPosition interiorAddress = data.getOrCreateInterior(record);
        int configuredInteriorDimension = TardisDimensionManager.getDimensionId();
        if (interiorAddress.getDimension() != configuredInteriorDimension) {
            send(player, TextFormatting.RED,
                "This TARDIS belongs to interior dimension " + interiorAddress.getDimension()
                    + ", but the configured interior dimension is " + configuredInteriorDimension
                    + ". Restore the original dimension ID before using the transmat.");
            playFailure(player);
            return false;
        }
        try {
            WorldServer interiorWorld = TardisDimensionManager.getOrLoadWorld(server);
            TardisInteriorBuilder.ensureGenerated(interiorWorld, data, record);
        } catch (RuntimeException exception) {
            RenewedTardisMod.logger.error(
                "Failed to prepare transmat point for TARDIS {}.",
                record.getTardisId(),
                exception
            );
            send(player, TextFormatting.RED, "The TARDIS transmat point could not be prepared.");
            playFailure(player);
            return false;
        }

        DimensionPosition transmat = record.getTransmatPosition();
        if (transmat == null) {
            send(player, TextFormatting.RED, "This TARDIS has no active transmat pad.");
            playFailure(player);
            return false;
        }

        boolean inside = player.dimension == transmat.getDimension();
        if (inside && distanceSquared(player, transmat.getPosition()) > MAX_INTERIOR_PAD_RANGE * MAX_INTERIOR_PAD_RANGE) {
            send(player, TextFormatting.YELLOW, "Stand near the active transmat pad before activating it.");
            playFailure(player);
            return false;
        }

        DimensionPosition exterior = record.getExteriorPosition();
        if (exterior == null) {
            send(player, TextFormatting.RED, "The TARDIS has no materialized exterior endpoint.");
            playFailure(player);
            return false;
        }

        BlockPos target;
        WorldServer targetWorld;
        float targetYaw;
        if (inside) {
            targetWorld = server.getWorld(exterior.getDimension());
            if (targetWorld == null) {
                send(player, TextFormatting.RED, "The exterior dimension is not currently available.");
                playFailure(player);
                return false;
            }
            IBlockState exteriorState = targetWorld.getBlockState(exterior.getPosition());
            if (exteriorState.getBlock() != ModBlocks.TARDIS_EXTERIOR) {
                send(player, TextFormatting.RED, "The saved TARDIS exterior is missing.");
                playFailure(player);
                return false;
            }
            EnumFacing front = exteriorState.getValue(BlockTardisExterior.FACING);
            target = TardisTeleportService.findSafeExteriorExit(targetWorld, exterior.getPosition(), front);
            targetYaw = front.getHorizontalAngle();
        } else {
            if (player.dimension != exterior.getDimension()) {
                send(player, TextFormatting.RED, "You must be in the same dimension as the landed exterior.");
                playFailure(player);
                return false;
            }
            if (distanceSquared(player, exterior.getPosition()) > MAX_EXTERIOR_RANGE * MAX_EXTERIOR_RANGE) {
                send(player, TextFormatting.YELLOW,
                    "The linked exterior is outside the transmat's " + Math.round(MAX_EXTERIOR_RANGE) + " block range.");
                playFailure(player);
                return false;
            }
            targetWorld = TardisDimensionManager.getOrLoadWorld(server);
            target = transmat.getPosition().up();
            if (!targetWorld.isAirBlock(target) || !targetWorld.isAirBlock(target.up())) {
                send(player, TextFormatting.RED, "The active interior transmat pad is obstructed.");
                playFailure(player);
                return false;
            }
            targetYaw = 180.0F;
        }

        long available = TardisEnergyService.refresh(data, record);
        if (available < ENERGY_COST || !TardisEnergyService.consume(data, record, ENERGY_COST)) {
            send(player, TextFormatting.RED,
                "Transmat needs " + ENERGY_COST + " AU, but the TARDIS has only " + available + " AU.");
            playFailure(player);
            return false;
        }

        WorldServer sourceWorld = player.getServerWorld();
        BlockPos source = player.getPosition();
        sourceWorld.playSound(null, source, ModSounds.TRANSMAT, SoundCategory.BLOCKS, 0.8F, 1.0F);
        try {
            TardisTeleportService.teleportPlayer(player, targetWorld, target, targetYaw, 0.0F);
            player.getServerWorld().playSound(
                null,
                player.getPosition(),
                ModSounds.TRANSMAT,
                SoundCategory.BLOCKS,
                0.8F,
                1.0F
            );
            send(player, TextFormatting.LIGHT_PURPLE,
                inside ? "Transmat complete: exterior endpoint." : "Transmat complete: interior endpoint.");
            return true;
        } catch (RuntimeException exception) {
            TardisEnergyService.refund(data, record, ENERGY_COST);
            RenewedTardisMod.logger.error(
                "Transmat failed for player {} and TARDIS {}.",
                player.getName(),
                record.getTardisId(),
                exception
            );
            send(player, TextFormatting.RED, "Transmat failed; its Artron cost was refunded.");
            playFailure(player);
            return false;
        }
    }

    private static double distanceSquared(EntityPlayerMP player, BlockPos pos) {
        double dx = player.posX - (pos.getX() + 0.5D);
        double dy = player.posY - (pos.getY() + 0.5D);
        double dz = player.posZ - (pos.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    private static void playFailure(EntityPlayerMP player) {
        player.getServerWorld().playSound(
            null,
            player.getPosition(),
            ModSounds.TRANSMAT_FAIL,
            SoundCategory.BLOCKS,
            0.7F,
            1.0F
        );
    }

    private static void send(EntityPlayerMP player, TextFormatting color, String message) {
        player.sendStatusMessage(
            new TextComponentString(color + "[TRANSMAT] " + TextFormatting.RESET + message),
            false
        );
    }
}
