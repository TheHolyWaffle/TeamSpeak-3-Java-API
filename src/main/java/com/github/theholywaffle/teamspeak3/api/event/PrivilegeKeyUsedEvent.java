package com.github.theholywaffle.teamspeak3.api.event;

import com.github.theholywaffle.teamspeak3.api.ChannelGroupId;
import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.PrivilegeKeyType;
import com.github.theholywaffle.teamspeak3.api.ServerGroupId;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import java.util.Optional;

public class PrivilegeKeyUsedEvent extends BaseEvent {

    public PrivilegeKeyUsedEvent(Wrapper wrapper) {
        super(wrapper);
    }

    public ClientId getClientId() {
        return requireClientId("clid");
    }

    public ClientDatabaseId getClientDatabaseId() {
        return requireDatabaseId("cldbid");
    }

    public String getClientUniqueIdentifier() {
        return require("cluid");
    }

    public String getPrivilegeKey() {
        return require("token");
    }

    public PrivilegeKeyType getPrivilegeKeyType() {
        return getPrivilegeKeyChannelId().isEmpty() ? PrivilegeKeyType.SERVER_GROUP : PrivilegeKeyType.CHANNEL_GROUP;
    }

    /** @return the server group the key granted, or empty if it granted a channel group */
    public Optional<ServerGroupId> getPrivilegeKeyServerGroupId() {
        return getPrivilegeKeyChannelId().isEmpty()
                ? Optional.of(requireId("token1", ServerGroupId::new))
                : Optional.empty();
    }

    /** @return the channel group the key granted, or empty if it granted a server group */
    public Optional<ChannelGroupId> getPrivilegeKeyChannelGroupId() {
        return getPrivilegeKeyChannelId().isPresent()
                ? Optional.of(requireId("token1", ChannelGroupId::new))
                : Optional.empty();
    }

    /** @return the channel of a channel group key, or empty for a server group key */
    public Optional<ChannelId> getPrivilegeKeyChannelId() {
        return requireOptionalId("token2", ChannelId::new);
    }

    @Override
    public void fire(TS3Listener listener) {
        listener.onPrivilegeKeyUsed(this);
    }
}
