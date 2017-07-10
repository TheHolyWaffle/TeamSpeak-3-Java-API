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

import com.github.theholywaffle.teamspeak3.api.PrivilegeKeyType;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public final class PrivilegeKeyCommands {

	private PrivilegeKeyCommands() {
		throw new Error("No instances");
	}

	public static Command privilegeKeyAdd(PrivilegeKeyType type, int groupId, int channelId, String description) {
		if (type == null) {
			throw new IllegalArgumentException("Privilege key type cannot be null");
		}

		CommandBuilder builder = new CommandBuilder("privilegekeyadd", 4);
		builder.add(new KeyValueParam("tokentype", type.getIndex()));
		builder.add(new KeyValueParam("tokenid1", groupId));
		builder.add(new KeyValueParam("tokenid2", channelId));
		builder.addIf(description != null, new KeyValueParam("tokendescription", description));
		return builder.build();
	}

	public static Command privilegeKeyDelete(String token) {
		if (token == null || token.isEmpty()) {
			throw new IllegalArgumentException("Privilege key must be a non-empty string");
		}

		return new CommandBuilder("privilegekeydelete", 1).add(new KeyValueParam("token", token)).build();
	}

	public static Command privilegeKeyList() {
		return new CommandBuilder("privilegekeylist").build();
	}

	public static Command privilegeKeyUse(String token) {
		if (token == null || token.isEmpty()) {
			throw new IllegalArgumentException("Privilege key must be a non-empty string");
		}

		return new CommandBuilder("privilegekeyuse", 1).add(new KeyValueParam("token", token)).build();
	}
}
