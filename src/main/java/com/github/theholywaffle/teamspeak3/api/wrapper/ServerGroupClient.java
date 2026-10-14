package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import java.util.Objects;

/**
 * A server group member from {@code servergroupclientlist -names}.
 *
 * @param clientDatabaseId the member
 * @param nickname the member's last nickname
 * @param uniqueIdentifier the member's unique identifier
 */
public record ServerGroupClient(ClientDatabaseId clientDatabaseId, String nickname, String uniqueIdentifier) {

    public ServerGroupClient {
        Objects.requireNonNull(clientDatabaseId, "clientDatabaseId");
        Objects.requireNonNull(nickname, "nickname");
        Objects.requireNonNull(uniqueIdentifier, "uniqueIdentifier");
    }

    /** @throws com.github.theholywaffle.teamspeak3.api.exception.TS3InvalidValueException if a property is missing or malformed */
    public static ServerGroupClient from(Wrapper row) {
        return new ServerGroupClient(
                row.requireDatabaseId("cldbid"),
                row.require("client_nickname"),
                row.require("client_unique_identifier"));
    }
}
