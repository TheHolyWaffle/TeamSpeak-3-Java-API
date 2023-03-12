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

import com.github.theholywaffle.teamspeak3.api.ServerGroupType;
import com.github.theholywaffle.teamspeak3.commands.parameter.ArrayParameter;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;

public final class PermissionCommands {

	private PermissionCommands() {
		throw new Error("No instances");
	}

	/*
	 * General commands
	 */

	public static Command permFind(String permName) {
		nonEmptyPermissionName(permName);

		return new CommandBuilder("permfind", 1).add(new KeyValueParam("permsid", permName)).build();
	}

	public static Command permGet(String... permNames) {
		nonEmptyPermissionArray(permNames);

		CommandBuilder builder = new CommandBuilder("permget", 1);

		ArrayParameter permissions = new ArrayParameter(permNames.length);
		for (String permName : permNames) {
			nonEmptyPermissionName(permName);
			permissions.add(new KeyValueParam("permsid", permName));
		}
		builder.add(permissions);

		return builder.build();
	}

	public static Command permIdGetByName(String permName) {
		nonEmptyPermissionName(permName);

		return new CommandBuilder("permidgetbyname", 1).add(new KeyValueParam("permsid", permName)).build();
	}

	public static Command permIdGetByName(String... permNames) {
		nonEmptyPermissionArray(permNames);

		CommandBuilder builder = new CommandBuilder("permidgetbyname", 1);

		ArrayParameter permissions = new ArrayParameter(permNames.length);
		for (String permName : permNames) {
			nonEmptyPermissionName(permName);
			permissions.add(new KeyValueParam("permsid", permName));
		}
		builder.add(permissions);

		return builder.build();
	}

	public static Command permissionList() {
		return new CommandBuilder("permissionlist").build();
	}

	public static Command permOverview(int channelId, int clientDBId) {
		CommandBuilder builder = new CommandBuilder("permoverview", 3);
		builder.add(new KeyValueParam("cid", channelId));
		builder.add(new KeyValueParam("cldbid", clientDBId));
		builder.add(new KeyValueParam("permid", 0));
		return builder.build();
	}

	public static Command permReset() {
		return new CommandBuilder("permreset").build();
	}

	/*
	 * Client commands
	 */

	public static Command clientAddPerm(int clientDBId, String permName, int permValue, boolean skip) {
		nonEmptyPermissionName(permName);

		CommandBuilder builder = new CommandBuilder("clientaddperm", 4);
		builder.add(new KeyValueParam("cldbid", clientDBId));
		builder.add(new KeyValueParam("permsid", permName));
		builder.add(new KeyValueParam("permvalue", permValue));
		builder.add(new KeyValueParam("permskip", skip));
		return builder.build();
	}

	public static Command clientAddPerm(int clientDBId, String permName, boolean permValue, boolean skip) {
		nonEmptyPermissionName(permName);

		CommandBuilder builder = new CommandBuilder("clientaddperm", 4);
		builder.add(new KeyValueParam("cldbid", clientDBId));
		builder.add(new KeyValueParam("permsid", permName));
		builder.add(new KeyValueParam("permvalue", permValue));
		builder.add(new KeyValueParam("permskip", skip));
		return builder.build();
	}

	public static Command clientDelPerm(int clientDBId, String permName) {
		nonEmptyPermissionName(permName);

		CommandBuilder builder = new CommandBuilder("clientdelperm", 2);
		builder.add(new KeyValueParam("cldbid", clientDBId));
		builder.add(new KeyValueParam("permsid", permName));
		return builder.build();
	}

	public static Command clientPermList(int clientDBId) {
		CommandBuilder builder = new CommandBuilder("clientpermlist", 2);
		builder.add(new KeyValueParam("cldbid", clientDBId));
		builder.add(new OptionParam("permsid"));
		return builder.build();
	}

	/*
	 * Channel commands
	 */

	public static Command channelAddPerm(int channelId, String permName, int permValue) {
		nonEmptyPermissionName(permName);

		CommandBuilder builder = new CommandBuilder("channeladdperm", 3);
		builder.add(new KeyValueParam("cid", channelId));
		builder.add(new KeyValueParam("permsid", permName));
		builder.add(new KeyValueParam("permvalue", permValue));
		return builder.build();
	}

	public static Command channelDelPerm(int channelId, String permName) {
		nonEmptyPermissionName(permName);

		CommandBuilder builder = new CommandBuilder("channeldelperm", 2);
		builder.add(new KeyValueParam("cid", channelId));
		builder.add(new KeyValueParam("permsid", permName));
		return builder.build();
	}

	public static Command channelPermList(int channelId) {
		CommandBuilder builder = new CommandBuilder("channelpermlist", 2);
		builder.add(new KeyValueParam("cid", channelId));
		builder.add(new OptionParam("permsid"));
		return builder.build();
	}

	/*
	 * Channel client commands
	 */

