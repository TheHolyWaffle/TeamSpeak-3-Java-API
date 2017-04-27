package com.github.theholywaffle.teamspeak3;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2014 Bert De Geyter
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

import com.github.theholywaffle.teamspeak3.api.*;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.event.TS3Listener;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3FileTransferFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.*;
import com.github.theholywaffle.teamspeak3.commands.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * API to interact with the {@link TS3Query} synchronously.
 * <p>
 * This class is used to easily interact with a {@link TS3Query}. It constructs commands,
 * sends them to the TeamSpeak3 server, processes the response and returns the result.
 * </p><p>
 * All methods in this class are synchronous, so they will block until the response arrives.
 * Calls to this API will usually take about 50 milliseconds to complete (plus ping),
 * but delays can range up to 4 seconds.
 * If a command takes longer than 4 seconds to complete, a {@link TS3ConnectionFailedException}
 * will be thrown.
 * </p><p>
 * You won't be able to execute most commands while you're not logged in due to missing permissions.
 * Make sure to either pass your login credentials to the {@link TS3Config} object when
 * creating the {@code TS3Query} or to call {@link #login(String, String)} to log in.
 * </p><p>
 * After that, most commands also require you to select a {@linkplain VirtualServer virtual server}.
 * To do so, call either {@link #selectVirtualServerByPort(int)} or {@link #selectVirtualServerById(int)}.
 * </p><p>
 * Be aware that many methods in this class will return {@code null} or {@code -1} if a command fails.
 * </p>
 *
 * @see TS3ApiAsync The asynchronous version of the API
 */
public class TS3Api {

	/**
	 * The TS3 query to which this API sends its commands.
	 */
	private final TS3Query query;

	/**
	 * Creates a new synchronous API object for the given {@code TS3Query}.
	 * <p>
	 * <b>Usually, this constructor should not be called.</b> Use {@link TS3Query#getApi()} instead.
	 * </p>
	 *
	 * @param query
	 * 		the TS3Query to call
	 */
	public TS3Api(TS3Query query) {
		this.query = query;
	}

	/**
	 * Adds a new ban entry. At least one of the parameters {@code ip}, {@code name} or {@code uid} needs to be not null.
	 * Returns the ID of the newly created ban.
	 *
	 * @param ip
	 * 		a RegEx pattern to match a client's IP against, can be null
	 * @param name
	 * 		a RegEx pattern to match a client's name against, can be null
	 * @param uid
	 * 		the unique identifier of a client, can be null
	 * @param timeInSeconds
	 * 		the duration of the ban in seconds. 0 equals a permanent ban
	 * @param reason
	 * 		the reason for the ban, can be null
	 *
	 * @return the ID of the newly created ban entry
	 *
	 * @querycommands 1
	 * @see Pattern RegEx Pattern
	 * @see Client#getId()
	 * @see Client#getUniqueIdentifier()
	 * @see ClientInfo#getIp()
	 */
	public int addBan(String ip, String name, String uid, long timeInSeconds, String reason) {
		if (ip == null && name == null && uid == null) {
			throw new IllegalArgumentException("Either IP, Name or UID must be set");
		}

		final CBanAdd add = new CBanAdd(ip, name, uid, timeInSeconds, reason);
		if (query.doCommand(add)) {
			return add.getFirstResponse().getInt("banid");
		}
		return -1;
	}

	/**
	 * Adds a specified permission to a client in a specific channel.
	 *
	 * @param channelId
	 * 		the ID of the channel wherein the permission should be granted
	 * @param clientDBId
	 * 		the database ID of the client to add a permission to
	 * @param permName
	 * 		the name of the permission to grant
	 * @param permValue
	 * 		the numeric value of the permission (or for boolean permissions: 1 = true, 0 = false)
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see Client#getDatabaseId()
	 * @see Permission
	 */
	public boolean addChannelClientPermission(int channelId, int clientDBId, String permName, int permValue) {
		final CChannelClientAddPerm add = new CChannelClientAddPerm(channelId, clientDBId, permName, permValue);
		return query.doCommand(add);
	}

	/**
	 * Creates a new channel group for clients using a given name and returns its ID.
	 * <p>
	 * To create channel group templates or ones for server queries,
	 * use {@link #addChannelGroup(String, PermissionGroupDatabaseType)}.
	 * </p>
	 *
	 * @param name
	 * 		the name of the new channel group
	 *
	 * @return the ID of the newly created channel group
	 *
	 * @querycommands 1
	 * @see ChannelGroup
	 */
	public int addChannelGroup(String name) {
		return addChannelGroup(name, null);
	}

	/**
	 * Creates a new channel group using a given name and returns its ID.
	 *
	 * @param name
	 * 		the name of the new channel group
	 * @param type
	 * 		the desired type of channel group
	 *
	 * @return the ID of the newly created channel group
	 *
	 * @querycommands 1
	 * @see ChannelGroup
	 */
	public int addChannelGroup(String name, PermissionGroupDatabaseType type) {
		final CChannelGroupAdd add = new CChannelGroupAdd(name, type);
		if (query.doCommand(add)) {
			return add.getFirstResponse().getInt("cgid");
		}
		return -1;
	}

	/**
	 * Adds a specified permission to a channel group.
	 *
	 * @param groupId
	 * 		the ID of the channel group to grant the permission
	 * @param permName
	 * 		the name of the permission to be granted
	 * @param permValue
	 * 		the numeric value of the permission (or for boolean permissions: 1 = true, 0 = false)
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 * @see Permission
	 */
	public boolean addChannelGroupPermission(int groupId, String permName, int permValue) {
		final CChannelGroupAddPerm add = new CChannelGroupAddPerm(groupId, permName, permValue);
		return query.doCommand(add);
	}

	/**
	 * Adds a specified permission to a channel.
	 *
	 * @param channelId
	 * 		the ID of the channel wherein the permission should be granted
	 * @param permName
	 * 		the name of the permission to grant
	 * @param permValue
	 * 		the numeric value of the permission (or for boolean permissions: 1 = true, 0 = false)
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see Permission
	 */
	public boolean addChannelPermission(int channelId, String permName, int permValue) {
		final CChannelAddPerm perm = new CChannelAddPerm(channelId, permName, permValue);
		return query.doCommand(perm);
	}

	/**
	 * Adds a specified permission to a channel.
	 *
	 * @param clientDBId
	 * 		the database ID of the client to grant the permission
	 * @param permName
	 * 		the name of the permission to grant
	 * @param value
	 * 		the numeric value of the permission (or for boolean permissions: 1 = true, 0 = false)
	 * @param skipped
	 * 		if set to {@code true}, the permission will not be overridden by channel group permissions
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see Permission
	 */
	public boolean addClientPermission(int clientDBId, String permName, int value, boolean skipped) {
		final CClientAddPerm add = new CClientAddPerm(clientDBId, permName, value, skipped);
		return query.doCommand(add);
	}

	/**
	 * Adds a client to the specified server group.
	 * <p>
	 * Please note that a client cannot be added to default groups or template groups.
	 * </p>
	 *
	 * @param groupId
	 * 		the ID of the server group to add the client to
	 * @param clientDatabaseId
	 * 		the database ID of the client to add
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 * @see Client#getDatabaseId()
	 */
	public boolean addClientToServerGroup(int groupId, int clientDatabaseId) {
		final CServerGroupAddClient add = new CServerGroupAddClient(groupId, clientDatabaseId);
		return query.doCommand(add);
	}

	/**
	 * Submits a complaint about the specified client.
	 * The length of the message is limited to 200 UTF-8 bytes and BB codes in it will be ignored.
	 *
	 * @param clientDBId
	 * 		the database ID of the client
	 * @param message
	 * 		the message of the complaint, may not contain BB codes
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see Complaint#getMessage()
	 */
	public boolean addComplaint(int clientDBId, String message) {
		final CComplainAdd add = new CComplainAdd(clientDBId, message);
		return query.doCommand(add);
	}

	/**
	 * Adds a specified permission to all server groups of the type specified by {@code type} on all virtual servers.
	 *
	 * @param type
	 * 		the kind of server group this permission should be added to
	 * @param permName
	 * 		the name of the permission to be granted
	 * @param value
	 * 		the numeric value of the permission (or for boolean permissions: 1 = true, 0 = false)
	 * @param negated
	 * 		if set to true, the lowest permission value will be selected instead of the highest
	 * @param skipped
	 * 		if set to true, this permission will not be overridden by client or channel group permissions
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see ServerGroupType
	 * @see Permission
	 */
	public boolean addPermissionToAllServerGroups(ServerGroupType type, String permName, int value, boolean negated, boolean skipped) {
		final CServerGroupAutoAddPerm add = new CServerGroupAutoAddPerm(type, permName, value, negated, skipped);
		return query.doCommand(add);
	}

	/**
	 * Create a new privilege key that allows one client to join a server or channel group.
	 * <ul>
	 * <li>If {@code type} is set to {@linkplain PrivilegeKeyType#SERVER_GROUP SERVER_GROUP},
	 * {@code groupId} is used as a server group ID and {@code channelId} is ignored.</li>
	 * <li>If {@code type} is set to {@linkplain PrivilegeKeyType#CHANNEL_GROUP CHANNEL_GROUP},
	 * {@code groupId} is used as a channel group ID and {@code channelId} is used as the channel in which the group should be set.</li>
	 * </ul>
	 *
	 * @param type
	 * 		the type of token that should be created
	 * @param groupId
	 * 		the ID of the server or channel group
	 * @param channelId
	 * 		the ID of the channel, in case the token is channel group token
	 * @param description
	 * 		the description for the token, can be null
	 *
	 * @return the created token for a client to use
	 *
	 * @querycommands 1
	 * @see PrivilegeKeyType
	 * @see #addPrivilegeKeyServerGroup(int, String)
	 * @see #addPrivilegeKeyChannelGroup(int, int, String)
	 */
	public String addPrivilegeKey(PrivilegeKeyType type, int groupId, int channelId, String description) {
		final CPrivilegeKeyAdd add = new CPrivilegeKeyAdd(type, groupId, channelId, description);
		if (query.doCommand(add)) {
			return add.getFirstResponse().get("token");
		}
		return null;
	}

	/**
	 * Creates a new privilege key for a channel group.
	 *
	 * @param channelGroupId
	 * 		the ID of the channel group
	 * @param channelId
	 * 		the ID of the channel in which the channel group should be set
	 * @param description
	 * 		the description for the token, can be null
	 *
	 * @return the created token for a client to use
	 *
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 * @see Channel#getId()
	 * @see #addPrivilegeKey(PrivilegeKeyType, int, int, String)
	 * @see #addPrivilegeKeyServerGroup(int, String)
	 */
	public String addPrivilegeKeyChannelGroup(int channelGroupId, int channelId, String description) {
		return addPrivilegeKey(PrivilegeKeyType.CHANNEL_GROUP, channelGroupId, channelId, description);
	}

	/**
	 * Creates a new privilege key for a server group.
	 *
	 * @param serverGroupId
	 * 		the ID of the server group
	 * @param description
	 * 		the description for the token, can be null
	 *
	 * @return the created token for a client to use
	 *
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 * @see #addPrivilegeKey(PrivilegeKeyType, int, int, String)
	 * @see #addPrivilegeKeyChannelGroup(int, int, String)
	 */
	public String addPrivilegeKeyServerGroup(int serverGroupId, String description) {
		return addPrivilegeKey(PrivilegeKeyType.SERVER_GROUP, serverGroupId, 0, description);
	}

	/**
	 * Creates a new server group for clients using a given name and returns its ID.
	 * <p>
	 * To create server group templates or ones for server queries,
	 * use {@link #addServerGroup(String, PermissionGroupDatabaseType)}.
	 * </p>
	 *
	 * @param name
	 * 		the name of the new server group
	 *
	 * @return the ID of the newly created server group
	 *
	 * @querycommands 1
	 * @see ServerGroup
	 */
	public int addServerGroup(String name) {
		return addServerGroup(name, PermissionGroupDatabaseType.REGULAR);
	}

	/**
	 * Creates a new server group using a given name and returns its ID.
	 *
	 * @param name
	 * 		the name of the new server group
	 * @param type
	 * 		the desired type of server group
	 *
	 * @return the ID of the newly created server group
	 *
	 * @querycommands 1
	 * @see ServerGroup
	 * @see PermissionGroupDatabaseType
	 */
	public int addServerGroup(String name, PermissionGroupDatabaseType type) {
		final CServerGroupAdd add = new CServerGroupAdd(name, type);
		if (query.doCommand(add)) {
			return add.getFirstResponse().getInt("sgid");
		}
		return -1;
	}

	/**
	 * Adds a specified permission to a server group.
	 *
	 * @param groupId
	 * 		the ID of the channel group to which the permission should be added
	 * @param permName
	 * 		the name of the permission to add
	 * @param value
	 * 		the numeric value of the permission (or for boolean permissions: 1 = true, 0 = false)
	 * @param negated
	 * 		if set to true, the lowest permission value will be selected instead of the highest
	 * @param skipped
	 * 		if set to true, this permission will not be overridden by client or channel group permissions
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 * @see Permission
	 */
	public boolean addServerGroupPermission(int groupId, String permName, int value, boolean negated, boolean skipped) {
		final CServerGroupAddPerm add = new CServerGroupAddPerm(groupId, permName, value, negated, skipped);
		return query.doCommand(add);
	}

	/**
	 * Adds one or more {@link TS3Listener}s to the event manager of the query.
	 * These listeners will be notified when the TS3 server fires an event.
	 * <p>
	 * Note that for the TS3 server to fire events, you must first also register
	 * the event types you want to listen to.
	 * </p>
	 *
	 * @param listeners
	 * 		one or more listeners to register
	 *
	 * @see #registerAllEvents()
	 * @see #registerEvent(TS3EventType, int)
	 * @see TS3Listener
	 * @see TS3EventType
	 */
	public void addTS3Listeners(TS3Listener... listeners) {
		query.getEventManager().addListeners(listeners);
	}

	/**
	 * Bans a client with a given client ID for a given time.
	 * <p>
	 * Please note that this will create two separate ban rules,
	 * one for the targeted client's IP address and their unique identifier.
	 * </p><p>
	 * <i>Exception:</i> If the banned client connects via a loopback address
	 * (i.e. {@code 127.0.0.1} or {@code localhost}), no IP ban is created
	 * and the returned array will only have 1 entry.
	 * </p>
	 *
	 * @param clientId
	 * 		the ID of the client
	 * @param timeInSeconds
	 * 		the duration of the ban in seconds. 0 equals a permanent ban
	 *
	 * @return an array containing the IDs of the first and the second ban entry
	 *
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #addBan(String, String, String, long, String)
	 */
	public int[] banClient(int clientId, long timeInSeconds) {
		return banClient(clientId, timeInSeconds, null);
	}

	/**
	 * Bans a client with a given client ID for a given time for the specified reason.
	 * <p>
	 * Please note that this will create two separate ban rules,
	 * one for the targeted client's IP address and their unique identifier.
	 * </p><p>
	 * <i>Exception:</i> If the banned client connects via a loopback address
	 * (i.e. {@code 127.0.0.1} or {@code localhost}), no IP ban is created
	 * and the returned array will only have 1 entry.
	 * </p>
	 *
	 * @param clientId
	 * 		the ID of the client
	 * @param timeInSeconds
	 * 		the duration of the ban in seconds. 0 equals a permanent ban
	 * @param reason
	 * 		the reason for the ban, can be null
	 *
	 * @return an array containing the IDs of the first and the second ban entry
	 *
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #addBan(String, String, String, long, String)
	 */
	public int[] banClient(int clientId, long timeInSeconds, String reason) {
		final CBanClient client = new CBanClient(clientId, timeInSeconds, reason);
		if (query.doCommand(client)) {
			final List<Wrapper> response = client.getResponse();
			final int[] banIds = new int[response.size()];
			for (int i = 0; i < banIds.length; ++i) {
				banIds[i] = response.get(i).getInt("banid");
			}
			return banIds;
		}
		return null;
	}

	/**
	 * Bans a client with a given client ID permanently for the specified reason.
	 * <p>
	 * Please note that this will create two separate ban rules,
	 * one for the targeted client's IP address and their unique identifier.
	 * </p><p>
	 * <i>Exception:</i> If the banned client connects via a loopback address
	 * (i.e. {@code 127.0.0.1} or {@code localhost}), no IP ban is created
	 * and the returned array will only have 1 entry.
	 * </p>
	 *
	 * @param clientId
	 * 		the ID of the client
	 * @param reason
	 * 		the reason for the ban, can be null
	 *
	 * @return an array containing the IDs of the first and the second ban entry
	 *
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #addBan(String, String, String, long, String)
	 */
	public int[] banClient(int clientId, String reason) {
		return banClient(clientId, 0, reason);
	}

	/**
	 * Sends a text message to all clients on all virtual servers.
	 * These messages will appear to clients in the tab for server messages.
	 *
	 * @param message
	 * 		the message to be sent
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 */
	public boolean broadcast(String message) {
		final CGM broadcast = new CGM(message);
		return query.doCommand(broadcast);
	}

	/**
	 * Creates a copy of the channel group specified by {@code sourceGroupId},
	 * overwriting any other channel group specified by {@code targetGroupId}.
	 * <p>
	 * The parameter {@code type} can be used to create server query and template groups.
	 * </p>
	 *
	 * @param sourceGroupId
	 * 		the ID of the channel group to copy
	 * @param targetGroupId
	 * 		the ID of another channel group to overwrite
	 * @param type
	 * 		the desired type of channel group
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 */
	public boolean copyChannelGroup(int sourceGroupId, int targetGroupId, PermissionGroupDatabaseType type) {
		if (targetGroupId <= 0) {
			throw new IllegalArgumentException("To create a new channel group, use the method with a String argument");
		}

		final CChannelGroupCopy copy = new CChannelGroupCopy(sourceGroupId, targetGroupId, type);
		return query.doCommand(copy);
	}

	/**
	 * Creates a copy of the channel group specified by {@code sourceGroupId} with a given name
	 * and returns the ID of the newly created channel group.
	 *
	 * @param sourceGroupId
	 * 		the ID of the channel group to copy
	 * @param targetName
	 * 		the name for the copy of the channel group
	 * @param type
	 * 		the desired type of channel group
	 *
	 * @return the ID of the newly created channel group
	 *
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 */
	public int copyChannelGroup(int sourceGroupId, String targetName, PermissionGroupDatabaseType type) {
		final CChannelGroupCopy copy = new CChannelGroupCopy(sourceGroupId, targetName, type);
		if (query.doCommand(copy)) {
			return copy.getFirstResponse().getInt("cgid");
		}
		return -1;
	}

	/**
	 * Creates a copy of the server group specified by {@code sourceGroupId},
	 * overwriting another server group specified by {@code targetGroupId}.
	 * <p>
	 * The parameter {@code type} can be used to create server query and template groups.
	 * </p>
	 *
	 * @param sourceGroupId
	 * 		the ID of the server group to copy
	 * @param targetGroupId
	 * 		the ID of another server group to overwrite
	 * @param type
	 * 		the desired type of server group
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 */
	public boolean copyServerGroup(int sourceGroupId, int targetGroupId, PermissionGroupDatabaseType type) {
		if (targetGroupId <= 0) {
			throw new IllegalArgumentException("To create a new server group, use the method with a String argument");
		}

		final CServerGroupCopy copy = new CServerGroupCopy(sourceGroupId, targetGroupId, type);
		return query.doCommand(copy);
	}

	/**
	 * Creates a copy of the server group specified by {@code sourceGroupId} with a given name
	 * and returns the ID of the newly created server group.
	 *
	 * @param sourceGroupId
	 * 		the ID of the server group to copy
	 * @param targetName
	 * 		the name for the copy of the server group
	 * @param type
	 * 		the desired type of server group
	 *
	 * @return the ID of the newly created server group
	 *
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 */
	public int copyServerGroup(int sourceGroupId, String targetName, PermissionGroupDatabaseType type) {
		final CServerGroupCopy copy = new CServerGroupCopy(sourceGroupId, targetName, type);
		if (query.doCommand(copy)) {
			return copy.getFirstResponse().getInt("sgid");
		}
		return -1;
	}

	/**
	 * Creates a new channel with a given name using the given properties and returns its ID.
	 *
	 * @param name
	 * 		the name for the new channel
	 * @param options
	 * 		a map of options that should be set for the channel
	 *
	 * @return the ID of the newly created channel
	 *
	 * @querycommands 1
	 * @see Channel
	 */
	public int createChannel(String name, Map<ChannelProperty, String> options) {
		final CChannelCreate create = new CChannelCreate(name, options);
		if (query.doCommand(create)) {
			return create.getFirstResponse().getInt("cid");
		}
		return -1;
	}

	/**
	 * Creates a new directory on the file repository in the specified channel.
	 *
	 * @param directoryPath
	 * 		the path to the directory that should be created
	 * @param channelId
	 * 		the ID of the channel the directory should be created in
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public boolean createFileDirectory(String directoryPath, int channelId) {
		return createFileDirectory(directoryPath, channelId, null);
	}

	/**
	 * Creates a new directory on the file repository in the specified channel.
	 *
	 * @param directoryPath
	 * 		the path to the directory that should be created
	 * @param channelId
	 * 		the ID of the channel the directory should be created in
	 * @param channelPassword
	 * 		the password of that channel
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public boolean createFileDirectory(String directoryPath, int channelId, String channelPassword) {
		final CFtCreateDir create = new CFtCreateDir(directoryPath, channelId, channelPassword);
		return query.doCommand(create);
	}

	/**
	 * Creates a new virtual server with the given name and returns an object containing the ID of the newly
	 * created virtual server, the default server admin token and the virtual server's voice port. Usually,
	 * the virtual server is also automatically started. This can be turned off on the TS3 server, though.
	 * <p>
	 * If {@link VirtualServerProperty#VIRTUALSERVER_PORT} is not specified in the virtual server properties,
	 * the server will test for the first unused UDP port.
	 * </p><p>
	 * Please also note that creating virtual servers usually requires the server query admin account
	 * and that there is a limit to how many virtual servers can be created, which is dependent on your license.
	 * Unlicensed TS3 server instances are limited to 1 virtual server with up to 32 client slots.
	 * </p>
	 *
	 * @param name
	 * 		the name for the new virtual server
	 * @param options
	 * 		a map of options that should be set for the virtual server
	 *
	 * @return information about the newly created virtual server
	 *
	 * @querycommands 1
	 * @see VirtualServer
	 */
	public CreatedVirtualServer createServer(String name, Map<VirtualServerProperty, String> options) {
		final CServerCreate create = new CServerCreate(name, options);
		if (query.doCommand(create)) {
			return new CreatedVirtualServer(create.getFirstResponse().getMap());
		}
		return null;
	}

	/**
	 * Creates a {@link Snapshot} of the selected virtual server containing all settings,
	 * groups and known client identities. The data from a server snapshot can be
	 * used to restore a virtual servers configuration.
	 *
	 * @return a snapshot of the virtual server
	 *
	 * @querycommands 1
	 * @see #deployServerSnapshot(Snapshot)
	 */
	public Snapshot createServerSnapshot() {
		final CServerSnapshotCreate create = new CServerSnapshotCreate();
		if (query.doCommand(create)) {
			return new Snapshot(create.getRawResponse());
		}
		return null;
	}

	/**
	 * Deletes all active ban rules from the server. Use with caution.
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 */
	public boolean deleteAllBans() {
		final CBanDelAll del = new CBanDelAll();
		return query.doCommand(del);
	}

	/**
	 * Deletes all complaints about the client with specified database ID from the server.
	 *
	 * @param clientDBId
	 * 		the database ID of the client
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see Complaint
	 */
	public boolean deleteAllComplaints(int clientDBId) {
		final CComplainDelAll del = new CComplainDelAll(clientDBId);
		return query.doCommand(del);
	}

	/**
	 * Deletes the ban rule with the specified ID from the server.
	 *
	 * @param banId
	 * 		the ID of the ban to delete
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Ban#getId()
	 */
	public boolean deleteBan(int banId) {
		final CBanDel del = new CBanDel(banId);
		return query.doCommand(del);
	}

	/**
	 * Deletes an existing channel specified by its ID, kicking all clients out of the channel.
	 *
	 * @param channelId
	 * 		the ID of the channel to delete
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see #deleteChannel(int, boolean)
	 * @see #kickClientFromChannel(String, int...)
	 */
	public boolean deleteChannel(int channelId) {
		return deleteChannel(channelId, true);
	}

	/**
	 * Deletes an existing channel with a given ID.
	 * If {@code force} is true, the channel will be deleted even if there are clients within,
	 * else the command will fail in this situation.
	 *
	 * @param channelId
	 * 		the ID of the channel to delete
	 * @param force
	 * 		whether clients should be kicked out of the channel
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see #kickClientFromChannel(String, int...)
	 */
	public boolean deleteChannel(int channelId, boolean force) {
		final CChannelDelete del = new CChannelDelete(channelId, force);
		return query.doCommand(del);
	}

	/**
	 * Removes a specified permission from a client in a specific channel.
	 *
	 * @param channelId
	 * 		the ID of the channel wherein the permission should be removed
	 * @param clientDBId
	 * 		the database ID of the client
	 * @param permName
	 * 		the name of the permission to revoke
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see Client#getDatabaseId()
	 * @see Permission#getName()
	 */
	public boolean deleteChannelClientPermission(int channelId, int clientDBId, String permName) {
		final CChannelClientDelPerm del = new CChannelClientDelPerm(channelId, clientDBId, permName);
		return query.doCommand(del);
	}

	/**
	 * Removes the channel group with the given ID.
	 *
	 * @param groupId
	 * 		the ID of the channel group
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 */
	public boolean deleteChannelGroup(int groupId) {
		return deleteChannelGroup(groupId, true);
	}

	/**
	 * Removes the channel group with the given ID.
	 * If {@code force} is true, the channel group will be deleted even if it still contains clients,
	 * else the command will fail in this situation.
	 *
	 * @param groupId
	 * 		the ID of the channel group
	 * @param force
	 * 		whether the channel group should be deleted even if it still contains clients
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 */
	public boolean deleteChannelGroup(int groupId, boolean force) {
		final CChannelGroupDel del = new CChannelGroupDel(groupId, force);
		return query.doCommand(del);
	}

	/**
	 * Removes a permission from the channel group with the given ID.
	 *
	 * @param groupId
	 * 		the ID of the channel group
	 * @param permName
	 * 		the name of the permission to revoke
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 * @see Permission#getName()
	 */
	public boolean deleteChannelGroupPermission(int groupId, String permName) {
		final CChannelGroupDelPerm del = new CChannelGroupDelPerm(groupId, permName);
		return query.doCommand(del);
	}

	/**
	 * Removes a permission from the channel with the given ID.
	 *
	 * @param channelId
	 * 		the ID of the channel
	 * @param permName
	 * 		the name of the permission to revoke
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see Permission#getName()
	 */
	public boolean deleteChannelPermission(int channelId, String permName) {
		final CChannelDelPerm del = new CChannelDelPerm(channelId, permName);
		return query.doCommand(del);
	}

	/**
	 * Removes a permission from a client.
	 *
	 * @param clientDBId
	 * 		the database ID of the client
	 * @param permName
	 * 		the name of the permission to revoke
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see Permission#getName()
	 */
	public boolean deleteClientPermission(int clientDBId, String permName) {
		final CClientDelPerm del = new CClientDelPerm(clientDBId, permName);
		return query.doCommand(del);
	}

	/**
	 * Deletes the complaint about the client with database ID {@code targetClientDBId} submitted by
	 * the client with database ID {@code fromClientDBId} from the server.
	 *
	 * @param targetClientDBId
	 * 		the database ID of the client the complaint is about
	 * @param fromClientDBId
	 * 		the database ID of the client who added the complaint
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Complaint
	 * @see Client#getDatabaseId()
	 */
	public boolean deleteComplaint(int targetClientDBId, int fromClientDBId) {
		final CComplainDel del = new CComplainDel(targetClientDBId, fromClientDBId);
		return query.doCommand(del);
	}

	/**
	 * Removes all stored database information about the specified client.
	 * Please note that this data is also automatically removed after a configured time (usually 90 days).
	 * <p>
	 * See {@link DatabaseClientInfo} for a list of stored information about a client.
	 * </p>
	 *
	 * @param clientDBId
	 * 		the database ID of the client
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see #getDatabaseClientInfo(int)
	 * @see DatabaseClientInfo
	 */
	public boolean deleteDatabaseClientProperties(int clientDBId) {
		final CClientDBDelete del = new CClientDBDelete(clientDBId);
		return query.doCommand(del);
	}

	/**
	 * Deletes a file or directory from the file repository in the specified channel.
	 *
	 * @param filePath
	 * 		the path to the file or directory
	 * @param channelId
	 * 		the ID of the channel the file or directory resides in
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public boolean deleteFile(String filePath, int channelId) {
		return deleteFile(filePath, channelId, null);
	}

	/**
	 * Deletes a file or directory from the file repository in the specified channel.
	 *
	 * @param filePath
	 * 		the path to the file or directory
	 * @param channelId
	 * 		the ID of the channel the file or directory resides in
	 * @param channelPassword
	 * 		the password of that channel
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public boolean deleteFile(String filePath, int channelId, String channelPassword) {
		final CFtDeleteFile delete = new CFtDeleteFile(channelId, channelPassword, filePath);
		return query.doCommand(delete);
	}

	/**
	 * Deletes multiple files or directories from the file repository in the specified channel.
	 *
	 * @param filePaths
	 * 		the paths to the files or directories
	 * @param channelId
	 * 		the ID of the channel the file or directory resides in
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public boolean deleteFiles(String[] filePaths, int channelId) {
		return deleteFiles(filePaths, channelId, null);
	}

	/**
	 * Deletes multiple files or directories from the file repository in the specified channel.
	 *
	 * @param filePaths
	 * 		the paths to the files or directories
	 * @param channelId
	 * 		the ID of the channel the file or directory resides in
	 * @param channelPassword
	 * 		the password of that channel
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public boolean deleteFiles(String[] filePaths, int channelId, String channelPassword) {
		final CFtDeleteFile delete = new CFtDeleteFile(channelId, channelPassword, filePaths);
		return query.doCommand(delete);
	}

	/**
	 * Deletes an icon from the icon directory in the file repository.
	 *
	 * @param iconId
	 * 		the ID of the icon to delete
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see IconFile#getIconId()
	 */
	public boolean deleteIcon(long iconId) {
		final String iconPath = "/icon_" + iconId;
		return deleteFile(iconPath, 0);
	}

	/**
	 * Deletes multiple icons from the icon directory in the file repository.
	 *
	 * @param iconIds
	 * 		the IDs of the icons to delete
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see IconFile#getIconId()
	 */
	public boolean deleteIcons(long[] iconIds) {
		final String[] iconPaths = new String[iconIds.length];
		for (int i = 0; i < iconIds.length; ++i) {
			iconPaths[i] = "/icon_" + iconIds[i];
		}
		return deleteFiles(iconPaths, 0);
	}

	/**
	 * Deletes the offline message with the specified ID.
	 *
	 * @param messageId
	 * 		the ID of the offline message to delete
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Message#getId()
	 */
	public boolean deleteOfflineMessage(int messageId) {
		final CMessageDel del = new CMessageDel(messageId);
		return query.doCommand(del);
	}

	/**
	 * Removes a specified permission from all server groups of the type specified by {@code type} on all virtual servers.
	 *
	 * @param type
	 * 		the kind of server group this permission should be removed from
	 * @param permName
	 * 		the name of the permission to remove
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see ServerGroupType
	 * @see Permission#getName()
	 */
	public boolean deletePermissionFromAllServerGroups(ServerGroupType type, String permName) {
		final CServerGroupAutoDelPerm del = new CServerGroupAutoDelPerm(type, permName);
		return query.doCommand(del);
	}

	/**
	 * Deletes the privilege key with the given token.
	 *
	 * @param token
	 * 		the token of the privilege key
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see PrivilegeKey
	 */
	public boolean deletePrivilegeKey(String token) {
		final CPrivilegeKeyDelete del = new CPrivilegeKeyDelete(token);
		return query.doCommand(del);
	}

	/**
	 * Deletes the virtual server with the specified ID.
	 * <p>
	 * Only stopped virtual servers can be deleted.
	 * </p>
	 *
	 * @param serverId
	 * 		the ID of the virtual server
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see VirtualServer#getId()
	 * @see #stopServer(int)
	 */
	public boolean deleteServer(int serverId) {
		final CServerDelete delete = new CServerDelete(serverId);
		return query.doCommand(delete);
	}

	/**
	 * Deletes the server group with the specified ID, even if the server group still contains clients.
	 *
	 * @param groupId
	 * 		the ID of the server group
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 */
	public boolean deleteServerGroup(int groupId) {
		return deleteServerGroup(groupId, true);
	}

	/**
	 * Deletes a server group with the specified ID.
	 * <p>
	 * If {@code force} is true, the server group will be deleted even if it contains clients,
	 * else the command will fail in this situation.
	 * </p>
	 *
	 * @param groupId
	 * 		the ID of the server group
	 * @param force
	 * 		whether the server group should be deleted if it still contains clients
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 */
	public boolean deleteServerGroup(int groupId, boolean force) {
		final CServerGroupDel del = new CServerGroupDel(groupId, force);
		return query.doCommand(del);
	}

	/**
	 * Removes a permission from the server group with the given ID.
	 *
	 * @param groupId
	 * 		the ID of the server group
	 * @param permName
	 * 		the name of the permission to revoke
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 * @see Permission#getName()
	 */
	public boolean deleteServerGroupPermission(int groupId, String permName) {
		final CServerGroupDelPerm del = new CServerGroupDelPerm(groupId, permName);
		return query.doCommand(del);
	}

	/**
	 * Restores the selected virtual servers configuration using the data from a
	 * previously created server snapshot.
	 *
	 * @param snapshot
	 * 		the snapshot to restore
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #createServerSnapshot()
	 */
	public boolean deployServerSnapshot(Snapshot snapshot) {
		return deployServerSnapshot(snapshot.get());
	}

	/**
	 * Restores the configuration of the selected virtual server using the data from a
	 * previously created server snapshot.
	 *
	 * @param snapshot
	 * 		the snapshot to restore
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #createServerSnapshot()
	 */
	public boolean deployServerSnapshot(String snapshot) {
		final CServerSnapshotDeploy deploy = new CServerSnapshotDeploy(snapshot);
		return query.doCommand(deploy);
	}

	/**
	 * Downloads a file from the file repository at a given path and channel
	 * and writes the file's bytes to an open {@link OutputStream}.
	 * <p>
	 * It is the user's responsibility to ensure that the given {@code OutputStream} is
	 * open and to close the stream again once the download has finished.
	 * </p><p>
	 * Note that this method will not read the entire file to memory and can thus
	 * download arbitrarily sized files from the file repository.
	 * </p>
	 *
	 * @param dataOut
	 * 		a stream that the downloaded data should be written to
	 * @param filePath
	 * 		the path of the file on the file repository
	 * @param channelId
	 * 		the ID of the channel to download the file from
	 *
	 * @return how many bytes were downloaded
	 *
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #downloadFileDirect(String, int)
	 */
	public long downloadFile(OutputStream dataOut, String filePath, int channelId) {
		return downloadFile(dataOut, filePath, channelId, null);
	}

	/**
	 * Downloads a file from the file repository at a given path and channel
	 * and writes the file's bytes to an open {@link OutputStream}.
	 * <p>
	 * It is the user's responsibility to ensure that the given {@code OutputStream} is
	 * open and to close the stream again once the download has finished.
	 * </p><p>
	 * Note that this method will not read the entire file to memory and can thus
	 * download arbitrarily sized files from the file repository.
	 * </p>
	 *
	 * @param dataOut
	 * 		a stream that the downloaded data should be written to
	 * @param filePath
	 * 		the path of the file on the file repository
	 * @param channelId
	 * 		the ID of the channel to download the file from
	 * @param channelPassword
	 * 		that channel's password
	 *
	 * @return how many bytes were downloaded
	 *
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #downloadFileDirect(String, int, String)
	 */
	public long downloadFile(OutputStream dataOut, String filePath, int channelId, String channelPassword) {
		final FileTransferHelper helper = query.getFileTransferHelper();
		final int transferId = helper.getClientTransferId();
		final CFtInitDownload download = new CFtInitDownload(transferId, filePath, channelId, channelPassword);

		if (!query.doCommand(download)) return -1L;
		FileTransferParameters params = new FileTransferParameters(download.getFirstResponse().getMap());
		QueryError error = params.getQueryError();
		if (!error.isSuccessful()) return -1L;

		try {
			query.getFileTransferHelper().downloadFile(dataOut, params);
		} catch (IOException e) {
			throw new TS3FileTransferFailedException("Download failed", e);
		}
		return params.getFileSize();
	}

	/**
	 * Downloads a file from the file repository at a given path and channel
	 * and returns the file's bytes as a byte array.
	 * <p>
	 * Note that this method <strong>will read the entire file to memory</strong>.
	 * That means that if a file is larger than 2<sup>31</sup>-1 bytes in size,
	 * the download will fail.
	 * </p>
	 *
	 * @param filePath
	 * 		the path of the file on the file repository
	 * @param channelId
	 * 		the ID of the channel to download the file from
	 *
	 * @return a byte array containing the file's data
	 *
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #downloadFile(OutputStream, String, int)
	 */
	public byte[] downloadFileDirect(String filePath, int channelId) {
		return downloadFileDirect(filePath, channelId, null);
	}

	/**
	 * Downloads a file from the file repository at a given path and channel
	 * and returns the file's bytes as a byte array.
	 * <p>
	 * Note that this method <strong>will read the entire file to memory</strong>.
	 * That means that if a file is larger than 2<sup>31</sup>-1 bytes in size,
	 * the download will fail.
	 * </p>
	 *
	 * @param filePath
	 * 		the path of the file on the file repository
	 * @param channelId
	 * 		the ID of the channel to download the file from
	 * @param channelPassword
	 * 		that channel's password
	 *
	 * @return a byte array containing the file's data
	 *
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #downloadFile(OutputStream, String, int, String)
	 */
	public byte[] downloadFileDirect(String filePath, int channelId, String channelPassword) {
		final FileTransferHelper helper = query.getFileTransferHelper();
		final int transferId = helper.getClientTransferId();
		final CFtInitDownload download = new CFtInitDownload(transferId, filePath, channelId, channelPassword);

		if (!query.doCommand(download)) return null;
		FileTransferParameters params = new FileTransferParameters(download.getFirstResponse().getMap());
		QueryError error = params.getQueryError();
		if (!error.isSuccessful()) return null;

		long fileSize = params.getFileSize();
		if (fileSize > Integer.MAX_VALUE) throw new TS3FileTransferFailedException("File too big for byte array");
		ByteArrayOutputStream dataOut = new ByteArrayOutputStream((int) fileSize);

		try {
			query.getFileTransferHelper().downloadFile(dataOut, params);
		} catch (IOException e) {
			throw new TS3FileTransferFailedException("Download failed", e);
		}

		return dataOut.toByteArray();
	}

	/**
	 * Downloads an icon from the icon directory in the file repository
	 * and writes the file's bytes to an open {@link OutputStream}.
	 * <p>
	 * It is the user's responsibility to ensure that the given {@code OutputStream} is
	 * open and to close the stream again once the download has finished.
	 * </p>
	 *
	 * @param dataOut
	 * 		a stream that the downloaded data should be written to
	 * @param iconId
	 * 		the ID of the icon that should be downloaded
	 *
	 * @return a byte array containing the icon file's data
	 *
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see IconFile#getIconId()
	 * @see #downloadIconDirect(long)
	 * @see #uploadIcon(InputStream, long)
	 */
	public long downloadIcon(OutputStream dataOut, long iconId) {
		final String iconPath = "/icon_" + iconId;
		return downloadFile(dataOut, iconPath, 0);
	}

	/**
	 * Downloads an icon from the icon directory in the file repository
	 * and returns the file's bytes as a byte array.
	 * <p>
	 * Note that this method <strong>will read the entire file to memory</strong>.
	 * </p>
	 *
	 * @param iconId
	 * 		the ID of the icon that should be downloaded
	 *
	 * @return a byte array containing the icon file's data
	 *
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see IconFile#getIconId()
	 * @see #downloadIcon(OutputStream, long)
	 * @see #uploadIconDirect(byte[])
	 */
	public byte[] downloadIconDirect(long iconId) {
		final String iconPath = "/icon_" + iconId;
		return downloadFileDirect(iconPath, 0);
	}

	/**
	 * Changes a channel's configuration using the given properties.
	 *
	 * @param channelId
	 * 		the ID of the channel to edit
	 * @param options
	 * 		the map of properties to modify
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Channel#getId()
	 */
	public boolean editChannel(int channelId, Map<ChannelProperty, String> options) {
		final CChannelEdit edit = new CChannelEdit(channelId, options);
		return query.doCommand(edit);
	}

	/**
	 * Changes a client's configuration using given properties.
	 * <p>
	 * Only {@link ClientProperty#CLIENT_DESCRIPTION} can be changed for other clients.
	 * To update the current client's properties, use {@link #updateClient(Map)}.
	 * </p>
	 *
	 * @param clientId
	 * 		the ID of the client to edit
	 * @param options
	 * 		the map of properties to modify
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #updateClient(Map)
	 */
	public boolean editClient(int clientId, Map<ClientProperty, String> options) {
		final CClientEdit edit = new CClientEdit(clientId, options);
		return query.doCommand(edit);
	}

	/**
	 * Changes a client's database settings using given properties.
	 *
	 * @param clientDBId
	 * 		the database ID of the client to edit
	 * @param options
	 * 		the map of properties to modify
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see DatabaseClientInfo
	 * @see Client#getDatabaseId()
	 */
	public boolean editDatabaseClient(int clientDBId, Map<ClientProperty, String> options) {
		final CClientDBEdit edit = new CClientDBEdit(clientDBId, options);
		return query.doCommand(edit);
	}

	/**
	 * Changes the server instance configuration using given properties.
	 * If the given property is not changeable, {@code IllegalArgumentException} will be thrown.
	 *
	 * @param property
	 * 		the property to edit, must be changeable
	 * @param value
	 * 		the new value for the edit
	 *
	 * @return whether the command succeeded or not
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code property} is not changeable
	 * @querycommands 1
	 * @see ServerInstanceProperty#isChangeable()
	 */
	public boolean editInstance(ServerInstanceProperty property, String value) {
		if (!property.isChangeable()) {
			throw new IllegalArgumentException("Property is not changeable");
		}

		final CInstanceEdit edit = new CInstanceEdit(property, value);
		return query.doCommand(edit);
	}

	/**
	 * Changes the configuration of the selected virtual server using given properties.
	 *
	 * @param options
	 * 		the map of properties to edit
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see VirtualServerProperty
	 */
	public boolean editServer(Map<VirtualServerProperty, String> options) {
		final CServerEdit edit = new CServerEdit(options);
		return query.doCommand(edit);
	}

	/**
	 * Gets a list of all bans on the selected virtual server.
	 *
	 * @return a list of all bans on the virtual server
	 *
	 * @querycommands 1
	 * @see Ban
	 */
	public List<Ban> getBans() {
		final CBanList list = new CBanList();
		if (query.doCommand(list)) {
			final List<Wrapper> responses = list.getResponse();
			final List<Ban> bans = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				bans.add(new Ban(response.getMap()));
			}
			return bans;
		}
		return null;
	}

	/**
	 * Gets a list of IP addresses used by the server instance.
	 *
	 * @return the list of bound IP addresses
	 *
	 * @querycommands 1
	 * @see Binding
	 */
	public List<Binding> getBindings() {
		final CBindingList list = new CBindingList();
		if (query.doCommand(list)) {
			final List<Wrapper> responses = list.getResponse();
			final List<Binding> bindings = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				bindings.add(new Binding(response.getMap()));
			}
			return bindings;
		}
		return null;
	}

	/**
	 * Finds and returns the channel matching the given name exactly.
	 *
	 * @param name
	 * 		the name of the channel
	 * @param ignoreCase
	 * 		whether the case of the name should be ignored
	 *
	 * @return the found channel or {@code null} if no channel was found
	 *
	 * @querycommands 1
	 * @see Channel
	 * @see #getChannelsByName(String)
	 */
	public Channel getChannelByNameExact(String name, final boolean ignoreCase) {
		final String caseName = ignoreCase ? name.toLowerCase(Locale.ROOT) : name;
		final List<Channel> allChannels = getChannels();
		if (allChannels == null) return null;

		for (final Channel channel : allChannels) {
			final String channelName = ignoreCase ? channel.getName().toLowerCase(Locale.ROOT) : channel.getName();
			if (caseName.equals(channelName)) {
				return channel;
			}
		}
		return null; // Not found
	}

	/**
	 * Gets a list of channels whose names contain the given search string.
	 *
	 * @param name
	 * 		the name to search
	 *
	 * @return a list of all channels with names matching the search pattern
	 *
	 * @querycommands 2
	 * @see Channel
	 * @see #getChannelByNameExact(String, boolean)
	 */
	public List<Channel> getChannelsByName(String name) {
		final CChannelFind find = new CChannelFind(name);
		final List<Channel> allChannels = getChannels();
		if (allChannels == null) return null;

		if (query.doCommand(find)) {
			final List<Wrapper> responses = find.getResponse();
			final List<Channel> channels = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				final int channelId = response.getInt("cid");
				for (final Channel channel : allChannels) {
					if (channel.getId() == channelId) {
						channels.add(channel);
						break;
					}
				}
			}
			return channels;
		}
		return null;
	}

	/**
	 * Displays a list of permissions defined for a client in a specific channel.
	 *
	 * @param channelId
	 * 		the ID of the channel
	 * @param clientDBId
	 * 		the database ID of the client
	 *
	 * @return a list of permissions for the user in the specified channel
	 *
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see Client#getDatabaseId()
	 * @see Permission
	 */
	public List<Permission> getChannelClientPermissions(int channelId, int clientDBId) {
		final CChannelClientPermList list = new CChannelClientPermList(channelId, clientDBId);
		if (query.doCommand(list)) {
			final List<Wrapper> responses = list.getResponse();
			final List<Permission> permissions = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				permissions.add(new Permission(response.getMap()));
			}
			return permissions;
		}
		return null;
	}

	/**
	 * Gets all client / channel ID combinations currently assigned to channel groups.
	 * All three parameters are optional and can be turned off by setting it to {@code -1}.
	 *
	 * @param channelId
	 * 		restricts the search to the channel with a specified ID. Set to {@code -1} to ignore.
	 * @param clientDBId
	 * 		restricts the search to the client with a specified database ID. Set to {@code -1} to ignore.
	 * @param groupId
	 * 		restricts the search to the channel group with the specified ID. Set to {@code -1} to ignore.
	 *
	 * @return a list of combinations of channel ID, client database ID and channel group ID
	 *
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see Client#getDatabaseId()
	 * @see ChannelGroup#getId()
	 * @see ChannelGroupClient
	 */
	public List<ChannelGroupClient> getChannelGroupClients(int channelId, int clientDBId, int groupId) {
		final CChannelGroupClientList list = new CChannelGroupClientList(channelId, clientDBId, groupId);
		if (query.doCommand(list)) {
			final List<Wrapper> responses = list.getResponse();
			final List<ChannelGroupClient> clients = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				clients.add(new ChannelGroupClient(response.getMap()));
			}
			return clients;
		}
		return null;
	}

	/**
	 * Gets all client / channel ID combinations currently assigned to the specified channel group.
	 *
	 * @param groupId
	 * 		the ID of the channel group whose client / channel assignments should be returned.
	 *
	 * @return a list of combinations of channel ID, client database ID and channel group ID
	 *
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 * @see ChannelGroupClient
	 * @see #getChannelGroupClients(int, int, int)
	 */
	public List<ChannelGroupClient> getChannelGroupClientsByChannelGroupId(int groupId) {
		return getChannelGroupClients(-1, -1, groupId);
	}

	/**
	 * Gets all channel group assignments in the specified channel.
	 *
	 * @param channelId
	 * 		the ID of the channel whose channel group assignments should be returned.
	 *
	 * @return a list of combinations of channel ID, client database ID and channel group ID
	 *
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see ChannelGroupClient
	 * @see #getChannelGroupClients(int, int, int)
	 */
	public List<ChannelGroupClient> getChannelGroupClientsByChannelId(int channelId) {
		return getChannelGroupClients(channelId, -1, -1);
	}

	/**
	 * Gets all channel group assignments for the specified client.
	 *
	 * @param clientDBId
	 * 		the database ID of the client whose channel group
	 *
	 * @return a list of combinations of channel ID, client database ID and channel group ID
	 *
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see ChannelGroupClient
	 * @see #getChannelGroupClients(int, int, int)
	 */
	public List<ChannelGroupClient> getChannelGroupClientsByClientDBId(int clientDBId) {
		return getChannelGroupClients(-1, clientDBId, -1);
	}

	/**
	 * Gets a list of all permissions assigned to the specified channel group.
	 *
	 * @param groupId
	 * 		the ID of the channel group.
	 *
	 * @return a list of permissions assigned to the channel group
	 *
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 * @see Permission
	 */
	public List<Permission> getChannelGroupPermissions(int groupId) {
		final CChannelGroupPermList list = new CChannelGroupPermList(groupId);
		if (query.doCommand(list)) {
			final List<Wrapper> responses = list.getResponse();
			final List<Permission> permissions = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				permissions.add(new Permission(response.getMap()));
			}
			return permissions;
		}
		return null;
	}

	/**
	 * Gets a list of all channel groups on the selected virtual server.
	 *
	 * @return a list of all channel groups on the virtual server
	 *
	 * @querycommands 1
	 * @see ChannelGroup
	 */
	public List<ChannelGroup> getChannelGroups() {
		final CChannelGroupList list = new CChannelGroupList();
		if (query.doCommand(list)) {
			final List<Wrapper> responses = list.getResponse();
			final List<ChannelGroup> groups = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				groups.add(new ChannelGroup(response.getMap()));
			}
			return groups;
		}
		return null;
	}

	/**
	 * Gets detailed configuration information about the channel specified channel.
	 *
	 * @param channelId
	 * 		the ID of the channel
	 *
	 * @return information about the channel
	 *
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see ChannelInfo
	 */
	public ChannelInfo getChannelInfo(int channelId) {
		final CChannelInfo info = new CChannelInfo(channelId);
		if (query.doCommand(info)) {
			return new ChannelInfo(channelId, info.getFirstResponse().getMap());
		}
		return null;
	}

	/**
	 * Gets a list of all permissions assigned to the specified channel.
	 *
	 * @param channelId
	 * 		the ID of the channel
	 *
	 * @return a list of all permissions assigned to the channel
	 *
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see Permission
	 */
	public List<Permission> getChannelPermissions(int channelId) {
		final CChannelPermList list = new CChannelPermList(channelId);
		if (query.doCommand(list)) {
			final List<Wrapper> responses = list.getResponse();
			final List<Permission> permissions = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				permissions.add(new Permission(response.getMap()));
			}
			return permissions;
		}
		return null;
	}

	/**
	 * Gets a list of all channels on the selected virtual server.
	 *
	 * @return a list of all channels on the virtual server
	 *
	 * @querycommands 1
	 * @see Channel
	 */
	public List<Channel> getChannels() {
		final CChannelList list = new CChannelList();
		if (query.doCommand(list)) {
			final List<Wrapper> responses = list.getResponse();
			final List<Channel> channels = new ArrayList<>(responses.size());
			for (final Wrapper response : responses) {
				channels.add(new Channel(response.getMap()));
			}
			return channels;
		}
		return null;
	}

	/**
	 * Finds and returns the client whose nickname matches the given name exactly.
	 *
	 * @param name
	 * 		the name of the client
	 * @param ignoreCase
	 * 		whether the case of the name should be ignored
	 *
	 * @return the found client or {@code null} if no client was found
	 *
	 * @querycommands 1
	 * @see Client
	 * @see #getClientsByName(String)
	 */
	public Client getClientByNameExact(String name, final boolean ignoreCase) {
		final String caseName = ignoreCase ? name.toLowerCase(Locale.ROOT) : name;
		final List<Client> allClients = getClients();
		if (allClients == null) return null;

		for (final Client client : allClients) {
			final String clientName = ignoreCase ? client.getNickname().toLowerCase(Locale.ROOT) : client.getNickname();
			if (caseName.equals(clientName)) {
				return client;
			}
		}
		return null; // Not found
	}

	/**
	 * Gets a list of clients whose nicknames contain the given search string.
	 *
	 * @param name
	 * 		the name to search
	 *
	 * @return a list of all clients with nicknames matching the search pattern
	 *
	 * @querycommands 2
	 * @see Client
	 * @see #getClientByNameExact(String, boolean)
	 */
	public List<Client> getClientsByName(String name) {
		final CClientFind find = new CClientFind(name);
		final List<Client> allClients = getClients();
		if (allClients == null) return null;

		if (query.doCommand(find)) {
			final List<Wrapper> responses = find.getResponse();
			final List<Client> clients = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				for (final Client client : allClients) {
					if (client.getId() == response.getInt("clid")) {
						clients.add(client);
						break;
					}
				}
			}
			return clients;
		}
		return null;
	}

	/**
	 * Gets information about the client with the specified unique identifier.
	 *
	 * @param clientUId
	 * 		the unique identifier of the client
	 *
	 * @return the client or {@code null} if no client was found
	 *
	 * @querycommands 2
	 * @see Client#getUniqueIdentifier()
	 * @see ClientInfo
	 */
	public ClientInfo getClientByUId(String clientUId) {
		final CClientGetIds get = new CClientGetIds(clientUId);
		if (query.doCommand(get)) {
			return getClientInfo(get.getFirstResponse().getInt("clid"));
		}
		return null;
	}

	/**
	 * Gets information about the client with the specified client ID.
	 *
	 * @param clientId
	 * 		the client ID of the client
	 *
	 * @return the client or {@code null} if no client was found
	 *
	 * @querycommands 1
	 * @see Client#getId()
	 * @see ClientInfo
	 */
	public ClientInfo getClientInfo(int clientId) {
		final CClientInfo info = new CClientInfo(clientId);
		if (query.doCommand(info)) {
			return new ClientInfo(clientId, info.getFirstResponse().getMap());
		}
		return null;
	}

	/**
	 * Gets a list of all permissions assigned to the specified client.
	 *
	 * @param clientDBId
	 * 		the database ID of the client
	 *
	 * @return a list of all permissions assigned to the client
	 *
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see Permission
	 */
	public List<Permission> getClientPermissions(int clientDBId) {
		final CClientPermList list = new CClientPermList(clientDBId);
		if (query.doCommand(list)) {
			final List<Wrapper> responses = list.getResponse();
			final List<Permission> permissions = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				permissions.add(new Permission(response.getMap()));
			}
			return permissions;
		}
		return null;
	}

	/**
	 * Gets a list of all clients on the selected virtual server.
	 *
	 * @return a list of all clients on the virtual server
	 *
	 * @querycommands 1
	 * @see Client
	 */
	public List<Client> getClients() {
		final CClientList list = new CClientList();
		if (query.doCommand(list)) {
			final List<Wrapper> responses = list.getResponse();
			final List<Client> clients = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				clients.add(new Client(response.getMap()));
			}
			return clients;
		}
		return null;
	}

	/**
	 * Gets a list of all complaints on the selected virtual server.
	 *
	 * @return a list of all complaints on the virtual server
	 *
	 * @querycommands 1
	 * @see Complaint
	 * @see #getComplaints(int)
	 */
	public List<Complaint> getComplaints() {
		return getComplaints(-1);
	}

	/**
	 * Gets a list of all complaints about the specified client.
	 *
	 * @param clientDBId
	 * 		the database ID of the client
	 *
	 * @return a list of all complaints about the specified client
	 *
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see Complaint
	 */
	public List<Complaint> getComplaints(int clientDBId) {
		final CComplainList list = new CComplainList(clientDBId);
		if (query.doCommand(list)) {
			final List<Wrapper> responses = list.getResponse();
			final List<Complaint> complaints = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				complaints.add(new Complaint(response.getMap()));
			}
			return complaints;
		}
		return null;
	}

	/**
	 * Gets detailed connection information about the selected virtual server.
	 *
	 * @return connection information about the selected virtual server
	 *
	 * @querycommands 1
	 * @see ConnectionInfo
	 * @see #getServerInfo()
	 */
	public ConnectionInfo getConnectionInfo() {
		final CServerRequestConnectionInfo info = new CServerRequestConnectionInfo();
		if (query.doCommand(info)) {
			return new ConnectionInfo(info.getFirstResponse().getMap());
		}
		return null;
	}

	/**
	 * Gets all clients in the database whose last nickname matches the specified name <b>exactly</b>.
	 *
	 * @param name
	 * 		the nickname for the clients to match
	 *
	 * @return a list of all clients with a matching nickname
	 *
	 * @querycommands 1 + n,
	 * where n is the amount of database clients with a matching nickname
	 * @see Client#getNickname()
	 */
	public List<DatabaseClientInfo> getDatabaseClientsByName(String name) {
		final CClientDBFind find = new CClientDBFind(name, false);

		if (query.doCommand(find)) {
			final List<Wrapper> responses = find.getResponse();
			final List<DatabaseClientInfo> clients = new ArrayList<>(responses.size());

			for (Wrapper response : responses) {
				final int databaseId = response.getInt("cldbid");
				final DatabaseClientInfo clientInfo = getDatabaseClientInfo(databaseId);
				if (clientInfo != null) {
					clients.add(clientInfo);
				}
			}
			return clients;
		}
		return null;
	}

	/**
	 * Gets information about the client with the specified unique identifier in the server database.
	 *
	 * @param clientUId
	 * 		the unique identifier of the client
	 *
	 * @return the database client or {@code null} if no client was found
	 *
	 * @querycommands 2
	 * @see Client#getUniqueIdentifier()
	 * @see DatabaseClientInfo
	 */
	public DatabaseClientInfo getDatabaseClientByUId(String clientUId) {
		final CClientGetDBIdFromUId get = new CClientGetDBIdFromUId(clientUId);
		if (query.doCommand(get)) {
			return getDatabaseClientInfo(get.getFirstResponse().getInt("cldbid"));
		}
		return null;
	}

	/**
	 * Gets information about the client with the specified database ID in the server database.
	 *
	 * @param clientDBId
	 * 		the database ID of the client
	 *
	 * @return the database client or {@code null} if no client was found
	 *
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see DatabaseClientInfo
	 */
	public DatabaseClientInfo getDatabaseClientInfo(int clientDBId) {
		final CClientDBInfo info = new CClientDBInfo(clientDBId);
		if (query.doCommand(info)) {
			return new DatabaseClientInfo(info.getFirstResponse().getMap());
		}
		return null;
	}

	/**
	 * Gets information about all clients in the server database.
	 * <p>
	 * As this method uses internal commands which can only return 200 clients at once,
	 * this method can take quite some time to execute.
	 * </p><p>
	 * Also keep in mind that the client database can easily accumulate several thousand entries.
	 * </p>
	 *
	 * @return a {@link List} of all database clients
	 *
	 * @querycommands 1 + n,
	 * where n = Math.ceil([amount of database clients] / 200)
	 * @see DatabaseClient
	 */
	public List<DatabaseClient> getDatabaseClients() {
		final CClientDBList countList = new CClientDBList(0, 1, true);
		if (query.doCommand(countList)) {
			final int count = countList.getFirstResponse().getInt("count");
			final List<DatabaseClient> clients = new ArrayList<>(count);

			int i = 0;
			while (i < count) {
				final CClientDBList list = new CClientDBList(i, 200, false);
				if (query.doCommand(list)) {
					for (final Wrapper response : list.getResponse()) {
						clients.add(new DatabaseClient(response.getMap()));
					}
				}
				i += 200;
			}
			return clients;
		}
		return null;
	}

	/**
	 * Gets information about a set number of clients in the server database, starting at {@code offset}.
	 *
	 * @param offset
	 * 		the index of the first database client to be returned.
	 * 		Note that this is <b>not</b> a database ID, but an arbitrary, 0-based index.
	 * @param count
	 * 		the number of database clients that should be returned.
	 * 		Any integer greater than 200 might cause problems with the connection
	 *
	 * @return a {@link List} of database clients
	 *
	 * @querycommands 1
	 * @see DatabaseClient
	 */
	public List<DatabaseClient> getDatabaseClients(final int offset, final int count) {
		final CClientDBList list = new CClientDBList(offset, count, false);
		if (query.doCommand(list)) {
			final List<DatabaseClient> clients = new ArrayList<>(count);
			for (final Wrapper response : list.getResponse()) {
				clients.add(new DatabaseClient(response.getMap()));
			}
			return clients;
		}
		return null;
	}

	/**
	 * Gets information about a file on the file repository in the specified channel.
	 * <p>
	 * Note that this method does not work on directories and the information returned by this
	 * method is identical to the one returned by {@link #getFileList(String, int, String)}
	 * </p>
	 *
	 * @param filePath
	 * 		the path to the file
	 * @param channelId
	 * 		the ID of the channel the file resides in
	 *
	 * @return some information about the file
	 *
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public FileInfo getFileInfo(String filePath, int channelId) {
		return getFileInfo(filePath, channelId, null);
	}

	/**
	 * Gets information about a file on the file repository in the specified channel.
	 * <p>
	 * Note that this method does not work on directories and the information returned by this
	 * method is identical to the one returned by {@link #getFileList(String, int, String)}
	 * </p>
	 *
	 * @param filePath
	 * 		the path to the file
	 * @param channelId
	 * 		the ID of the channel the file resides in
	 * @param channelPassword
	 * 		the password of that channel
	 *
	 * @return some information about the file
	 *
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public FileInfo getFileInfo(String filePath, int channelId, String channelPassword) {
		final CFtGetFileInfo info = new CFtGetFileInfo(channelId, channelPassword, filePath);
		if (query.doCommand(info)) {
			return new FileInfo(info.getFirstResponse().getMap());
		}
		return null;
	}

	/**
	 * Gets information about multiple files on the file repository in the specified channel.
	 * <p>
	 * Note that this method does not work on directories and the information returned by this
	 * method is identical to the one returned by {@link #getFileList(String, int, String)}
	 * </p>
	 *
	 * @param filePaths
	 * 		the paths to the files
	 * @param channelId
	 * 		the ID of the channel the file resides in
	 *
	 * @return some information about the file
	 *
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public List<FileInfo> getFileInfos(String filePaths[], int channelId) {
		return getFileInfos(filePaths, channelId, null);
	}

	/**
	 * Gets information about multiple files on the file repository in the specified channel.
	 * <p>
	 * Note that this method does not work on directories and the information returned by this
	 * method is identical to the one returned by {@link #getFileList(String, int, String)}
	 * </p>
	 *
	 * @param filePaths
	 * 		the paths to the files
	 * @param channelId
	 * 		the ID of the channel the file resides in
	 * @param channelPassword
	 * 		the password of that channel
	 *
	 * @return some information about the file
	 *
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public List<FileInfo> getFileInfos(String filePaths[], int channelId, String channelPassword) {
		final CFtGetFileInfo info = new CFtGetFileInfo(channelId, channelPassword, filePaths);
		if (query.doCommand(info)) {
			final List<Wrapper> responses = info.getResponse();
			final List<FileInfo> files = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				files.add(new FileInfo(response.getMap()));
			}
			return files;
		}
		return null;
	}

	/**
	 * Gets information about multiple files on the file repository in multiple channels.
	 * <p>
	 * Note that this method does not work on directories and the information returned by this
	 * method is identical to the one returned by {@link #getFileList(String, int, String)}
	 * </p>
	 *
	 * @param filePaths
	 * 		the paths to the files, may not be {@code null} and may not contain {@code null} elements
	 * @param channelIds
	 * 		the IDs of the channels the file resides in, may not be {@code null}
	 * @param channelPasswords
	 * 		the passwords of those channels, may be {@code null} and may contain {@code null} elements
	 *
	 * @return some information about the files
	 *
	 * @throws IllegalArgumentException
	 * 		if the dimensions of {@code filePaths}, {@code channelIds} and {@code channelPasswords} don't match
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public List<FileInfo> getFileInfos(String filePaths[], int[] channelIds, String[] channelPasswords) {
		final CFtGetFileInfo info = new CFtGetFileInfo(channelIds, channelPasswords, filePaths);
		if (query.doCommand(info)) {
			final List<Wrapper> responses = info.getResponse();
			final List<FileInfo> files = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				files.add(new FileInfo(response.getMap()));
			}
			return files;
		}
		return null;
	}

	/**
	 * Gets a list of files and directories in the specified parent directory and channel.
	 *
	 * @param directoryPath
	 * 		the path to the parent directory
	 * @param channelId
	 * 		the ID of the channel the directory resides in
	 *
	 * @return the files and directories in the parent directory
	 *
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public List<FileListEntry> getFileList(String directoryPath, int channelId) {
		return getFileList(directoryPath, channelId, null);
	}

	/**
	 * Gets a list of files and directories in the specified parent directory and channel.
	 *
	 * @param directoryPath
	 * 		the path to the parent directory
	 * @param channelId
	 * 		the ID of the channel the directory resides in
	 * @param channelPassword
	 * 		the password of that channel
	 *
	 * @return the files and directories in the parent directory
	 *
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public List<FileListEntry> getFileList(String directoryPath, int channelId, String channelPassword) {
		final CFtGetFileList list = new CFtGetFileList(directoryPath, channelId, channelPassword);
		if (query.doCommand(list)) {
			final List<Wrapper> responses = list.getResponse();
			final List<FileListEntry> files = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				files.add(new FileListEntry(response.getMap()));
			}
			return files;
		}
		return null;
	}

	/**
	 * Gets a list of active or recently active file transfers.
	 *
	 * @return a list of file transfers
	 */
	public List<FileTransfer> getFileTransfers() {
		final CFtList list = new CFtList();
		if (query.doCommand(list)) {
			final List<Wrapper> responses = list.getResponse();
			final List<FileTransfer> transfers = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				transfers.add(new FileTransfer(response.getMap()));
			}
			return transfers;
		}
		return null;
	}

	/**
	 * Displays detailed configuration information about the server instance including
	 * uptime, number of virtual servers online, traffic information, etc.
	 *
	 * @return information about the host
	 *
	 * @querycommands 1
	 */
	public HostInfo getHostInfo() {
		final CHostInfo info = new CHostInfo();
		if (query.doCommand(info)) {
			return new HostInfo(info.getFirstResponse().getMap());
		}
		return null;
	}

	/**
	 * Gets a list of all icon files on this virtual server.
	 *
	 * @return a list of all icons
	 */
	public List<IconFile> getIconList() {
		List<FileListEntry> files = getFileList("/icons/", 0);
		if (files == null) return null;
		List<IconFile> icons = new ArrayList<>(files.size());
		for (FileListEntry file : files) {
			if (file.isDirectory() || file.isStillUploading()) continue;
			icons.add(new IconFile(file.getMap()));
		}
		return icons;
	}

	/**
	 * Displays the server instance configuration including database revision number,
	 * the file transfer port, default group IDs, etc.
	 *
	 * @return information about the TeamSpeak server instance.
	 *
	 * @querycommands 1
	 */
	public InstanceInfo getInstanceInfo() {
		final CInstanceInfo info = new CInstanceInfo();
		if (query.doCommand(info)) {
			return new InstanceInfo(info.getFirstResponse().getMap());
		}
		return null;
	}

	/**
	 * Fetches the specified amount of log entries from the server log.
	 *
	 * @param lines
	 * 		the amount of log entries to fetch, in the range between 1 and 100.
	 * 		Returns 100 entries if the argument is not in range
	 *
	 * @return a list of the latest log entries
	 *
	 * @querycommands 1
	 */
	public List<String> getInstanceLogEntries(int lines) {
		final CLogView logs = new CLogView(lines, true);
		if (query.doCommand(logs)) {
			return logs.getLines();
		}
		return null;
	}

	/**
	 * Fetches the last 100 log entries from the server log.
	 *
	 * @return a list of up to 100 log entries
	 *
	 * @querycommands 1
	 */
	public List<String> getInstanceLogEntries() {
		return getInstanceLogEntries(100);
	}

	/**
	 * Reads the message body of a message. This will not set the read flag, though.
	 *
	 * @param messageId
	 * 		the ID of the message to be read
	 *
	 * @return the body of the message with the specified ID or {@code null} if there was no message with that ID
	 *
	 * @querycommands 1
	 * @see Message#getId()
	 * @see #setMessageRead(int)
	 */
	public String getOfflineMessage(int messageId) {
		final CMessageGet get = new CMessageGet(messageId);
		if (query.doCommand(get)) {
			return get.getFirstResponse().get("message");
		}
		return null;
	}

	/**
	 * Reads the message body of a message. This will not set the read flag, though.
	 *
	 * @param message
	 * 		the message to be read
	 *
	 * @return the body of the message with the specified ID or {@code null} if there was no message with that ID
	 *
	 * @querycommands 1
	 * @see Message#getId()
	 * @see #setMessageRead(Message)
	 */
	public String getOfflineMessage(Message message) {
		return getOfflineMessage(message.getId());
	}

	/**
	 * Gets a list of all offline messages for the server query.
	 * The returned messages lack their message body, though.
	 * To read the actual message, use {@link #getOfflineMessage(int)} or {@link #getOfflineMessage(Message)}.
	 *
	 * @return a list of all offline messages this server query has received
	 *
	 * @querycommands 1
	 */
	public List<Message> getOfflineMessages() {
		final CMessageList list = new CMessageList();
		if (query.doCommand(list)) {
			final List<Wrapper> responses = list.getResponse();
			final List<Message> msg = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				msg.add(new Message(response.getMap()));
			}
			return msg;
		}
		return null;
	}

	/**
	 * Displays detailed information about all assignments of the permission specified
	 * with {@code permName}. The output includes the type and the ID of the client,
	 * channel or group associated with the permission.
	 *
	 * @param permName
	 * 		the name of the permission
	 *
	 * @return a list of permission assignments
	 *
	 * @querycommands 1
	 * @see #getPermissionOverview(int, int)
	 */
	public List<PermissionAssignment> getPermissionAssignments(String permName) {
		final CPermFind find = new CPermFind(permName);
		if (query.doCommand(find)) {
			final List<Wrapper> responses = find.getResponse();
			final List<PermissionAssignment> assignments = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				assignments.add(new PermissionAssignment(response.getMap()));
			}
			return assignments;
		}
		return null;
	}

	/**
	 * Gets the ID of the permission specified by {@code permName}.
	 * <p>
	 * Note that the use of numeric permission IDs is deprecated
	 * and that this API only uses the string variant of the IDs.
	 * </p>
	 *
	 * @param permName
	 * 		the name of the permission
	 *
	 * @return the numeric ID of the specified permission
	 *
	 * @querycommands 1
	 */
	public int getPermissionIdByName(String permName) {
		final CPermIdGetByName get = new CPermIdGetByName(permName);
		if (query.doCommand(get)) {
			return get.getFirstResponse().getInt("permid");
		}
		return -1;
	}

	/**
	 * Gets the IDs of the permissions specified by {@code permNames}.
	 * <p>
	 * Note that the use of numeric permission IDs is deprecated
	 * and that this API only uses the string variant of the IDs.
	 * </p>
	 *
	 * @param permNames
	 * 		the names of the permissions
	 *
	 * @return the numeric IDs of the specified permission
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code permNames} is {@code null}
	 * @querycommands 1
	 */
	public int[] getPermissionIdsByName(String[] permNames) {
		if (permNames == null) throw new IllegalArgumentException("permNames was null");

		final CPermIdGetByName get = new CPermIdGetByName(permNames);
		if (query.doCommand(get)) {
			int[] ids = new int[get.getResponse().size()];
			int i = 0;
			for (Wrapper response : get.getResponse()) {
				ids[i++] = response.getInt("permid");
			}
			return ids;
		}
		return null;
	}

	/**
	 * Gets a list of all assigned permissions for a client in a specified channel.
	 * If you do not care about channel permissions, set {@code channelId} to {@code -1}.
	 *
	 * @param channelId
	 * 		the ID of the channel
	 * @param clientDBId
	 * 		the database ID of the client to create the overview for
	 *
	 * @return a list of all permission assignments for the client in the specified channel
	 *
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see Client#getDatabaseId()
	 */
	public List<PermissionAssignment> getPermissionOverview(int channelId, int clientDBId) {
		final CPermOverview overview = new CPermOverview(channelId, clientDBId);
		if (query.doCommand(overview)) {
			final List<Wrapper> responses = overview.getResponse();
			final List<PermissionAssignment> permissions = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				permissions.add(new PermissionAssignment(response.getMap()));
			}
			return permissions;
		}
		return null;
	}

	/**
	 * Displays a list of all permissions, including ID, name and description.
	 *
	 * @return a list of all permissions
	 *
	 * @querycommands 1
	 */
	public List<PermissionInfo> getPermissions() {
		final CPermissionList list = new CPermissionList();
		if (query.doCommand(list)) {
			final List<Wrapper> responses = list.getResponse();
			final List<PermissionInfo> permissions = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				permissions.add(new PermissionInfo(response.getMap()));
			}
			return permissions;
		}
		return null;
	}

	/**
	 * Displays the current value of the specified permission for this server query instance.
	 *
	 * @param permName
	 * 		the name of the permission
	 *
	 * @return the permission value, usually ranging from 0 to 100
	 *
	 * @querycommands 1
	 */
	public int getPermissionValue(String permName) {
		final CPermGet get = new CPermGet(permName);
		if (query.doCommand(get)) {
			return get.getFirstResponse().getInt("permvalue");
		}
		return -1;
	}

	/**
	 * Displays the current values of the specified permissions for this server query instance.
	 *
	 * @param permNames
	 * 		the names of the permissions
	 *
	 * @return the permission values, usually ranging from 0 to 100
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code permNames} is {@code null}
	 * @querycommands 1
	 */
	public int[] getPermissionValues(String[] permNames) {
		if (permNames == null) throw new IllegalArgumentException("permNames was null");

		final CPermGet get = new CPermGet(permNames);
		if (query.doCommand(get)) {
			int[] values = new int[get.getResponse().size()];
			int i = 0;
			for (Wrapper response : get.getResponse()) {
				values[i++] = response.getInt("permvalue");
			}
			return values;
		}
		return null;
	}

	/**
	 * Gets a list of all available tokens to join channel or server groups,
	 * including their type and group IDs.
	 *
	 * @return a list of all generated, but still unclaimed privilege keys
	 *
	 * @querycommands 1
	 * @see #addPrivilegeKey(PrivilegeKeyType, int, int, String)
	 * @see #usePrivilegeKey(String)
	 */
	public List<PrivilegeKey> getPrivilegeKeys() {
		final CPrivilegeKeyList list = new CPrivilegeKeyList();
		if (query.doCommand(list)) {
			final List<Wrapper> responses = list.getResponse();
			final List<PrivilegeKey> keys = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				keys.add(new PrivilegeKey(response.getMap()));
			}
			return keys;
		}
		return null;
	}

	/**
	 * Gets a list of all clients in the specified server group.
	 *
	 * @param serverGroupId
	 * 		the ID of the server group for which the clients should be looked up
	 *
	 * @return a list of all clients in the server group
	 *
	 * @querycommands 1
	 */
	public List<ServerGroupClient> getServerGroupClients(int serverGroupId) {
		final CServerGroupClientList list = new CServerGroupClientList(serverGroupId);
		if (query.doCommand(list)) {
			final List<Wrapper> responses = list.getResponse();
			final List<ServerGroupClient> clients = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				clients.add(new ServerGroupClient(response.getMap()));
			}
			return clients;
		}
		return null;
	}

	/**
	 * Gets a list of all clients in the specified server group.
	 *
	 * @param serverGroup
	 * 		the server group for which the clients should be looked up
	 *
	 * @return a list of all clients in the server group
	 *
	 * @querycommands 1
	 */
	public List<ServerGroupClient> getServerGroupClients(ServerGroup serverGroup) {
		return getServerGroupClients(serverGroup.getId());
	}

	/**
	 * Gets a list of all permissions assigned to the specified server group.
	 *
	 * @param serverGroupId
	 * 		the ID of the server group for which the permissions should be looked up
	 *
	 * @return a list of all permissions assigned to the server group
	 *
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 * @see #getServerGroupPermissions(ServerGroup)
	 */
	public List<Permission> getServerGroupPermissions(int serverGroupId) {
		final CServerGroupPermList list = new CServerGroupPermList(serverGroupId);
		if (query.doCommand(list)) {
			final List<Wrapper> responses = list.getResponse();
			final List<Permission> permissions = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				permissions.add(new Permission(response.getMap()));
			}
			return permissions;
		}
		return null;
	}

	/**
	 * Gets a list of all permissions assigned to the specified server group.
	 *
	 * @param serverGroup
	 * 		the server group for which the permissions should be looked up
	 *
	 * @return a list of all permissions assigned to the server group
	 *
	 * @querycommands 1
	 */
	public List<Permission> getServerGroupPermissions(ServerGroup serverGroup) {
		return getServerGroupPermissions(serverGroup.getId());
	}

	/**
	 * Gets a list of all server groups on the virtual server.
	 * <p>
	 * Depending on your permissions, the output may also contain
	 * global server query groups and template groups.
	 * </p>
	 *
	 * @return a list of all server groups
	 *
	 * @querycommands 1
	 */
	public List<ServerGroup> getServerGroups() {
		final CServerGroupList list = new CServerGroupList();
		if (query.doCommand(list)) {
			final List<Wrapper> responses = list.getResponse();
			final List<ServerGroup> groups = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				groups.add(new ServerGroup(response.getMap()));
			}
			return groups;
		}
		return null;
	}

	/**
	 * Gets a list of all server groups set for a client.
	 *
	 * @param clientDatabaseId
	 * 		the database ID of the client for which the server groups should be looked up
	 *
	 * @return a list of all server groups set for the client
	 *
	 * @querycommands 2
	 * @see Client#getDatabaseId()
	 * @see #getServerGroupsByClient(Client)
	 */
	public List<ServerGroup> getServerGroupsByClientId(int clientDatabaseId) {
		final CServerGroupsByClientId client = new CServerGroupsByClientId(clientDatabaseId);
		if (query.doCommand(client)) {
			final List<Wrapper> responses = client.getResponse();
			final List<ServerGroup> list = new ArrayList<>(responses.size());
			final List<ServerGroup> allGroups = getServerGroups();

			for (final Wrapper response : responses) {
				for (final ServerGroup s : allGroups) {
					if (s.getId() == response.getInt("sgid")) {
						list.add(s);
					}
				}
			}
			return list;
		}
		return null;
	}

	/**
	 * Gets a list of all server groups set for a client.
	 *
	 * @param client
	 * 		the client for which the server groups should be looked up
	 *
	 * @return a list of all server group set for the client
	 *
	 * @querycommands 2
	 * @see #getServerGroupsByClientId(int)
	 */
	public List<ServerGroup> getServerGroupsByClient(Client client) {
		return getServerGroupsByClientId(client.getDatabaseId());
	}

	/**
	 * Gets the ID of a virtual server by its port.
	 *
	 * @param port
	 * 		the port of a virtual server
	 *
	 * @return the ID of the virtual server
	 *
	 * @querycommands 1
	 * @see VirtualServer#getPort()
	 * @see VirtualServer#getId()
	 */
	public int getServerIdByPort(int port) {
		final CServerIdGetByPort s = new CServerIdGetByPort(port);
		if (query.doCommand(s)) {
			return s.getFirstResponse().getInt("server_id");
		}
		return -1;
	}

	/**
	 * Gets detailed information about the virtual server the server query is currently in.
	 *
	 * @return information about the current virtual server
	 *
	 * @querycommands 1
	 */
	public VirtualServerInfo getServerInfo() {
		final CServerInfo info = new CServerInfo();
		if (query.doCommand(info)) {
			return new VirtualServerInfo(info.getFirstResponse().getMap());
		}
		return null;
	}

	/**
	 * Gets the version, build number and platform of the TeamSpeak3 server.
	 *
	 * @return the version information of the server
	 *
	 * @querycommands 1
	 */
	public Version getVersion() {
		final CVersion version = new CVersion();
		if (query.doCommand(version)) {
			return new Version(version.getFirstResponse().getMap());
		}
		return null;
	}

	/**
	 * Gets a list of all virtual servers including their ID, status, number of clients online, etc.
	 *
	 * @return a list of all virtual servers
	 *
	 * @querycommands 1
	 */
	public List<VirtualServer> getVirtualServers() {
		final CServerList serverList = new CServerList();
		if (query.doCommand(serverList)) {
			final List<Wrapper> responses = serverList.getResponse();
			final List<VirtualServer> servers = new ArrayList<>(responses.size());

			for (final Wrapper response : responses) {
				servers.add((new VirtualServer(response.getMap())));
			}
			return servers;
		}
		return null;
	}

	/**
	 * Fetches the specified amount of log entries from the currently selected virtual server.
	 * If no virtual server is selected, the entries will be read from the server log instead.
	 *
	 * @param lines
	 * 		the amount of log entries to fetch, in the range between 1 and 100.
	 * 		Returns 100 entries if the argument is not in range
	 *
	 * @return a list of the latest log entries
	 *
	 * @querycommands 1
	 */
	public List<String> getVirtualServerLogEntries(int lines) {
		final CLogView logs = new CLogView(lines, false);
		if (query.doCommand(logs)) {
			return logs.getLines();
		}
		return null;
	}

	/**
	 * Fetches the last 100 log entries from the currently selected virtual server.
	 * If no virtual server is selected, the entries will be read from the server log instead.
	 *
	 * @return a list of up to 100 log entries
	 *
	 * @querycommands 1
	 */
	public List<String> getVirtualServerLogEntries() {
		return getVirtualServerLogEntries(100);
	}

	/**
	 * Kicks one or more clients from their current channels.
	 * This will move the kicked clients into the default channel and
	 * won't do anything if the clients are already in the default channel.
	 *
	 * @param clientIds
	 * 		the IDs of the clients to kick
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #kickClientFromChannel(Client...)
	 * @see #kickClientFromChannel(String, int...)
	 */
	public boolean kickClientFromChannel(int... clientIds) {
		return kickClients(ReasonIdentifier.REASON_KICK_CHANNEL, null, clientIds);
	}

	/**
	 * Kicks one or more clients from their current channels.
	 * This will move the kicked clients into the default channel and
	 * won't do anything if the clients are already in the default channel.
	 *
	 * @param clients
	 * 		the clients to kick
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #kickClientFromChannel(int...)
	 * @see #kickClientFromChannel(String, Client...)
	 */
	public boolean kickClientFromChannel(Client... clients) {
		return kickClients(ReasonIdentifier.REASON_KICK_CHANNEL, null, clients);
	}

	/**
	 * Kicks one or more clients from their current channels for the specified reason.
	 * This will move the kicked clients into the default channel and
	 * won't do anything if the clients are already in the default channel.
	 *
	 * @param message
	 * 		the reason message to display to the clients
	 * @param clientIds
	 * 		the IDs of the clients to kick
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #kickClientFromChannel(int...)
	 * @see #kickClientFromChannel(String, Client...)
	 */
	public boolean kickClientFromChannel(String message, int... clientIds) {
		return kickClients(ReasonIdentifier.REASON_KICK_CHANNEL, message, clientIds);
	}

	/**
	 * Kicks one or more clients from their current channels for the specified reason.
	 * This will move the kicked clients into the default channel and
	 * won't do anything if the clients are already in the default channel.
	 *
	 * @param message
	 * 		the reason message to display to the clients
	 * @param clients
	 * 		the clients to kick
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #kickClientFromChannel(Client...)
	 * @see #kickClientFromChannel(String, int...)
	 */
	public boolean kickClientFromChannel(String message, Client... clients) {
		return kickClients(ReasonIdentifier.REASON_KICK_CHANNEL, message, clients);
	}

	/**
	 * Kicks one or more clients from the server.
	 *
	 * @param clientIds
	 * 		the IDs of the clients to kick
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #kickClientFromServer(Client...)
	 * @see #kickClientFromServer(String, int...)
	 */
	public boolean kickClientFromServer(int... clientIds) {
		return kickClients(ReasonIdentifier.REASON_KICK_SERVER, null, clientIds);
	}

	/**
	 * Kicks one or more clients from the server.
	 *
	 * @param clients
	 * 		the clients to kick
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #kickClientFromServer(int...)
	 * @see #kickClientFromServer(String, Client...)
	 */
	public boolean kickClientFromServer(Client... clients) {
		return kickClients(ReasonIdentifier.REASON_KICK_SERVER, null, clients);
	}

	/**
	 * Kicks one or more clients from the server for the specified reason.
	 *
	 * @param message
	 * 		the reason message to display to the clients
	 * @param clientIds
	 * 		the IDs of the clients to kick
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #kickClientFromServer(int...)
	 * @see #kickClientFromServer(String, Client...)
	 */
	public boolean kickClientFromServer(String message, int... clientIds) {
		return kickClients(ReasonIdentifier.REASON_KICK_SERVER, message, clientIds);
	}

	/**
	 * Kicks one or more clients from the server for the specified reason.
	 *
	 * @param message
	 * 		the reason message to display to the clients
	 * @param clients
	 * 		the clients to kick
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #kickClientFromServer(Client...)
	 * @see #kickClientFromServer(String, int...)
	 */
	public boolean kickClientFromServer(String message, Client... clients) {
		return kickClients(ReasonIdentifier.REASON_KICK_SERVER, message, clients);
	}

	/**
	 * Kicks a list of clients from either the channel or the server for a given reason.
	 *
	 * @param reason
	 * 		where to kick the clients from
	 * @param message
	 * 		the reason message to display to the clients
	 * @param clients
	 * 		the clients to kick
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 */
	private boolean kickClients(ReasonIdentifier reason, String message, Client... clients) {
		int[] clientIds = new int[clients.length];
		for (int i = 0; i < clients.length; ++i) {
			clientIds[i] = clients[i].getId();
		}
		return kickClients(reason, message, clientIds);
	}

	/**
	 * Kicks a list of clients from either the channel or the server for a given reason.
	 *
	 * @param reason
	 * 		where to kick the clients from
	 * @param message
	 * 		the reason message to display to the clients
	 * @param clientIds
	 * 		the IDs of the clients to kick
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Client#getId()
	 */
	private boolean kickClients(ReasonIdentifier reason, String message, int... clientIds) {
		final CClientKick kick = new CClientKick(reason, message, clientIds);
		return query.doCommand(kick);
	}

	/**
	 * Logs the server query in using the specified username and password.
	 * <p>
	 * Note that you can also set the login in the {@link TS3Config},
	 * so that you will be logged in right after the connection is established.
	 * </p>
	 *
	 * @param username
	 * 		the username of the server query
	 * @param password
	 * 		the password to use
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #logout()
	 */
	public boolean login(String username, String password) {
		final CLogin login = new CLogin(username, password);
		return query.doCommand(login);
	}

	/**
	 * Logs the server query out and deselects the current virtual server.
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #login(String, String)
	 */
	public boolean logout() {
		final CLogout logout = new CLogout();
		return query.doCommand(logout);
	}

	/**
	 * Moves a channel to a new parent channel specified by its ID.
	 * To move a channel to root level, set {@code channelTargetId} to {@code 0}.
	 * <p>
	 * This will move the channel right below the specified parent channel, above all other child channels.
	 * The command will fail if the channel already has the specified target channel as the parent channel.
	 * </p>
	 *
	 * @param channelId
	 * 		the channel to move
	 * @param channelTargetId
	 * 		the new parent channel for the specified channel
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see #moveChannel(int, int, int)
	 */
	public boolean moveChannel(int channelId, int channelTargetId) {
		return moveChannel(channelId, channelTargetId, 0);
	}

	/**
	 * Moves a channel to a new parent channel specified by its ID.
	 * To move a channel to root level, set {@code channelTargetId} to {@code 0}.
	 * <p>
	 * The channel will be ordered below the channel with the ID specified by {@code order}.
	 * To move the channel right below the parent channel, set {@code order} to {@code 0}.
	 * </p><p>
	 * Note that re-ordering a channel without also changing its parent channel cannot be done with this method.
	 * Use {@link #editChannel(int, Map)} to change {@link ChannelProperty#CHANNEL_ORDER} instead.
	 * </p>
	 *
	 * @param channelId
	 * 		the channel to move
	 * @param channelTargetId
	 * 		the new parent channel for the specified channel
	 * @param order
	 * 		the channel to sort the specified channel below
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see #moveChannel(int, int)
	 */
	public boolean moveChannel(int channelId, int channelTargetId, int order) {
		final CChannelMove move = new CChannelMove(channelId, channelTargetId, order);
		return query.doCommand(move);
	}

	/**
	 * Moves a client into a channel.
	 * <p>
	 * Consider using {@link #moveClients(int[], int)}
	 * for moving multiple clients.
	 * </p>
	 *
	 * @param clientId
	 * 		the ID of the client to move
	 * @param channelId
	 * 		the ID of the channel to move the client into
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Client#getId()
	 * @see Channel#getId()
	 */
	public boolean moveClient(int clientId, int channelId) {
		return moveClient(clientId, channelId, null);
	}

	/**
	 * Moves multiple clients into a channel.
	 * Immediately returns {@code true} for an empty client ID array.
	 * <p>
	 * Use this method instead of {@link #moveClient(int, int)} for moving
	 * several clients as this will only send 1 command to the server and thus complete faster.
	 * </p>
	 *
	 * @param clientIds
	 * 		the IDs of the clients to move, cannot be {@code null}
	 * @param channelId
	 * 		the ID of the channel to move the clients into
	 *
	 * @return whether the command succeeded or not
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code clientIds} is {@code null}
	 * @querycommands 1
	 * @see Client#getId()
	 * @see Channel#getId()
	 */
	public boolean moveClients(int[] clientIds, int channelId) {
		return moveClients(clientIds, channelId, null);
	}

	/**
	 * Moves a client into a channel.
	 * <p>
	 * Consider using {@link #moveClients(Client[], ChannelBase)}
	 * for moving multiple clients.
	 * </p>
	 *
	 * @param client
	 * 		the client to move, cannot be {@code null}
	 * @param channel
	 * 		the channel to move the client into, cannot be {@code null}
	 *
	 * @return whether the command succeeded or not
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code client} or {@code channel} is {@code null}
	 * @querycommands 1
	 */
	public boolean moveClient(Client client, ChannelBase channel) {
		return moveClient(client, channel, null);
	}

	/**
	 * Moves multiple clients into a channel.
	 * Immediately returns {@code true} for an empty client array.
	 * <p>
	 * Use this method instead of {@link #moveClient(Client, ChannelBase)} for moving
	 * several clients as this will only send 1 command to the server and thus complete faster.
	 * </p>
	 *
	 * @param clients
	 * 		the clients to move, cannot be {@code null}
	 * @param channel
	 * 		the channel to move the clients into, cannot be {@code null}
	 *
	 * @return whether the command succeeded or not
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code clients} or {@code channel} is {@code null}
	 * @querycommands 1
	 */
	public boolean moveClients(Client[] clients, ChannelBase channel) {
		return moveClients(clients, channel, null);
	}

	/**
	 * Moves a client into a channel using the specified password.
	 * <p>
	 * Consider using {@link #moveClients(int[], int, String)}
	 * for moving multiple clients.
	 * </p>
	 *
	 * @param clientId
	 * 		the ID of the client to move
	 * @param channelId
	 * 		the ID of the channel to move the client into
	 * @param channelPassword
	 * 		the password of the channel, can be {@code null}
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Client#getId()
	 * @see Channel#getId()
	 */
	public boolean moveClient(int clientId, int channelId, String channelPassword) {
		final CClientMove move = new CClientMove(clientId, channelId, channelPassword);
		return query.doCommand(move);
	}

	/**
	 * Moves multiple clients into a channel using the specified password.
	 * Immediately returns {@code true} for an empty client ID array.
	 * <p>
	 * Use this method instead of {@link #moveClient(int, int, String)} for moving
	 * several clients as this will only send 1 command to the server and thus complete faster.
	 * </p>
	 *
	 * @param clientIds
	 * 		the IDs of the clients to move, cannot be {@code null}
	 * @param channelId
	 * 		the ID of the channel to move the clients into
	 * @param channelPassword
	 * 		the password of the channel, can be {@code null}
	 *
	 * @return whether the command succeeded or not
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code clientIds} is {@code null}
	 * @querycommands 1
	 * @see Client#getId()
	 * @see Channel#getId()
	 */
	public boolean moveClients(int[] clientIds, int channelId, String channelPassword) {
		if (clientIds == null) throw new IllegalArgumentException("clientIds was null");
		if (clientIds.length == 0) return true;

		final CClientMove move = new CClientMove(clientIds, channelId, channelPassword);
		return query.doCommand(move);
	}

	/**
	 * Moves a single client into a channel using the specified password.
	 * <p>
	 * Consider using {@link #moveClients(Client[], ChannelBase, String)}
	 * for moving multiple clients.
	 * </p>
	 *
	 * @param client
	 * 		the client to move, cannot be {@code null}
	 * @param channel
	 * 		the channel to move the client into, cannot be {@code null}
	 * @param channelPassword
	 * 		the password of the channel, can be {@code null}
	 *
	 * @return whether the command succeeded or not
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code client} or {@code channel} is {@code null}
	 * @querycommands 1
	 */
	public boolean moveClient(Client client, ChannelBase channel, String channelPassword) {
		if (client == null) throw new IllegalArgumentException("client was null");
		if (channel == null) throw new IllegalArgumentException("channel was null");

		return moveClient(client.getId(), channel.getId(), channelPassword);
	}

	/**
	 * Moves multiple clients into a channel using the specified password.
	 * Immediately returns {@code true} for an empty client array.
	 * <p>
	 * Use this method instead of {@link #moveClient(Client, ChannelBase, String)} for moving
	 * several clients as this will only send 1 command to the server and thus complete faster.
	 * </p>
	 *
	 * @param clients
	 * 		the clients to move, cannot be {@code null}
	 * @param channel
	 * 		the channel to move the clients into, cannot be {@code null}
	 * @param channelPassword
	 * 		the password of the channel, can be {@code null}
	 *
	 * @return whether the command succeeded or not
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code clients} or {@code channel} is {@code null}
	 * @querycommands 1
	 */
	public boolean moveClients(Client[] clients, ChannelBase channel, String channelPassword) {
		if (clients == null) throw new IllegalArgumentException("clients was null");
		if (channel == null) throw new IllegalArgumentException("channel was null");

		int[] clientIds = new int[clients.length];
		for (int i = 0; i < clients.length; i++) {
			Client client = clients[i];
			clientIds[i] = client.getId();
		}
		return moveClients(clientIds, channel.getId(), channelPassword);
	}

	/**
	 * Moves and renames a file on the file repository within the same channel.
	 *
	 * @param oldPath
	 * 		the current path to the file
	 * @param newPath
	 * 		the desired new path
	 * @param channelId
	 * 		the ID of the channel the file resides in
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #moveFile(String, String, int, int) moveFile to a different channel
	 */
	public boolean moveFile(String oldPath, String newPath, int channelId) {
		return moveFile(oldPath, newPath, channelId, null);
	}

	/**
	 * Renames a file on the file repository and moves it to a new path in a different channel.
	 *
	 * @param oldPath
	 * 		the current path to the file
	 * @param newPath
	 * 		the desired new path
	 * @param oldChannelId
	 * 		the ID of the channel the file currently resides in
	 * @param newChannelId
	 * 		the ID of the channel the file should be moved to
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #moveFile(String, String, int) moveFile within the same channel
	 */
	public boolean moveFile(String oldPath, String newPath, int oldChannelId, int newChannelId) {
		return moveFile(oldPath, newPath, oldChannelId, null, newChannelId, null);
	}

	/**
	 * Moves and renames a file on the file repository within the same channel.
	 *
	 * @param oldPath
	 * 		the current path to the file
	 * @param newPath
	 * 		the desired new path
	 * @param channelId
	 * 		the ID of the channel the file resides in
	 * @param channelPassword
	 * 		the password of the channel
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #moveFile(String, String, int, String, int, String) moveFile to a different channel
	 */
	public boolean moveFile(String oldPath, String newPath, int channelId, String channelPassword) {
		final CFtRenameFile rename = new CFtRenameFile(oldPath, newPath, channelId, channelPassword);
		return query.doCommand(rename);
	}

	/**
	 * Renames a file on the file repository and moves it to a new path in a different channel.
	 *
	 * @param oldPath
	 * 		the current path to the file
	 * @param newPath
	 * 		the desired new path
	 * @param oldChannelId
	 * 		the ID of the channel the file currently resides in
	 * @param oldPassword
	 * 		the password of the current channel
	 * @param newChannelId
	 * 		the ID of the channel the file should be moved to
	 * @param newPassword
	 * 		the password of the new channel
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #moveFile(String, String, int, String) moveFile within the same channel
	 */
	public boolean moveFile(String oldPath, String newPath, int oldChannelId, String oldPassword, int newChannelId, String newPassword) {
		final CFtRenameFile rename = new CFtRenameFile(oldPath, newPath, oldChannelId, oldPassword, newChannelId, newPassword);
		return query.doCommand(rename);
	}

	/**
	 * Moves the server query into a channel.
	 *
	 * @param channelId
	 * 		the ID of the channel to move the server query into
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Channel#getId()
	 */
	public boolean moveQuery(int channelId) {
		return moveClient(0, channelId, null);
	}

	/**
	 * Moves the server query into a channel.
	 *
	 * @param channel
	 * 		the channel to move the server query into, cannot be {@code null}
	 *
	 * @return whether the command succeeded or not
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code channel} is {@code null}
	 * @querycommands 1
	 */
	public boolean moveQuery(ChannelBase channel) {
		if (channel == null) throw new IllegalArgumentException("channel was null");

		return moveClient(0, channel.getId(), null);
	}

	/**
	 * Moves the server query into a channel using the specified password.
	 *
	 * @param channelId
	 * 		the ID of the channel to move the client into
	 * @param channelPassword
	 * 		the password of the channel, can be {@code null}
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Channel#getId()
	 */
	public boolean moveQuery(int channelId, String channelPassword) {
		return moveClient(0, channelId, channelPassword);
	}

	/**
	 * Moves the server query into a channel using the specified password.
	 *
	 * @param channel
	 * 		the channel to move the client into, cannot be {@code null}
	 * @param channelPassword
	 * 		the password of the channel, can be {@code null}
	 *
	 * @return whether the command succeeded or not
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code channel} is {@code null}
	 * @querycommands 1
	 */
	public boolean moveQuery(ChannelBase channel, String channelPassword) {
		if (channel == null) throw new IllegalArgumentException("channel was null");

		return moveClient(0, channel.getId(), channelPassword);
	}

	/**
	 * Pokes the client with the specified client ID.
	 * This opens up a small popup window for the client containing your message and plays a sound.
	 * The displayed message will be formatted like this: <br>
	 * {@code hh:mm:ss - "Your Nickname" poked you: <your message in green color>}
	 * <p>
	 * The displayed message length is limited to 100 UTF-8 bytes.
	 * If a client has already received a poke message, all subsequent pokes will simply add a line
	 * to the already opened popup window and will still play a sound.
	 * </p>
	 *
	 * @param clientId
	 * 		the ID of the client to poke
	 * @param message
	 * 		the message to send, may contain BB codes
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Client#getId()
	 */
	public boolean pokeClient(int clientId, String message) {
		final CClientPoke poke = new CClientPoke(clientId, message);
		return query.doCommand(poke);
	}

	/**
	 * Terminates the connection with the TeamSpeak3 server.
	 * This command should never be executed manually.
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @deprecated This command leaves the query in an undefined state,
	 * where the connection is closed without the socket being closed and no more command can be executed.
	 * To terminate a connection, use {@link TS3Query#exit()}.
	 */
	@Deprecated
	public boolean quit() {
		return query.doCommand(new CQuit());
	}

	/**
	 * Registers the server query to receive notifications about all server events.
	 * <p>
	 * This means that the following actions will trigger event notifications:
	 * </p>
	 * <ul>
	 * <li>A client joins the server or disconnects from it</li>
	 * <li>A client switches channels</li>
	 * <li>A client sends a server message</li>
	 * <li>A client sends a channel message <b>in the channel the query is in</b></li>
	 * <li>A client sends <b>the server query</b> a private message or a response to a private message</li>
	 * </ul>
	 * <p>
	 * The limitations to when the query receives notifications about chat events cannot be circumvented.
	 * </p>
	 * To be able to process these events in your application, register an event listener.
	 *
	 * @return whether all commands succeeded or not
	 *
	 * @querycommands 6
	 * @see #addTS3Listeners(TS3Listener...)
	 */
	public boolean registerAllEvents() {
		// No immediate return because even if one registration fails, a following one might still work
		boolean success = registerEvent(TS3EventType.SERVER);
		success &= registerEvent(TS3EventType.TEXT_SERVER);
		success &= registerEvent(TS3EventType.CHANNEL, 0);
		success &= registerEvent(TS3EventType.TEXT_CHANNEL, 0);
		success &= registerEvent(TS3EventType.TEXT_PRIVATE);
		success &= registerEvent(TS3EventType.PRIVILEGE_KEY_USED);

		return success;
	}

	/**
	 * Registers the server query to receive notifications about a given event type.
	 * <p>
	 * If used with {@link TS3EventType#TEXT_CHANNEL}, this will listen to chat events in the current channel.
	 * If used with {@link TS3EventType#CHANNEL}, this will listen to <b>all</b> channel events.
	 * To specify a different channel for channel events, use {@link #registerEvent(TS3EventType, int)}.
	 * </p>
	 *
	 * @param eventType
	 * 		the event type to be notified about
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #addTS3Listeners(TS3Listener...)
	 * @see #registerEvent(TS3EventType, int)
	 * @see #registerAllEvents()
	 */
	public boolean registerEvent(TS3EventType eventType) {
		if (eventType == TS3EventType.CHANNEL || eventType == TS3EventType.TEXT_CHANNEL) {
			return registerEvent(eventType, 0);
		}
		return registerEvent(eventType, -1);
	}

	/**
	 * Registers the server query to receive notifications about a given event type.
	 *
	 * @param eventType
	 * 		the event type to be notified about
	 * @param channelId
	 * 		the ID of the channel to listen to, will be ignored if set to {@code -1}.
	 * 		Can be set to {@code 0} for {@link TS3EventType#CHANNEL} to receive notifications about all channel switches.
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see #addTS3Listeners(TS3Listener...)
	 * @see #registerAllEvents()
	 */
	public boolean registerEvent(TS3EventType eventType, int channelId) {
		final CServerNotifyRegister register = new CServerNotifyRegister(eventType, channelId);
		return query.doCommand(register);
	}

	/**
	 * Registers the server query to receive notifications about multiple given event types.
	 * <p>
	 * If used with {@link TS3EventType#TEXT_CHANNEL}, this will listen to chat events in the current channel.
	 * If used with {@link TS3EventType#CHANNEL}, this will listen to <b>all</b> channel events.
	 * To specify a different channel for channel events, use {@link #registerEvent(TS3EventType, int)}.
	 * </p>
	 *
	 * @param eventTypes
	 * 		the event types to be notified about
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands n, one command per TS3EventType
	 * @see #addTS3Listeners(TS3Listener...)
	 * @see #registerEvent(TS3EventType, int)
	 * @see #registerAllEvents()
	 */
	public boolean registerEvents(TS3EventType... eventTypes) {
		for (final TS3EventType type : eventTypes) {
			if (!registerEvent(type)) return false;
		}
		return true;
	}

	/**
	 * Removes the client specified by its database ID from the specified server group.
	 *
	 * @param serverGroupId
	 * 		the ID of the server group
	 * @param clientDatabaseId
	 * 		the database ID of the client
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 * @see Client#getDatabaseId()
	 * @see #removeClientFromServerGroup(ServerGroup, Client)
	 */
	public boolean removeClientFromServerGroup(int serverGroupId, int clientDatabaseId) {
		final CServerGroupDelClient del = new CServerGroupDelClient(serverGroupId, clientDatabaseId);
		return query.doCommand(del);
	}

	/**
	 * Removes the specified client from the specified server group.
	 *
	 * @param serverGroup
	 * 		the server group to remove the client from
	 * @param client
	 * 		the client to remove from the server group
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #removeClientFromServerGroup(int, int)
	 */
	public boolean removeClientFromServerGroup(ServerGroup serverGroup, Client client) {
		return removeClientFromServerGroup(serverGroup.getId(), client.getDatabaseId());
	}

	/**
	 * Removes one or more {@link TS3Listener}s to the event manager of the query.
	 * <p>
	 * If a listener was not actually registered, it will be ignored and no exception will be thrown.
	 * </p>
	 *
	 * @param listeners
	 * 		one or more listeners to remove
	 *
	 * @see #addTS3Listeners(TS3Listener...)
	 * @see TS3Listener
	 * @see TS3EventType
	 */
	public void removeTS3Listeners(TS3Listener... listeners) {
		query.getEventManager().removeListeners(listeners);
	}

	/**
	 * Renames the channel group with the specified ID.
	 *
	 * @param channelGroupId
	 * 		the ID of the channel group to rename
	 * @param name
	 * 		the new name for the channel group
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 * @see #renameChannelGroup(ChannelGroup, String)
	 */
	public boolean renameChannelGroup(int channelGroupId, String name) {
		final CChannelGroupRename rename = new CChannelGroupRename(channelGroupId, name);
		return query.doCommand(rename);
	}

	/**
	 * Renames the specified channel group.
	 *
	 * @param channelGroup
	 * 		the channel group to rename
	 * @param name
	 * 		the new name for the channel group
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #renameChannelGroup(int, String)
	 */
	public boolean renameChannelGroup(ChannelGroup channelGroup, String name) {
		return renameChannelGroup(channelGroup.getId(), name);
	}

	/**
	 * Renames the server group with the specified ID.
	 *
	 * @param serverGroupId
	 * 		the ID of the server group to rename
	 * @param name
	 * 		the new name for the server group
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 * @see #renameServerGroup(ServerGroup, String)
	 */
	public boolean renameServerGroup(int serverGroupId, String name) {
		final CServerGroupRename rename = new CServerGroupRename(serverGroupId, name);
		return query.doCommand(rename);
	}

	/**
	 * Renames the specified server group.
	 *
	 * @param serverGroup
	 * 		the server group to rename
	 * @param name
	 * 		the new name for the server group
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #renameServerGroup(int, String)
	 */
	public boolean renameServerGroup(ServerGroup serverGroup, String name) {
		return renameChannelGroup(serverGroup.getId(), name);
	}

	/**
	 * Resets all permissions and deletes all server / channel groups. Use carefully.
	 *
	 * @return a token for a new administrator account
	 *
	 * @querycommands 1
	 */
	public String resetPermissions() {
		final CPermReset reset = new CPermReset();
		if (query.doCommand(reset)) {
			return reset.getFirstResponse().get("token");
		}
		return null;
	}

	/**
	 * Moves the server query into the virtual server with the specified ID.
	 *
	 * @param id
	 * 		the ID of the virtual server
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see VirtualServer#getId()
	 * @see #selectVirtualServerByPort(int)
	 * @see #selectVirtualServer(VirtualServer)
	 */
	public boolean selectVirtualServerById(int id) {
		final CUse use = new CUse(id, -1);
		return query.doCommand(use);
	}

	/**
	 * Moves the server query into the virtual server with the specified voice port.
	 *
	 * @param port
	 * 		the voice port of the virtual server
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see VirtualServer#getPort()
	 * @see #selectVirtualServerById(int)
	 * @see #selectVirtualServer(VirtualServer)
	 */
	public boolean selectVirtualServerByPort(int port) {
		final CUse use = new CUse(-1, port);
		return query.doCommand(use);
	}

	/**
	 * Moves the server query into the specified virtual server.
	 *
	 * @param server
	 * 		the virtual server to move into
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #selectVirtualServerById(int)
	 * @see #selectVirtualServerByPort(int)
	 */
	public boolean selectVirtualServer(VirtualServer server) {
		return selectVirtualServerById(server.getId());
	}

	/**
	 * Sends an offline message to the client with the given unique identifier.
	 * <p>
	 * The message subject's length is limited to 200 UTF-8 bytes and BB codes in it will be ignored.
	 * The message body's length is limited to 4096 UTF-8 bytes and accepts BB codes
	 * </p>
	 *
	 * @param clientUId
	 * 		the unique identifier of the client to send the message to
	 * @param subject
	 * 		the subject for the message, may not contain BB codes
	 * @param message
	 * 		the actual message body, may contain BB codes
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Client#getUniqueIdentifier()
	 * @see Message
	 */
	public boolean sendOfflineMessage(String clientUId, String subject, String message) {
		final CMessageAdd add = new CMessageAdd(clientUId, subject, message);
		return query.doCommand(add);
	}

	/**
	 * Sends a text message either to the whole virtual server, a channel or specific client.
	 * Your message may contain BB codes, but its length is limited to 1024 UTF-8 bytes.
	 * <p>
	 * To send a message to all virtual servers, use {@link #broadcast(String)}.
	 * To send an offline message, use {@link #sendOfflineMessage(String, String, String)}.
	 * </p>
	 *
	 * @param targetMode
	 * 		where the message should be sent to
	 * @param targetId
	 * 		the client ID of the recipient of this message. This value is ignored unless {@code targetMode} is {@code CLIENT}
	 * @param message
	 * 		the text message to send
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Client#getId()
	 */
	public boolean sendTextMessage(TextMessageTargetMode targetMode, int targetId, String message) {
		final CSendTextMessage msg = new CSendTextMessage(targetMode.getIndex(), targetId, message);
		return query.doCommand(msg);
	}

	/**
	 * Sends a text message to the channel with the specified ID.
	 * Your message may contain BB codes, but its length is limited to 1024 UTF-8 bytes.
	 * <p>
	 * This will move the client into the channel with the specified channel ID,
	 * <b>but will not move it back to the original channel!</b>
	 * </p>
	 *
	 * @param channelId
	 * 		the ID of the channel to which the message should be sent to
	 * @param message
	 * 		the text message to send
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #sendChannelMessage(String)
	 * @see Channel#getId()
	 */
	public boolean sendChannelMessage(int channelId, String message) {
		return moveQuery(channelId) && sendTextMessage(TextMessageTargetMode.CHANNEL, 0, message);
	}

	/**
	 * Sends a text message to the channel the server query is currently in.
	 * Your message may contain BB codes, but its length is limited to 1024 UTF-8 bytes.
	 *
	 * @param message
	 * 		the text message to send
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 */
	public boolean sendChannelMessage(String message) {
		return sendTextMessage(TextMessageTargetMode.CHANNEL, 0, message);
	}

	/**
	 * Sends a text message to the virtual server with the specified ID.
	 * Your message may contain BB codes, but its length is limited to 1024 UTF-8 bytes.
	 * <p>
	 * This will move the client to the virtual server with the specified server ID,
	 * <b>but will not move it back to the original virtual server!</b>
	 * </p>
	 *
	 * @param serverId
	 * 		the ID of the virtual server to which the message should be sent to
	 * @param message
	 * 		the text message to send
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #sendServerMessage(String)
	 * @see VirtualServer#getId()
	 */
	public boolean sendServerMessage(int serverId, String message) {
		return selectVirtualServerById(serverId) && sendTextMessage(TextMessageTargetMode.SERVER, 0, message);
	}

	/**
	 * Sends a text message to the virtual server the server query is currently in.
	 * Your message may contain BB codes, but its length is limited to 1024 UTF-8 bytes.
	 *
	 * @param message
	 * 		the text message to send
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 */
	public boolean sendServerMessage(String message) {
		return sendTextMessage(TextMessageTargetMode.SERVER, 0, message);
	}

	/**
	 * Sends a private message to the client with the specified client ID.
	 * Your message may contain BB codes, but its length is limited to 1024 UTF-8 bytes.
	 *
	 * @param clientId
	 * 		the ID of the client to send the message to
	 * @param message
	 * 		the text message to send
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see Client#getId()
	 */
	public boolean sendPrivateMessage(int clientId, String message) {
		return sendTextMessage(TextMessageTargetMode.CLIENT, clientId, message);
	}

	/**
	 * Sets a channel group for a client in a specific channel.
	 *
	 * @param groupId
	 * 		the ID of the group the client should join
	 * @param channelId
	 * 		the ID of the channel where the channel group should be assigned
	 * @param clientDBId
	 * 		the database ID of the client for which the channel group should be set
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 * @see Channel#getId()
	 * @see Client#getDatabaseId()
	 */
	public boolean setClientChannelGroup(int groupId, int channelId, int clientDBId) {
		final CSetClientChannelGroup group = new CSetClientChannelGroup(groupId, channelId, clientDBId);
		return query.doCommand(group);
	}

	/**
	 * Sets the read flag to true for a given message. This will not delete the message.
	 *
	 * @param messageId
	 * 		the ID of the message for which the read flag should be set
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #setMessageReadFlag(int, boolean)
	 */
	public boolean setMessageRead(int messageId) {
		return setMessageReadFlag(messageId, true);
	}

	/**
	 * Sets the read flag to true for a given message. This will not delete the message.
	 *
	 * @param message
	 * 		the message for which the read flag should be set
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #setMessageRead(int)
	 * @see #setMessageReadFlag(Message, boolean)
	 * @see #deleteOfflineMessage(int)
	 */
	public boolean setMessageRead(Message message) {
		return setMessageReadFlag(message.getId(), true);
	}

	/**
	 * Sets the read flag for a given message. This will not delete the message.
	 *
	 * @param messageId
	 * 		the ID of the message for which the read flag should be set
	 * @param read
	 * 		the boolean value to which the read flag should be set
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #setMessageRead(int)
	 * @see #setMessageReadFlag(Message, boolean)
	 * @see #deleteOfflineMessage(int)
	 */
	public boolean setMessageReadFlag(int messageId, boolean read) {
		final CMessageUpdateFlag flag = new CMessageUpdateFlag(messageId, read);
		return query.doCommand(flag);
	}

	/**
	 * Sets the read flag for a given message. This will not delete the message.
	 *
	 * @param message
	 * 		the message for which the read flag should be set
	 * @param read
	 * 		the boolean value to which the read flag should be set
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #setMessageRead(Message)
	 * @see #setMessageReadFlag(int, boolean)
	 * @see #deleteOfflineMessage(int)
	 */
	public boolean setMessageReadFlag(Message message, boolean read) {
		return setMessageReadFlag(message.getId(), read);
	}

	/**
	 * Sets the nickname of the server query client.
	 * The nickname must be between 3 and 30 UTF-8 bytes long and BB codes will be ignored.
	 *
	 * @param nickname
	 * 		the new nickname, may not contain any BB codes and may not be {@code null}
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #updateClient(Map)
	 */
	public boolean setNickname(String nickname) {
		final Map<ClientProperty, String> options = Collections.singletonMap(ClientProperty.CLIENT_NICKNAME, nickname);
		return updateClient(options);
	}

	/**
	 * Starts the virtual server with the specified ID.
	 *
	 * @param serverId
	 * 		the ID of the virtual server
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 */
	public boolean startServer(int serverId) {
		final CServerStart start = new CServerStart(serverId);
		return query.doCommand(start);
	}

	/**
	 * Starts the specified virtual server.
	 *
	 * @param virtualServer
	 * 		the virtual server to start
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 */
	public boolean startServer(VirtualServer virtualServer) {
		return startServer(virtualServer.getId());
	}

	/**
	 * Stops the virtual server with the specified ID.
	 *
	 * @param serverId
	 * 		the ID of the virtual server
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 */
	public boolean stopServer(int serverId) {
		final CServerStop stop = new CServerStop(serverId);
		return query.doCommand(stop);
	}

	/**
	 * Stops the specified virtual server.
	 *
	 * @param virtualServer
	 * 		the virtual server to stop
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 */
	public boolean stopServer(VirtualServer virtualServer) {
		return stopServer(virtualServer.getId());
	}

	/**
	 * Stops the entire TeamSpeak 3 Server instance by shutting down the process.
	 * <p>
	 * To have permission to use this command, you need to use the server query admin login.
	 * </p>
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 */
	public boolean stopServerProcess() {
		final CServerProcessStop stop = new CServerProcessStop();
		return query.doCommand(stop);
	}

	/**
	 * Unregisters the server query from receiving any event notifications.
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 */
	public boolean unregisterAllEvents() {
		final CServerNotifyUnregister unr = new CServerNotifyUnregister();
		return query.doCommand(unr);
	}

	/**
	 * Updates several client properties for this server query instance.
	 *
	 * @param options
	 * 		the map of properties to update
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see #editClient(int, Map)
	 */
	public boolean updateClient(Map<ClientProperty, String> options) {
		final CClientUpdate update = new CClientUpdate(options);
		return query.doCommand(update);
	}

	/**
	 * Generates new login credentials for the currently connected server query instance, using the given name.
	 * <p>
	 * <b>This will remove the current login credentials!</b> You won't be logged out, but after disconnecting,
	 * the old credentials will no longer work. Make sure to not lock yourselves out!
	 * </p>
	 *
	 * @param loginName
	 * 		the name for the server query login
	 *
	 * @return the generated password for the server query login
	 *
	 * @querycommands 1
	 */
	public String updateServerQueryLogin(String loginName) {
		final CClientSetServerQueryLogin login = new CClientSetServerQueryLogin(loginName);
		if (query.doCommand(login)) {
			return login.getFirstResponse().get("client_login_password");
		}
		return null;
	}

	/**
	 * Uploads a file to the file repository at a given path and channel
	 * by reading {@code dataLength} bytes from an open {@link InputStream}.
	 * <p>
	 * It is the user's responsibility to ensure that the given {@code InputStream} is
	 * open and that {@code dataLength} bytes can eventually be read from it. The user is
	 * also responsible for closing the stream once the upload has finished.
	 * </p><p>
	 * Note that this method will not read the entire file to memory and can thus
	 * upload arbitrarily sized files to the file repository.
	 * </p>
	 *
	 * @param dataIn
	 * 		a stream that contains the data that should be uploaded
	 * @param dataLength
	 * 		how many bytes should be read from the stream
	 * @param filePath
	 * 		the path the file should have after being uploaded
	 * @param overwrite
	 * 		if {@code false}, fails if there's already a file at {@code filePath}
	 * @param channelId
	 * 		the ID of the channel to upload the file to
	 *
	 * @return whether the command succeeded or not
	 *
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #uploadFileDirect(byte[], String, boolean, int, String)
	 */
	public boolean uploadFile(InputStream dataIn, long dataLength, String filePath, boolean overwrite, int channelId) {
		return uploadFile(dataIn, dataLength, filePath, overwrite, channelId, null);
	}

	/**
	 * Uploads a file to the file repository at a given path and channel
	 * by reading {@code dataLength} bytes from an open {@link InputStream}.
	 * <p>
	 * It is the user's responsibility to ensure that the given {@code InputStream} is
	 * open and that {@code dataLength} bytes can eventually be read from it. The user is
	 * also responsible for closing the stream once the upload has finished.
	 * </p><p>
	 * Note that this method will not read the entire file to memory and can thus
	 * upload arbitrarily sized files to the file repository.
	 * </p>
	 *
	 * @param dataIn
	 * 		a stream that contains the data that should be uploaded
	 * @param dataLength
	 * 		how many bytes should be read from the stream
	 * @param filePath
	 * 		the path the file should have after being uploaded
	 * @param overwrite
	 * 		if {@code false}, fails if there's already a file at {@code filePath}
	 * @param channelId
	 * 		the ID of the channel to upload the file to
	 * @param channelPassword
	 * 		that channel's password
	 *
	 * @return whether the command succeeded or not
	 *
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #uploadFileDirect(byte[], String, boolean, int, String)
	 */
	public boolean uploadFile(InputStream dataIn, long dataLength, String filePath, boolean overwrite, int channelId, String channelPassword) {
		final FileTransferHelper helper = query.getFileTransferHelper();
		final int transferId = helper.getClientTransferId();
		final CFtInitUpload upload = new CFtInitUpload(transferId, filePath, channelId, channelPassword, dataLength, overwrite);

		if (!query.doCommand(upload)) return false;
		FileTransferParameters params = new FileTransferParameters(upload.getFirstResponse().getMap());
		QueryError error = params.getQueryError();
		if (!error.isSuccessful()) return false;

		try {
			query.getFileTransferHelper().uploadFile(dataIn, dataLength, params);
		} catch (IOException e) {
			throw new TS3FileTransferFailedException("Upload failed", e);
		}
		return true;
	}

	/**
	 * Uploads a file that is already stored in memory to the file repository
	 * at a given path and channel.
	 *
	 * @param data
	 * 		the file's data as a byte array
	 * @param filePath
	 * 		the path the file should have after being uploaded
	 * @param overwrite
	 * 		if {@code false}, fails if there's already a file at {@code filePath}
	 * @param channelId
	 * 		the ID of the channel to upload the file to
	 *
	 * @return whether the command succeeded or not
	 *
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #uploadFile(InputStream, long, String, boolean, int)
	 */
	public boolean uploadFileDirect(byte[] data, String filePath, boolean overwrite, int channelId) {
		return uploadFileDirect(data, filePath, overwrite, channelId, null);
	}

	/**
	 * Uploads a file that is already stored in memory to the file repository
	 * at a given path and channel.
	 *
	 * @param data
	 * 		the file's data as a byte array
	 * @param filePath
	 * 		the path the file should have after being uploaded
	 * @param overwrite
	 * 		if {@code false}, fails if there's already a file at {@code filePath}
	 * @param channelId
	 * 		the ID of the channel to upload the file to
	 * @param channelPassword
	 * 		that channel's password
	 *
	 * @return whether the command succeeded or not
	 *
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #uploadFile(InputStream, long, String, boolean, int, String)
	 */
	public boolean uploadFileDirect(byte[] data, String filePath, boolean overwrite, int channelId, String channelPassword) {
		return uploadFile(new ByteArrayInputStream(data), data.length, filePath, overwrite, channelId, channelPassword);
	}

	/**
	 * Uploads an icon to the icon directory in the file repository
	 * by reading {@code dataLength} bytes from an open {@link InputStream}.
	 * <p>
	 * It is the user's responsibility to ensure that the given {@code InputStream} is
	 * open and that {@code dataLength} bytes can eventually be read from it. The user is
	 * also responsible for closing the stream once the upload has finished.
	 * </p><p>
	 * Note that unlike the file upload methods, this <strong>will read the entire file to memory</strong>.
	 * This is because the CRC32 hash must be calculated before the icon can be uploaded.
	 * That means that all icon files must be less than 2<sup>31</sup>-1 bytes in size.
	 * </p>
	 * Uploads  that is already stored in memory to the icon directory
	 * in the file repository. If this icon has already been uploaded or
	 * if a hash collision occurs (CRC32), this command will fail.
	 *
	 * @param dataIn
	 * 		a stream that contains the data that should be uploaded
	 * @param dataLength
	 * 		how many bytes should be read from the stream
	 *
	 * @return the ID of the uploaded icon
	 *
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see IconFile#getIconId()
	 * @see #uploadIconDirect(byte[])
	 * @see #downloadIcon(OutputStream, long)
	 */
	public long uploadIcon(InputStream dataIn, long dataLength) {
		final FileTransferHelper helper = query.getFileTransferHelper();
		final byte[] data;
		try {
			data = helper.readFully(dataIn, dataLength);
		} catch (IOException e) {
			throw new TS3FileTransferFailedException("Reading stream failed", e);
		}
		return uploadIconDirect(data);
	}

	/**
	 * Uploads an icon that is already stored in memory to the icon directory
	 * in the file repository. If this icon has already been uploaded or
	 * if a CRC32 hash collision occurs, this command will fail.
	 *
	 * @param data
	 * 		the icon's data as a byte array
	 *
	 * @return the ID of the uploaded icon
	 *
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see IconFile#getIconId()
	 * @see #uploadIcon(InputStream, long)
	 * @see #downloadIconDirect(long)
	 */
	public long uploadIconDirect(byte[] data) {
		final FileTransferHelper helper = query.getFileTransferHelper();
		final long iconId;
		iconId = helper.getIconId(data);

		final String path = "/icon_" + iconId;
		if (uploadFileDirect(data, path, false, 0)) {
			return iconId;
		}
		return -1L;
	}

	/**
	 * Uses an existing privilege key to join a server or channel group.
	 *
	 * @param token
	 * 		the privilege key to use
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see PrivilegeKey
	 * @see #addPrivilegeKey(PrivilegeKeyType, int, int, String)
	 * @see #usePrivilegeKey(PrivilegeKey)
	 */
	public boolean usePrivilegeKey(String token) {
		final CPrivilegeKeyUse use = new CPrivilegeKeyUse(token);
		return query.doCommand(use);
	}

	/**
	 * Uses an existing privilege key to join a server or channel group.
	 *
	 * @param privilegeKey
	 * 		the privilege key to use
	 *
	 * @return whether the command succeeded or not
	 *
	 * @querycommands 1
	 * @see PrivilegeKey
	 * @see #addPrivilegeKey(PrivilegeKeyType, int, int, String)
	 * @see #usePrivilegeKey(String)
	 */
	public boolean usePrivilegeKey(PrivilegeKey privilegeKey) {
		return usePrivilegeKey(privilegeKey.getToken());
	}

	/**
	 * Gets information about the current server query instance.
	 *
	 * @return information about the server query instance
	 *
	 * @querycommands 1
	 * @see #getClientInfo(int)
	 */
	public ServerQueryInfo whoAmI() {
		final CWhoAmI whoAmI = new CWhoAmI();
		if (query.doCommand(whoAmI)) {
			return new ServerQueryInfo(whoAmI.getFirstResponse().getMap());
		}
		return null;
	}
}
