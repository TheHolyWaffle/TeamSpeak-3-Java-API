package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ApiKeyId;
import com.github.theholywaffle.teamspeak3.api.ApiKeyScope;
import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import java.time.Duration;
import java.util.Optional;

public final class ApiKeyCommands {

    private ApiKeyCommands() {
        throw new Error("No instances");
    }

    /**
     * @param lifetime whole days, or {@link Duration#ZERO} for a key that never expires
     * @param owner the owning client, or empty for the invoker
     */
    public static Command apiKeyAdd(ApiKeyScope scope, Duration lifetime, Optional<ClientDatabaseId> owner) {
        if (scope == null || scope == ApiKeyScope.UNKNOWN) {
            throw new IllegalArgumentException("API key scope must be MANAGE, WRITE or READ");
        }
        if (lifetime == null || lifetime.isNegative() || !lifetime.equals(Duration.ofDays(lifetime.toDays()))) {
            throw new IllegalArgumentException("API key lifetime must be whole days, or zero for no expiry");
        }
        return new CommandBuilder("apikeyadd")
                .add(new KeyValueParam("scope", scope.getName()))
                .add(new KeyValueParam("lifetime", lifetime.toDays()))
                .addIfPresent("cldbid", owner)
                .build();
    }

    /** @param owner the owning client, or empty for the invoker */
    public static Command apiKeyList(Optional<ClientDatabaseId> owner) {
        return new CommandBuilder("apikeylist").addIfPresent("cldbid", owner).build();
    }

    public static Command apiKeyListAll() {
        return new CommandBuilder("apikeylist")
                .add(new KeyValueParam("cldbid", "*"))
                .build();
    }

    public static Command apiKeyDelete(ApiKeyId id) {
        return new CommandBuilder("apikeydel").add(new KeyValueParam("id", id)).build();
    }
}
