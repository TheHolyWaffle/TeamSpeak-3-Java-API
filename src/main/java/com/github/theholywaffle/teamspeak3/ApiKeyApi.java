package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ApiKeyScope;
import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.wrapper.ApiKey;
import java.time.Duration;
import java.util.List;

/**
 * Administration of the API keys that authenticate {@link TS3WebQuery} requests, available over every
 * transport.
 * <p>
 * A key is bound to the virtual server selected when it was created, or to the server instance when none
 * was, and acts with its owner's permissions within its {@link ApiKeyScope}. Creating keys needs
 * {@code b_virtualserver_apikey_add}; keys of other clients need {@code b_virtualserver_apikey_manage}.
 * </p>
 */
public final class ApiKeyApi {

    private final ApiKeyApiAsync async;

    ApiKeyApi(ApiKeyApiAsync async) {
        this.async = async;
    }

    /**
     * Creates an API key owned by this client.
     *
     * @param scope what the key may do
     * @param lifetime whole days, or {@link Duration#ZERO} for a key that never expires
     * @return the new key, including its secret, which the server never returns again
     * @throws IllegalArgumentException if the scope is {@link ApiKeyScope#UNKNOWN} or the lifetime is
     *     negative or not whole days
     */
    public ApiKey add(ApiKeyScope scope, Duration lifetime) {
        return CommandFutures.join(async.add(scope, lifetime));
    }

    /**
     * Creates an API key owned by another client.
     *
     * @param scope what the key may do
     * @param lifetime whole days, or {@link Duration#ZERO} for a key that never expires
     * @param owner the client whose permissions the key uses
     * @return the new key, including its secret, which the server never returns again
     */
    public ApiKey add(ApiKeyScope scope, Duration lifetime, ClientDatabaseId owner) {
        return CommandFutures.join(async.add(scope, lifetime, owner));
    }

    /** @return this client's keys bound to the current virtual server or instance, without secrets */
    public List<ApiKey> list() {
        return CommandFutures.join(async.list());
    }

    /**
     * @param owner the owning client
     * @return that client's keys bound to the current virtual server or instance, without secrets
     */
    public List<ApiKey> list(ClientDatabaseId owner) {
        return CommandFutures.join(async.list(owner));
    }

    /**
     * Lists the keys of all clients. On the instance this includes keys bound to any virtual server.
     *
     * @return the keys, without secrets
     */
    public List<ApiKey> listAll() {
        return CommandFutures.join(async.listAll());
    }

    /**
     * Deletes an API key, immediately rejecting its requests.
     *
     * @param id the key ID
     * @throws com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException with error 5123 for an unknown ID
     */
    public void delete(int id) {
        CommandFutures.join(async.delete(id));
    }
}
