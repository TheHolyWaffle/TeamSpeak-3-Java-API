package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public class DatabaseClient extends Wrapper {

    public DatabaseClient(Map<String, String> map) {
        super(map);
    }

    public ClientDatabaseId getDatabaseId() {
        return requireDatabaseId("cldbid");
    }

    public String getUniqueIdentifier() {
        return require(ClientProperty.CLIENT_UNIQUE_IDENTIFIER);
    }

    public String getNickname() {
        return require(ClientProperty.CLIENT_NICKNAME);
    }

    /** @return when the identity was first seen, or empty if not recorded */
    public Optional<Instant> getCreatedDate() {
        return requireTimestamp(ClientProperty.CLIENT_CREATED);
    }

    /** @return when the identity last connected, or empty if not recorded */
    public Optional<Instant> getLastConnectedDate() {
        return requireTimestamp(ClientProperty.CLIENT_LASTCONNECTED);
    }

    public int getTotalConnections() {
        return requireInt(ClientProperty.CLIENT_TOTALCONNECTIONS);
    }

    public String getDescription() {
        return require(ClientProperty.CLIENT_DESCRIPTION);
    }

    /** @return the last IP address, or empty if none is recorded */
    public Optional<String> getLastIp() {
        return findText("client_lastip");
    }
}
