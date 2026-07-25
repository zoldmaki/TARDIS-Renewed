package dev.zoldmaki.renewedtardis.command;

import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.item.ItemTardisKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * Small Phase 2 command surface for testing persistent data without exposing
 * destructive world-management operations.
 */
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
        return "/tardisrenewed <info|create|key|count>";
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
            TextFormatting.AQUA + "Interior dimension: " + TextFormatting.WHITE
                + (record.hasInteriorDimension()
                    ? Integer.toString(record.getInteriorDimension())
                    : "not assigned")
        ));
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "Exterior: " + TextFormatting.WHITE
                + (record.getExteriorPosition() == null ? "not placed" : record.getExteriorPosition().toString())
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

    @Override
    public List<String> getTabCompletions(
        MinecraftServer server,
        ICommandSender sender,
        String[] args,
        @Nullable BlockPos targetPos
    ) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("info", "create", "key", "count"));
        }
        return Collections.emptyList();
    }
}
