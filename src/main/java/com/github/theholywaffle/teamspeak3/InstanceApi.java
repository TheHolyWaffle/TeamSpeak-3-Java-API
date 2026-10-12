package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.ServerInstanceProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.Binding;
import com.github.theholywaffle.teamspeak3.api.wrapper.HostInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.InstanceInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.Version;

import java.util.List;
import java.util.Map;

/**
 * Administration of the server instance and its process.
 *
 * @see InstanceApiAsync the asynchronous form
 */
public final class InstanceApi {

	private final InstanceApiAsync async;

	InstanceApi(InstanceApiAsync async) {
		this.async = async;
	}

	/**
	 * Gets the server instance configuration.
	 *
	 * @return the instance information
	 */
	public InstanceInfo info() {
		return CommandFutures.join(async.info());
	}

	/**
	 * Changes server instance properties.
	 *
	 * @param properties the properties to change
	 */
	public void edit(Map<ServerInstanceProperty, String> properties) {
		CommandFutures.join(async.edit(properties));
	}

	/**
	 * Gets statistics of the server instance and host.
	 *
	 * @return the host information
	 */
	public HostInfo hostInfo() {
		return CommandFutures.join(async.hostInfo());
	}

	/**
	 * Gets the server version, build and platform.
	 *
	 * @return the version information
	 */
	public Version version() {
		return CommandFutures.join(async.version());
	}

	/**
	 * Lists the IP addresses the server instance is bound to.
	 *
	 * @return the bindings
	 */
	public List<Binding> bindings() {
		return CommandFutures.join(async.bindings());
	}

	/**
	 * Reads the most recent instance log lines.
	 *
	 * @param lines the number of lines, between 1 and 100
	 * @return the log lines, oldest first
	 */
	public List<String> logEntries(int lines) {
		return CommandFutures.join(async.logEntries(lines));
	}

	/**
	 * Stops the entire server process, telling clients why.
	 *
	 * @param reason the reason, or {@code null}
	 */
	public void stopProcess(String reason) {
		CommandFutures.join(async.stopProcess(reason));
	}
}
