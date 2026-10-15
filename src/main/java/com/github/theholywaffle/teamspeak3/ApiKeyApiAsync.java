package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ApiKeyId;
import com.github.theholywaffle.teamspeak3.api.ApiKeyScope;
import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.wrapper.ApiKey;
import com.github.theholywaffle.teamspeak3.commands.ApiKeyCommands;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous Web Query API key administration.
 *
 * @see ApiKeyApi the synchronous form and full documentation
 */
public final class ApiKeyApiAsync {

    private final QueryRequests requests;

    ApiKeyApiAsync(QueryRequests requests) {
        this.requests = requests;
    }

    /** @see ApiKeyApi#add(ApiKeyScope, Duration) */
    public CompletableFuture<ApiKey> add(ApiKeyScope scope, Duration lifetime) {
        return requests.model(ApiKeyCommands.apiKeyAdd(scope, lifetime, Optional.empty()), ApiKey::new);
    }

    /** @see ApiKeyApi#add(ApiKeyScope, Duration, ClientDatabaseId) */
    public CompletableFuture<ApiKey> add(ApiKeyScope scope, Duration lifetime, ClientDatabaseId owner) {
        return requests.model(ApiKeyCommands.apiKeyAdd(scope, lifetime, Optional.of(owner)), ApiKey::new);
    }

    /** @see ApiKeyApi#list() */
    public CompletableFuture<List<ApiKey>> list() {
        return requests.models(ApiKeyCommands.apiKeyList(Optional.empty()), ApiKey::new);
    }

    /** @see ApiKeyApi#list(ClientDatabaseId) */
    public CompletableFuture<List<ApiKey>> list(ClientDatabaseId owner) {
        return requests.models(ApiKeyCommands.apiKeyList(Optional.of(owner)), ApiKey::new);
    }

    /** @see ApiKeyApi#listAll() */
    public CompletableFuture<List<ApiKey>> listAll() {
        return requests.models(ApiKeyCommands.apiKeyListAll(), ApiKey::new);
    }

    /** @see ApiKeyApi#delete(ApiKeyId) */
    public CompletableFuture<Void> delete(ApiKeyId id) {
        return requests.run(ApiKeyCommands.apiKeyDelete(id));
    }
}
