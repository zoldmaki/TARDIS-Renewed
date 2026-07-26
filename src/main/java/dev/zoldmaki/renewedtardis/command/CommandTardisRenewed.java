package dev.zoldmaki.renewedtardis.command;

import dev.zoldmaki.renewedtardis.block.BlockTardisExterior;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.dimension.TardisDimensionManager;
import dev.zoldmaki.renewedtardis.flight.TardisFlightService;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import dev.zoldmaki.renewedtardis.item.ItemTardisKey;
import dev.zoldmaki.renewedtardis.teleport.TardisTeleportService;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisExterior;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

/** Development-facing command surface for ownership, travel, and flight testing. */
public final class CommandTardisRenewed extends CommandBase {
    @Override
    public String getName() {
        return "tardisrenewed";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("tr");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/tardisrenewed <info|create|key|exterior|interior|exit|destination|takeoff|land|count>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        String subCommand = args.length == 0 ? "info" : args[0].toLowerCase(Locale.ROOT);
        TardisWorldData data = TardisWorldData.get(server);

        switch (subCommand) {
            case "info":
                showInfo(sender, data);
                return;
            case "create":
                createRecord(sender, data);
                return;
            case "key":
                giveKey(sender, data);
                return;
            case "exterior":
                placeExterior(server, sender, data);
                return;
            case "interior":
                enterInterior(sender, data);
                return;
            case "exit":
                exitInterior(sender, data);
                return;
            case "destination":
                setDestination(sender, data, args);
                return;
            case "takeoff":
                takeOff(sender, data);
                return;
            case "land":
                land(sender, data);
                return;
            case "count":
                if (!sender.canUseCommand(2, getName())) {
                    throw new CommandException("commands.generic.permission");
                }
                sender.sendMessage(new TextComponentString(
                    TextFormatting.AQUA + "TARDIS records: " + TextFormatting.WHITE + data.size()
                ));
                return;
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }

    private static void showInfo(ICommandSender sender, TardisWorldData data) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        TardisRecord record = data.getByOwner(player.getUniqueID());

        if (record == null) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "You do not have a TARDIS record yet. Use /tr create or right-click an unbound key."
            ));
            return;
        }

        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "TARDIS ID: " + TextFormatting.WHITE + record.getTardisId()
        ));
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Owner: " + TextFormatting.WHITE + record.getOwnerName()
                + " (" + record.getOwnerId() + ")"
        ));
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Interior: " + TextFormatting.WHITE
                + (record.getInteriorPosition() == null
                    ? "not assigned"
                    : record.getInteriorPosition().toString()
                        + " [cell " + record.getInteriorIndex()
                        + ", generated=" + record.isInteriorGenerated() + "]")
        ));
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Flight: " + TextFormatting.WHITE
                + (record.isInFlight() ? "IN FLIGHT" : "LANDED")
                + ", exterior locked=" + record.isExteriorLocked()
        ));
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Exterior: " + TextFormatting.WHITE
                + (record.getExteriorPosition() == null ? "not currently placed" : record.getExteriorPosition().toString())
        ));
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Departure: " + TextFormatting.WHITE
                + (record.getFlightDeparture() == null ? "none" : record.getFlightDeparture().toString())
        ));
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Destination: " + TextFormatting.WHITE
                + (record.getDestinationPosition() == null
                    ? "not set"
                    : record.getDestinationPosition().toString() + " facing " + record.getDestinationFacing().getName())
        ));
    }

    private static void createRecord(ICommandSender sender, TardisWorldData data) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        TardisRecord existing = data.getByOwner(player.getUniqueID());
        TardisRecord record = data.getOrCreateForOwner(player.getUniqueID(), player.getName());

        String message = existing == null
            ? "Created TARDIS record " + record.getTardisId() + "."
            : "You already own TARDIS record " + record.getTardisId() + ".";
        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + message));
    }

    private static void giveKey(ICommandSender sender, TardisWorldData data) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        TardisRecord record = data.getOrCreateForOwner(player.getUniqueID(), player.getName());
        ItemStack key = ItemTardisKey.createBoundKey(record);

        if (!player.inventory.addItemStackToInventory(key)) {
            player.dropItem(key, false);
        }
        sender.sendMessage(new TextComponentString(
            TextFormatting.GOLD + "Created a key bound to TARDIS " + record.getTardisId() + "."
        ));
    }

    private static void enterInterior(ICommandSender sender, TardisWorldData data) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        TardisRecord record = data.getOrCreateForOwner(player.getUniqueID(), player.getName());
        TardisTeleportService.enterInterior(player, record);
    }

    private static void exitInterior(ICommandSender sender, TardisWorldData data) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        TardisRecord record = requireOwnedRecord(player, data);
        TardisTeleportService.exitToExterior(player, record);
    }

    private static void setDestination(
        ICommandSender sender,
        TardisWorldData data,
        String[] args
    ) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        TardisRecord record = data.getOrCreateForOwner(player.getUniqueID(), player.getName());

        if (args.length == 2 && "clear".equalsIgnoreCase(args[1])) {
            data.clearDestination(record.getTardisId());
            sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Cleared the TARDIS destination."));
            return;
        }

        int dimension;
        BlockPos target;
        if (args.length == 1) {
            dimension = player.dimension;
            RayTraceResult trace = player.rayTrace(64.0D, 1.0F);
            if (trace == null || trace.typeOfHit != RayTraceResult.Type.BLOCK || trace.sideHit == null) {
                throw new CommandException("Look at a block within 64 blocks, or provide x y z [dimension].");
            }
            target = trace.getBlockPos().offset(trace.sideHit);
        } else if (args.length == 4 || args.length == 5) {
            target = new BlockPos(parseInt(args[1]), parseInt(args[2]), parseInt(args[3]));
            dimension = args.length == 5 ? parseInt(args[4]) : player.dimension;
        } else {
            throw new WrongUsageException("/tr destination [clear|<x> <y> <z> [dimension]]");
        }

        if (dimension == TardisDimensionManager.getDimensionId()) {
            throw new CommandException("The exterior cannot land inside the TARDIS interior dimension.");
        }
        if (!DimensionManager.isDimensionRegistered(dimension)) {
            throw new CommandException("Dimension " + dimension + " is not registered.");
        }
        if (target.getY() < 1 || target.getY() > 254) {
            throw new CommandException("The destination height must be between 1 and 254.");
        }

        WorldServer targetWorld = player.getServer() == null ? null : player.getServer().getWorld(dimension);
        if (targetWorld != null && (!targetWorld.isAirBlock(target) || !targetWorld.isAirBlock(target.up()))) {
            throw new CommandException("The destination needs two clear blocks.");
        }

        EnumFacing facing = player.getHorizontalFacing().getOpposite();
        data.setDestination(record.getTardisId(), new DimensionPosition(dimension, target), facing);
        sender.sendMessage(new TextComponentString(
            TextFormatting.GOLD + "Destination set to dim " + dimension + " @ "
                + target.getX() + ", " + target.getY() + ", " + target.getZ()
                + " facing " + facing.getName() + "."
        ));
    }

    private static void takeOff(ICommandSender sender, TardisWorldData data) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        TardisRecord record = requireOwnedRecord(player, data);
        TardisFlightService.takeOff(player, record, player.getPosition());
    }

    private static void land(ICommandSender sender, TardisWorldData data) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        TardisRecord record = requireOwnedRecord(player, data);
        TardisFlightService.land(player, record, player.getPosition());
    }

    private static void placeExterior(
        MinecraftServer server,
        ICommandSender sender,
        TardisWorldData data
    ) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        WorldServer world = player.getServerWorld();
        if (world.provider.getDimension() == TardisDimensionManager.getDimensionId()) {
            throw new CommandException(
                "The TARDIS exterior cannot be placed inside the shared TARDIS interior dimension."
            );
        }

        TardisRecord record = data.getOrCreateForOwner(player.getUniqueID(), player.getName());
        if (record.isInFlight()) {
            throw new CommandException("The TARDIS is in flight. Land it instead of using the exterior debug command.");
        }

        EnumFacing playerFacing = player.getHorizontalFacing();
        BlockPos target = player.getPosition().offset(playerFacing, 2);

        DimensionPosition previous = record.getExteriorPosition();
        boolean targetIsCurrentExterior = previous != null
            && previous.getDimension() == world.provider.getDimension()
            && previous.getPosition().equals(target)
            && world.getBlockState(target).getBlock() == ModBlocks.TARDIS_EXTERIOR;

        if (!targetIsCurrentExterior && (!world.isAirBlock(target) || !world.isAirBlock(target.up()))) {
            throw new CommandException("The two-block space in front of you must be clear.");
        }

        if (previous != null && !targetIsCurrentExterior) {
            WorldServer previousWorld = server.getWorld(previous.getDimension());
            if (previousWorld != null) {
                BlockPos previousPos = previous.getPosition();
                TileEntity previousTile = previousWorld.getTileEntity(previousPos);
                if (
                    previousWorld.getBlockState(previousPos).getBlock() == ModBlocks.TARDIS_EXTERIOR
                        && previousTile instanceof TileEntityTardisExterior
                        && record.getTardisId().equals(((TileEntityTardisExterior) previousTile).getTardisId())
                ) {
                    previousWorld.setBlockToAir(previousPos);
                }
            }
        }

        IBlockState exteriorState = ModBlocks.TARDIS_EXTERIOR
            .getDefaultState()
            .withProperty(BlockTardisExterior.FACING, playerFacing.getOpposite());

        if (!targetIsCurrentExterior && !world.setBlockState(target, exteriorState, 3)) {
            throw new CommandException("The TARDIS exterior could not be placed.");
        }

        TileEntity tile = world.getTileEntity(target);
        if (!(tile instanceof TileEntityTardisExterior)) {
            world.setBlockToAir(target);
            throw new CommandException("The TARDIS exterior tile entity was not created correctly.");
        }

        data.setExteriorLocked(record.getTardisId(), true);
        ((TileEntityTardisExterior) tile).bind(record);
        data.setExteriorPosition(
            record.getTardisId(),
            new DimensionPosition(world.provider.getDimension(), target)
        );

        sender.sendMessage(new TextComponentString(
            TextFormatting.GOLD + "Placed and locked your prototype TARDIS exterior at "
                + target.getX() + ", " + target.getY() + ", " + target.getZ() + "."
        ));
    }

    private static TardisRecord requireOwnedRecord(EntityPlayerMP player, TardisWorldData data)
        throws CommandException {
        TardisRecord record = data.getByOwner(player.getUniqueID());
        if (record == null) {
            throw new CommandException("You do not own a TARDIS record.");
        }
        return record;
    }

    @Override
    public List<String> getTabCompletions(
        MinecraftServer server,
        ICommandSender sender,
        String[] args,
        @Nullable BlockPos targetPos
    ) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                Arrays.asList(
                    "info",
                    "create",
                    "key",
                    "exterior",
                    "interior",
                    "exit",
                    "destination",
                    "takeoff",
                    "land",
                    "count"
                )
            );
        }
        if (args.length == 2 && "destination".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, Collections.singletonList("clear"));
        }
        return Collections.emptyList();
    }
}
