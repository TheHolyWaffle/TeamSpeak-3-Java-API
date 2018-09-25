package com.github.theholywaffle.teamspeak3.commands;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2017 Bert De Geyter, Roger Baumgartner
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.RawParam;

import java.util.Map;

public final class VirtualServerCommands {

	private VirtualServerCommands() {
		throw new Error("No instances");
	}

	public static Command serverCreate(String name, Map<VirtualServerProperty, String> options) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("Server name must be a non-empty string");
		}

		CommandBuilder builder = new CommandBuilder("servercreate", 2);
		builder.add(new KeyValueParam(VirtualServerProperty.VIRTUALSERVER_NAME.getName(), name));
		builder.addProperties(options);
		return builder.build();
	}

	public static Command serverDelete(int id) {
		return new CommandBuilder("serverdelete", 1).add(new KeyValueParam("sid", id)).build();
	}

	public static Command serverEdit(Map<VirtualServerProperty, String> options) {
		return new CommandBuilder("serveredit", 1).addProperties(options).build();
	}

	public static Command serverIdGetByPort(int port) {
		return new CommandBuilder("serveridgetbyport", 1).add(new KeyValueParam("virtualserver_port", port)).build();
	}

	public static Command serverInfo() {
		return new CommandBuilder("serverinfo").build();
	}

	public static Command serverList() {
		CommandBuilder builder = new CommandBuilder("serverlist", 2);
		builder.add(new OptionParam("uid"));
		builder.add(new OptionParam("all"));
		return builder.build();
	}

	public static Command serverRequestConnectionInfo() {
		return new CommandBuilder("serverrequestconnectioninfo").build();
	}

	public static Command serverSnapshotCreate() {
		return new CommandBuilder("serversnapshotcreate").build();
	}

	public static Command serverSnapshotDeploy(String snapshot) {
		if (snapshot == null || snapshot.isEmpty()) {
			throw new IllegalArgumentException("Server snapshot must be a non-empty string");
		}

		return new CommandBuilder("serversnapshotdeploy", 1).add(new RawParam(snapshot)).build();
	}

	public static Command serverStart(int id) {
		return new CommandBuilder("serverstart", 1).add(new KeyValueParam("sid", id)).build();
	}

	public static Command serverStop(int id, String reason) {
		CommandBuilder builder = new CommandBuilder("serverstop", 2);
		builder.add(new KeyValueParam("sid", id));
		builder.addIf(reason != null, new KeyValueParam("reasonmsg", reason));
		return builder.build();
	}
}
