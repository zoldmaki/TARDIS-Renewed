package dev.zoldmaki.renewedtardis.access;

import dev.zoldmaki.renewedtardis.data.TardisRecord;
import net.minecraft.entity.player.EntityPlayer;

/** Centralized owner, operator, guest, key, console, and maintenance checks. */
public final class TardisAccessService {
    private TardisAccessService() {
    }

    public static boolean isOperator(EntityPlayer player) {
        return player != null && player.canUseCommand(2, "tardisrenewed");
    }

    public static boolean isOwner(EntityPlayer player, TardisRecord record) {
        return player != null && record != null && record.getOwnerId().equals(player.getUniqueID());
    }

    public static boolean hasPermission(EntityPlayer player, TardisRecord record, TardisPermission permission) {
        if (player == null || record == null) {
            return false;
        }
        return isOwner(player, record)
            || isOperator(player)
            || record.hasCrewPermission(player.getUniqueID(), permission);
    }

    public static boolean canEnter(EntityPlayer player, TardisRecord record) {
        if (player == null || record == null) {
            return false;
        }
        if (isOwner(player, record) || isOperator(player)) {
            return true;
        }
        switch (record.getAccessMode()) {
            case PUBLIC:
                return true;
            case CREW:
                return record.hasCrewPermission(player.getUniqueID(), TardisPermission.DOOR);
            case OWNER:
            default:
                return false;
        }
    }

    public static boolean canUseKey(EntityPlayer player, TardisRecord record) {
        return hasPermission(player, record, TardisPermission.KEY);
    }

    public static boolean canUseConsole(EntityPlayer player, TardisRecord record) {
        return hasPermission(player, record, TardisPermission.CONSOLE);
    }

    public static boolean canMaintain(EntityPlayer player, TardisRecord record) {
        return hasPermission(player, record, TardisPermission.MAINTENANCE);
    }

    /**
     * Sonic linking and read-only use accepts any technical TARDIS role. More
     * powerful sonic actions still perform their own key/console checks.
     */
    public static boolean canUseSonic(EntityPlayer player, TardisRecord record) {
        if (player == null || record == null) {
            return false;
        }
        return isOwner(player, record)
            || isOperator(player)
            || record.hasCrewPermission(player.getUniqueID(), TardisPermission.KEY)
            || record.hasCrewPermission(player.getUniqueID(), TardisPermission.CONSOLE)
            || record.hasCrewPermission(player.getUniqueID(), TardisPermission.MAINTENANCE);
    }
}
