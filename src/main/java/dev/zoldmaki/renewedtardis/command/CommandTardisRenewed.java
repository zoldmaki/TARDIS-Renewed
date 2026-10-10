package dev.zoldmaki.renewedtardis.command;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.access.TardisAccessMode;
import dev.zoldmaki.renewedtardis.access.TardisCrewMember;
import dev.zoldmaki.renewedtardis.access.TardisPermission;
import dev.zoldmaki.renewedtardis.block.BlockTardisExterior;
import dev.zoldmaki.renewedtardis.config.TardisConfig;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisRoomConnection;
import dev.zoldmaki.renewedtardis.data.TardisRoomRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.damage.TardisDamageService;
import dev.zoldmaki.renewedtardis.damage.TardisSubsystem;
import dev.zoldmaki.renewedtardis.dimension.TardisDimensionManager;
import dev.zoldmaki.renewedtardis.dimension.TardisInteriorBuilder;
import dev.zoldmaki.renewedtardis.energy.TardisEnergyService;
import dev.zoldmaki.renewedtardis.flight.FlightAccuracy;
import dev.zoldmaki.renewedtardis.flight.FlightSpeed;
import dev.zoldmaki.renewedtardis.flight.TardisFlightService;
import dev.zoldmaki.renewedtardis.exterior.ExteriorShell;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import dev.zoldmaki.renewedtardis.init.ModItems;
import dev.zoldmaki.renewedtardis.interior.InteriorRoomManager;
import dev.zoldmaki.renewedtardis.interior.InteriorRoomSchema;
import dev.zoldmaki.renewedtardis.integration.IntegrationManager;
import dev.zoldmaki.renewedtardis.item.ItemTardisKey;
import dev.zoldmaki.renewedtardis.item.ItemTardisUpgrade;
import dev.zoldmaki.renewedtardis.maintenance.TardisMaintenanceService;
import dev.zoldmaki.renewedtardis.maintenance.TardisMaintenanceService.DiagnosticReport;
import dev.zoldmaki.renewedtardis.maintenance.TardisMaintenanceService.Finding;
import dev.zoldmaki.renewedtardis.maintenance.TardisMaintenanceService.RepairResult;
import dev.zoldmaki.renewedtardis.upgrade.TardisUpgradeService;
import dev.zoldmaki.renewedtardis.upgrade.TardisUpgradeType;
import dev.zoldmaki.renewedtardis.teleport.TardisTeleportService;
import dev.zoldmaki.renewedtardis.transmat.TardisTransmatService;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisExterior;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
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
        "room",
        "transmat",
        "energy",
        "systems",
        "upgrades",
        "crew",
        "access",
        "integrations",
        "doctor",
        "repair",
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
        "shell",
        "room",
        "transmat",
        "energy",
        "systems",
        "upgrades",
        "crew",
        "access",
        "doctor",
        "repair"
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
        return "/tardisrenewed <info|create|key|exterior|interior|exit|destination|flight|takeoff|land|shell|room|transmat|energy|systems|upgrades|crew|access|integrations|doctor|repair|transfer|as|count>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        // The command root is intentionally available to normal players. Each
        // destructive or administrative branch performs its own operator check.
        return true;
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
                manageFlight(
                    sender,
                    data,
                    requireOwnedRecord(getCommandSenderAsPlayer(sender), data),
                    args,
                    1
                );
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
            case "room":
                manageRooms(server, sender, data, requireOwnedRecord(getCommandSenderAsPlayer(sender), data), args, 1);
                return;
            case "transmat":
                manageTransmat(sender, data, requireOwnedRecord(getCommandSenderAsPlayer(sender), data), args, 1);
                return;
            case "energy":
                manageEnergy(sender, data, requireOwnedRecord(getCommandSenderAsPlayer(sender), data), args, 1);
                return;
            case "systems":
                manageSystems(sender, data, requireOwnedRecord(getCommandSenderAsPlayer(sender), data), args, 1);
                return;
            case "upgrades":
                manageUpgrades(sender, data, requireOwnedRecord(getCommandSenderAsPlayer(sender), data), args, 1);
                return;
            case "crew":
                manageCrew(server, sender, data, requireOwnedRecord(getCommandSenderAsPlayer(sender), data), args, 1);
                return;
            case "access":
                manageAccess(sender, data, requireOwnedRecord(getCommandSenderAsPlayer(sender), data), args, 1);
                return;
            case "integrations":
                showIntegrationStatus(sender);
                return;
            case "doctor":
                runDoctor(
                    server,
                    sender,
                    requireOwnedRecord(getCommandSenderAsPlayer(sender), data),
                    args,
                    1
                );
                return;
            case "repair":
                manageRepair(
                    server,
                    sender,
                    data,
                    requireOwnedRecord(getCommandSenderAsPlayer(sender), data),
                    args,
                    1
                );
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

    private static void showIntegrationStatus(ICommandSender sender) {
        sender.sendMessage(new TextComponentString(
            TextFormatting.GOLD + "[TARDIS INTEGRATIONS] " + TextFormatting.GRAY
                + "Optional hooks on the " + IntegrationManager.describeSide() + "."
        ));

        String hwylaState;
        if (!TardisConfig.isHwylaIntegrationEnabled()) {
            hwylaState = "disabled in config";
        } else if (!IntegrationManager.isHwylaInstalled()) {
            hwylaState = "not installed";
        } else if (IntegrationManager.isHwylaRegistrationQueued()) {
            hwylaState = "registration queued";
        } else {
            hwylaState = "detected, registration unavailable";
        }
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "HWYLA/Waila: " + TextFormatting.WHITE + hwylaState
        ));

        String computerState;
        if (!TardisConfig.isComputerCraftIntegrationEnabled()) {
            computerState = "disabled in config";
        } else if (!IntegrationManager.isComputerCraftInstalled()) {
            computerState = "not installed";
        } else if (IntegrationManager.isComputerCraftRegistered()) {
            computerState = "registered — read-only engine-core peripheral";
        } else {
            computerState = "detected, registration failed";
        }
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "ComputerCraft: " + TextFormatting.WHITE + computerState
        ));
        if (!IntegrationManager.getComputerCraftError().isEmpty()) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.RED + "ComputerCraft detail: " + IntegrationManager.getComputerCraftError()
            ));
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

        showInfo(sender, data, record);
    }

    private static void showInfo(ICommandSender sender, TardisWorldData data, TardisRecord record) {
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "TARDIS ID: " + TextFormatting.WHITE + record.getTardisId()
        ));
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Owner: " + TextFormatting.WHITE + record.getOwnerName()
                + " (" + record.getOwnerId() + ")"
        ));
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Access: " + TextFormatting.WHITE
                + record.getAccessMode().getDisplayName() + ", crew=" + record.getCrew().size()
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
            TextFormatting.AQUA + "Rooms: " + TextFormatting.WHITE
                + "1 protected console room + " + record.getRooms().size() + "/"
                + TardisUpgradeService.getRoomLimit(record) + " expandable room(s), "
                + record.getRoomConnections().size() + " secondary corridor(s)"
        ));
        TardisEnergyService.refresh(data, record);
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Transmat: " + TextFormatting.WHITE
                + (record.getTransmatPosition() == null ? "not assigned" : record.getTransmatPosition().toString())
        ));
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Artron energy: " + TextFormatting.WHITE
                + TardisEnergyService.describe(record)
        ));
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Systems: " + TextFormatting.WHITE
                + TardisDamageService.describe(record)
        ));
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Upgrades: " + TextFormatting.WHITE
                + TardisUpgradeService.describe(record)
        ));
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Flight: " + TextFormatting.WHITE
                + TardisFlightService.describeFlightProgress(record, System.currentTimeMillis())
                + ", exterior locked=" + record.isExteriorLocked()
                + ", " + TardisFlightService.describeFlightProfile(record)
        ));
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Console: " + TextFormatting.WHITE
                + "takeoff sequence " + record.getConsoleState().getTakeoffSequenceProgress() + "/4, "
                + record.getConsoleState().getSavedStates().size() + " non-default physical control state(s)"
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
        if (record.isInFlight() && record.getFlightResolvedDestination() != null) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "Resolved landing target: " + TextFormatting.WHITE
                    + record.getFlightResolvedDestination().toString()
            ));
        }
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
        giveKeyToPlayer(player, record);
        sender.sendMessage(new TextComponentString(
            TextFormatting.GOLD + "Created a key bound to " + record.getOwnerName()
                + "'s TARDIS " + record.getTardisId() + "."
        ));
    }

    private static void giveKeyToPlayer(EntityPlayerMP player, TardisRecord record) {
        ItemStack key = ItemTardisKey.createBoundKey(record);
        if (!player.inventory.addItemStackToInventory(key)) {
            player.dropItem(key, false);
        }
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

        validateDestination(player, record, dimension, target);
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

    private static void validateDestination(
        EntityPlayerMP player,
        TardisRecord record,
        int dimension,
        BlockPos target
    ) throws CommandException {
        if (dimension == TardisDimensionManager.getDimensionId()) {
            throw new CommandException("The exterior cannot land inside the TARDIS interior dimension.");
        }
        if (!DimensionManager.isDimensionRegistered(dimension)) {
            throw new CommandException("Dimension " + dimension + " is not registered.");
        }
        if (TardisConfig.isDestinationDimensionBlocked(dimension)) {
            throw new CommandException("Dimension " + dimension + " is blocked by the TARDIS flight config.");
        }
        DimensionPosition departure = record.isInFlight()
            ? record.getFlightDeparture()
            : record.getExteriorPosition();
        if (departure != null) {
            String restriction = TardisConfig.describeFlightRestriction(departure.getDimension(), dimension);
            if (!restriction.isEmpty()) {
                throw new CommandException(restriction);
            }
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

    private static void manageFlight(
        ICommandSender sender,
        TardisWorldData data,
        TardisRecord record,
        String[] args,
        int firstArgument
    ) throws CommandException {
        int remaining = args.length - firstArgument;
        if (remaining == 0 || (remaining == 1 && "status".equalsIgnoreCase(args[firstArgument]))) {
            showFlightStatus(sender, data, record);
            return;
        }

        String action = args[firstArgument].toLowerCase(Locale.ROOT);
        if ("target".equals(action)) {
            if (remaining != 1) {
                throw new WrongUsageException("/tr flight target");
            }
            DimensionPosition target = record.isInFlight()
                && record.getFlightResolvedDestination() != null
                    ? record.getFlightResolvedDestination()
                    : record.getDestinationPosition();
            sender.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "Flight target: " + TextFormatting.WHITE
                    + (target == null ? "not set" : target.toString())
            ));
            return;
        }

        if (record.isInFlight()) {
            throw new CommandException("Flight speed and accuracy cannot be changed while the TARDIS is in flight.");
        }

        if ("speed".equals(action)) {
            if (remaining == 1) {
                sender.sendMessage(new TextComponentString(
                    TextFormatting.AQUA + "Flight speed: " + TextFormatting.WHITE
                        + record.getFlightSpeed().getName()
                ));
                return;
            }
            if (remaining != 2) {
                throw new WrongUsageException("/tr flight speed <slow|normal|fast>");
            }
            FlightSpeed speed = FlightSpeed.fromName(args[firstArgument + 1]);
            if (speed == null) {
                throw new CommandException("Flight speed must be slow, normal, or fast.");
            }
            if (!data.setFlightSpeed(record.getTardisId(), speed)) {
                throw new CommandException("The flight speed could not be saved.");
            }
            sender.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "Flight speed set to " + speed.getName()
                    + " (" + speed.getDurationPercent() + "% normal travel time)."
            ));
            return;
        }

        if ("accuracy".equals(action)) {
            if (remaining == 1) {
                sender.sendMessage(new TextComponentString(
                    TextFormatting.AQUA + "Flight accuracy: " + TextFormatting.WHITE
                        + record.getFlightAccuracy().getName()
                ));
                return;
            }
            if (remaining != 2) {
                throw new WrongUsageException("/tr flight accuracy <precise|normal|risky>");
            }
            FlightAccuracy accuracy = FlightAccuracy.fromName(args[firstArgument + 1]);
            if (accuracy == null) {
                throw new CommandException("Flight accuracy must be precise, normal, or risky.");
            }
            if (!data.setFlightAccuracy(record.getTardisId(), accuracy)) {
                throw new CommandException("The flight accuracy could not be saved.");
            }
            sender.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "Flight accuracy set to " + accuracy.getName()
                    + " (maximum horizontal drift " + accuracy.getLandingRadius() + " block(s))."
            ));
            return;
        }

        throw new WrongUsageException(
            "/tr flight [status|target|speed <slow|normal|fast>|accuracy <precise|normal|risky>]"
        );
    }

    private static void showFlightStatus(ICommandSender sender, TardisWorldData data, TardisRecord record) {
        long now = System.currentTimeMillis();
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Flight status: " + TextFormatting.WHITE
                + TardisFlightService.describeFlightProgress(record, now)
        ));
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Flight profile: " + TextFormatting.WHITE
                + TardisFlightService.describeFlightProfile(record)
        ));
        if (record.getDestinationPosition() != null) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "Destination: " + TextFormatting.WHITE
                    + record.getDestinationPosition().toString()
                    + " facing " + record.getDestinationFacing().getName()
            ));
        }
        if (record.isInFlight() && record.getFlightResolvedDestination() != null) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "Resolved landing target: " + TextFormatting.WHITE
                    + record.getFlightResolvedDestination().toString()
            ));
        }
        if (record.isInFlight() && record.getFlightDeparture() != null) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "Recovery departure: " + TextFormatting.WHITE
                    + record.getFlightDeparture().toString()
                    + " facing " + record.getFlightDepartureFacing().getName()
            ));
        }
        TardisEnergyService.refresh(data, record);
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Artron energy: " + TextFormatting.WHITE
                + TardisEnergyService.describe(record)
        ));
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Systems: " + TextFormatting.WHITE
                + TardisDamageService.describe(record)
        ));
        DimensionPosition departure = record.isInFlight()
            ? record.getFlightDeparture()
            : record.getExteriorPosition();
        if (departure != null && record.getDestinationPosition() != null) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "Current route cost: " + TextFormatting.WHITE
                    + TardisEnergyService.format(
                        TardisEnergyService.calculateFlightCost(record, departure, record.getDestinationPosition())
                    ) + " AU"
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
            throw new WrongUsageException("/tr land [exact|surface|solid|nearest|departure] [force]");
        }
        for (int index = firstArgument; index < args.length; index++) {
            String token = args[index].toLowerCase(Locale.ROOT);
            switch (token) {
                case "exact":
                    mode = TardisFlightService.LandingMode.EXACT;
                    break;
                case "surface":
                case "top":
                    mode = TardisFlightService.LandingMode.SURFACE;
                    break;
                case "solid":
                case "ground":
                    mode = TardisFlightService.LandingMode.NEAREST_SOLID;
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
                    throw new WrongUsageException("/tr land [exact|surface|solid|nearest|departure] [force]");
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

    private static void manageRooms(
        MinecraftServer server,
        ICommandSender sender,
        TardisWorldData data,
        TardisRecord record,
        String[] args,
        int firstArgument
    ) throws CommandException {
        int remaining = args.length - firstArgument;
        String action = remaining == 0 ? "list" : args[firstArgument].toLowerCase(Locale.ROOT);

        switch (action) {
            case "list":
                if (remaining > 1) {
                    throw new WrongUsageException("/tr room list");
                }
                listRooms(sender, record);
                return;
            case "schemas":
            case "schema":
                if (remaining > 1) {
                    throw new WrongUsageException("/tr room schemas");
                }
                listRoomSchemas(sender, record);
                return;
            case "add":
                addRoom(server, sender, data, record, args, firstArgument + 1);
                return;
            case "remove":
            case "delete":
                removeRoom(server, sender, data, record, args, firstArgument + 1);
                return;
            case "connections":
                if (remaining > 1) {
                    throw new WrongUsageException("/tr room connections");
                }
                listRoomConnections(sender, record);
                return;
            case "connect":
                connectRooms(server, sender, data, record, args, firstArgument + 1);
                return;
            case "disconnect":
                disconnectRooms(server, sender, data, record, args, firstArgument + 1);
                return;
            case "tp":
            case "teleport":
                teleportToRoom(sender, record, args, firstArgument + 1);
                return;
            default:
                throw new WrongUsageException(
                    "/tr room <list|schemas|connections|add <schema> <direction> [parentId]|connect <firstId> <secondId>|disconnect <firstId> <secondId> [force]|remove <id> [force]|tp <id>>"
                );
        }
    }

    private static void listRooms(ICommandSender sender, TardisRecord record) {
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Expandable room capacity: " + TextFormatting.WHITE
                + record.getRooms().size() + "/" + TardisUpgradeService.getRoomLimit(record)
        ));
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Room 0: " + TextFormatting.WHITE
                + "Protected Console Room at grid 0,0"
        ));
        if (record.getRooms().isEmpty()) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "No expandable rooms have been installed. Use /tr room schemas first."
            ));
            return;
        }
        for (TardisRoomRecord room : record.getRooms()) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "Room " + room.getRoomId() + ": " + TextFormatting.WHITE
                    + room.getSchemaName() + " at grid " + room.getGridX() + "," + room.getGridZ()
                    + " from room " + room.getParentRoomId()
                    + " toward " + room.getConnectionFacing().getName()
                    + " [generated=" + room.isGenerated() + "]"
            ));
        }
        if (!record.getRoomConnections().isEmpty()) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "Secondary corridors: " + TextFormatting.WHITE
                    + record.getRoomConnections().size()
            ));
        }
    }

    private static void listRoomSchemas(ICommandSender sender, TardisRecord record) {
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Available interior schemas:"
        ));
        for (InteriorRoomSchema schema : InteriorRoomSchema.valuesList()) {
            int width = schema.getHalfWidth() * 2 + 1;
            int depth = schema.getHalfDepth() * 2 + 1;
            sender.sendMessage(new TextComponentString(
                TextFormatting.GOLD + schema.getName() + TextFormatting.WHITE
                    + " - " + schema.getDisplayName() + " (" + width + "x" + depth
                    + ", height " + schema.getHeight() + ", cost "
                    + TardisEnergyService.format(
                        TardisUpgradeService.applyRoomCost(record, schema.getEnergyCost())
                    ) + " AU)"
            ));
        }
    }

    private static void addRoom(
        MinecraftServer server,
        ICommandSender sender,
        TardisWorldData data,
        TardisRecord record,
        String[] args,
        int firstArgument
    ) throws CommandException {
        int remaining = args.length - firstArgument;
        if (remaining < 2 || remaining > 3) {
            throw new WrongUsageException("/tr room add <schema> <north|south|east|west> [parentId]");
        }

        InteriorRoomSchema schema = InteriorRoomSchema.fromName(args[firstArgument]);
        if (schema == null) {
            throw new CommandException(
                "Unknown room schema '" + args[firstArgument] + "'. Use /tr room schemas."
            );
        }
        EnumFacing direction = parseHorizontalFacing(args[firstArgument + 1]);
        int roomLimit = TardisUpgradeService.getRoomLimit(record);
        if (record.getRooms().size() >= roomLimit) {
            throw new CommandException(
                "This TARDIS has reached its expandable-room limit of " + roomLimit
                    + ". Install an Architectural Expansion upgrade."
            );
        }
        int parentRoomId = remaining == 3 ? parseRoomId(args[firstArgument + 2]) : InteriorRoomManager.MAIN_ROOM_ID;
        if (parentRoomId != InteriorRoomManager.MAIN_ROOM_ID && record.getRoom(parentRoomId) == null) {
            throw new CommandException("Parent room " + parentRoomId + " does not exist.");
        }

        int gridX = InteriorRoomManager.getTargetGridX(record, parentRoomId, direction);
        int gridZ = InteriorRoomManager.getTargetGridZ(record, parentRoomId, direction);
        if (gridX == 0 && gridZ == 0) {
            throw new CommandException("The protected console room already occupies grid 0,0.");
        }
        if (!InteriorRoomManager.isGridWithinCell(gridX, gridZ)) {
            throw new CommandException("That room would exceed the safe boundary of this TARDIS interior cell.");
        }
        TardisRoomRecord occupied = record.getRoomAtGrid(gridX, gridZ);
        if (occupied != null) {
            throw new CommandException(
                "Room " + occupied.getRoomId() + " already occupies grid " + gridX + "," + gridZ + "."
            );
        }

        WorldServer world = prepareInteriorWorld(server, data, record);
        long energyCost = TardisUpgradeService.applyRoomCost(record, schema.getEnergyCost());
        long availableEnergy = TardisEnergyService.refresh(data, record);
        if (availableEnergy < energyCost) {
            throw new CommandException(
                "Installing " + schema.getDisplayName() + " needs "
                    + TardisEnergyService.format(energyCost) + " AU, but this TARDIS has only "
                    + TardisEnergyService.format(availableEnergy) + " AU."
            );
        }
        if (!TardisEnergyService.consume(data, record, energyCost)) {
            throw new CommandException("The room's Artron energy cost could not be reserved.");
        }

        TardisRoomRecord created = data.addRoom(
            record.getTardisId(),
            schema.getName(),
            gridX,
            gridZ,
            parentRoomId,
            direction
        );
        if (created == null) {
            TardisEnergyService.refund(data, record, energyCost);
            throw new CommandException("The room record could not be created. Its Artron cost was refunded.");
        }

        try {
            InteriorRoomManager.ensureGeneratedRooms(world, data, record);
        } catch (RuntimeException exception) {
            RenewedTardisMod.logger.error(
                "Failed to generate room {} for TARDIS {}.",
                created.getRoomId(),
                record.getTardisId(),
                exception
            );
            try {
                InteriorRoomManager.removeRoomStructure(world, record, created);
            } catch (RuntimeException cleanupException) {
                RenewedTardisMod.logger.error(
                    "Failed to clean partial room {} after generation failure.",
                    created.getRoomId(),
                    cleanupException
                );
            }
            data.removeRoom(record.getTardisId(), created.getRoomId());
            TardisEnergyService.refund(data, record, energyCost);
            throw new CommandException(
                "Room generation failed; its saved record was rolled back and its Artron cost was refunded. Check the server log."
            );
        }

        sender.sendMessage(new TextComponentString(
            TextFormatting.GREEN + "Installed " + schema.getDisplayName() + " as room "
                + created.getRoomId() + " at grid " + gridX + "," + gridZ
                + ", connected " + direction.getName() + " of room " + parentRoomId
                + ". Cost: " + TardisEnergyService.format(energyCost) + " AU; remaining: "
                + TardisEnergyService.format(record.getArtronEnergy()) + " AU."
        ));
    }

    private static void removeRoom(
        MinecraftServer server,
        ICommandSender sender,
        TardisWorldData data,
        TardisRecord record,
        String[] args,
        int firstArgument
    ) throws CommandException {
        int remaining = args.length - firstArgument;
        if (remaining < 1 || remaining > 2) {
            throw new WrongUsageException("/tr room remove <id> [force]");
        }

        int roomId = parseRoomId(args[firstArgument]);
        if (roomId == InteriorRoomManager.MAIN_ROOM_ID) {
            throw new CommandException("Room 0 is the protected console room and cannot be removed.");
        }
        TardisRoomRecord room = record.getRoom(roomId);
        if (room == null) {
            throw new CommandException("Room " + roomId + " does not exist.");
        }
        int childCount = InteriorRoomManager.countChildren(record, roomId);
        if (childCount > 0) {
            throw new CommandException(
                "Room " + roomId + " still has " + childCount + " child room(s). Remove those first."
            );
        }
        int secondaryCount = InteriorRoomManager.countSecondaryConnections(record, roomId);
        if (secondaryCount > 0) {
            throw new CommandException(
                "Room " + roomId + " still has " + secondaryCount
                    + " secondary corridor connection(s). Disconnect those first."
            );
        }

        boolean force = remaining == 2 && "force".equalsIgnoreCase(args[firstArgument + 1]);
        if (remaining == 2 && !force) {
            throw new WrongUsageException("/tr room remove <id> [force]");
        }
        if (force) {
            requireOperator(sender);
        }

        WorldServer world = prepareInteriorWorld(server, data, record);
        List<EntityPlayerMP> players = InteriorRoomManager.getPlayersInRemovalArea(world, record, room);
        if (!players.isEmpty() && !force) {
            throw new CommandException(
                "Room " + roomId + " or its corridor contains " + players.size()
                    + " player(s). Move them out or use operator-only force."
            );
        }
        if (!players.isEmpty()) {
            InteriorRoomManager.movePlayersToMainRoom(players, record);
        }

        InteriorRoomManager.removeRoomStructure(world, record, room);
        if (!data.removeRoom(record.getTardisId(), roomId)) {
            throw new CommandException("The room structure was cleared, but its saved record could not be removed.");
        }

        sender.sendMessage(new TextComponentString(
            TextFormatting.GREEN + "Removed room " + roomId + " (" + room.getSchemaName() + ")."
                + (players.isEmpty() ? "" : " Moved " + players.size() + " player(s) to the console room.")
        ));
    }

    private static void listRoomConnections(ICommandSender sender, TardisRecord record) {
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Secondary room connections: " + TextFormatting.WHITE
                + record.getRoomConnections().size()
        ));
        if (record.getRoomConnections().isEmpty()) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "No secondary corridors are installed."
            ));
            return;
        }
        for (TardisRoomConnection connection : record.getRoomConnections()) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "Room " + connection.getFirstRoomId()
                    + TextFormatting.WHITE + " <-> "
                    + TextFormatting.GOLD + "Room " + connection.getSecondRoomId()
                    + TextFormatting.GRAY + " [generated=" + connection.isGenerated() + "]"
            ));
        }
    }

    private static void connectRooms(
        MinecraftServer server,
        ICommandSender sender,
        TardisWorldData data,
        TardisRecord record,
        String[] args,
        int firstArgument
    ) throws CommandException {
        if (args.length - firstArgument != 2) {
            throw new WrongUsageException("/tr room connect <firstId> <secondId>");
        }
        int firstRoomId = parseRoomId(args[firstArgument]);
        int secondRoomId = parseRoomId(args[firstArgument + 1]);
        if (firstRoomId == secondRoomId) {
            throw new CommandException("A room cannot be connected to itself.");
        }
        if (!InteriorRoomManager.roomsExist(record, firstRoomId, secondRoomId)) {
            throw new CommandException("Both room IDs must exist before they can be connected.");
        }
        if (!InteriorRoomManager.areRoomsAdjacent(record, firstRoomId, secondRoomId)) {
            throw new CommandException("Those rooms are not directly adjacent on the interior grid.");
        }
        if (InteriorRoomManager.isPrimaryConnection(record, firstRoomId, secondRoomId)) {
            throw new CommandException("Those rooms are already connected by the original parent corridor.");
        }
        if (record.getRoomConnection(firstRoomId, secondRoomId) != null) {
            throw new CommandException("Those rooms already have a secondary corridor.");
        }

        WorldServer world = prepareInteriorWorld(server, data, record);
        TardisRoomConnection created = data.addRoomConnection(
            record.getTardisId(), firstRoomId, secondRoomId
        );
        if (created == null) {
            throw new CommandException("The secondary corridor record could not be created.");
        }
        try {
            InteriorRoomManager.ensureGeneratedRooms(world, data, record);
        } catch (RuntimeException exception) {
            RenewedTardisMod.logger.error(
                "Failed to connect rooms {} and {} for TARDIS {}.",
                firstRoomId,
                secondRoomId,
                record.getTardisId(),
                exception
            );
            try {
                InteriorRoomManager.removeSecondaryConnectionStructure(
                    world, record, firstRoomId, secondRoomId
                );
            } catch (RuntimeException cleanupException) {
                RenewedTardisMod.logger.error(
                    "Failed to clean a partial secondary corridor between rooms {} and {}.",
                    firstRoomId,
                    secondRoomId,
                    cleanupException
                );
            }
            data.removeRoomConnection(record.getTardisId(), firstRoomId, secondRoomId);
            throw new CommandException(
                "Secondary corridor generation failed and was rolled back. Check the server log."
            );
        }
        sender.sendMessage(new TextComponentString(
            TextFormatting.GREEN + "Connected room " + firstRoomId + " to room " + secondRoomId
                + " with a secondary corridor."
        ));
    }

    private static void disconnectRooms(
        MinecraftServer server,
        ICommandSender sender,
        TardisWorldData data,
        TardisRecord record,
        String[] args,
        int firstArgument
    ) throws CommandException {
        int remaining = args.length - firstArgument;
        if (remaining < 2 || remaining > 3) {
            throw new WrongUsageException("/tr room disconnect <firstId> <secondId> [force]");
        }
        int firstRoomId = parseRoomId(args[firstArgument]);
        int secondRoomId = parseRoomId(args[firstArgument + 1]);
        boolean force = remaining == 3 && "force".equalsIgnoreCase(args[firstArgument + 2]);
        if (remaining == 3 && !force) {
            throw new WrongUsageException("/tr room disconnect <firstId> <secondId> [force]");
        }
        if (force) {
            requireOperator(sender);
        }
        if (InteriorRoomManager.isPrimaryConnection(record, firstRoomId, secondRoomId)) {
            throw new CommandException("Original parent corridors cannot be disconnected independently.");
        }
        if (record.getRoomConnection(firstRoomId, secondRoomId) == null) {
            throw new CommandException("Those rooms do not have a saved secondary corridor.");
        }

        WorldServer world = prepareInteriorWorld(server, data, record);
        List<EntityPlayerMP> players = InteriorRoomManager.getPlayersInConnectionArea(
            world, record, firstRoomId, secondRoomId
        );
        if (!players.isEmpty() && !force) {
            throw new CommandException(
                "The corridor contains " + players.size()
                    + " player(s). Move them out or use operator-only force."
            );
        }
        if (!players.isEmpty()) {
            InteriorRoomManager.movePlayersToMainRoom(players, record);
        }
        InteriorRoomManager.removeSecondaryConnectionStructure(
            world, record, firstRoomId, secondRoomId
        );
        if (!data.removeRoomConnection(record.getTardisId(), firstRoomId, secondRoomId)) {
            throw new CommandException("The corridor was cleared, but its saved connection could not be removed.");
        }
        sender.sendMessage(new TextComponentString(
            TextFormatting.GREEN + "Disconnected room " + firstRoomId + " from room " + secondRoomId + "."
                + (players.isEmpty() ? "" : " Moved " + players.size() + " player(s) to the console room.")
        ));
    }

    private static void teleportToRoom(
        ICommandSender sender,
        TardisRecord record,
        String[] args,
        int firstArgument
    ) throws CommandException {
        if (args.length - firstArgument != 1) {
            throw new WrongUsageException("/tr room tp <id>");
        }
        int roomId = parseRoomId(args[firstArgument]);
        if (roomId != InteriorRoomManager.MAIN_ROOM_ID && record.getRoom(roomId) == null) {
            throw new CommandException("Room " + roomId + " does not exist.");
        }
        if (!TardisTeleportService.teleportToInteriorRoom(getCommandSenderAsPlayer(sender), record, roomId)) {
            throw new CommandException("The requested room could not be opened.");
        }
    }

    private static int parseRoomId(String token) throws CommandException {
        try {
            int roomId = Integer.parseInt(token);
            if (roomId < 0) {
                throw new NumberFormatException("negative");
            }
            return roomId;
        } catch (NumberFormatException exception) {
            throw new CommandException("Room ID must be a non-negative whole number: " + token);
        }
    }

    private static WorldServer prepareInteriorWorld(
        MinecraftServer server,
        TardisWorldData data,
        TardisRecord record
    ) throws CommandException {
        DimensionPosition address = data.getOrCreateInterior(record);
        if (address.getDimension() != TardisDimensionManager.getDimensionId()) {
            throw new CommandException(
                "This TARDIS interior was assigned to dimension " + address.getDimension()
                    + ", but the config currently uses " + TardisDimensionManager.getDimensionId() + "."
            );
        }
        WorldServer world = TardisDimensionManager.getOrLoadWorld(server);
        try {
            TardisInteriorBuilder.ensureGenerated(world, data, record);
        } catch (RuntimeException exception) {
            throw new CommandException("The TARDIS interior could not be prepared. Check the server log.");
        }
        return world;
    }

    private static void manageTransmat(
        ICommandSender sender,
        TardisWorldData data,
        TardisRecord record,
        String[] args,
        int firstArgument
    ) throws CommandException {
        int remaining = args.length - firstArgument;
        String action = remaining == 0 ? "use" : args[firstArgument].toLowerCase(Locale.ROOT);
        if (remaining > 1 || (!("use".equals(action)) && !("status".equals(action)))) {
            throw new WrongUsageException("/tr transmat [use|status]");
        }
        if ("status".equals(action)) {
            DimensionPosition point = record.getTransmatPosition();
            sender.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "Transmat point: " + TextFormatting.WHITE
                    + (point == null ? "not assigned" : point.toString())
            ));
            sender.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "Cost/range: " + TextFormatting.WHITE
                    + TardisTransmatService.ENERGY_COST + " AU, "
                    + Math.round(TardisTransmatService.MAX_EXTERIOR_RANGE) + " exterior blocks"
            ));
            return;
        }
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        if (!TardisTransmatService.transmat(player, record)) {
            throw new CommandException("The transmat request could not be completed.");
        }
    }

    private static void manageEnergy(
        ICommandSender sender,
        TardisWorldData data,
        TardisRecord record,
        String[] args,
        int firstArgument
    ) throws CommandException {
        int remaining = args.length - firstArgument;
        if (remaining == 0 || (remaining == 1 && "status".equalsIgnoreCase(args[firstArgument]))) {
            showEnergyStatus(sender, data, record);
            return;
        }

        String action = args[firstArgument].toLowerCase(Locale.ROOT);
        requireOperator(sender);
        long now = System.currentTimeMillis();
        switch (action) {
            case "fill":
                if (remaining != 1) {
                    throw new WrongUsageException("/tr energy fill");
                }
                data.setArtronEnergy(record.getTardisId(), record.getArtronCapacity(), now);
                break;
            case "set":
                if (remaining != 2) {
                    throw new WrongUsageException("/tr energy set <amount>");
                }
                data.setArtronEnergy(record.getTardisId(), parseNonNegativeLong(args[firstArgument + 1], "energy"), now);
                break;
            case "add":
                if (remaining != 2) {
                    throw new WrongUsageException("/tr energy add <amount>");
                }
                data.addArtronEnergy(
                    record.getTardisId(),
                    parseNonNegativeLong(args[firstArgument + 1], "energy"),
                    now
                );
                break;
            case "capacity":
                if (remaining != 2) {
                    throw new WrongUsageException("/tr energy capacity <amount>");
                }
                long capacity = parseNonNegativeLong(args[firstArgument + 1], "capacity");
                if (capacity < 1L) {
                    throw new CommandException("Artron capacity must be at least 1 AU.");
                }
                data.setArtronCapacity(record.getTardisId(), capacity, now);
                break;
            default:
                throw new WrongUsageException("/tr energy [status|fill|set <amount>|add <amount>|capacity <amount>]");
        }

        sender.sendMessage(new TextComponentString(
            TextFormatting.GREEN + "Updated " + record.getOwnerName() + "'s TARDIS energy: "
                + TardisEnergyService.describe(record) + "."
        ));
    }

    private static void showEnergyStatus(ICommandSender sender, TardisWorldData data, TardisRecord record) {
        TardisEnergyService.refresh(data, record);
        String state = record.getArtronEnergy() >= record.getArtronCapacity()
            ? "fully charged"
            : "regenerating " + TardisUpgradeService.getRegenerationPerSecond(record)
                + " AU/s";
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Artron energy: " + TextFormatting.WHITE
                + TardisEnergyService.describe(record) + " (" + state + ")"
        ));
        DimensionPosition departure = record.isInFlight()
            ? record.getFlightDeparture()
            : record.getExteriorPosition();
        if (departure != null && record.getDestinationPosition() != null) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "Current route cost: " + TextFormatting.WHITE
                    + TardisEnergyService.format(
                        TardisEnergyService.calculateFlightCost(record, departure, record.getDestinationPosition())
                    ) + " AU"
            ));
        }
    }

    private static long parseNonNegativeLong(String token, String label) throws CommandException {
        try {
            long value = Long.parseLong(token);
            if (value < 0L) {
                throw new NumberFormatException("negative");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new CommandException(label + " must be a non-negative whole number: " + token);
        }
    }


    private static void manageUpgrades(
        ICommandSender sender,
        TardisWorldData data,
        TardisRecord record,
        String[] args,
        int firstArgument
    ) throws CommandException {
        int remaining = args.length - firstArgument;
        if (remaining == 0
            || (remaining == 1 && (
                "status".equalsIgnoreCase(args[firstArgument])
                    || "list".equalsIgnoreCase(args[firstArgument])
            ))) {
            showUpgradeStatus(sender, record);
            return;
        }

        String action = args[firstArgument].toLowerCase(Locale.ROOT);
        requireOperator(sender);

        if ("give".equals(action)) {
            if (remaining != 2) {
                throw new WrongUsageException("/tr upgrades give <type>");
            }
            TardisUpgradeType type = parseUpgradeType(args[firstArgument + 1]);
            ItemTardisUpgrade item = ModItems.getUpgradeItem(type);
            if (item == null) {
                throw new CommandException("The upgrade item is unavailable.");
            }
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            ItemStack stack = new ItemStack(item);
            if (!player.inventory.addItemStackToInventory(stack)) {
                player.dropItem(stack, false);
            }
            sender.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "Created one " + type.getDisplayName() + " component."
            ));
            return;
        }

        if (record.isInFlight()) {
            throw new CommandException("Upgrades cannot be changed while the TARDIS is in flight.");
        }

        if ("clear".equals(action)) {
            if (remaining != 1) {
                throw new WrongUsageException("/tr upgrades clear");
            }
            data.clearUpgrades(record.getTardisId());
            sender.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "Cleared all installed TARDIS upgrades."
            ));
            showUpgradeStatus(sender, record);
            return;
        }

        if (remaining < 2) {
            throw new WrongUsageException(
                "/tr upgrades <status|install|remove|set|clear|give> [type] [count]"
            );
        }
        TardisUpgradeType type = parseUpgradeType(args[firstArgument + 1]);

        switch (action) {
            case "install": {
                if (remaining != 2) {
                    throw new WrongUsageException("/tr upgrades install <type>");
                }
                TardisUpgradeService.InstallResult result =
                    TardisUpgradeService.install(data, record, type);
                if (!result.isSuccess()) {
                    throw new CommandException(result.getMessage());
                }
                sender.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + result.getMessage()
                ));
                break;
            }
            case "remove":
                if (remaining != 2) {
                    throw new WrongUsageException("/tr upgrades remove <type>");
                }
                if (!data.removeUpgrade(record.getTardisId(), type)) {
                    throw new CommandException(type.getDisplayName() + " is not installed.");
                }
                sender.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "Removed one " + type.getDisplayName() + " component."
                ));
                break;
            case "set": {
                if (remaining != 3) {
                    throw new WrongUsageException("/tr upgrades set <type> <count>");
                }
                int count = parseInt(
                    args[firstArgument + 2],
                    0,
                    TardisUpgradeService.getMaximumPerType(type)
                );
                int projectedTotal = record.getInstalledUpgradeCount()
                    - record.getUpgradeCount(type)
                    + count;
                if (projectedTotal > TardisConfig.getUpgradeSlotCount()) {
                    throw new CommandException(
                        "That would use " + projectedTotal + "/" + TardisConfig.getUpgradeSlotCount()
                            + " upgrade slots."
                    );
                }
                if (record.getUpgradeCount(type) != count
                    && !data.setUpgradeCount(record.getTardisId(), type, count)) {
                    throw new CommandException("The upgrade count could not be saved.");
                }
                sender.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + type.getDisplayName() + " count set to " + count + "."
                ));
                break;
            }
            default:
                throw new WrongUsageException(
                    "/tr upgrades <status|install|remove|set|clear|give> [type] [count]"
                );
        }
        showUpgradeStatus(sender, record);
    }

    private static TardisUpgradeType parseUpgradeType(String token) throws CommandException {
        TardisUpgradeType type = TardisUpgradeType.fromName(token);
        if (type == null) {
            throw new CommandException(
                "Unknown upgrade type. Use speed, capacity, regeneration, room, or protection."
            );
        }
        return type;
    }

    private static void showUpgradeStatus(ICommandSender sender, TardisRecord record) {
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "TARDIS upgrades: " + TextFormatting.WHITE
                + TardisUpgradeService.describe(record)
        ));
        for (TardisUpgradeType type : TardisUpgradeType.values()) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.GOLD + type.getName() + TextFormatting.WHITE
                    + ": " + record.getUpgradeCount(type) + "/"
                    + TardisUpgradeService.getMaximumPerType(type)
                    + " - " + TardisUpgradeService.describeEffect(record, type)
            ));
        }
        sender.sendMessage(new TextComponentString(
            TextFormatting.GRAY + "Effective room capacity: "
                + record.getRooms().size() + "/" + TardisUpgradeService.getRoomLimit(record)
                + "; Artron regeneration: "
                + TardisUpgradeService.getRegenerationPerSecond(record) + " AU/s."
        ));
    }

    private static void manageSystems(
        ICommandSender sender,
        TardisWorldData data,
        TardisRecord record,
        String[] args,
        int firstArgument
    ) throws CommandException {
        if (args.length <= firstArgument || "status".equalsIgnoreCase(args[firstArgument])) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "TARDIS subsystem diagnostics:"
            ));
            for (TardisSubsystem subsystem : TardisSubsystem.values()) {
                int health = record.getSubsystemHealth(subsystem);
                TextFormatting color = health < TardisConfig.getCriticalSubsystemHealth()
                    ? TextFormatting.RED
                    : health < 50 ? TextFormatting.YELLOW : TextFormatting.GREEN;
                sender.sendMessage(new TextComponentString(
                    color + TardisDamageService.formatSubsystem(record, subsystem)
                ));
            }
            sender.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "Damage system "
                    + (TardisConfig.isDamageSystemEnabled() ? "enabled" : "disabled")
                    + "; Chronosteel repairs " + TardisConfig.getRepairPerChronosteel()
                    + "% of the most damaged subsystem at the engine core."
            ));
            return;
        }

        requireOperator(sender);
        String action = args[firstArgument].toLowerCase(Locale.ROOT);
        if ("repairall".equals(action) || "fill".equals(action)) {
            data.repairAllSubsystems(record.getTardisId());
            sender.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "All subsystems restored to 100%."
            ));
            return;
        }
        if (args.length <= firstArgument + 2) {
            throw new WrongUsageException(
                "/tr systems <status|damage|repair|set|repairall> [subsystem] [amount]"
            );
        }

        TardisSubsystem subsystem = TardisSubsystem.fromName(args[firstArgument + 1]);
        if (subsystem == null) {
            throw new CommandException(
                "Unknown subsystem. Use engines, navigation, stabilizers, or dematerialization."
            );
        }
        int amount = parseInt(args[firstArgument + 2], 0, 100);
        switch (action) {
            case "damage":
                data.damageSubsystem(record.getTardisId(), subsystem, amount);
                break;
            case "repair":
                data.repairSubsystem(record.getTardisId(), subsystem, amount);
                break;
            case "set":
                data.setSubsystemHealth(record.getTardisId(), subsystem, amount);
                break;
            default:
                throw new WrongUsageException(
                    "/tr systems <status|damage|repair|set|repairall> [subsystem] [amount]"
                );
        }
        sender.sendMessage(new TextComponentString(
            TextFormatting.GREEN + TardisDamageService.formatSubsystem(record, subsystem)
        ));
    }

    private static void manageAccess(
        ICommandSender sender,
        TardisWorldData data,
        TardisRecord record,
        String[] args,
        int firstArgument
    ) throws CommandException {
        requireOwnerOrOperator(sender, record);
        int remaining = args.length - firstArgument;
        if (remaining == 0 || (remaining == 1 && "status".equalsIgnoreCase(args[firstArgument]))) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "Exterior access mode: " + TextFormatting.WHITE
                    + record.getAccessMode().getName() + " (" + record.getAccessMode().getDisplayName() + ")."
            ));
            return;
        }

        String modeToken;
        if (remaining == 1) {
            modeToken = args[firstArgument];
        } else if (remaining == 2 && "mode".equalsIgnoreCase(args[firstArgument])) {
            modeToken = args[firstArgument + 1];
        } else {
            throw new WrongUsageException("/tr access [status|mode <owner|crew|public>]");
        }

        TardisAccessMode mode = TardisAccessMode.fromName(modeToken);
        if (mode == null) {
            throw new CommandException("Unknown access mode '" + modeToken + "'. Use owner, crew, or public.");
        }
        data.setAccessMode(record.getTardisId(), mode);
        sender.sendMessage(new TextComponentString(
            TextFormatting.GREEN + "Exterior access set to " + mode.getName()
                + " (" + mode.getDisplayName() + ") for " + record.getOwnerName() + "'s TARDIS."
        ));
    }

    private static void manageCrew(
        MinecraftServer server,
        ICommandSender sender,
        TardisWorldData data,
        TardisRecord record,
        String[] args,
        int firstArgument
    ) throws CommandException {
        requireOwnerOrOperator(sender, record);
        int remaining = args.length - firstArgument;
        if (remaining == 0 || (remaining == 1 && "list".equalsIgnoreCase(args[firstArgument]))) {
            showCrew(sender, record);
            return;
        }

        String action = args[firstArgument].toLowerCase(Locale.ROOT);
        switch (action) {
            case "add": {
                if (remaining < 2 || remaining > 3) {
                    throw new WrongUsageException("/tr crew add <onlinePlayer> [guest|keyholder|pilot|engineer|full]");
                }
                EntityPlayerMP target = server.getPlayerList().getPlayerByUsername(args[firstArgument + 1]);
                if (target == null) {
                    throw new CommandException("The player must be online so their UUID can be saved safely.");
                }
                if (target.getUniqueID().equals(record.getOwnerId())) {
                    throw new CommandException("The owner already has every TARDIS permission.");
                }
                String role = remaining == 3 ? args[firstArgument + 2] : "guest";
                EnumSet<TardisPermission> permissions = permissionsForRole(role);
                data.addOrUpdateCrewMember(
                    record.getTardisId(),
                    target.getUniqueID(),
                    target.getName(),
                    permissions
                );
                sender.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "Added " + target.getName() + " as " + role.toLowerCase(Locale.ROOT)
                        + " crew with permissions: " + describePermissions(permissions) + "."
                ));
                return;
            }
            case "remove": {
                if (remaining != 2) {
                    throw new WrongUsageException("/tr crew remove <player|uuid>");
                }
                TardisCrewMember member = resolveCrewMember(server, data, record, args[firstArgument + 1]);
                data.removeCrewMember(record.getTardisId(), member.getPlayerId());
                sender.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "Removed " + member.getPlayerName() + " from the TARDIS crew."
                ));
                return;
            }
            case "grant":
            case "revoke": {
                if (remaining != 3) {
                    throw new WrongUsageException("/tr crew " + action + " <player|uuid> <door|key|console|maintenance>");
                }
                TardisCrewMember member = resolveCrewMember(server, data, record, args[firstArgument + 1]);
                TardisPermission permission = TardisPermission.fromName(args[firstArgument + 2]);
                if (permission == null) {
                    throw new CommandException("Unknown crew permission. Use door, key, console, or maintenance.");
                }
                boolean changed = "grant".equals(action)
                    ? data.grantCrewPermission(record.getTardisId(), member.getPlayerId(), permission)
                    : data.revokeCrewPermission(record.getTardisId(), member.getPlayerId(), permission);
                sender.sendMessage(new TextComponentString(
                    (changed ? TextFormatting.GREEN : TextFormatting.YELLOW)
                        + ("grant".equals(action) ? "Granted " : "Revoked ")
                        + permission.getName() + " permission "
                        + ("grant".equals(action) ? "to " : "from ") + member.getPlayerName() + "."
                ));
                return;
            }
            case "role": {
                if (remaining != 3) {
                    throw new WrongUsageException("/tr crew role <player|uuid> <guest|keyholder|pilot|engineer|full>");
                }
                TardisCrewMember member = resolveCrewMember(server, data, record, args[firstArgument + 1]);
                String role = args[firstArgument + 2];
                EnumSet<TardisPermission> permissions = permissionsForRole(role);
                data.addOrUpdateCrewMember(
                    record.getTardisId(),
                    member.getPlayerId(),
                    member.getPlayerName(),
                    permissions
                );
                sender.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "Set " + member.getPlayerName() + " to "
                        + role.toLowerCase(Locale.ROOT) + " permissions: " + describePermissions(permissions) + "."
                ));
                return;
            }
            case "key": {
                if (remaining != 2) {
                    throw new WrongUsageException("/tr crew key <onlinePlayer>");
                }
                EntityPlayerMP target = server.getPlayerList().getPlayerByUsername(args[firstArgument + 1]);
                if (target == null) {
                    throw new CommandException("The key recipient must be online.");
                }
                TardisCrewMember member = record.getCrewMember(target.getUniqueID());
                if (member == null) {
                    throw new CommandException(target.getName() + " is not on this TARDIS's crew list.");
                }
                if (!member.hasPermission(TardisPermission.KEY)) {
                    throw new CommandException(target.getName() + " does not have key permission.");
                }
                if (!member.getPlayerName().equals(target.getName())) {
                    data.addOrUpdateCrewMember(
                        record.getTardisId(),
                        target.getUniqueID(),
                        target.getName(),
                        member.getPermissions()
                    );
                }
                giveKeyToPlayer(target, record);
                sender.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "Issued a bound TARDIS key to " + target.getName() + "."
                ));
                if (sender != target) {
                    target.sendMessage(new TextComponentString(
                        TextFormatting.GOLD + "You received a key for " + record.getOwnerName() + "'s TARDIS."
                    ));
                }
                return;
            }
            case "clear":
                if (remaining != 1) {
                    throw new WrongUsageException("/tr crew clear");
                }
                data.clearCrew(record.getTardisId());
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Cleared the TARDIS crew list."));
                return;
            default:
                throw new WrongUsageException(
                    "/tr crew <list|add|remove|grant|revoke|role|key|clear> [...]"
                );
        }
    }

    private static void showCrew(ICommandSender sender, TardisRecord record) {
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Crew for " + record.getOwnerName() + "'s TARDIS: "
                + TextFormatting.WHITE + record.getCrew().size() + " member(s); access mode="
                + record.getAccessMode().getName() + "."
        ));
        if (record.getCrew().isEmpty()) {
            sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "No crew members are registered."));
            return;
        }
        for (TardisCrewMember member : record.getCrew().values()) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.GOLD + member.getPlayerName() + TextFormatting.DARK_GRAY
                    + " (" + member.getPlayerId() + ") " + TextFormatting.WHITE
                    + member.describePermissions()
            ));
        }
    }

    private static TardisCrewMember resolveCrewMember(
        MinecraftServer server,
        TardisWorldData data,
        TardisRecord record,
        String token
    ) throws CommandException {
        EntityPlayerMP online = server.getPlayerList().getPlayerByUsername(token);
        if (online != null) {
            TardisCrewMember byOnlineId = record.getCrewMember(online.getUniqueID());
            if (byOnlineId != null) {
                if (!byOnlineId.getPlayerName().equals(online.getName())) {
                    data.addOrUpdateCrewMember(
                        record.getTardisId(),
                        online.getUniqueID(),
                        online.getName(),
                        byOnlineId.getPermissions()
                    );
                }
                return byOnlineId;
            }
        }

        TardisCrewMember byName = record.getCrewMemberByName(token);
        if (byName != null) {
            return byName;
        }
        try {
            TardisCrewMember byId = record.getCrewMember(UUID.fromString(token));
            if (byId != null) {
                return byId;
            }
        } catch (IllegalArgumentException ignored) {
            // Normal player name.
        }
        throw new CommandException("No crew member was found for '" + token + "'.");
    }

    private static EnumSet<TardisPermission> permissionsForRole(String role) throws CommandException {
        String normalized = role == null ? "" : role.toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "guest":
                return EnumSet.of(TardisPermission.DOOR);
            case "keyholder":
            case "key_holder":
                return EnumSet.of(TardisPermission.DOOR, TardisPermission.KEY);
            case "pilot":
                return EnumSet.of(
                    TardisPermission.DOOR,
                    TardisPermission.KEY,
                    TardisPermission.CONSOLE
                );
            case "engineer":
                return EnumSet.of(TardisPermission.DOOR, TardisPermission.MAINTENANCE);
            case "full":
            case "crew":
                return EnumSet.allOf(TardisPermission.class);
            default:
                throw new CommandException(
                    "Unknown crew role '" + role + "'. Use guest, keyholder, pilot, engineer, or full."
                );
        }
    }

    private static String describePermissions(Set<TardisPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        for (TardisPermission permission : TardisPermission.values()) {
            if (!permissions.contains(permission)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(permission.getName());
        }
        return builder.toString();
    }

    private static void runDoctor(
        MinecraftServer server,
        ICommandSender sender,
        TardisRecord record,
        String[] args,
        int firstArgument
    ) throws CommandException {
        boolean deep = false;
        if (args.length > firstArgument) {
            if (args.length != firstArgument + 1 || !"deep".equalsIgnoreCase(args[firstArgument])) {
                throw new WrongUsageException("/tr doctor [deep]");
            }
            requireOperator(sender);
            deep = true;
        }

        DiagnosticReport report = TardisMaintenanceService.inspect(server, record, deep);
        TextFormatting summaryColor = report.getErrorCount() > 0
            ? TextFormatting.RED
            : (report.getWarningCount() > 0 ? TextFormatting.YELLOW : TextFormatting.GREEN);
        sender.sendMessage(new TextComponentString(
            summaryColor + "TARDIS doctor: " + report.getErrorCount() + " error(s), "
                + report.getWarningCount() + " warning(s)"
                + (deep ? " [deep world check]" : " [record-only]") + "."
        ));

        int maximum = TardisConfig.getMaximumDoctorMessages();
        int shown = 0;
        for (Finding finding : report.getFindings()) {
            if (shown >= maximum) {
                break;
            }
            TextFormatting color;
            switch (finding.getSeverity()) {
                case ERROR:
                    color = TextFormatting.RED;
                    break;
                case WARNING:
                    color = TextFormatting.YELLOW;
                    break;
                default:
                    color = TextFormatting.GRAY;
                    break;
            }
            sender.sendMessage(new TextComponentString(
                color + "- [" + finding.getSeverity().name() + "] " + finding.getMessage()
            ));
            shown++;
        }
        int hidden = report.getFindings().size() - shown;
        if (hidden > 0) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "... " + hidden + " additional finding(s) hidden by the config limit."
            ));
        }
        if (!deep) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.DARK_GRAY
                    + "Operators may use '/tr doctor deep' to load and verify the saved exterior blocks."
            ));
        }
    }

    private static void manageRepair(
        MinecraftServer server,
        ICommandSender sender,
        TardisWorldData data,
        TardisRecord record,
        String[] args,
        int firstArgument
    ) throws CommandException {
        requireOperator(sender);
        if (args.length <= firstArgument) {
            throw new WrongUsageException("/tr repair <exterior|interior|all> [force]");
        }

        String target = args[firstArgument].toLowerCase(Locale.ROOT);
        boolean force = false;
        if (args.length > firstArgument + 1) {
            if (args.length != firstArgument + 2 || !"force".equalsIgnoreCase(args[firstArgument + 1])) {
                throw new WrongUsageException("/tr repair <exterior|interior|all> [force]");
            }
            force = true;
        }

        if ("interior".equals(target)) {
            sendRepairResult(sender, "Interior", TardisMaintenanceService.repairInterior(server, data, record));
            return;
        }
        if ("exterior".equals(target)) {
            sendRepairResult(
                sender,
                "Exterior",
                TardisMaintenanceService.repairExterior(server, data, record, force)
            );
            return;
        }
        if ("all".equals(target)) {
            RepairResult interior = TardisMaintenanceService.repairInterior(server, data, record);
            sendRepairResult(sender, "Interior", interior);
            RepairResult exterior = TardisMaintenanceService.repairExterior(server, data, record, force);
            sendRepairResult(sender, "Exterior", exterior);
            if (!interior.isSuccess() || !exterior.isSuccess()) {
                throw new CommandException("One or more TARDIS repair steps failed; review the messages above.");
            }
            return;
        }
        throw new WrongUsageException("/tr repair <exterior|interior|all> [force]");
    }

    private static void sendRepairResult(ICommandSender sender, String label, RepairResult result) {
        TextFormatting color = result.isSuccess() ? TextFormatting.GREEN : TextFormatting.RED;
        sender.sendMessage(new TextComponentString(
            color + label + " repair: " + result.getMessage()
        ));
        if (!result.getDetails().isEmpty()) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.DARK_GRAY + "Details: " + result.getDetails()
            ));
        }
    }

    private static void requireOwnerOrOperator(ICommandSender sender, TardisRecord record)
        throws CommandException {
        if (sender.canUseCommand(2, "tardisrenewed")) {
            return;
        }
        if (sender instanceof EntityPlayerMP
            && record.getOwnerId().equals(((EntityPlayerMP) sender).getUniqueID())) {
            return;
        }
        throw new CommandException("Only the TARDIS owner or an operator may change crew and access settings.");
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
                "/tr as <owner> <info|key|exterior|interior|exit|destination|flight|takeoff|land|shell|room|transmat|energy|systems|upgrades|crew|access|doctor|repair> [...]"
            );
        }

        TardisRecord record = resolveRecord(server, data, args[1]);
        String subCommand = args[2].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "info":
                showInfo(sender, data, record);
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
                manageFlight(sender, data, record, args, 3);
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
            case "room":
                manageRooms(server, sender, data, record, args, 3);
                return;
            case "transmat":
                manageTransmat(sender, data, record, args, 3);
                return;
            case "energy":
                manageEnergy(sender, data, record, args, 3);
                return;
            case "systems":
                manageSystems(sender, data, record, args, 3);
                return;
            case "upgrades":
                manageUpgrades(sender, data, record, args, 3);
                return;
            case "crew":
                manageCrew(server, sender, data, record, args, 3);
                return;
            case "access":
                manageAccess(sender, data, record, args, 3);
                return;
            case "doctor":
                runDoctor(server, sender, record, args, 3);
                return;
            case "repair":
                manageRepair(server, sender, data, record, args, 3);
                return;
            default:
                throw new WrongUsageException(
                    "/tr as <owner> <info|key|exterior|interior|exit|destination|flight|takeoff|land|shell|room|transmat|energy|systems|upgrades|crew|access|doctor|repair> [...]"
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

        if ("flight".equalsIgnoreCase(args[0])) {
            return flightTabCompletions(args, 1);
        }

        if ("land".equalsIgnoreCase(args[0])) {
            return landTabCompletions(args, 1);
        }

        if ("shell".equalsIgnoreCase(args[0])) {
            return shellTabCompletions(args, 1);
        }

        if ("transmat".equalsIgnoreCase(args[0])) {
            return transmatTabCompletions(args, 1);
        }

        if ("energy".equalsIgnoreCase(args[0])) {
            return energyTabCompletions(args, 1);
        }

        if ("systems".equalsIgnoreCase(args[0])) {
            return systemsTabCompletions(args, 1);
        }

        if ("upgrades".equalsIgnoreCase(args[0])) {
            return upgradesTabCompletions(args, 1);
        }

        if ("crew".equalsIgnoreCase(args[0])) {
            TardisRecord record = sender instanceof EntityPlayerMP
                ? TardisWorldData.get(server).getByOwner(((EntityPlayerMP) sender).getUniqueID())
                : null;
            return crewTabCompletions(server, args, 1, record);
        }

        if ("access".equalsIgnoreCase(args[0])) {
            return accessTabCompletions(args, 1);
        }

        if ("room".equalsIgnoreCase(args[0])) {
            TardisRecord record = null;
            if (sender instanceof EntityPlayerMP) {
                record = TardisWorldData.get(server).getByOwner(((EntityPlayerMP) sender).getUniqueID());
            }
            return roomTabCompletions(args, 1, record);
        }

        if ("doctor".equalsIgnoreCase(args[0])) {
            return doctorTabCompletions(args, 1);
        }

        if ("repair".equalsIgnoreCase(args[0])) {
            return repairTabCompletions(args, 1);
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
            if (args.length >= 4 && "flight".equalsIgnoreCase(args[2])) {
                return flightTabCompletions(args, 3);
            }
            if (args.length >= 4 && "land".equalsIgnoreCase(args[2])) {
                return landTabCompletions(args, 3);
            }
            if (args.length >= 4 && "shell".equalsIgnoreCase(args[2])) {
                return shellTabCompletions(args, 3);
            }
            if (args.length >= 4 && "transmat".equalsIgnoreCase(args[2])) {
                return transmatTabCompletions(args, 3);
            }
            if (args.length >= 4 && "energy".equalsIgnoreCase(args[2])) {
                return energyTabCompletions(args, 3);
            }
            if (args.length >= 4 && "systems".equalsIgnoreCase(args[2])) {
                return systemsTabCompletions(args, 3);
            }
            if (args.length >= 4 && "upgrades".equalsIgnoreCase(args[2])) {
                return upgradesTabCompletions(args, 3);
            }
            if (args.length >= 4 && "crew".equalsIgnoreCase(args[2])) {
                TardisRecord record = null;
                try {
                    record = resolveRecord(server, TardisWorldData.get(server), args[1]);
                } catch (CommandException ignored) {
                    // Partial owner token; no crew-name completion is available yet.
                }
                return crewTabCompletions(server, args, 3, record);
            }
            if (args.length >= 4 && "access".equalsIgnoreCase(args[2])) {
                return accessTabCompletions(args, 3);
            }
            if (args.length >= 4 && "doctor".equalsIgnoreCase(args[2])) {
                return doctorTabCompletions(args, 3);
            }
            if (args.length >= 4 && "repair".equalsIgnoreCase(args[2])) {
                return repairTabCompletions(args, 3);
            }
            if (args.length >= 4 && "room".equalsIgnoreCase(args[2])) {
                TardisRecord record = null;
                try {
                    record = resolveRecord(server, TardisWorldData.get(server), args[1]);
                } catch (CommandException ignored) {
                    // Partial owner token; no room-ID completion is available yet.
                }
                return roomTabCompletions(args, 3, record);
            }
        }

        return Collections.emptyList();
    }

    private static List<String> doctorTabCompletions(String[] args, int firstArgument) {
        if (args.length - firstArgument == 1) {
            return getListOfStringsMatchingLastWord(args, Collections.singletonList("deep"));
        }
        return Collections.emptyList();
    }

    private static List<String> repairTabCompletions(String[] args, int firstArgument) {
        int remaining = args.length - firstArgument;
        if (remaining == 1) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("exterior", "interior", "all"));
        }
        if (remaining == 2 && (
            "exterior".equalsIgnoreCase(args[firstArgument])
                || "all".equalsIgnoreCase(args[firstArgument])
        )) {
            return getListOfStringsMatchingLastWord(args, Collections.singletonList("force"));
        }
        return Collections.emptyList();
    }

    private static List<String> accessTabCompletions(String[] args, int firstArgument) {
        int remaining = args.length - firstArgument;
        if (remaining == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                Arrays.asList("status", "mode", "owner", "crew", "public")
            );
        }
        if (remaining == 2 && "mode".equalsIgnoreCase(args[firstArgument])) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("owner", "crew", "public"));
        }
        return Collections.emptyList();
    }

    private static List<String> crewTabCompletions(
        MinecraftServer server,
        String[] args,
        int firstArgument,
        @Nullable TardisRecord record
    ) {
        int remaining = args.length - firstArgument;
        if (remaining == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                Arrays.asList("list", "add", "remove", "grant", "revoke", "role", "key", "clear")
            );
        }
        if (remaining == 2) {
            String action = args[firstArgument].toLowerCase(Locale.ROOT);
            if ("add".equals(action) || "key".equals(action)) {
                return getListOfStringsMatchingLastWord(args, Arrays.asList(server.getOnlinePlayerNames()));
            }
            if (
                "remove".equals(action)
                    || "grant".equals(action)
                    || "revoke".equals(action)
                    || "role".equals(action)
            ) {
                return getListOfStringsMatchingLastWord(args, crewNameCandidates(record));
            }
        }
        if (remaining == 3) {
            String action = args[firstArgument].toLowerCase(Locale.ROOT);
            if ("add".equals(action) || "role".equals(action)) {
                return getListOfStringsMatchingLastWord(
                    args,
                    Arrays.asList("guest", "keyholder", "pilot", "engineer", "full")
                );
            }
            if ("grant".equals(action) || "revoke".equals(action)) {
                return getListOfStringsMatchingLastWord(
                    args,
                    Arrays.asList("door", "key", "console", "maintenance")
                );
            }
        }
        return Collections.emptyList();
    }

    private static List<String> crewNameCandidates(@Nullable TardisRecord record) {
        if (record == null || record.getCrew().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        for (TardisCrewMember member : record.getCrew().values()) {
            if (!member.getPlayerName().isEmpty()) {
                names.add(member.getPlayerName());
            }
        }
        return names;
    }

    private static List<String> flightTabCompletions(String[] args, int firstArgument) {
        int remaining = args.length - firstArgument;
        if (remaining == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                Arrays.asList("status", "target", "speed", "accuracy")
            );
        }
        if (remaining == 2 && "speed".equalsIgnoreCase(args[firstArgument])) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("slow", "normal", "fast"));
        }
        if (remaining == 2 && "accuracy".equalsIgnoreCase(args[firstArgument])) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("precise", "normal", "risky"));
        }
        return Collections.emptyList();
    }

    private static List<String> transmatTabCompletions(String[] args, int firstArgument) {
        if (args.length - firstArgument == 1) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("use", "status"));
        }
        return Collections.emptyList();
    }

    private static List<String> energyTabCompletions(String[] args, int firstArgument) {
        int remaining = args.length - firstArgument;
        if (remaining == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                Arrays.asList("status", "fill", "set", "add", "capacity")
            );
        }
        return Collections.emptyList();
    }

    private static List<String> systemsTabCompletions(String[] args, int firstArgument) {
        int remaining = args.length - firstArgument;
        if (remaining == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                Arrays.asList("status", "damage", "repair", "set", "repairall")
            );
        }
        if (remaining == 2 && (
            "damage".equalsIgnoreCase(args[firstArgument])
                || "repair".equalsIgnoreCase(args[firstArgument])
                || "set".equalsIgnoreCase(args[firstArgument])
        )) {
            return getListOfStringsMatchingLastWord(
                args,
                Arrays.asList("engines", "navigation", "stabilizers", "dematerialization")
            );
        }
        return Collections.emptyList();
    }

    private static List<String> upgradesTabCompletions(String[] args, int firstArgument) {
        int remaining = args.length - firstArgument;
        if (remaining == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                Arrays.asList("status", "list", "install", "remove", "set", "clear", "give")
            );
        }
        if (remaining == 2 && (
            "install".equalsIgnoreCase(args[firstArgument])
                || "remove".equalsIgnoreCase(args[firstArgument])
                || "set".equalsIgnoreCase(args[firstArgument])
                || "give".equalsIgnoreCase(args[firstArgument])
        )) {
            return getListOfStringsMatchingLastWord(
                args,
                Arrays.asList("speed", "capacity", "regeneration", "room", "protection")
            );
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
                Arrays.asList("exact", "surface", "solid", "nearest", "departure", "force")
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

    private static List<String> roomTabCompletions(
        String[] args,
        int firstArgument,
        @Nullable TardisRecord record
    ) {
        int remaining = args.length - firstArgument;
        if (remaining == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                Arrays.asList("list", "schemas", "connections", "add", "connect", "disconnect", "remove", "tp")
            );
        }
        if (remaining == 2 && "add".equalsIgnoreCase(args[firstArgument])) {
            return getListOfStringsMatchingLastWord(args, InteriorRoomManager.getSchemaNames());
        }
        if (remaining == 3 && "add".equalsIgnoreCase(args[firstArgument])) {
            return getListOfStringsMatchingLastWord(args, HORIZONTAL_FACINGS);
        }
        if (remaining == 4 && "add".equalsIgnoreCase(args[firstArgument])) {
            return getListOfStringsMatchingLastWord(args, roomIdCandidates(record, true));
        }
        if ((remaining == 2 || remaining == 3) && (
            "connect".equalsIgnoreCase(args[firstArgument])
                || "disconnect".equalsIgnoreCase(args[firstArgument])
        )) {
            return getListOfStringsMatchingLastWord(args, roomIdCandidates(record, true));
        }
        if (remaining == 4 && "disconnect".equalsIgnoreCase(args[firstArgument])) {
            return getListOfStringsMatchingLastWord(args, Collections.singletonList("force"));
        }
        if (remaining == 2 && (
            "remove".equalsIgnoreCase(args[firstArgument])
                || "delete".equalsIgnoreCase(args[firstArgument])
        )) {
            return getListOfStringsMatchingLastWord(args, roomIdCandidates(record, false));
        }
        if (remaining == 3 && (
            "remove".equalsIgnoreCase(args[firstArgument])
                || "delete".equalsIgnoreCase(args[firstArgument])
        )) {
            return getListOfStringsMatchingLastWord(args, Collections.singletonList("force"));
        }
        if (remaining == 2 && (
            "tp".equalsIgnoreCase(args[firstArgument])
                || "teleport".equalsIgnoreCase(args[firstArgument])
        )) {
            return getListOfStringsMatchingLastWord(args, roomIdCandidates(record, true));
        }
        return Collections.emptyList();
    }

    private static List<String> roomIdCandidates(@Nullable TardisRecord record, boolean includeMain) {
        List<String> ids = new ArrayList<>();
        if (includeMain) {
            ids.add("0");
        }
        if (record != null) {
            for (TardisRoomRecord room : record.getRooms()) {
                ids.add(Integer.toString(room.getRoomId()));
            }
        }
        return ids;
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
