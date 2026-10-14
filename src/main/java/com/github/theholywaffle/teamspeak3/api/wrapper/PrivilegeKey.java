package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.PrivilegeKeyType;
import java.time.Instant;
import java.util.Map;

public class PrivilegeKey extends Wrapper {

    public PrivilegeKey(Map<String, String> map) {
        super(map);
    }

    public String getToken() {
        return require("token");
    }

    /** @return the key type, or {@link PrivilegeKeyType#UNKNOWN} for a type this library does not know */
    public PrivilegeKeyType getType() {
        return requireCode("token_type", PrivilegeKeyType::fromIndex);
    }

    public boolean isServerGroupToken() {
        return getType() == PrivilegeKeyType.SERVER_GROUP;
    }

    public boolean isChannelGroupToken() {
        return getType() == PrivilegeKeyType.CHANNEL_GROUP;
    }

    public int getGroupId() {
        return requireInt("token_id1");
    }

    public int getChannelId() {
        return requireInt("token_id2");
    }

    public Instant getCreated() {
        return requireInstant("token_created");
    }

    public String getDescription() {
        return require("token_description");
    }
}
