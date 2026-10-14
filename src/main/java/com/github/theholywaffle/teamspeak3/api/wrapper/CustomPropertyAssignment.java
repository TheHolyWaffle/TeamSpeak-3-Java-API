package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import java.util.Objects;

/**
 * A custom client property matched by {@code customsearch}.
 *
 * @param clientDatabaseId the client that has the property
 * @param key the property key
 * @param value the property value
 */
public record CustomPropertyAssignment(ClientDatabaseId clientDatabaseId, String key, String value) {

    public CustomPropertyAssignment {
        Objects.requireNonNull(clientDatabaseId, "clientDatabaseId");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
    }

    /** @throws com.github.theholywaffle.teamspeak3.api.exception.TS3InvalidValueException if a property is missing or malformed */
    public static CustomPropertyAssignment from(Wrapper row) {
        return new CustomPropertyAssignment(
                row.requireDatabaseId("cldbid"), row.require("ident"), row.require("value"));
    }
}