	public static Command channelClientAddPerm(int channelId, int clientDBId, String permName, int permValue) {
		nonEmptyPermissionName(permName);

		CommandBuilder builder = new CommandBuilder("channelclientaddperm", 4);
		builder.add(new KeyValueParam("cid", channelId));
		builder.add(new KeyValueParam("cldbid", clientDBId));
		builder.add(new KeyValueParam("permsid", permName));
		builder.add(new KeyValueParam("permvalue", permValue));
		return builder.build();
	}

	public static Command channelClientDelPerm(int channelId, int clientDBId, String permName) {
		nonEmptyPermissionName(permName);

		CommandBuilder builder = new CommandBuilder("channelclientdelperm", 3);
		builder.add(new KeyValueParam("cid", channelId));
		builder.add(new KeyValueParam("cldbid", clientDBId));
		builder.add(new KeyValueParam("permsid", permName));
		return builder.build();
	}

	public static Command channelClientPermList(int channelId, int clientDBId) {
		CommandBuilder builder = new CommandBuilder("channelclientpermlist", 3);
		builder.add(new KeyValueParam("cid", channelId));
		builder.add(new KeyValueParam("cldbid", clientDBId));
		builder.add(new OptionParam("permsid"));
		return builder.build();
	}

	/*
	 * Channel group commands
	 */

	public static Command channelGroupAddPerm(int groupId, String permName, int permValue) {
		nonEmptyPermissionName(permName);

		CommandBuilder builder = new CommandBuilder("channelgroupaddperm", 3);
		builder.add(new KeyValueParam("cgid", groupId));
		builder.add(new KeyValueParam("permsid", permName));
		builder.add(new KeyValueParam("permvalue", permValue));
		return builder.build();
	}

	public static Command channelGroupDelPerm(int groupId, String permName) {
		nonEmptyPermissionName(permName);

		CommandBuilder builder = new CommandBuilder("channelgroupdelperm", 2);
		builder.add(new KeyValueParam("cgid", groupId));
		builder.add(new KeyValueParam("permsid", permName));
		return builder.build();
	}

	public static Command channelGroupPermList(int groupId) {
		CommandBuilder builder = new CommandBuilder("channelgrouppermlist", 2);
		builder.add(new KeyValueParam("cgid", groupId));
		builder.add(new OptionParam("permsid"));
		return builder.build();
	}

	/*
	 * Server group commands
	 */

	public static Command serverGroupAddPerm(int groupId, String permName, int permValue,
	                                         boolean negate, boolean skip) {
		nonEmptyPermissionName(permName);

		CommandBuilder builder = new CommandBuilder("servergroupaddperm", 5);
		builder.add(new KeyValueParam("sgid", groupId));
		builder.add(new KeyValueParam("permsid", permName));
		builder.add(new KeyValueParam("permvalue", permValue));
		builder.add(new KeyValueParam("permnegated", negate));
		builder.add(new KeyValueParam("permskip", skip));
		return builder.build();
	}

	public static Command serverGroupAutoAddPerm(ServerGroupType type, String permName, int permValue,
	                                             boolean negate, boolean skip) {
		nonNullServerGroupType(type);
		nonEmptyPermissionName(permName);

		CommandBuilder builder = new CommandBuilder("servergroupautoaddperm", 5);
		builder.add(new KeyValueParam("sgtype", type.getIndex()));
		builder.add(new KeyValueParam("permsid", permName));
		builder.add(new KeyValueParam("permvalue", permValue));
		builder.add(new KeyValueParam("permnegated", negate));
		builder.add(new KeyValueParam("permskip", skip));
		return builder.build();
	}

	public static Command serverGroupDelPerm(int groupId, String permName) {
		nonEmptyPermissionName(permName);

		CommandBuilder builder = new CommandBuilder("servergroupdelperm", 2);
		builder.add(new KeyValueParam("sgid", groupId));
		builder.add(new KeyValueParam("permsid", permName));
		return builder.build();
	}

	public static Command serverGroupAutoDelPerm(ServerGroupType type, String permName) {
		nonNullServerGroupType(type);
		nonEmptyPermissionName(permName);

		CommandBuilder builder = new CommandBuilder("servergroupautodelperm", 2);
		builder.add(new KeyValueParam("sgtype", type.getIndex()));
		builder.add(new KeyValueParam("permsid", permName));
		return builder.build();
	}

	public static Command serverGroupPermList(int groupId) {
		CommandBuilder builder = new CommandBuilder("servergrouppermlist", 2);
		builder.add(new KeyValueParam("sgid", groupId));
		builder.add(new OptionParam("permsid"));
		return builder.build();
	}

	/*
	 * Validation
	 */

	private static void nonEmptyPermissionName(String permName) {
		if (permName == null || permName.isEmpty()) {
			throw new IllegalArgumentException("Permission name must be a non-empty string");
		}
	}

	private static void nonEmptyPermissionArray(String[] permNames) {
		if (permNames == null || permNames.length == 0) {
			throw new IllegalArgumentException("Permission name array cannot be null or empty");
		}
	}

	private static void nonNullServerGroupType(ServerGroupType type) {
		if (type == null) {
			throw new IllegalArgumentException("Server group type cannot be null");
		}
	}
}
