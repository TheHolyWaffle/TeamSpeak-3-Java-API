package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.Snapshot;
import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.ConnectionInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.CreatedVirtualServer;
import com.github.theholywaffle.teamspeak3.api.wrapper.VirtualServer;
import com.github.theholywaffle.teamspeak3.api.wrapper.VirtualServerInfo;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Virtual server lifecycle on the instance and administration of the selected virtual server.
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
	public Optional<Integer> findIdByPort(int port) {
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
	public void delete(int serverId) {
		CommandFutures.join(async.delete(serverId));
	}

	/**
	 * Starts a virtual server.
	 *
	 * @param serverId the virtual server ID
	 */
	public void start(int serverId) {
		CommandFutures.join(async.start(serverId));
	}

	/**
	 * Stops a virtual server.
	 *
	 * @param serverId the virtual server ID
	 */
	public void stop(int serverId) {
		CommandFutures.join(async.stop(serverId));
	}

	/**
	 * Stops a virtual server, telling clients why.
	 *
	 * @param serverId the virtual server ID
	 * @param reason the reason, or {@code null}
	 */
	public void stop(int serverId, String reason) {
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
	 * Reads the most recent log lines of the selected virtual server.
	 *
	 * @param lines the number of lines, between 1 and 100
	 * @return the log lines, oldest first
	 */
	public List<String> logEntries(int lines) {
		return CommandFutures.join(async.logEntries(lines));
	}
}
