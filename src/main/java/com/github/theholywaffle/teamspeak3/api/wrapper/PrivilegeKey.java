package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ChannelGroupId;
import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.PrivilegeKeyType;
import com.github.theholywaffle.teamspeak3.api.ServerGroupId;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

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

    /** @return the server group the key grants, or empty if it grants a channel group */
    public Optional<ServerGroupId> getServerGroupId() {
        return isServerGroupToken() ? Optional.of(requireId("token_id1", ServerGroupId::new)) : Optional.empty();
    }

    /** @return the channel group the key grants, or empty if it grants a server group */
    public Optional<ChannelGroupId> getChannelGroupId() {
        return isChannelGroupToken() ? Optional.of(requireId("token_id1", ChannelGroupId::new)) : Optional.empty();
    }

    /** @return the channel of a channel group key, or empty for a server group key */
    public Optional<ChannelId> getChannelId() {
        return requireOptionalId("token_id2", ChannelId::new);
    }

    public Instant getCreated() {
        return requireInstant("token_created");
    }

    public String getDescription() {
        return require("token_description");
    }
}
