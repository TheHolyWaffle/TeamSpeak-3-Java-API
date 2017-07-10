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

import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.ReasonIdentifier;
import com.github.theholywaffle.teamspeak3.commands.parameter.ArrayParameter;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;

import java.util.Map;

public final class ClientCommands {

	private ClientCommands() {
		throw new Error("No instances");
	}

	public static Command clientEdit(int clientId, Map<ClientProperty, String> options) {
		CommandBuilder builder = new CommandBuilder("clientedit", 2);
		builder.add(new KeyValueParam("clid", clientId));
		builder.addProperties(options);
		return builder.build();
	}

	public static Command clientFind(String pattern) {
		if (pattern == null || pattern.isEmpty()) {
			throw new IllegalArgumentException("Client name pattern must be a non-empty string");
		}

		return new CommandBuilder("clientfind", 1).add(new KeyValueParam("pattern", pattern)).build();
	}

	public static Command clientGetDBIdFromUId(String clientUId) {
		if (clientUId == null || clientUId.isEmpty()) {
			throw new IllegalArgumentException("Client UId must be a non-empty string");
		}

		return new CommandBuilder("clientgetdbidfromuid", 1).add(new KeyValueParam("cluid", clientUId)).build();
	}

	public static Command clientGetIds(String clientUId) {
		if (clientUId == null || clientUId.isEmpty()) {
			throw new IllegalArgumentException("Client UId must be a non-empty string");
		}

		return new CommandBuilder("clientgetids", 1).add(new KeyValueParam("cluid", clientUId)).build();
	}

	public static Command clientInfo(int clientId) {
		return new CommandBuilder("clientinfo", 1).add(new KeyValueParam("clid", clientId)).build();
	}

	public static Command clientKick(ReasonIdentifier reason, String reasonMessage, int... clientIds) {
		if (clientIds == null || clientIds.length == 0) {
			throw new IllegalArgumentException("Client ID array cannot be null or empty");
		}

		CommandBuilder builder = new CommandBuilder("clientkick", 3);
		builder.add(new KeyValueParam("reasonid", reason.getIndex()));
		builder.addIf(reasonMessage != null, new KeyValueParam("reasonmsg", reasonMessage));

		ArrayParameter clients = new ArrayParameter(clientIds.length);
		for (final int id : clientIds) {
			clients.add(new KeyValueParam("clid", id));
		}
		builder.add(clients);

		return builder.build();
	}

	public static Command clientList() {
		CommandBuilder builder = new CommandBuilder("clientlist", 10);
		builder.add(new OptionParam("uid"));
		builder.add(new OptionParam("away"));
		builder.add(new OptionParam("voice"));
		builder.add(new OptionParam("times"));
		builder.add(new OptionParam("groups"));
		builder.add(new OptionParam("info"));
		builder.add(new OptionParam("icon"));
		builder.add(new OptionParam("country"));
		builder.add(new OptionParam("ip"));
		builder.add(new OptionParam("badges"));
		return builder.build();
	}

	public static Command clientMove(int clientId, int channelId, String channelPassword) {
		CommandBuilder builder = new CommandBuilder("clientmove", 3);
		builder.add(new KeyValueParam("clid", clientId));
		builder.add(new KeyValueParam("cid", channelId));
		builder.addIf(channelPassword != null, new KeyValueParam("cpw", channelPassword));
		return builder.build();
	}

	public static Command clientMove(int[] clientIds, int channelId, String channelPassword) {
		if (clientIds == null || clientIds.length == 0) {
			throw new IllegalArgumentException("Client ID array cannot be null or empty");
		}

		CommandBuilder builder = new CommandBuilder("clientmove", 3);
		builder.add(new KeyValueParam("cid", channelId));
		builder.addIf(channelPassword != null, new KeyValueParam("cpw", channelPassword));

		ArrayParameter clients = new ArrayParameter(clientIds.length);
		for (final int clientId : clientIds) {
			clients.add(new KeyValueParam("clid", clientId));
		}
		builder.add(clients);

		return builder.build();
	}

	public static Command clientPoke(int clientId, String message) {
		CommandBuilder builder = new CommandBuilder("clientpoke", 2);
		builder.add(new KeyValueParam("clid", clientId));
		builder.add(new KeyValueParam("msg", message));
		return builder.build();
	}

	public static Command clientSetServerQueryLogin(String username) {
		CommandBuilder builder = new CommandBuilder("clientsetserverquerylogin", 1);
		builder.add(new KeyValueParam("client_login_name", username));
		return builder.build();
	}

	public static Command clientUpdate(Map<ClientProperty, String> options) {
		return new CommandBuilder("clientupdate", 1).addProperties(options).build();
	}

	public static Command sendTextMessage(int targetMode, int targetId, String message) {
		CommandBuilder builder = new CommandBuilder("sendtextmessage", 3);
		builder.add(new KeyValueParam("targetmode", targetMode));
		builder.add(new KeyValueParam("target", targetId));
		builder.add(new KeyValueParam("msg", message));
		return builder.build();
	}
}
