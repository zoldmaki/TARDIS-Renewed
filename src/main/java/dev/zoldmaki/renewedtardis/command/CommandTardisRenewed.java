package dev.zoldmaki.renewedtardis.command;

import dev.zoldmaki.renewedtardis.block.BlockTardisExterior;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.dimension.TardisDimensionManager;
import dev.zoldmaki.renewedtardis.flight.TardisFlightService;
import dev.zoldmaki.renewedtardis.exterior.ExteriorShell;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import dev.zoldmaki.renewedtardis.item.ItemTardisKey;
import dev.zoldmaki.renewedtardis.teleport.TardisTeleportService;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisExterior;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
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
    private static final List<String> SUB_COMMANDS = Arrays.asList(
        "info",
        "create",
        "key",
        "exterior",
        "interior",
        "exit",
        "destination",
        "flight",
        "takeoff",
        "land",
        "shell",
        "transfer",
        "as",
        "count"
    );

    private static final List<String> ADMIN_TARDIS_COMMANDS = Arrays.asList(
        "info",
        "key",
        "exterior",
        "interior",
        "exit",
        "destination",
        "flight",
        "takeoff",
        "land",
        "shell"
    );

    private static final List<String> HORIZONTAL_FACINGS = Arrays.asList(
        "north",
        "south",
        "east",
        "west"
    );

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
        return "/tardisrenewed <info|create|key|exterior|interior|exit|destination|flight|takeoff|land|shell|transfer|as|count>";
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
                showOwnInfo(sender, data);
                return;
            case "create":
                createRecord(sender, data);
                return;
            case "key": {
                EntityPlayerMP player = getCommandSenderAsPlayer(sender);
                TardisRecord record = data.getOrCreateForOwner(player.getUniqueID(), player.getName());
                giveKey(sender, record);
                return;
            }
            case "exterior": {
                EntityPlayerMP player = getCommandSenderAsPlayer(sender);
                TardisRecord record = data.getOrCreateForOwner(player.getUniqueID(), player.getName());
                placeExterior(server, sender, data, record);
                return;
            }
            case "interior": {
                EntityPlayerMP player = getCommandSenderAsPlayer(sender);
                TardisRecord record = data.getOrCreateForOwner(player.getUniqueID(), player.getName());
                enterInterior(sender, record);
                return;
            }
            case "exit":
                exitInterior(sender, requireOwnedRecord(getCommandSenderAsPlayer(sender), data));
                return;
            case "destination": {
                EntityPlayerMP player = getCommandSenderAsPlayer(sender);
                TardisRecord record = data.getOrCreateForOwner(player.getUniqueID(), player.getName());
                setDestination(sender, data, record, args, 1);
                return;
            }
            case "flight":
                showFlightStatus(sender, requireOwnedRecord(getCommandSenderAsPlayer(sender), data));
                return;
            case "takeoff":
                takeOff(sender, requireOwnedRecord(getCommandSenderAsPlayer(sender), data));
                return;
            case "land":
                land(sender, requireOwnedRecord(getCommandSenderAsPlayer(sender), data), args, 1);
                return;
            case "shell":
                setExteriorShell(server, sender, data, requireOwnedRecord(getCommandSenderAsPlayer(sender), data), args, 1);
                return;
            case "transfer":
                transferOwnership(server, sender, data, args);
                return;
            case "as":
                executeAs(server, sender, data, args);
                return;
            case "count":
                requireOperator(sender);
                sender.sendMessage(new TextComponentString(
                    TextFormatting.AQUA + "TARDIS records: " + TextFormatting.WHITE + data.size()
                ));
                return;
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }

    private static void showOwnInfo(ICommandSender sender, TardisWorldData data) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        TardisRecord record = data.getByOwner(player.getUniqueID());

        if (record == null) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "You do not have a TARDIS record yet. Use /tr create or right-click an unbound key."
            ));
            return;
        }

        showInfo(sender, record);
    }

    private static void showInfo(ICommandSender sender, TardisRecord record) {
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
                + TardisFlightService.describeFlightProgress(record, System.currentTimeMillis())
                + ", exterior locked=" + record.isExteriorLocked()
        ));
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Exterior: " + TextFormatting.WHITE
                + (record.getExteriorPosition() == null ? "not currently placed" : record.getExteriorPosition().toString())
                + ", shell=" + record.getExteriorShell().getName()
        ));
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Departure: " + TextFormatting.WHITE
                + (record.getFlightDeparture() == null
                    ? "none"
                    : record.getFlightDeparture().toString() + " facing " + record.getFlightDepartureFacing().getName())
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

    private static void giveKey(ICommandSender sender, TardisRecord record) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ItemStack key = ItemTardisKey.createBoundKey(record);

        if (!player.inventory.addItemStackToInventory(key)) {
            player.dropItem(key, false);
        }
        sender.sendMessage(new TextComponentString(
            TextFormatting.GOLD + "Created a key bound to " + record.getOwnerName()
                + "'s TARDIS " + record.getTardisId() + "."
        ));
    }

    private static void enterInterior(ICommandSender sender, TardisRecord record) throws CommandException {
        TardisTeleportService.enterInterior(getCommandSenderAsPlayer(sender), record);
    }

    private static void exitInterior(ICommandSender sender, TardisRecord record) throws CommandException {
        TardisTeleportService.exitToExterior(getCommandSenderAsPlayer(sender), record);
    }

    private static void setDestination(
        ICommandSender sender,
        TardisWorldData data,
        TardisRecord record,
        String[] args,
        int firstArgument
    ) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        int remaining = args.length - firstArgument;

        if (remaining == 1 && "clear".equalsIgnoreCase(args[firstArgument])) {
            data.clearDestination(record.getTardisId());
            sender.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "Cleared the destination for " + describeRecord(player, record) + "."
            ));
            return;
        }

        int dimension = player.dimension;
        BlockPos target;
        EnumFacing facing = record.getDestinationFacing();

        if (remaining == 0) {
            RayTraceResult trace = player.rayTrace(64.0D, 1.0F);
            if (trace == null || trace.typeOfHit != RayTraceResult.Type.BLOCK || trace.sideHit == null) {
                throw new CommandException(
                    "Look at a block within 64 blocks, use 'here', or provide x y z [dimension] [facing]."
                );
            }
            target = trace.getBlockPos().offset(trace.sideHit);
        } else if (remaining == 1 && "here".equalsIgnoreCase(args[firstArgument])) {
            target = player.getPosition();
            facing = player.getHorizontalFacing();
        } else if (remaining == 2 && "here".equalsIgnoreCase(args[firstArgument])) {
            target = player.getPosition();
            facing = parseHorizontalFacing(args[firstArgument + 1]);
        } else if (remaining >= 3 && remaining <= 5) {
            BlockPos base = player.getPosition();
            int x = parseCoordinate(args[firstArgument], base.getX(), "x");
            int y = parseCoordinate(args[firstArgument + 1], base.getY(), "y");
            int z = parseCoordinate(args[firstArgument + 2], base.getZ(), "z");
            target = new BlockPos(x, y, z);

            if (remaining == 4) {
                EnumFacing possibleFacing = tryParseHorizontalFacing(args[firstArgument + 3]);
                if (possibleFacing != null) {
                    facing = possibleFacing;
                } else {
                    dimension = parseDimension(args[firstArgument + 3]);
                }
            } else if (remaining == 5) {
                dimension = parseDimension(args[firstArgument + 3]);
                facing = parseHorizontalFacing(args[firstArgument + 4]);
            }
        } else {
            throw new WrongUsageException(
                "/tr destination [clear|here [facing]|<x|~> <y|~> <z|~> [dimension] [facing]]"
            );
        }

        validateDestination(player, dimension, target);
        data.setDestination(record.getTardisId(), new DimensionPosition(dimension, target), facing);
        sender.sendMessage(new TextComponentString(
            TextFormatting.GOLD + "Destination for " + describeRecord(player, record)
                + " set to dim " + dimension + " @ "
                + target.getX() + ", " + target.getY() + ", " + target.getZ()
                + " facing " + facing.getName() + "."
        ));
    }

    private static int parseCoordinate(String token, int base, String axis) throws CommandException {
        boolean relative = token.startsWith("~");
        String number = relative ? token.substring(1) : token;
        double value;

        try {
            double parsed = number.isEmpty() ? 0.0D : Double.parseDouble(number);
            value = relative ? base + parsed : parsed;
        } catch (NumberFormatException exception) {
            throw new CommandException("Invalid " + axis + " coordinate: " + token);
        }

        if (Double.isNaN(value) || Double.isInfinite(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new CommandException("The " + axis + " coordinate is outside the supported range: " + token);
        }
        return (int) Math.floor(value);
    }

    private static int parseDimension(String token) throws CommandException {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException exception) {
            throw new CommandException("Invalid dimension ID: " + token);
        }
    }

    private static EnumFacing parseHorizontalFacing(String token) throws CommandException {
        EnumFacing facing = tryParseHorizontalFacing(token);
        if (facing == null) {
            throw new CommandException("Facing must be north, south, east, or west.");
        }
        return facing;
    }

    @Nullable
    private static EnumFacing tryParseHorizontalFacing(String token) {
        switch (token.toLowerCase(Locale.ROOT)) {
            case "north":
            case "n":
                return EnumFacing.NORTH;
            case "south":
            case "s":
                return EnumFacing.SOUTH;
            case "east":
            case "e":
                return EnumFacing.EAST;
            case "west":
            case "w":
                return EnumFacing.WEST;
            default:
                return null;
        }
    }

    private static void validateDestination(EntityPlayerMP player, int dimension, BlockPos target)
        throws CommandException {
        if (dimension == TardisDimensionManager.getDimensionId()) {
            throw new CommandException("The exterior cannot land inside the TARDIS interior dimension.");
        }
        if (!DimensionManager.isDimensionRegistered(dimension)) {
            throw new CommandException("Dimension " + dimension + " is not registered.");
        }
        if (target.getY() < 1 || target.getY() > 254) {
            throw new CommandException("The destination height must be between 1 and 254.");
        }

        MinecraftServer server = player.getServer();
        WorldServer targetWorld = server == null ? null : server.getWorld(dimension);
        if (targetWorld != null && (!targetWorld.isAirBlock(target) || !targetWorld.isAirBlock(target.up()))) {
            throw new CommandException("The destination needs two clear blocks.");
        }
    }

    private static void takeOff(ICommandSender sender, TardisRecord record) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        TardisFlightService.takeOff(player, record, player.getPosition());
    }

    private static void showFlightStatus(ICommandSender sender, TardisRecord record) {
        long now = System.currentTimeMillis();
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Flight status: " + TextFormatting.WHITE
                + TardisFlightService.describeFlightProgress(record, now)
        ));
        if (record.getDestinationPosition() != null) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "Destination: " + TextFormatting.WHITE
                    + record.getDestinationPosition().toString()
                    + " facing " + record.getDestinationFacing().getName()
            ));
        }
        if (record.isInFlight() && record.getFlightDeparture() != null) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "Recovery departure: " + TextFormatting.WHITE
                    + record.getFlightDeparture().toString()
                    + " facing " + record.getFlightDepartureFacing().getName()
            ));
        }
    }

    private static void land(
        ICommandSender sender,
        TardisRecord record,
        String[] args,
        int firstArgument
    ) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        int remaining = args.length - firstArgument;
        TardisFlightService.LandingMode mode = TardisFlightService.LandingMode.EXACT;
        boolean force = false;

        if (remaining > 2) {
            throw new WrongUsageException("/tr land [exact|nearest|departure] [force]");
        }
        for (int index = firstArgument; index < args.length; index++) {
            String token = args[index].toLowerCase(Locale.ROOT);
            switch (token) {
                case "exact":
                    mode = TardisFlightService.LandingMode.EXACT;
                    break;
                case "nearest":
                case "safe":
                    mode = TardisFlightService.LandingMode.NEAREST_SAFE;
                    break;
                case "departure":
                case "return":
                    mode = TardisFlightService.LandingMode.DEPARTURE;
                    break;
                case "force":
                    force = true;
                    break;
                default:
                    throw new WrongUsageException("/tr land [exact|nearest|departure] [force]");
            }
        }

        if (force) {
            requireOperator(sender);
        }
        TardisFlightService.land(player, record, player.getPosition(), mode, force);
    }

    private static void placeExterior(
        MinecraftServer server,
        ICommandSender sender,
        TardisWorldData data,
        TardisRecord record
    ) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        WorldServer world = player.getServerWorld();
        if (world.provider.getDimension() == TardisDimensionManager.getDimensionId()) {
            throw new CommandException(
                "The TARDIS exterior cannot be placed inside the shared TARDIS interior dimension."
            );
        }
        if (record.isInFlight()) {
            throw new CommandException("That TARDIS is in flight. Land it instead of using the exterior debug command.");
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
            WorldServer previousWorld = getOrLoadWorld(server, previous.getDimension());
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
            .withProperty(BlockTardisExterior.FACING, playerFacing.getOpposite())
            .withProperty(BlockTardisExterior.SHELL, record.getExteriorShell());

        boolean placed = targetIsCurrentExterior
            ? BlockTardisExterior.synchronizeStructure(world, target, exteriorState)
            : BlockTardisExterior.placeStructure(world, target, exteriorState);
        if (!placed) {
            throw new CommandException("The two-block TARDIS exterior could not be placed or repaired.");
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
            TextFormatting.GOLD + "Placed and locked " + describeRecord(player, record) + " at "
                + target.getX() + ", " + target.getY() + ", " + target.getZ() + "."
        ));
    }

    private static void setExteriorShell(
        MinecraftServer server,
        ICommandSender sender,
        TardisWorldData data,
        TardisRecord record,
        String[] args,
        int firstArgument
    ) throws CommandException {
        int remaining = args.length - firstArgument;
        if (remaining == 0) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "Exterior shell: " + TextFormatting.WHITE
                    + record.getExteriorShell().getDisplayName()
                    + " (" + record.getExteriorShell().getName() + ")"
            ));
            sender.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "Available shells: prototype, police_box"
            ));
            return;
        }
        if (remaining != 1) {
            throw new WrongUsageException("/tr shell [prototype|police_box|list]");
        }

        String token = args[firstArgument];
        if ("list".equalsIgnoreCase(token)) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "Available exterior shells: " + TextFormatting.WHITE
                    + "prototype, police_box"
            ));
            return;
        }

        ExteriorShell shell = ExteriorShell.fromName(token);
        if (shell == null) {
            throw new CommandException("Unknown exterior shell '" + token + "'. Use prototype or police_box.");
        }

        ExteriorShell previousShell = record.getExteriorShell();
        DimensionPosition exteriorPosition = record.getExteriorPosition();
        WorldServer exteriorWorld = null;
        BlockPos exteriorPos = null;
        boolean updatePlacedExterior = false;

        if (!record.isInFlight() && exteriorPosition != null) {
            exteriorWorld = getOrLoadWorld(server, exteriorPosition.getDimension());
            if (exteriorWorld == null) {
                throw new CommandException("The exterior dimension could not be loaded.");
            }

            exteriorPos = exteriorPosition.getPosition();
            IBlockState state = exteriorWorld.getBlockState(exteriorPos);
            TileEntity tile = exteriorWorld.getTileEntity(exteriorPos);
            updatePlacedExterior = state.getBlock() == ModBlocks.TARDIS_EXTERIOR
                && tile instanceof TileEntityTardisExterior
                && record.getTardisId().equals(((TileEntityTardisExterior) tile).getTardisId());

            if (updatePlacedExterior) {
                IBlockState upperState = exteriorWorld.getBlockState(exteriorPos.up());
                if (
                    upperState.getBlock() != ModBlocks.TARDIS_EXTERIOR_TOP
                        && !upperState.getMaterial().isReplaceable()
                ) {
                    throw new CommandException(
                        "The exterior shell could not change because its upper half is obstructed."
                    );
                }
            }
        }

        // Save first so any tile entity recreated by the state change reads the new shell.
        if (!data.setExteriorShell(record.getTardisId(), shell)) {
            throw new CommandException("The exterior shell selection could not be saved.");
        }

        if (updatePlacedExterior && exteriorWorld != null && exteriorPos != null) {
            if (!BlockTardisExterior.applyShell(exteriorWorld, exteriorPos, shell)) {
                data.setExteriorShell(record.getTardisId(), previousShell);
                BlockTardisExterior.applyShell(exteriorWorld, exteriorPos, previousShell);
                throw new CommandException("The exterior shell could not be changed safely.");
            }

            TileEntity updatedTile = exteriorWorld.getTileEntity(exteriorPos);
            if (!(updatedTile instanceof TileEntityTardisExterior)) {
                data.setExteriorShell(record.getTardisId(), previousShell);
                BlockTardisExterior.applyShell(exteriorWorld, exteriorPos, previousShell);
                throw new CommandException("The exterior lost its tile entity while changing shell; the change was rolled back.");
            }

            TileEntityTardisExterior exterior = (TileEntityTardisExterior) updatedTile;
            exterior.bind(record);
            exterior.setLocked(record.isExteriorLocked());
        }

        sender.sendMessage(new TextComponentString(
            TextFormatting.GREEN + "Exterior shell for " + record.getOwnerName() + "'s TARDIS set to "
                + shell.getDisplayName() + " (" + shell.getName() + ")."
                + (record.isInFlight() ? " It will be used on the next landing." : "")
        ));
    }

    private static void transferOwnership(
        MinecraftServer server,
        ICommandSender sender,
        TardisWorldData data,
        String[] args
    ) throws CommandException {
        requireOperator(sender);
        if (args.length != 3) {
            throw new WrongUsageException("/tr transfer <currentOwner> <newOwner>");
        }

        TardisRecord record = resolveRecord(server, data, args[1]);
        EntityPlayerMP newOwner = server.getPlayerList().getPlayerByUsername(args[2]);
        if (newOwner == null) {
            throw new CommandException("The new owner must be online so their UUID can be resolved safely.");
        }

        TardisRecord conflicting = data.getByOwner(newOwner.getUniqueID());
        if (conflicting != null && !conflicting.getTardisId().equals(record.getTardisId())) {
            throw new CommandException(
                newOwner.getName() + " already owns TARDIS " + conflicting.getTardisId()
                    + ". Transfer or remove that record first."
            );
        }

        String oldOwnerName = record.getOwnerName();
        UUID oldOwnerId = record.getOwnerId();
        if (!data.transferOwnership(record.getTardisId(), newOwner.getUniqueID(), newOwner.getName())) {
            throw new CommandException("The ownership transfer could not be saved.");
        }
        synchronizePlacedExteriorOwner(server, record);

        sender.sendMessage(new TextComponentString(
            TextFormatting.GREEN + "Transferred TARDIS " + record.getTardisId() + " from "
                + oldOwnerName + " (" + oldOwnerId + ") to "
                + newOwner.getName() + " (" + newOwner.getUniqueID() + ")."
        ));
        if (sender != newOwner) {
            newOwner.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "You now own TARDIS " + record.getTardisId() + "."
            ));
        }
    }

    private static void executeAs(
        MinecraftServer server,
        ICommandSender sender,
        TardisWorldData data,
        String[] args
    ) throws CommandException {
        requireOperator(sender);
        if (args.length < 3) {
            throw new WrongUsageException(
                "/tr as <owner> <info|key|exterior|interior|exit|destination|flight|takeoff|land|shell> [...]"
            );
        }

        TardisRecord record = resolveRecord(server, data, args[1]);
        String subCommand = args[2].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "info":
                showInfo(sender, record);
                return;
            case "key":
                giveKey(sender, record);
                return;
            case "exterior":
                placeExterior(server, sender, data, record);
                return;
            case "interior":
                enterInterior(sender, record);
                return;
            case "exit":
                exitInterior(sender, record);
                return;
            case "destination":
                setDestination(sender, data, record, args, 3);
                return;
            case "flight":
                showFlightStatus(sender, record);
                return;
            case "takeoff":
                takeOff(sender, record);
                return;
            case "land":
                land(sender, record, args, 3);
                return;
            case "shell":
                setExteriorShell(server, sender, data, record, args, 3);
                return;
            default:
                throw new WrongUsageException(
                    "/tr as <owner> <info|key|exterior|interior|exit|destination|flight|takeoff|land|shell> [...]"
                );
        }
    }

    private static TardisRecord resolveRecord(MinecraftServer server, TardisWorldData data, String token)
        throws CommandException {
        TardisRecord byName = data.getByOwnerName(token);
        if (byName != null) {
            return byName;
        }

        EntityPlayerMP online = server.getPlayerList().getPlayerByUsername(token);
        if (online != null) {
            TardisRecord byOnlineOwner = data.getByOwner(online.getUniqueID());
            if (byOnlineOwner != null) {
                return byOnlineOwner;
            }
        }

        try {
            UUID id = UUID.fromString(token);
            TardisRecord byTardisId = data.getById(id);
            if (byTardisId != null) {
                return byTardisId;
            }
            TardisRecord byOwnerId = data.getByOwner(id);
            if (byOwnerId != null) {
                return byOwnerId;
            }
        } catch (IllegalArgumentException ignored) {
            // The token was a normal player name rather than a UUID.
        }

        throw new CommandException("No TARDIS record was found for owner or UUID '" + token + "'.");
    }

    private static void synchronizePlacedExteriorOwner(MinecraftServer server, TardisRecord record) {
        DimensionPosition exteriorPosition = record.getExteriorPosition();
        if (exteriorPosition == null) {
            return;
        }

        WorldServer world = getOrLoadWorld(server, exteriorPosition.getDimension());
        if (world == null) {
            return;
        }

        TileEntity tile = world.getTileEntity(exteriorPosition.getPosition());
        if (
            tile instanceof TileEntityTardisExterior
                && record.getTardisId().equals(((TileEntityTardisExterior) tile).getTardisId())
        ) {
            ((TileEntityTardisExterior) tile).bind(record);
        }
    }

    @Nullable
    private static WorldServer getOrLoadWorld(MinecraftServer server, int dimension) {
        WorldServer world = server.getWorld(dimension);
        if (world != null) {
            return world;
        }
        if (!DimensionManager.isDimensionRegistered(dimension)) {
            return null;
        }
        DimensionManager.initDimension(dimension);
        return server.getWorld(dimension);
    }

    private static TardisRecord requireOwnedRecord(EntityPlayerMP player, TardisWorldData data)
        throws CommandException {
        TardisRecord record = data.getByOwner(player.getUniqueID());
        if (record == null) {
            throw new CommandException("You do not own a TARDIS record.");
        }
        return record;
    }

    private static void requireOperator(ICommandSender sender) throws CommandException {
        if (!sender.canUseCommand(2, "tardisrenewed")) {
            throw new CommandException("commands.generic.permission");
        }
    }

    private static String describeRecord(EntityPlayerMP actor, TardisRecord record) {
        return record.getOwnerId().equals(actor.getUniqueID())
            ? "your TARDIS"
            : record.getOwnerName() + "'s TARDIS";
    }

    @Override
    public List<String> getTabCompletions(
        MinecraftServer server,
        ICommandSender sender,
        String[] args,
        @Nullable BlockPos targetPos
    ) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUB_COMMANDS);
        }

        if ("destination".equalsIgnoreCase(args[0])) {
            return destinationTabCompletions(args, 1);
        }

        if ("land".equalsIgnoreCase(args[0])) {
            return landTabCompletions(args, 1);
        }

        if ("shell".equalsIgnoreCase(args[0])) {
            return shellTabCompletions(args, 1);
        }

        if ("transfer".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return getListOfStringsMatchingLastWord(args, ownerCandidates(server));
            }
            if (args.length == 3) {
                return getListOfStringsMatchingLastWord(args, Arrays.asList(server.getOnlinePlayerNames()));
            }
            return Collections.emptyList();
        }

        if ("as".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return getListOfStringsMatchingLastWord(args, ownerCandidates(server));
            }
            if (args.length == 3) {
                return getListOfStringsMatchingLastWord(args, ADMIN_TARDIS_COMMANDS);
            }
            if (args.length >= 4 && "destination".equalsIgnoreCase(args[2])) {
                return destinationTabCompletions(args, 3);
            }
            if (args.length >= 4 && "land".equalsIgnoreCase(args[2])) {
                return landTabCompletions(args, 3);
            }
            if (args.length >= 4 && "shell".equalsIgnoreCase(args[2])) {
                return shellTabCompletions(args, 3);
            }
        }

        return Collections.emptyList();
    }

    private static List<String> destinationTabCompletions(String[] args, int firstArgument) {
        int remaining = args.length - firstArgument;
        if (remaining == 1) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("clear", "here", "~"));
        }
        if (remaining == 2 && "here".equalsIgnoreCase(args[firstArgument])) {
            return getListOfStringsMatchingLastWord(args, HORIZONTAL_FACINGS);
        }
        if (remaining >= 2 && remaining <= 3 && !"clear".equalsIgnoreCase(args[firstArgument])) {
            return getListOfStringsMatchingLastWord(args, Collections.singletonList("~"));
        }
        if (remaining == 4 || remaining == 5) {
            return getListOfStringsMatchingLastWord(args, HORIZONTAL_FACINGS);
        }
        return Collections.emptyList();
    }

    private static List<String> landTabCompletions(String[] args, int firstArgument) {
        int remaining = args.length - firstArgument;
        if (remaining == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                Arrays.asList("exact", "nearest", "departure", "force")
            );
        }
        if (remaining == 2) {
            return getListOfStringsMatchingLastWord(args, Collections.singletonList("force"));
        }
        return Collections.emptyList();
    }

    private static List<String> shellTabCompletions(String[] args, int firstArgument) {
        if (args.length - firstArgument == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                Arrays.asList("prototype", "police_box", "list")
            );
        }
        return Collections.emptyList();
    }

    private static List<String> ownerCandidates(MinecraftServer server) {
        Set<String> names = new LinkedHashSet<>();
        TardisWorldData data = TardisWorldData.get(server);
        for (TardisRecord record : data.getRecords()) {
            if (!record.getOwnerName().isEmpty()) {
                names.add(record.getOwnerName());
            }
        }
        names.addAll(Arrays.asList(server.getOnlinePlayerNames()));
        return new ArrayList<>(names);
    }
}
