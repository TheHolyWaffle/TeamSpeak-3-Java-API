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

import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.ValueParam;

public final class QueryCommands {

	private QueryCommands() {
		throw new Error("No instances");
	}

	public static Command logIn(String username, String password) {
		if (username == null || username.isEmpty()) {
			throw new IllegalArgumentException("Username must be a non-empty string");
		}
		if (password == null || password.isEmpty()) {
			throw new IllegalArgumentException("Password must be a non-empty string");
		}

		return new CommandBuilder("login", 2).add(new ValueParam(username)).add(new ValueParam(password)).build();
	}

	public static Command logOut() {
		return new CommandBuilder("logout").build();
	}

	public static Command quit() {
		return new CommandBuilder("quit").build();
	}

	public static Command serverNotifyRegister(TS3EventType eventType, int channelId) {
		if (eventType == null) {
			throw new IllegalArgumentException("Event type cannot be null");
		}

		CommandBuilder builder = new CommandBuilder("servernotifyregister", 2);
		builder.add(new KeyValueParam("event", eventType.toString()));
		builder.addIf(channelId >= 0, new KeyValueParam("id", channelId));
		return builder.build();
	}

	public static Command serverNotifyUnregister() {
		return new CommandBuilder("servernotifyunregister").build();
	}

	public static Command useId(int id) {
		return new CommandBuilder("use", 1).add(new KeyValueParam("sid", id)).build();
	}

	public static Command usePort(int port) {
		return new CommandBuilder("use", 1).add(new KeyValueParam("port", port)).build();
	}

	public static Command whoAmI() {
		return new CommandBuilder("whoami").build();
	}
}
