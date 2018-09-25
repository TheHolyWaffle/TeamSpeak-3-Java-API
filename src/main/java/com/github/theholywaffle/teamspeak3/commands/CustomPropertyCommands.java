package com.github.theholywaffle.teamspeak3.commands;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2018 Bert De Geyter, Roger Baumgartner
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

public final class CustomPropertyCommands {

	private CustomPropertyCommands() {
		throw new Error("No instances");
	}

	public static Command customDelete(int clientDBId, String key) {
		CommandBuilder builder = new CommandBuilder("customdelete", 2);
		builder.add(new KeyValueParam("cldbid", clientDBId));
		builder.add(new KeyValueParam("ident", key));
		return builder.build();
	}

	public static Command customInfo(int clientDBId) {
		return new CommandBuilder("custominfo", 1).add(new KeyValueParam("cldbid", clientDBId)).build();
	}

	public static Command customSet(int clientDBId, String key, String value) {
		CommandBuilder builder = new CommandBuilder("customset", 3);
		builder.add(new KeyValueParam("cldbid", clientDBId));
		builder.add(new KeyValueParam("ident", key));
		builder.add(new KeyValueParam("value", value));
		return builder.build();
	}

	public static Command customSearch(String key, String pattern) {
		CommandBuilder builder = new CommandBuilder("customsearch", 2);
		builder.add(new KeyValueParam("ident", key));
		builder.add(new KeyValueParam("pattern", pattern));
		return builder.build();
	}
}
