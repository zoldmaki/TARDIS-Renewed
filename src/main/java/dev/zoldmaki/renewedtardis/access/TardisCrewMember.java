package dev.zoldmaki.renewedtardis.access;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

/** Persistent UUID-based crew entry with a last-known player name. */
public final class TardisCrewMember {
    private final UUID playerId;
    private String playerName;
    private final EnumSet<TardisPermission> permissions;

    public TardisCrewMember(UUID playerId, String playerName, Set<TardisPermission> permissions) {
        if (playerId == null) {
            throw new IllegalArgumentException("playerId cannot be null");
        }
        this.playerId = playerId;
        this.playerName = playerName == null ? "" : playerName;
        this.permissions = permissions == null || permissions.isEmpty()
            ? EnumSet.noneOf(TardisPermission.class)
            : EnumSet.copyOf(permissions);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName == null ? "" : playerName;
    }

    public boolean hasPermission(TardisPermission permission) {
        return permission != null && permissions.contains(permission);
    }

    public Set<TardisPermission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public boolean grant(TardisPermission permission) {
        return permission != null && permissions.add(permission);
    }

    public boolean revoke(TardisPermission permission) {
        return permission != null && permissions.remove(permission);
    }

    public boolean setPermissions(Set<TardisPermission> newPermissions) {
        EnumSet<TardisPermission> replacement = newPermissions == null || newPermissions.isEmpty()
            ? EnumSet.noneOf(TardisPermission.class)
            : EnumSet.copyOf(newPermissions);
        if (permissions.equals(replacement)) {
            return false;
        }
        permissions.clear();
        permissions.addAll(replacement);
        return true;
    }

    public String describePermissions() {
        if (permissions.isEmpty()) {
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

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("PlayerId", playerId.toString());
        tag.setString("PlayerName", playerName);
        NBTTagList list = new NBTTagList();
        for (TardisPermission permission : TardisPermission.values()) {
            if (permissions.contains(permission)) {
                list.appendTag(new NBTTagString(permission.getName()));
            }
        }
        tag.setTag("Permissions", list);
        return tag;
    }

    public static TardisCrewMember readFromNBT(NBTTagCompound tag) {
        UUID playerId = UUID.fromString(tag.getString("PlayerId"));
        String playerName = tag.getString("PlayerName");
        EnumSet<TardisPermission> permissions = EnumSet.noneOf(TardisPermission.class);
        if (tag.hasKey("Permissions", Constants.NBT.TAG_LIST)) {
            NBTTagList list = tag.getTagList("Permissions", Constants.NBT.TAG_STRING);
            for (int index = 0; index < list.tagCount(); index++) {
                TardisPermission permission = TardisPermission.fromName(list.getStringTagAt(index));
                if (permission != null) {
                    permissions.add(permission);
                }
            }
        }
        return new TardisCrewMember(playerId, playerName, permissions);
    }
}
