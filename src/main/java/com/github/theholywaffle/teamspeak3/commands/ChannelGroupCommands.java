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

public final class ChannelGroupCommands {

	private ChannelGroupCommands() {
		throw new Error("No instances");
	}

	public static Command channelGroupAdd(String groupName, PermissionGroupDatabaseType type) {
		if (groupName == null || groupName.isEmpty()) {
			throw new IllegalArgumentException("Channel group name must be a non-empty string");
		}

		CommandBuilder builder = new CommandBuilder("channelgroupadd", 2);
		builder.add(new KeyValueParam("name", groupName));
		if (type != null) {
			builder.add(new KeyValueParam("type", type.getIndex()));
		}
		return builder.build();
	}

	public static Command channelGroupClientList(int channelId, int clientDBId, int groupId) {
		CommandBuilder builder = new CommandBuilder("channelgroupclientlist", 3);
		builder.addIf(channelId > 0, new KeyValueParam("cid", channelId));
		builder.addIf(clientDBId > 0, new KeyValueParam("cldbid", clientDBId));
		builder.addIf(groupId > 0, new KeyValueParam("cgid", groupId));
		return builder.build();
	}

	public static Command channelGroupCopy(int sourceGroupId, int targetGroupId, PermissionGroupDatabaseType type) {
		return channelGroupCopy(sourceGroupId, targetGroupId, "name", type);
	}

	public static Command channelGroupCopy(int sourceGroupId, String groupName, PermissionGroupDatabaseType type) {
		return channelGroupCopy(sourceGroupId, 0, groupName, type);
	}

	private static Command channelGroupCopy(int sourceGroupId, int targetGroupId, String groupName, PermissionGroupDatabaseType type) {
		if (type == null) {
			throw new IllegalArgumentException("Group type cannot be null");
		}
		if (groupName == null || groupName.isEmpty()) {
			throw new IllegalArgumentException("Channel group name must be a non-empty string");
		}

		CommandBuilder builder = new CommandBuilder("channelgroupcopy", 4);
		builder.add(new KeyValueParam("scgid", sourceGroupId));
		builder.add(new KeyValueParam("tcgid", targetGroupId));
		builder.add(new KeyValueParam("name", groupName));
		builder.add(new KeyValueParam("type", type.getIndex()));
		return builder.build();
	}

	public static Command channelGroupDel(int channelGroupId, boolean forced) {
		CommandBuilder builder = new CommandBuilder("channelgroupdel", 2);
		builder.add(new KeyValueParam("cgid", channelGroupId));
		builder.add(new KeyValueParam("force", forced));
		return builder.build();
	}

	public static Command channelGroupList() {
		return new CommandBuilder("channelgrouplist").build();
	}

	public static Command channelGroupRename(int groupId, String groupName) {
		if (groupName == null || groupName.isEmpty()) {
			throw new IllegalArgumentException("Channel group name must be a non-empty string");
		}

		CommandBuilder builder = new CommandBuilder("channelgrouprename", 2);
		builder.add(new KeyValueParam("cgid", groupId));
		builder.add(new KeyValueParam("name", groupName));
		return builder.build();
	}

	public static Command setClientChannelGroup(int groupId, int channelId, int clientDBId) {
		CommandBuilder builder = new CommandBuilder("setclientchannelgroup", 3);
		builder.add(new KeyValueParam("cgid", groupId));
		builder.add(new KeyValueParam("cid", channelId));
		builder.add(new KeyValueParam("cldbid", clientDBId));
		return builder.build();
	}
}
