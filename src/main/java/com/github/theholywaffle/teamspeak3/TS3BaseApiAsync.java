package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerQueryInfo;
import com.github.theholywaffle.teamspeak3.commands.ClientCommands;
import com.github.theholywaffle.teamspeak3.commands.CommandEncoding;
import com.github.theholywaffle.teamspeak3.commands.QueryCommands;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous API shared by {@link TS3Query} and {@link TS3WebQuery}: one domain API per
 * administrative area, plus operations on this query client. A {@link TS3Query} returns the
 * {@link TS3ApiAsync} subtype, which adds login and virtual server selection. Every method returns immediately with a {@link CompletableFuture}.
 * <p>
 * Failures complete the future exceptionally with a {@link com.github.theholywaffle.teamspeak3.api.exception.TS3Exception}:
 * {@code get} wraps it in {@link java.util.concurrent.ExecutionException} and {@code join} in
 * {@link java.util.concurrent.CompletionException}. Absent lookups complete with an empty
 * {@link java.util.Optional} rather than failing. See the synchronous {@link TS3BaseApi} for the
 * same operations with blocking results.
 * </p><p>
 * Accepted response completions are published in receive order on an owned completion
 * worker, away from protocol threads. Non-async callbacks must be short and must not
 * block waiting for another command. Use async stages with an application executor for
 * blocking work. Callbacks added after completion may run on the registering thread;
 * JDK async methods without an executor use the common pool.
 * </p><p>
 * Cancel the original API result to remove an unsent request. Sent requests still consume
 * their eventual responses; cancellation cannot undo a server-side mutation. Cancelling
 * an application-created dependent stage does not cancel its source. Timed get limits
 * only the wait; orTimeout changes the future's result without aborting the request.
 * </p>
 *
 * @see TS3BaseApi The synchronous version of the API
 */
public class TS3BaseApiAsync {

    final QueryRequests requests;
    private final ApiKeyApiAsync apiKeys;
    private final BanApiAsync bans;
    private final ChannelApiAsync channels;
    private final ChannelGroupApiAsync channelGroups;
    private final ClientApiAsync clients;
    private final ComplaintApiAsync complaints;
    private final DatabaseClientApiAsync databaseClients;
    private final EventApiAsync events;
    private final FileApiAsync files;
    private final InstanceApiAsync instance;
    private final MessageApiAsync messages;
    private final PermissionApiAsync permissions;
    private final PrivilegeKeyApiAsync privilegeKeys;
    private final QueryLoginApiAsync queryLogins;
    private final ServerGroupApiAsync serverGroups;
    private final VirtualServerApiAsync servers;

    TS3BaseApiAsync(QueryBackend backend) {
        requests = new QueryRequests(backend);
        apiKeys = new ApiKeyApiAsync(requests);
        bans = new BanApiAsync(requests);
        channels = new ChannelApiAsync(requests);
        channelGroups = new ChannelGroupApiAsync(requests);
        clients = new ClientApiAsync(requests);
        complaints = new ComplaintApiAsync(requests);
        databaseClients = new DatabaseClientApiAsync(requests);
        events = new EventApiAsync(requests);
        files = new FileApiAsync(requests);
        instance = new InstanceApiAsync(requests);
        messages = new MessageApiAsync(requests);
        permissions = new PermissionApiAsync(requests);
        privilegeKeys = new PrivilegeKeyApiAsync(requests);
        queryLogins = new QueryLoginApiAsync(requests);
        serverGroups = new ServerGroupApiAsync(requests);
        servers = new VirtualServerApiAsync(requests);
    }

    /** @return asynchronous Web Query API key administration */
    public ApiKeyApiAsync apiKeys() {
        return apiKeys;
    }

    /** @return ban rule administration */
    public BanApiAsync bans() {
        return bans;
    }

    /** @return channel administration */
    public ChannelApiAsync channels() {
        return channels;
    }

    /** @return channel group administration */
    public ChannelGroupApiAsync channelGroups() {
        return channelGroups;
    }

    /** @return administration of connected clients */
    public ClientApiAsync clients() {
        return clients;
    }

    /** @return complaint administration */
    public ComplaintApiAsync complaints() {
        return complaints;
    }

    /** @return administration of clients in the virtual server database */
    public DatabaseClientApiAsync databaseClients() {
        return databaseClients;
    }

    /** @return server notification registration and local listeners */
    public EventApiAsync events() {
        return events;
    }

    /** @return file repository access, transfers and icons */
    public FileApiAsync files() {
        return files;
    }

    /** @return server instance administration */
    public InstanceApiAsync instance() {
        return instance;
    }

    /** @return text and offline messages */
    public MessageApiAsync messages() {
        return messages;
    }

    /** @return permission metadata and assignments */
    public PermissionApiAsync permissions() {
        return permissions;
    }

    /** @return privilege key administration */
    public PrivilegeKeyApiAsync privilegeKeys() {
        return privilegeKeys;
    }

    /** @return ServerQuery login administration */
    public QueryLoginApiAsync queryLogins() {
        return queryLogins;
    }

    /** @return server group administration */
    public ServerGroupApiAsync serverGroups() {
        return serverGroups;
    }

    /** @return virtual server lifecycle and administration of the selected virtual server */
    public VirtualServerApiAsync servers() {
        return servers;
    }

    /**
     * Executes one already-encoded ServerQuery command through the bounded command queue.
     * Session restrictions, deadlines, cancellation and retry policy apply as for typed methods.
     * The caller owns value encoding and interpreting server-specific fields. Use typed methods
     * when available; do not pass untrusted command text. Unknown commands are not retried by default.
     *
     * @param line one command without control characters; encode values with
     *             {@link CommandEncoding#encode(String)}
     * @return the decoded rows and original response text; server errors fail the future with
     *         {@link TS3CommandFailedException}
     * @throws IllegalArgumentException if the command line is invalid
     * @see TS3BaseApi#executeRawCommand(String)
     */
    public CompletableFuture<DefaultArrayResponse> executeRawCommand(String line) {
        return requests.response(QueryCommands.rawCommand(line), response -> response);
    }

    /** @see TS3BaseApi#whoAmI() */
    public CompletableFuture<ServerQueryInfo> whoAmI() {
        return requests.model(QueryCommands.whoAmI(), ServerQueryInfo::new);
    }

    /** @see TS3BaseApi#setNickname(String) */
    public CompletableFuture<Void> setNickname(String nickname) {
        return updateClient(Map.of(ClientProperty.CLIENT_NICKNAME, nickname));
    }

    /** @see TS3BaseApi#updateClient(Map) */
    public CompletableFuture<Void> updateClient(Map<ClientProperty, String> properties) {
        return requests.run(ClientCommands.clientUpdate(properties));
    }

    /** @see TS3BaseApi#moveQuery(int) */
    public CompletableFuture<Void> moveQuery(int channelId) {
        return moveQuery(channelId, null);
    }

    /** @see TS3BaseApi#moveQuery(int, String) */
    public CompletableFuture<Void> moveQuery(int channelId, String channelPassword) {
        return requests.run(ClientCommands.clientMove(0, channelId, channelPassword));
    }
}
