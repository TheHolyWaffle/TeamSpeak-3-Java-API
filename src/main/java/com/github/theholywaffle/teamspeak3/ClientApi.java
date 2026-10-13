package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Administration of clients connected to the selected virtual server.
 * <p>
 * Client IDs identify a connection and change on reconnect; use database IDs or unique
 * identifiers for persistent identities. ID-based reads fail with server error 512 for an
 * unknown client; lookups by name or unique identifier return an empty result instead.
 * </p>
 *
 * @see ClientApiAsync the asynchronous form
 */
public final class ClientApi {

    private final ClientApiAsync async;

    ClientApi(ClientApiAsync async) {
        this.async = async;
    }

    /**
     * Lists all clients connected to the selected virtual server, including query clients.
     *
     * @return the connected clients
     */
    public List<Client> list() {
        return CommandFutures.join(async.list());
    }

    /**
     * Gets detailed information about one connected client.
     *
     * @param clientId the client ID
     * @return the client information
     * @throws com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException with error 512 if the client is not connected
     */
    public ClientInfo info(ClientId clientId) {
        return CommandFutures.join(async.info(clientId));
    }

    /**
     * Finds the first connected client whose complete nickname equals {@code nickname}.
     *
     * @param nickname the complete nickname
     * @param ignoreCase whether to compare case-insensitively
     * @return the client, or empty if no client has that nickname
     */
    public Optional<Client> findByName(String nickname, boolean ignoreCase) {
        return CommandFutures.join(async.findByName(nickname, ignoreCase));
    }

    /**
     * Finds connected clients whose nickname contains {@code nicknamePart}.
     *
     * @param nicknamePart a case-insensitive part of the nickname
     * @return the matching clients; empty if none match
     */
    public List<Client> search(String nicknamePart) {
        return CommandFutures.join(async.search(nicknamePart));
    }

    /**
     * Finds a connected client by unique identifier.
     *
     * @param uniqueId the client's unique identifier
     * @return the client information, or empty if the client is not connected
     */
    public Optional<ClientInfo> findByUniqueId(String uniqueId) {
        return CommandFutures.join(async.findByUniqueId(uniqueId));
    }

    /**
     * @param clientId the client ID
     * @return whether a client with this ID is connected
     */
    public boolean isOnline(ClientId clientId) {
        return CommandFutures.join(async.isOnline(clientId));
    }

    /**
     * @param uniqueId the client's unique identifier
     * @return whether a client with this unique identifier is connected
     */
    public boolean isOnline(String uniqueId) {
        return CommandFutures.join(async.isOnline(uniqueId));
    }

    /**
     * Moves a client to another channel. Client ID 0 moves this query client.
     *
     * @param clientId the client ID
     * @param channelId the target channel
     */
    public void move(ClientId clientId, int channelId) {
        CommandFutures.join(async.move(clientId, channelId));
    }

    /**
     * Moves a client to a password-protected channel.
     *
     * @param clientId the client ID
     * @param channelId the target channel
     * @param channelPassword the channel password, or {@code null}
     */
    public void move(ClientId clientId, int channelId, String channelPassword) {
        CommandFutures.join(async.move(clientId, channelId, channelPassword));
    }

    /**
     * Moves several clients to one channel in a single command. An empty collection sends nothing.
     *
     * @param clientIds the client IDs
     * @param channelId the target channel
     * @param channelPassword the channel password, or {@code null}
     */
    public void moveAll(Collection<ClientId> clientIds, int channelId, String channelPassword) {
        CommandFutures.join(async.moveAll(clientIds, channelId, channelPassword));
    }

    /**
     * Kicks clients from their channels into the default channel.
     *
     * @param reason the reason shown to the clients, or {@code null}
     * @param clientIds at least one client ID
     */
    public void kickFromChannel(String reason, ClientId... clientIds) {
        CommandFutures.join(async.kickFromChannel(reason, clientIds));
    }

    /**
     * Kicks clients from the virtual server.
     *
     * @param reason the reason shown to the clients, or {@code null}
     * @param clientIds at least one client ID
     */
    public void kickFromServer(String reason, ClientId... clientIds) {
        CommandFutures.join(async.kickFromServer(reason, clientIds));
    }

    /**
     * Pokes a client with a message.
     *
     * @param clientId the client ID
     * @param message the message
     */
    public void poke(ClientId clientId, String message) {
        CommandFutures.join(async.poke(clientId, message));
    }

    /**
     * Changes properties of another connected client. Use {@link TS3Api#updateClient(Map)} for this query client.
     *
     * @param clientId the client ID
     * @param properties the properties to change
     */
    public void edit(ClientId clientId, Map<ClientProperty, String> properties) {
        CommandFutures.join(async.edit(clientId, properties));
    }
}
