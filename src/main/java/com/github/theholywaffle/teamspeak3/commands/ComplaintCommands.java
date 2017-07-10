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

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public final class ComplaintCommands {

	private ComplaintCommands() {
		throw new Error("No instances");
	}

	public static Command complainAdd(int clientDBId, String message) {
		CommandBuilder builder = new CommandBuilder("complainadd", 2);
		builder.add(new KeyValueParam("tcldbid", clientDBId));
		builder.add(new KeyValueParam("message", message));
		return builder.build();
	}

	public static Command complainDel(int clientDBId, int fromClientDBId) {
		CommandBuilder builder = new CommandBuilder("complaindel", 2);
		builder.add(new KeyValueParam("tcldbid", clientDBId));
		builder.add(new KeyValueParam("fcldbid", fromClientDBId));
		return builder.build();
	}

	public static Command complainDelAll(int clientDBId) {
		return new CommandBuilder("complaindelall", 1).add(new KeyValueParam("tcldbid", clientDBId)).build();
	}

	public static Command complainList(int clientDBId) {
		return new CommandBuilder("complainlist", 1).addIf(clientDBId > 0, new KeyValueParam("tcldbid", clientDBId)).build();
	}
}
