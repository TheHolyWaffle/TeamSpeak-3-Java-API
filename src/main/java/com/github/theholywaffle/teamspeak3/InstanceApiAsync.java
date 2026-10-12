package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ServerInstanceProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.Binding;
import com.github.theholywaffle.teamspeak3.api.wrapper.HostInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.InstanceInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.Version;
import com.github.theholywaffle.teamspeak3.commands.ServerCommands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous administration of the server instance and its process.
 *
 * @see InstanceApi the synchronous form and full documentation
 */
public final class InstanceApiAsync {

	private final QueryRequests requests;

	InstanceApiAsync(QueryRequests requests) {
		this.requests = requests;
	}

	/** @see InstanceApi#info() */
	public CompletableFuture<InstanceInfo> info() {
		return requests.model(ServerCommands.instanceInfo(), InstanceInfo::new);
	}

	/** @see InstanceApi#edit(Map) */
	public CompletableFuture<Void> edit(Map<ServerInstanceProperty, String> properties) {
		return requests.run(ServerCommands.instanceEdit(properties));
	}

	/** @see InstanceApi#hostInfo() */
	public CompletableFuture<HostInfo> hostInfo() {
		return requests.model(ServerCommands.hostInfo(), HostInfo::new);
	}

	/** @see InstanceApi#version() */
	public CompletableFuture<Version> version() {
		return requests.model(ServerCommands.version(), Version::new);
	}

	/** @see InstanceApi#bindings() */
	public CompletableFuture<List<Binding>> bindings() {
		return requests.models(ServerCommands.bindingList(), Binding::new);
	}

	/** @see InstanceApi#logEntries(int) */
	public CompletableFuture<List<String>> logEntries(int lines) {
		return requests.rows(ServerCommands.logView(lines, true), row -> row.get("l"));
	}

	/** @see InstanceApi#stopProcess(String) */
	public CompletableFuture<Void> stopProcess(String reason) {
		return requests.run(ServerCommands.serverProcessStop(reason));
	}
}
