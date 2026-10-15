package com.github.theholywaffle.teamspeak3.api.event;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.PrivilegeKeyGrant;
import com.github.theholywaffle.teamspeak3.api.PrivilegeKeyType;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

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
        return getGrant().type();
    }

    /** @return what the used key granted */
    public PrivilegeKeyGrant getGrant() {
        return requirePrivilegeKeyGrant("token1", "token2");
    }

    @Override
    public void fire(TS3Listener listener) {
        listener.onPrivilegeKeyUsed(this);
    }
}
