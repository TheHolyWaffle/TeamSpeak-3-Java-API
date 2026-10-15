package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.LogLevel;
import com.github.theholywaffle.teamspeak3.api.Snapshot;
import com.github.theholywaffle.teamspeak3.api.VirtualServerId;
import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.ConnectionInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.CreatedVirtualServer;
import com.github.theholywaffle.teamspeak3.api.wrapper.TemporaryServerPassword;
import com.github.theholywaffle.teamspeak3.api.wrapper.VirtualServer;
import com.github.theholywaffle.teamspeak3.api.wrapper.VirtualServerInfo;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Virtual server lifecycle on the instance and administration of the selected virtual server.
 * <p>Temporary passwords allow time-limited access to the selected server:</p>
 * <pre>{@code
 * api.servers().addTemporaryPassword("guest-pass", "Guest access", Duration.ofHours(2));
 * var passwords = api.servers().temporaryPasswords();
 * api.servers().deleteTemporaryPassword("guest-pass");
 * }</pre>
 *
 * @see VirtualServerApiAsync the asynchronous form
 */
public final class VirtualServerApi {

    private final VirtualServerApiAsync async;

    VirtualServerApi(VirtualServerApiAsync async) {
        this.async = async;
    }

    /**
     * Lists all virtual servers of the instance.
     *
     * @return the virtual servers
     */
    public List<VirtualServer> list() {
        return CommandFutures.join(async.list());
    }

    /**
     * Finds the virtual server listening on a voice port.
     *
     * @param port the UDP voice port
     * @return the virtual server ID, or empty if no virtual server uses that port
     */
    public Optional<VirtualServerId> findIdByPort(int port) {
        return CommandFutures.join(async.findIdByPort(port));
    }

    /**
     * Creates and starts a virtual server.
     *
     * @param name the server name
     * @param properties further server properties; may be empty
     * @return the new server's ID, port and administrator token
     */
    public CreatedVirtualServer create(String name, Map<VirtualServerProperty, String> properties) {
        return CommandFutures.join(async.create(name, properties));
    }

    /**
     * Deletes a stopped virtual server.
     *
     * @param serverId the virtual server ID
     */
    public void delete(VirtualServerId serverId) {
        CommandFutures.join(async.delete(serverId));
    }

    /**
     * Starts a virtual server.
     *
     * @param serverId the virtual server ID
     */
    public void start(VirtualServerId serverId) {
        CommandFutures.join(async.start(serverId));
    }

    /**
     * Stops a virtual server.
     *
     * @param serverId the virtual server ID
     */
    public void stop(VirtualServerId serverId) {
        CommandFutures.join(async.stop(serverId));
    }

    /**
     * Stops a virtual server, telling clients why.
     *
     * @param serverId the virtual server ID
     * @param reason the reason, or {@code null}
     */
    public void stop(VirtualServerId serverId, String reason) {
        CommandFutures.join(async.stop(serverId, reason));
    }

    /**
     * Gets detailed information about the selected virtual server.
     *
     * @return the server information
     */
    public VirtualServerInfo info() {
        return CommandFutures.join(async.info());
    }

    /**
     * Changes properties of the selected virtual server.
     *
     * @param properties the properties to change
     */
    public void edit(Map<VirtualServerProperty, String> properties) {
        CommandFutures.join(async.edit(properties));
    }

    /**
     * Gets connection statistics of the selected virtual server.
     *
     * @return the connection statistics
     */
    public ConnectionInfo connectionInfo() {
        return CommandFutures.join(async.connectionInfo());
    }

    /**
     * Creates a snapshot of the selected virtual server's configuration.
     *
     * @return the snapshot
     */
    public Snapshot createSnapshot() {
        return CommandFutures.join(async.createSnapshot());
    }

    /**
     * Restores the selected virtual server's configuration from a snapshot.
     *
     * @param snapshot the snapshot
     */
    public void deploySnapshot(Snapshot snapshot) {
        CommandFutures.join(async.deploySnapshot(snapshot));
    }

    /**
     * Writes a custom server log entry. The selected virtual server and the caller's permissions
     * determine its destination; this command has no explicit instance-log selector.
     *
     * @param level the severity
     * @param message the nonempty, unencoded log message
     */
    public void addLogEntry(LogLevel level, String message) {
        CommandFutures.join(async.addLogEntry(level, message));
    }

    /**
     * Reads the most recent log lines of the selected virtual server.
     *
     * @param lines the number of lines, between 1 and 100
     * @return the log lines, oldest first
     */
    public List<String> logEntries(int lines) {
        return CommandFutures.join(async.logEntries(lines));
    }

    /**
     * Creates a temporary password for the selected virtual server, placing joining clients in its default channel.
     * Requires {@code b_virtualserver_modify_password}.
     *
     * @param password the nonempty, unencoded temporary password
     * @param description the unencoded description, possibly empty
     * @param lifetime a positive duration in whole seconds
     */
    public void addTemporaryPassword(String password, String description, Duration lifetime) {
        CommandFutures.join(async.addTemporaryPassword(password, description, lifetime));
    }

    /**
     * Creates a temporary server password that also directs joining clients to a channel.
     * Requires {@code b_virtualserver_modify_password}.
     *
     * @param password the nonempty, unencoded temporary password
     * @param description the unencoded description, possibly empty
     * @param lifetime a positive duration in whole seconds
     * @param targetChannel the channel to join, or empty for the default channel
     * @param targetChannelPassword the unencoded channel password, or empty if none is required
     */
    public void addTemporaryPassword(
            String password,
            String description,
            Duration lifetime,
            Optional<ChannelId> targetChannel,
            Optional<String> targetChannelPassword) {
        CommandFutures.join(
                async.addTemporaryPassword(password, description, lifetime, targetChannel, targetChannelPassword));
    }

    /**
     * Lists active temporary passwords on the selected virtual server, including their clear-text secrets.
     * Requires {@code b_virtualserver_modify_password}.
     * Each result exposes the creator, validity times and target channel; its {@code toString()}
     * redacts both the server and target-channel passwords.
     *
     * @return the passwords, or an empty list if none are active
     */
    public List<TemporaryServerPassword> temporaryPasswords() {
        return CommandFutures.join(async.temporaryPasswords());
    }

    /**
     * Deletes one temporary password on the selected virtual server. Deleting a password that is
     * already absent succeeds.
     * Requires {@code b_virtualserver_modify_password}.
     *
     * @param password the nonempty, unencoded password to delete
     */
    public void deleteTemporaryPassword(String password) {
        CommandFutures.join(async.deleteTemporaryPassword(password));
    }
}
