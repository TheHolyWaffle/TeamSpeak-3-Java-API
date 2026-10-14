package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import java.util.Map;

public class QueryLogin extends Wrapper {

    public QueryLogin(Map<String, String> map) {
        super(map);
    }

    public ClientDatabaseId getClientDatabaseId() {
        return requireDatabaseId("cldbid");
    }

    public int getVirtualServerId() {
        return requireInt("sid");
    }

    public String getQueryClientLoginName() {
        return require("client_login_name");
    }
}
