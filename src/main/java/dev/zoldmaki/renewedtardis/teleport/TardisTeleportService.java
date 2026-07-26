package dev.zoldmaki.renewedtardis.teleport;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.block.BlockTardisExterior;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.dimension.InteriorAddress;
import dev.zoldmaki.renewedtardis.dimension.TardisDimensionManager;
import dev.zoldmaki.renewedtardis.dimension.TardisInteriorBuilder;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;

/** Server-authoritative entry and exit logic for the prototype interior. */
public final class TardisTeleportService {
    private TardisTeleportService() {
    }

    public static boolean enterInterior(EntityPlayerMP player, TardisRecord record) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        try {
            TardisWorldData data = TardisWorldData.get(server);
            DimensionPosition address = data.getOrCreateInterior(record);
            int configuredDimension = TardisDimensionManager.getDimensionId();
            if (address.getDimension() != configuredDimension) {
                RenewedTardisMod.logger.error(
                    "TARDIS {} points to interior dimension {}, but the configured dimension is {}.",
                    record.getTardisId(),
                    address.getDimension(),
                    configuredDimension
                );
                send(
                    player,
                    TextFormatting.RED,
                    "This interior belongs to dimension " + address.getDimension()
                        + ", but the config currently uses " + configuredDimension
                        + ". Restore the previous dimension ID before entering."
                );
                return false;
            }

            WorldServer interiorWorld = TardisDimensionManager.getOrLoadWorld(server);
            TardisInteriorBuilder.ensureGenerated(interiorWorld, data, record);

            BlockPos entry = InteriorAddress.getEntryPosition(address.getPosition());
            teleport(player, interiorWorld, entry, 180.0F, 0.0F);
            send(player, TextFormatting.AQUA, "You step into your TARDIS interior.");
            return true;
        } catch (RuntimeException exception) {
            RenewedTardisMod.logger.error(
                "Failed to open TARDIS interior {} for player {}.",
                record.getTardisId(),
                player.getName(),
                exception
            );
            send(player, TextFormatting.RED, "The TARDIS interior could not be opened. Check the server log.");
            return false;
        }
    }

    public static boolean exitToExterior(EntityPlayerMP player, TardisRecord record) {
        if (record.isInFlight()) {
            send(player, TextFormatting.YELLOW, "The TARDIS is in flight; the exterior door cannot open yet.");
            return false;
        }

        MinecraftServer server = player.getServer();
        DimensionPosition exteriorPosition = record.getExteriorPosition();
        if (server == null || exteriorPosition == null) {
            send(player, TextFormatting.RED, "This TARDIS has no exterior to return to.");
            return false;
        }

        WorldServer exteriorWorld = server.getWorld(exteriorPosition.getDimension());
        if (exteriorWorld == null) {
            send(player, TextFormatting.RED, "The exterior dimension is not currently available.");
            return false;
        }

        BlockPos exteriorPos = exteriorPosition.getPosition();
        if (exteriorWorld.getBlockState(exteriorPos).getBlock() != ModBlocks.TARDIS_EXTERIOR) {
            send(player, TextFormatting.RED, "The saved TARDIS exterior is missing.");
            return false;
        }

        IBlockState state = exteriorWorld.getBlockState(exteriorPos);
        EnumFacing front = state.getValue(BlockTardisExterior.FACING);
        BlockPos exit = findSafeExit(exteriorWorld, exteriorPos, front);
        teleport(player, exteriorWorld, exit, front.getHorizontalAngle(), 0.0F);
        send(player, TextFormatting.AQUA, "You step out of the TARDIS.");
        return true;
    }

    private static BlockPos findSafeExit(WorldServer world, BlockPos exterior, EnumFacing front) {
        BlockPos[] candidates = new BlockPos[] {
            exterior.offset(front),
            exterior.offset(front, 2),
            exterior.offset(front).offset(front.rotateY()),
            exterior.offset(front).offset(front.rotateYCCW()),
            exterior.up()
        };

        for (BlockPos candidate : candidates) {
            if (world.isAirBlock(candidate) && world.isAirBlock(candidate.up())) {
                return candidate;
            }
        }
        return exterior.up();
    }

    private static void teleport(
        EntityPlayerMP player,
        WorldServer targetWorld,
        BlockPos target,
        float yaw,
        float pitch
    ) {
        int targetDimension = targetWorld.provider.getDimension();
        if (player.dimension == targetDimension) {
            player.setPositionAndUpdate(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
            player.rotationYaw = yaw;
            player.rotationPitch = pitch;
            player.fallDistance = 0.0F;
            return;
        }

        player.changeDimension(
            targetDimension,
            new ExactTeleporter(targetWorld, target, yaw, pitch)
        );
    }

    private static void send(EntityPlayerMP player, TextFormatting color, String text) {
        player.sendStatusMessage(
            new TextComponentString(color + "[TARDIS] " + TextFormatting.RESET + text),
            false
        );
    }
}
