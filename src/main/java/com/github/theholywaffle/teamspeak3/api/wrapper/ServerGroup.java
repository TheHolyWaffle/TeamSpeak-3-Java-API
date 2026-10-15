package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.IconId;
import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import com.github.theholywaffle.teamspeak3.api.ServerGroupId;
import java.util.Map;
import java.util.Optional;

public class ServerGroup extends Wrapper {

    public ServerGroup(Map<String, String> map) {
        super(map);
    }

    public ServerGroupId getId() {
        return requireId("sgid", ServerGroupId::of);
    }

    public String getName() {
        return require("name");
    }

    /** @return the group type, or {@link PermissionGroupDatabaseType#UNKNOWN} for a type this library does not know */
    public PermissionGroupDatabaseType getType() {
        return requireCode("type", PermissionGroupDatabaseType::fromIndex);
    }

    /** @return the group's icon, or empty if it has none */
    public Optional<IconId> getIconId() {
        return requireIconId("iconid");
    }

    public boolean isSavedInDatabase() {
        return requireBoolean("savedb");
    }

    public int getSortId() {
        return requireInt("sortid");
    }

    public int getNameMode() {
        return requireInt("namemode");
    }

    public int getModifyPower() {
        return requireInt("n_modifyp");
    }

    public int getMemberAddPower() {
        return requireInt("n_member_addp");
    }

    public int getMemberRemovePower() {
        return requireInt("n_member_removep");
    }
}
