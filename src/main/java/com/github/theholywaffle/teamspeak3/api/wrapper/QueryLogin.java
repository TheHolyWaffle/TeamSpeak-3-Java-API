package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.VirtualServerId;
import java.util.Map;
import java.util.Optional;

public class QueryLogin extends Wrapper {

    public QueryLogin(Map<String, String> map) {
        super(map);
    }

    public ClientDatabaseId getClientDatabaseId() {
        return requireDatabaseId("cldbid");
    }

    /** @return the virtual server the login belongs to, or empty for an instance-wide login */
    public Optional<VirtualServerId> getVirtualServerId() {
        return requireOptionalId("sid", VirtualServerId::new);
    }

    public String getQueryClientLoginName() {
        return require("client_login_name");
    }
}
