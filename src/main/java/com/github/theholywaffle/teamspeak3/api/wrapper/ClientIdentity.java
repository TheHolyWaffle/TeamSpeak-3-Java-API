package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import java.util.Objects;

/**
 * A database identity returned by {@code clientgetnamefromuid} or {@code clientgetnamefromdbid}.
 * The client need not be online; its nickname is the last one known to the server.
 *
 * @param uniqueId the client's unique identifier
 * @param databaseId the client's database ID
 * @param nickname the last-known nickname
 */
public record ClientIdentity(String uniqueId, ClientDatabaseId databaseId, String nickname) {
    public ClientIdentity {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(databaseId, "databaseId");
        Objects.requireNonNull(nickname, "nickname");
    }

    /** @return the identity decoded from a name-lookup response */
    public static ClientIdentity from(Wrapper row) {
        return new ClientIdentity(row.require("cluid"), row.requireDatabaseId("cldbid"), row.require("name"));
    }
}
