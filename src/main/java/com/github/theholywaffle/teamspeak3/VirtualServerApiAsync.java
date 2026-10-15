package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.Snapshot;
import com.github.theholywaffle.teamspeak3.api.VirtualServerId;
import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.ConnectionInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.CreatedVirtualServer;
import com.github.theholywaffle.teamspeak3.api.wrapper.VirtualServer;
import com.github.theholywaffle.teamspeak3.api.wrapper.VirtualServerInfo;
import com.github.theholywaffle.teamspeak3.commands.ServerCommands;
import com.github.theholywaffle.teamspeak3.commands.VirtualServerCommands;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous virtual server lifecycle and administration of the selected virtual server.
 *
 * @see VirtualServerApi the synchronous form and full documentation
 */
public final class VirtualServerApiAsync {

    private final QueryRequests requests;

    VirtualServerApiAsync(QueryRequests requests) {
        this.requests = requests;
    }

    /** @see VirtualServerApi#list() */
    public CompletableFuture<List<VirtualServer>> list() {
        return requests.models(VirtualServerCommands.serverList(), VirtualServer::new);
    }

    /** @see VirtualServerApi#findIdByPort(int) */
    public CompletableFuture<Optional<VirtualServerId>> findIdByPort(int port) {
        var id = requests.optional(
                VirtualServerCommands.serverIdGetByPort(port), row -> row.requireId("server_id", VirtualServerId::of));
        return QueryRequests.recover(id, QueryRequests.INVALID_SERVER_ID, Optional.empty());
    }

    /** @see VirtualServerApi#create(String, Map) */
    public CompletableFuture<CreatedVirtualServer> create(String name, Map<VirtualServerProperty, String> properties) {
        return requests.one(VirtualServerCommands.serverCreate(name, properties), CreatedVirtualServer::from);
    }

    /** @see VirtualServerApi#delete(VirtualServerId) */
    public CompletableFuture<Void> delete(VirtualServerId serverId) {
        return requests.run(VirtualServerCommands.serverDelete(serverId));
    }

    /** @see VirtualServerApi#start(VirtualServerId) */
    public CompletableFuture<Void> start(VirtualServerId serverId) {
        return requests.run(VirtualServerCommands.serverStart(serverId));
    }

    /** @see VirtualServerApi#stop(VirtualServerId) */
    public CompletableFuture<Void> stop(VirtualServerId serverId) {
        return stop(serverId, null);
    }

    /** @see VirtualServerApi#stop(VirtualServerId, String) */
    public CompletableFuture<Void> stop(VirtualServerId serverId, String reason) {
        return requests.run(VirtualServerCommands.serverStop(serverId, reason));
    }

    /** @see VirtualServerApi#info() */
    public CompletableFuture<VirtualServerInfo> info() {
        return requests.model(VirtualServerCommands.serverInfo(), VirtualServerInfo::new);
    }

    /** @see VirtualServerApi#edit(Map) */
    public CompletableFuture<Void> edit(Map<VirtualServerProperty, String> properties) {
        return requests.run(VirtualServerCommands.serverEdit(properties));
    }

    /** @see VirtualServerApi#connectionInfo() */
    public CompletableFuture<ConnectionInfo> connectionInfo() {
        return requests.model(VirtualServerCommands.serverRequestConnectionInfo(), ConnectionInfo::new);
    }

    /** @see VirtualServerApi#createSnapshot() */
    public CompletableFuture<Snapshot> createSnapshot() {
        return requests.response(
                VirtualServerCommands.serverSnapshotCreate(), response -> new Snapshot(response.getRawResponse()));
    }

    /** @see VirtualServerApi#deploySnapshot(Snapshot) */
    public CompletableFuture<Void> deploySnapshot(Snapshot snapshot) {
        return requests.run(VirtualServerCommands.serverSnapshotDeploy(snapshot.data()));
    }

    /** @see VirtualServerApi#logEntries(int) */
    public CompletableFuture<List<String>> logEntries(int lines) {
        return requests.rows(ServerCommands.logView(lines, false), row -> row.require("l"));
    }
}
