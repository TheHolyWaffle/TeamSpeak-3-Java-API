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

import com.github.theholywaffle.teamspeak3.api.ServerInstanceProperty;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

import java.util.Map;

public final class ServerCommands {

	private ServerCommands() {
		throw new Error("No instances");
	}

	public static Command bindingList() {
		return new CommandBuilder("bindinglist").build();
	}

	public static Command gm(String message) {
		if (message == null || message.isEmpty()) {
			throw new IllegalArgumentException("Message must be a non-empty string");
		}

		return new CommandBuilder("gm", 1).add(new KeyValueParam("msg", message)).build();
	}

	public static Command hostInfo() {
		return new CommandBuilder("hostinfo").build();
	}

	public static Command instanceInfo() {
		return new CommandBuilder("instanceinfo").build();
	}

	public static Command instanceEdit(Map<ServerInstanceProperty, String> options) {
		return new CommandBuilder("instanceedit", 1).addProperties(options).build();
	}

	public static Command logView(int lines, boolean instance) {
		if (lines > 100) throw new IllegalArgumentException("Can only fetch up to 100 lines at once (" + lines + ")");

		CommandBuilder builder = new CommandBuilder("logview", 2);
		builder.addIf(lines > 0, new KeyValueParam("lines", lines));
		builder.addIf(instance, new KeyValueParam("instance", 1));
		return builder.build();
	}

	public static Command serverProcessStop(String reason) {
		CommandBuilder builder = new CommandBuilder("serverprocessstop", 1);
		builder.addIf(reason != null, new KeyValueParam("reasonmsg", reason));
		return builder.build();
	}

	public static Command version() {
		return new CommandBuilder("version").build();
	}
}
