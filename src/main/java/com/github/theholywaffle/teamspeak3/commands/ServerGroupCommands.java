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

import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;

public final class ServerGroupCommands {

	private ServerGroupCommands() {
		throw new Error("No instances");
	}

	public static Command serverGroupAdd(String groupName, PermissionGroupDatabaseType type) {
		if (groupName == null || groupName.isEmpty()) {
			throw new IllegalArgumentException("Server group name must be a non-empty string");
		}

		CommandBuilder builder = new CommandBuilder("servergroupadd", 2);
		builder.add(new KeyValueParam("name", groupName));
		if (type != null) {
			builder.add(new KeyValueParam("type", type.getIndex()));
		}
		return builder.build();
	}

	public static Command serverGroupAddClient(int groupId, int clientDBId) {
		CommandBuilder builder = new CommandBuilder("servergroupaddclient", 2);
		builder.add(new KeyValueParam("sgid", groupId));
		builder.add(new KeyValueParam("cldbid", clientDBId));
		return builder.build();
	}

	public static Command serverGroupClientList(int groupId) {
		CommandBuilder builder = new CommandBuilder("servergroupclientlist", 2);
		builder.add(new KeyValueParam("sgid", groupId));
		builder.add(new OptionParam("names"));
		return builder.build();
	}

	public static Command serverGroupCopy(int sourceGroupId, int targetGroupId, PermissionGroupDatabaseType type) {
		return serverGroupCopy(sourceGroupId, targetGroupId, "name", type);
	}

	public static Command serverGroupCopy(int sourceGroupId, String groupName, PermissionGroupDatabaseType type) {
		return serverGroupCopy(sourceGroupId, 0, groupName, type);
	}

	private static Command serverGroupCopy(int sourceGroupId, int targetGroupId, String groupName, PermissionGroupDatabaseType type) {
		if (type == null) {
			throw new IllegalArgumentException("Group type cannot be null");
		}
		if (groupName == null || groupName.isEmpty()) {
			throw new IllegalArgumentException("Server group name must be a non-empty string");
		}

		CommandBuilder builder = new CommandBuilder("servergroupcopy", 4);
		builder.add(new KeyValueParam("ssgid", sourceGroupId));
		builder.add(new KeyValueParam("tsgid", targetGroupId));
		builder.add(new KeyValueParam("name", groupName));
		builder.add(new KeyValueParam("type", type.getIndex()));
		return builder.build();
	}

	public static Command serverGroupDel(int id, boolean force) {
		CommandBuilder builder = new CommandBuilder("servergroupdel", 2);
		builder.add(new KeyValueParam("sgid", id));
		builder.add(new KeyValueParam("force", force));
		return builder.build();
	}

	public static Command serverGroupDelClient(int groupId, int clientDBId) {
		CommandBuilder builder = new CommandBuilder("servergroupdelclient", 2);
		builder.add(new KeyValueParam("sgid", groupId));
		builder.add(new KeyValueParam("cldbid", clientDBId));
		return builder.build();
	}

	public static Command serverGroupList() {
		return new CommandBuilder("servergrouplist").build();
	}

	public static Command serverGroupRename(int id, String groupName) {
		if (groupName == null || groupName.isEmpty()) {
			throw new IllegalArgumentException("Server group name must be a non-empty string");
		}

		CommandBuilder builder = new CommandBuilder("servergrouprename", 2);
		builder.add(new KeyValueParam("sgid", id));
		builder.add(new KeyValueParam("name", groupName));
		return builder.build();
	}

	public static Command serverGroupsByClientId(int clientDBId) {
		return new CommandBuilder("servergroupsbyclientid", 1).add(new KeyValueParam("cldbid", clientDBId)).build();
	}
}
