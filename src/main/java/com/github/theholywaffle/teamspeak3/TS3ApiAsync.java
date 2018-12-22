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
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3Exception;
import com.github.theholywaffle.teamspeak3.api.exception.TS3FileTransferFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.*;
import com.github.theholywaffle.teamspeak3.commands.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Asynchronous version of {@link TS3Api} to interact with the {@link TS3Query}.
 * <p>
 * This class is used to easily interact with a {@link TS3Query}. It constructs commands,
 * sends them to the TeamSpeak3 server, processes the response and returns the result.
 * </p><p>
 * All methods in this class are asynchronous (so they won't block) and
 * will return a {@link CommandFuture} of the corresponding return type in {@link TS3Api}.
 * If a command fails, no exception will be thrown directly. It will however be rethrown in
 * {@link CommandFuture#get()} and {@link CommandFuture#get(long, TimeUnit)}.
 * Usually, the thrown exception is a {@link TS3CommandFailedException}, which will get you
 * access to the {@link QueryError} from which more information about the error can be obtained.
 * </p><p>
 * Also note that while these methods are asynchronous, the commands will still be sent through a
 * synchronous command pipeline. That means if an asynchronous method is called immediately
 * followed by a synchronous method, the synchronous method will first have to wait until the
 * asynchronous method completed until it its command is sent.
 * </p><p>
 * You won't be able to execute most commands while you're not logged in due to missing permissions.
 * Make sure to either pass your login credentials to the {@link TS3Config} object when
 * creating the {@code TS3Query} or to call {@link #login(String, String)} to log in.
 * </p><p>
 * After that, most commands also require you to select a {@linkplain VirtualServer virtual server}.
 * To do so, call either {@link #selectVirtualServerByPort(int)} or {@link #selectVirtualServerById(int)}.
 * </p>
 *
 * @see TS3Api The synchronous version of the API
 */
public class TS3ApiAsync {

	/**
	 * The TS3 query that holds the event manager and the file transfer helper.
	 */
	private final TS3Query query;

	/**
	 * The queue that this TS3ApiAsync sends its commands to.
	 */
	private final CommandQueue commandQueue;

	/**
	 * Creates a new asynchronous API object for the given {@code TS3Query}.
	 * <p>
	 * <b>Usually, this constructor should not be called.</b> Use {@link TS3Query#getAsyncApi()} instead.
	 * </p>
	 *
	 * @param query
	 * 		the TS3Query to use
	 * @param commandQueue
	 * 		the queue to send commands to
	 */
	public TS3ApiAsync(TS3Query query, CommandQueue commandQueue) {
		this.query = query;
		this.commandQueue = commandQueue;
	}

	/**
	 * Adds a new ban entry. At least one of the parameters {@code ip}, {@code name} or {@code uid} needs to be non-null.
	 * Returns the ID of the newly created ban entry.
	 *
	 * @param ip
	 * 		a RegEx pattern to match a client's IP against, can be {@code null}
	 * @param name
	 * 		a RegEx pattern to match a client's name against, can be {@code null}
	 * @param uid
	 * 		the unique identifier of a client, can be {@code null}
	 * @param timeInSeconds
	 * 		the duration of the ban in seconds. 0 equals a permanent ban
	 * @param reason
	 * 		the reason for the ban, can be {@code null}
	 *
	 * @return the ID of the newly created ban entry
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Pattern RegEx Pattern
	 * @see #addBan(String, String, String, String, long, String)
	 * @see Client#getId()
	 * @see Client#getUniqueIdentifier()
	 * @see ClientInfo#getIp()
	 */
	public CommandFuture<Integer> addBan(String ip, String name, String uid, long timeInSeconds, String reason) {
		return addBan(ip, name, uid, null, timeInSeconds, reason);
	}

	/**
	 * Adds a new ban entry. At least one of the parameters {@code ip}, {@code name}, {@code uid}, or
	 * {@code myTSId} needs to be non-null. Returns the ID of the newly created ban entry.
	 * <p>
	 * Note that creating a ban entry for the {@code "empty"} "myTeamSpeak" ID will ban all clients who
	 * don't have a linked "myTeamSpeak" account.
	 * </p>
	 *
	 * @param ip
	 * 		a RegEx pattern to match a client's IP against, can be {@code null}
	 * @param name
	 * 		a RegEx pattern to match a client's name against, can be {@code null}
	 * @param uid
	 * 		the unique identifier of a client, can be {@code null}
	 * @param myTSId
	 * 		the "myTeamSpeak" ID of a client, the string {@code "empty"}, or {@code null}
	 * @param timeInSeconds
	 * 		the duration of the ban in seconds. 0 equals a permanent ban
	 * @param reason
	 * 		the reason for the ban, can be {@code null}
	 *
	 * @return the ID of the newly created ban entry
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Pattern RegEx Pattern
	 * @see Client#getId()
	 * @see Client#getUniqueIdentifier()
	 * @see ClientInfo#getIp()
	 */
	public CommandFuture<Integer> addBan(String ip, String name, String uid, String myTSId, long timeInSeconds, String reason) {
		Command cmd = BanCommands.banAdd(ip, name, uid, myTSId, timeInSeconds, reason);
		return executeAndReturnIntProperty(cmd, "banid");
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see Client#getDatabaseId()
	 * @see Permission
	 */
	public CommandFuture<Void> addChannelClientPermission(int channelId, int clientDBId, String permName, int permValue) {
		Command cmd = PermissionCommands.channelClientAddPerm(channelId, clientDBId, permName, permValue);
		return executeAndReturnError(cmd);
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup
	 */
	public CommandFuture<Integer> addChannelGroup(String name) {
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup
	 */
	public CommandFuture<Integer> addChannelGroup(String name, PermissionGroupDatabaseType type) {
		Command cmd = ChannelGroupCommands.channelGroupAdd(name, type);
		return executeAndReturnIntProperty(cmd, "cgid");
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 * @see Permission
	 */
	public CommandFuture<Void> addChannelGroupPermission(int groupId, String permName, int permValue) {
		Command cmd = PermissionCommands.channelGroupAddPerm(groupId, permName, permValue);
		return executeAndReturnError(cmd);
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see Permission
	 */
	public CommandFuture<Void> addChannelPermission(int channelId, String permName, int permValue) {
		Command cmd = PermissionCommands.channelAddPerm(channelId, permName, permValue);
		return executeAndReturnError(cmd);
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see Permission
	 */
	public CommandFuture<Void> addClientPermission(int clientDBId, String permName, int value, boolean skipped) {
		Command cmd = PermissionCommands.clientAddPerm(clientDBId, permName, value, skipped);
		return executeAndReturnError(cmd);
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 * @see Client#getDatabaseId()
	 */
	public CommandFuture<Void> addClientToServerGroup(int groupId, int clientDatabaseId) {
		Command cmd = ServerGroupCommands.serverGroupAddClient(groupId, clientDatabaseId);
		return executeAndReturnError(cmd);
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see Complaint#getMessage()
	 */
	public CommandFuture<Void> addComplaint(int clientDBId, String message) {
		Command cmd = ComplaintCommands.complainAdd(clientDBId, message);
		return executeAndReturnError(cmd);
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroupType
	 * @see Permission
	 */
	public CommandFuture<Void> addPermissionToAllServerGroups(ServerGroupType type, String permName, int value, boolean negated, boolean skipped) {
		Command cmd = PermissionCommands.serverGroupAutoAddPerm(type, permName, value, negated, skipped);
		return executeAndReturnError(cmd);
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see PrivilegeKeyType
	 * @see #addPrivilegeKeyServerGroup(int, String)
	 * @see #addPrivilegeKeyChannelGroup(int, int, String)
	 */
	public CommandFuture<String> addPrivilegeKey(PrivilegeKeyType type, int groupId, int channelId, String description) {
		Command cmd = PrivilegeKeyCommands.privilegeKeyAdd(type, groupId, channelId, description);
		return executeAndReturnStringProperty(cmd, "token");
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 * @see Channel#getId()
	 * @see #addPrivilegeKey(PrivilegeKeyType, int, int, String)
	 * @see #addPrivilegeKeyServerGroup(int, String)
	 */
	public CommandFuture<String> addPrivilegeKeyChannelGroup(int channelGroupId, int channelId, String description) {
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 * @see #addPrivilegeKey(PrivilegeKeyType, int, int, String)
	 * @see #addPrivilegeKeyChannelGroup(int, int, String)
	 */
	public CommandFuture<String> addPrivilegeKeyServerGroup(int serverGroupId, String description) {
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroup
	 */
	public CommandFuture<Integer> addServerGroup(String name) {
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroup
	 * @see PermissionGroupDatabaseType
	 */
	public CommandFuture<Integer> addServerGroup(String name, PermissionGroupDatabaseType type) {
		Command cmd = ServerGroupCommands.serverGroupAdd(name, type);
		return executeAndReturnIntProperty(cmd, "sgid");
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 * @see Permission
	 */
	public CommandFuture<Void> addServerGroupPermission(int groupId, String permName, int value, boolean negated, boolean skipped) {
		Command cmd = PermissionCommands.serverGroupAddPerm(groupId, permName, value, negated, skipped);
		return executeAndReturnError(cmd);
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
	 * Please note that this will create 2 or 3 separate ban rules,
	 * one for the targeted client's IP address, one for their unique identifier,
	 * and potentially one more for their "myTeamSpeak" ID.
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
	 * @return an array containing the IDs of the created ban entries
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #addBan(String, String, String, long, String)
	 */
	public CommandFuture<int[]> banClient(int clientId, long timeInSeconds) {
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #addBan(String, String, String, long, String)
	 */
	public CommandFuture<int[]> banClient(int clientId, long timeInSeconds, String reason) {
		Command cmd = BanCommands.banClient(clientId, timeInSeconds, reason);
		return executeAndReturnIntArray(cmd, "banid");
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #addBan(String, String, String, long, String)
	 */
	public CommandFuture<int[]> banClient(int clientId, String reason) {
		return banClient(clientId, 0, reason);
	}

	/**
	 * Sends a text message to all clients on all virtual servers.
	 * These messages will appear to clients in the tab for server messages.
	 *
	 * @param message
	 * 		the message to be sent
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Void> broadcast(String message) {
		Command cmd = ServerCommands.gm(message);
		return executeAndReturnError(cmd);
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 */
	public CommandFuture<Void> copyChannelGroup(int sourceGroupId, int targetGroupId, PermissionGroupDatabaseType type) {
		if (targetGroupId <= 0) {
			throw new IllegalArgumentException("To create a new channel group, use the method with a String argument");
		}

		Command cmd = ChannelGroupCommands.channelGroupCopy(sourceGroupId, targetGroupId, type);
		return executeAndReturnError(cmd);
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 */
	public CommandFuture<Integer> copyChannelGroup(int sourceGroupId, String targetName, PermissionGroupDatabaseType type) {
		Command cmd = ChannelGroupCommands.channelGroupCopy(sourceGroupId, targetName, type);
		return executeAndReturnIntProperty(cmd, "cgid");
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
	 * @return the ID of the newly created server group
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 */
	public CommandFuture<Integer> copyServerGroup(int sourceGroupId, int targetGroupId, PermissionGroupDatabaseType type) {
		if (targetGroupId <= 0) {
			throw new IllegalArgumentException("To create a new server group, use the method with a String argument");
		}

		Command cmd = ServerGroupCommands.serverGroupCopy(sourceGroupId, targetGroupId, type);
		return executeAndReturnIntProperty(cmd, "sgid");
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 */
	public CommandFuture<Integer> copyServerGroup(int sourceGroupId, String targetName, PermissionGroupDatabaseType type) {
		Command cmd = ServerGroupCommands.serverGroupCopy(sourceGroupId, targetName, type);
		return executeAndReturnIntProperty(cmd, "sgid");
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel
	 */
	public CommandFuture<Integer> createChannel(String name, Map<ChannelProperty, String> options) {
		Command cmd = ChannelCommands.channelCreate(name, options);
		return executeAndReturnIntProperty(cmd, "cid");
	}

	/**
	 * Creates a new directory on the file repository in the specified channel.
	 *
	 * @param directoryPath
	 * 		the path to the directory that should be created
	 * @param channelId
	 * 		the ID of the channel the directory should be created in
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public CommandFuture<Void> createFileDirectory(String directoryPath, int channelId) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public CommandFuture<Void> createFileDirectory(String directoryPath, int channelId, String channelPassword) {
		Command cmd = FileCommands.ftCreateDir(directoryPath, channelId, channelPassword);
		return executeAndReturnError(cmd);
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see VirtualServer
	 */
	public CommandFuture<CreatedVirtualServer> createServer(String name, Map<VirtualServerProperty, String> options) {
		Command cmd = VirtualServerCommands.serverCreate(name, options);
		return executeAndTransformFirst(cmd, CreatedVirtualServer::new);
	}

	/**
	 * Creates a {@link Snapshot} of the selected virtual server containing all settings,
	 * groups and known client identities. The data from a server snapshot can be
	 * used to restore a virtual servers configuration.
	 *
	 * @return a snapshot of the virtual server
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #deployServerSnapshot(Snapshot)
	 */
	public CommandFuture<Snapshot> createServerSnapshot() {
		Command cmd = VirtualServerCommands.serverSnapshotCreate();
		CommandFuture<Snapshot> future = cmd.getFuture()
				.map(result -> new Snapshot(result.getRawResponse()));

		commandQueue.enqueueCommand(cmd);
		return future;
	}

	/**
	 * Deletes all active ban rules from the server. Use with caution.
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Void> deleteAllBans() {
		Command cmd = BanCommands.banDelAll();
		return executeAndReturnError(cmd);
	}

	/**
	 * Deletes all complaints about the client with specified database ID from the server.
	 *
	 * @param clientDBId
	 * 		the database ID of the client
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see Complaint
	 */
	public CommandFuture<Void> deleteAllComplaints(int clientDBId) {
		Command cmd = ComplaintCommands.complainDelAll(clientDBId);
		return executeAndReturnError(cmd);
	}

	/**
	 * Deletes the ban rule with the specified ID from the server.
	 *
	 * @param banId
	 * 		the ID of the ban to delete
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Ban#getId()
	 */
	public CommandFuture<Void> deleteBan(int banId) {
		Command cmd = BanCommands.banDel(banId);
		return executeAndReturnError(cmd);
	}

	/**
	 * Deletes an existing channel specified by its ID, kicking all clients out of the channel.
	 *
	 * @param channelId
	 * 		the ID of the channel to delete
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see #deleteChannel(int, boolean)
	 * @see #kickClientFromChannel(String, int...)
	 */
	public CommandFuture<Void> deleteChannel(int channelId) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see #kickClientFromChannel(String, int...)
	 */
	public CommandFuture<Void> deleteChannel(int channelId, boolean force) {
		Command cmd = ChannelCommands.channelDelete(channelId, force);
		return executeAndReturnError(cmd);
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see Client#getDatabaseId()
	 * @see Permission#getName()
	 */
	public CommandFuture<Void> deleteChannelClientPermission(int channelId, int clientDBId, String permName) {
		Command cmd = PermissionCommands.channelClientDelPerm(channelId, clientDBId, permName);
		return executeAndReturnError(cmd);
	}

	/**
	 * Removes the channel group with the given ID.
	 *
	 * @param groupId
	 * 		the ID of the channel group
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 */
	public CommandFuture<Void> deleteChannelGroup(int groupId) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 */
	public CommandFuture<Void> deleteChannelGroup(int groupId, boolean force) {
		Command cmd = ChannelGroupCommands.channelGroupDel(groupId, force);
		return executeAndReturnError(cmd);
	}

	/**
	 * Removes a permission from the channel group with the given ID.
	 *
	 * @param groupId
	 * 		the ID of the channel group
	 * @param permName
	 * 		the name of the permission to revoke
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 * @see Permission#getName()
	 */
	public CommandFuture<Void> deleteChannelGroupPermission(int groupId, String permName) {
		Command cmd = PermissionCommands.channelGroupDelPerm(groupId, permName);
		return executeAndReturnError(cmd);
	}

	/**
	 * Removes a permission from the channel with the given ID.
	 *
	 * @param channelId
	 * 		the ID of the channel
	 * @param permName
	 * 		the name of the permission to revoke
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see Permission#getName()
	 */
	public CommandFuture<Void> deleteChannelPermission(int channelId, String permName) {
		Command cmd = PermissionCommands.channelDelPerm(channelId, permName);
		return executeAndReturnError(cmd);
	}

	/**
	 * Removes a permission from a client.
	 *
	 * @param clientDBId
	 * 		the database ID of the client
	 * @param permName
	 * 		the name of the permission to revoke
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see Permission#getName()
	 */
	public CommandFuture<Void> deleteClientPermission(int clientDBId, String permName) {
		Command cmd = PermissionCommands.clientDelPerm(clientDBId, permName);
		return executeAndReturnError(cmd);
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Complaint
	 * @see Client#getDatabaseId()
	 */
	public CommandFuture<Void> deleteComplaint(int targetClientDBId, int fromClientDBId) {
		Command cmd = ComplaintCommands.complainDel(targetClientDBId, fromClientDBId);
		return executeAndReturnError(cmd);
	}

	/**
	 * Removes the {@code key} custom client property from a client.
	 *
	 * @param clientDBId
	 * 		the database ID of the target client
	 * @param key
	 * 		the key of the custom property to delete, cannot be {@code null}
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 */
	public CommandFuture<Void> deleteCustomClientProperty(int clientDBId, String key) {
		if (key == null) throw new IllegalArgumentException("Key cannot be null");

		Command cmd = CustomPropertyCommands.customDelete(clientDBId, key);
		return executeAndReturnError(cmd);
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see #getDatabaseClientInfo(int)
	 * @see DatabaseClientInfo
	 */
	public CommandFuture<Void> deleteDatabaseClientProperties(int clientDBId) {
		Command cmd = DatabaseClientCommands.clientDBDelete(clientDBId);
		return executeAndReturnError(cmd);
	}

	/**
	 * Deletes a file or directory from the file repository in the specified channel.
	 *
	 * @param filePath
	 * 		the path to the file or directory
	 * @param channelId
	 * 		the ID of the channel the file or directory resides in
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public CommandFuture<Void> deleteFile(String filePath, int channelId) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public CommandFuture<Void> deleteFile(String filePath, int channelId, String channelPassword) {
		Command cmd = FileCommands.ftDeleteFile(channelId, channelPassword, filePath);
		return executeAndReturnError(cmd);
	}

	/**
	 * Deletes multiple files or directories from the file repository in the specified channel.
	 *
	 * @param filePaths
	 * 		the paths to the files or directories
	 * @param channelId
	 * 		the ID of the channel the file or directory resides in
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public CommandFuture<Void> deleteFiles(String[] filePaths, int channelId) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public CommandFuture<Void> deleteFiles(String[] filePaths, int channelId, String channelPassword) {
		Command cmd = FileCommands.ftDeleteFile(channelId, channelPassword, filePaths);
		return executeAndReturnError(cmd);
	}

	/**
	 * Deletes an icon from the icon directory in the file repository.
	 *
	 * @param iconId
	 * 		the ID of the icon to delete
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see IconFile#getIconId()
	 */
	public CommandFuture<Void> deleteIcon(long iconId) {
		String iconPath = "/icon_" + iconId;
		return deleteFile(iconPath, 0);
	}

	/**
	 * Deletes multiple icons from the icon directory in the file repository.
	 *
	 * @param iconIds
	 * 		the IDs of the icons to delete
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see IconFile#getIconId()
	 */
	public CommandFuture<Void> deleteIcons(long... iconIds) {
		String[] iconPaths = new String[iconIds.length];
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Message#getId()
	 */
	public CommandFuture<Void> deleteOfflineMessage(int messageId) {
		Command cmd = MessageCommands.messageDel(messageId);
		return executeAndReturnError(cmd);
	}

	/**
	 * Removes a specified permission from all server groups of the type specified by {@code type} on all virtual servers.
	 *
	 * @param type
	 * 		the kind of server group this permission should be removed from
	 * @param permName
	 * 		the name of the permission to remove
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroupType
	 * @see Permission#getName()
	 */
	public CommandFuture<Void> deletePermissionFromAllServerGroups(ServerGroupType type, String permName) {
		Command cmd = PermissionCommands.serverGroupAutoDelPerm(type, permName);
		return executeAndReturnError(cmd);
	}

	/**
	 * Deletes the privilege key with the given token.
	 *
	 * @param token
	 * 		the token of the privilege key
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see PrivilegeKey
	 */
	public CommandFuture<Void> deletePrivilegeKey(String token) {
		Command cmd = PrivilegeKeyCommands.privilegeKeyDelete(token);
		return executeAndReturnError(cmd);
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see VirtualServer#getId()
	 * @see #stopServer(int)
	 */
	public CommandFuture<Void> deleteServer(int serverId) {
		Command cmd = VirtualServerCommands.serverDelete(serverId);
		return executeAndReturnError(cmd);
	}

	/**
	 * Deletes the server group with the specified ID, even if the server group still contains clients.
	 *
	 * @param groupId
	 * 		the ID of the server group
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 */
	public CommandFuture<Void> deleteServerGroup(int groupId) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 */
	public CommandFuture<Void> deleteServerGroup(int groupId, boolean force) {
		Command cmd = ServerGroupCommands.serverGroupDel(groupId, force);
		return executeAndReturnError(cmd);
	}

	/**
	 * Removes a permission from the server group with the given ID.
	 *
	 * @param groupId
	 * 		the ID of the server group
	 * @param permName
	 * 		the name of the permission to revoke
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 * @see Permission#getName()
	 */
	public CommandFuture<Void> deleteServerGroupPermission(int groupId, String permName) {
		Command cmd = PermissionCommands.serverGroupDelPerm(groupId, permName);
		return executeAndReturnError(cmd);
	}

	/**
	 * Restores the selected virtual servers configuration using the data from a
	 * previously created server snapshot.
	 *
	 * @param snapshot
	 * 		the snapshot to restore
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #createServerSnapshot()
	 */
	public CommandFuture<Void> deployServerSnapshot(Snapshot snapshot) {
		return deployServerSnapshot(snapshot.get());
	}

	/**
	 * Restores the configuration of the selected virtual server using the data from a
	 * previously created server snapshot.
	 *
	 * @param snapshot
	 * 		the snapshot to restore
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #createServerSnapshot()
	 */
	public CommandFuture<Void> deployServerSnapshot(String snapshot) {
		Command cmd = VirtualServerCommands.serverSnapshotDeploy(snapshot);
		return executeAndReturnError(cmd);
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #downloadFileDirect(String, int)
	 */
	public CommandFuture<Long> downloadFile(OutputStream dataOut, String filePath, int channelId) {
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #downloadFileDirect(String, int, String)
	 */
	public CommandFuture<Long> downloadFile(OutputStream dataOut, String filePath, int channelId, String channelPassword) {
		FileTransferHelper helper = query.getFileTransferHelper();
		int transferId = helper.getClientTransferId();
		Command cmd = FileCommands.ftInitDownload(transferId, filePath, channelId, channelPassword);
		CommandFuture<Long> future = new CommandFuture<>();

		executeAndTransformFirst(cmd, FileTransferParameters::new).onSuccess(params -> {
			QueryError error = params.getQueryError();
			if (!error.isSuccessful()) {
				future.fail(new TS3CommandFailedException(error, cmd.getName()));
				return;
			}

			try {
				query.getFileTransferHelper().downloadFile(dataOut, params);
			} catch (IOException e) {
				future.fail(new TS3FileTransferFailedException("Download failed", e));
				return;
			}
			future.set(params.getFileSize());
		}).forwardFailure(future);

		return future;
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #downloadFile(OutputStream, String, int)
	 */
	public CommandFuture<byte[]> downloadFileDirect(String filePath, int channelId) {
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #downloadFile(OutputStream, String, int, String)
	 */
	public CommandFuture<byte[]> downloadFileDirect(String filePath, int channelId, String channelPassword) {
		FileTransferHelper helper = query.getFileTransferHelper();
		int transferId = helper.getClientTransferId();
		Command cmd = FileCommands.ftInitDownload(transferId, filePath, channelId, channelPassword);
		CommandFuture<byte[]> future = new CommandFuture<>();

		executeAndTransformFirst(cmd, FileTransferParameters::new).onSuccess(params -> {
			QueryError error = params.getQueryError();
			if (!error.isSuccessful()) {
				future.fail(new TS3CommandFailedException(error, cmd.getName()));
				return;
			}

			long fileSize = params.getFileSize();
			if (fileSize > Integer.MAX_VALUE) {
				future.fail(new TS3FileTransferFailedException("File too big for byte array"));
				return;
			}
			ByteArrayOutputStream dataOut = new ByteArrayOutputStream((int) fileSize);

			try {
				query.getFileTransferHelper().downloadFile(dataOut, params);
			} catch (IOException e) {
				future.fail(new TS3FileTransferFailedException("Download failed", e));
				return;
			}
			future.set(dataOut.toByteArray());
		}).forwardFailure(future);

		return future;
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see IconFile#getIconId()
	 * @see #downloadIconDirect(long)
	 * @see #uploadIcon(InputStream, long)
	 */
	public CommandFuture<Long> downloadIcon(OutputStream dataOut, long iconId) {
		String iconPath = "/icon_" + iconId;
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see IconFile#getIconId()
	 * @see #downloadIcon(OutputStream, long)
	 * @see #uploadIconDirect(byte[])
	 */
	public CommandFuture<byte[]> downloadIconDirect(long iconId) {
		String iconPath = "/icon_" + iconId;
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 */
	public CommandFuture<Void> editChannel(int channelId, Map<ChannelProperty, String> options) {
		Command cmd = ChannelCommands.channelEdit(channelId, options);
		return executeAndReturnError(cmd);
	}

	/**
	 * Changes a single property of the given channel.
	 * <p>
	 * Note that one can set many properties at once with the overloaded method that
	 * takes a map of channel properties and strings.
	 * </p>
	 *
	 * @param channelId
	 * 		the ID of the channel to edit
	 * @param property
	 * 		the channel property to modify, make sure it is editable
	 * @param value
	 * 		the new value of the property
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see #editChannel(int, Map)
	 */
	public CommandFuture<Void> editChannel(int channelId, ChannelProperty property, String value) {
		return editChannel(channelId, Collections.singletonMap(property, value));
	}

	/**
	 * Changes a client's configuration using given properties.
	 * <p>
	 * Only {@link ClientProperty#CLIENT_DESCRIPTION} can be changed for other clients.
	 * To update the current client's properties, use {@link #updateClient(Map)}
	 * or {@link #updateClient(ClientProperty, String)}.
	 * </p>
	 *
	 * @param clientId
	 * 		the ID of the client to edit
	 * @param options
	 * 		the map of properties to modify
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #updateClient(Map)
	 */
	public CommandFuture<Void> editClient(int clientId, Map<ClientProperty, String> options) {
		Command cmd = ClientCommands.clientEdit(clientId, options);
		return executeAndReturnError(cmd);
	}

	/**
	 * Changes a single property of the given client.
	 * <p>
	 * Only {@link ClientProperty#CLIENT_DESCRIPTION} can be changed for other clients.
	 * To update the current client's properties, use {@link #updateClient(Map)}
	 * or {@link #updateClient(ClientProperty, String)}.
	 * </p>
	 *
	 * @param clientId
	 * 		the ID of the client to edit
	 * @param property
	 * 		the client property to modify, make sure it is editable
	 * @param value
	 * 		the new value of the property
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #editClient(int, Map)
	 * @see #updateClient(Map)
	 */
	public CommandFuture<Void> editClient(int clientId, ClientProperty property, String value) {
		return editClient(clientId, Collections.singletonMap(property, value));
	}

	/**
	 * Changes a client's database settings using given properties.
	 *
	 * @param clientDBId
	 * 		the database ID of the client to edit
	 * @param options
	 * 		the map of properties to modify
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see DatabaseClientInfo
	 * @see Client#getDatabaseId()
	 */
	public CommandFuture<Void> editDatabaseClient(int clientDBId, Map<ClientProperty, String> options) {
		Command cmd = DatabaseClientCommands.clientDBEdit(clientDBId, options);
		return executeAndReturnError(cmd);
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
	 * @return a future to track the progress of this command
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code property} is not changeable
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerInstanceProperty#isChangeable()
	 */
	public CommandFuture<Void> editInstance(ServerInstanceProperty property, String value) {
		Command cmd = ServerCommands.instanceEdit(Collections.singletonMap(property, value));
		return executeAndReturnError(cmd);
	}

	/**
	 * Changes the configuration of the selected virtual server using given properties.
	 *
	 * @param options
	 * 		the map of properties to edit
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see VirtualServerProperty
	 */
	public CommandFuture<Void> editServer(Map<VirtualServerProperty, String> options) {
		Command cmd = VirtualServerCommands.serverEdit(options);
		return executeAndReturnError(cmd);
	}

	/**
	 * Gets a list of all bans on the selected virtual server.
	 *
	 * @return a list of all bans on the virtual server
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Ban
	 */
	public CommandFuture<List<Ban>> getBans() {
		Command cmd = BanCommands.banList();
		return executeAndTransform(cmd, Ban::new);
	}

	/**
	 * Gets a list of IP addresses used by the server instance.
	 *
	 * @return the list of bound IP addresses
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Binding
	 */
	public CommandFuture<List<Binding>> getBindings() {
		Command cmd = ServerCommands.bindingList();
		return executeAndTransform(cmd, Binding::new);
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel
	 * @see #getChannelsByName(String)
	 */
	public CommandFuture<Channel> getChannelByNameExact(String name, boolean ignoreCase) {
		String caseName = ignoreCase ? name.toLowerCase(Locale.ROOT) : name;

		return getChannels().map(allChannels -> {
			for (Channel c : allChannels) {
				String channelName = ignoreCase ? c.getName().toLowerCase(Locale.ROOT) : c.getName();
				if (caseName.equals(channelName)) return c;
			}
			return null; // Not found
		});
	}

	/**
	 * Gets a list of channels whose names contain the given search string.
	 *
	 * @param name
	 * 		the name to search
	 *
	 * @return a list of all channels with names matching the search pattern
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 2
	 * @see Channel
	 * @see #getChannelByNameExact(String, boolean)
	 */
	public CommandFuture<List<Channel>> getChannelsByName(String name) {
		Command cmd = ChannelCommands.channelFind(name);
		CommandFuture<List<Channel>> future = new CommandFuture<>();

		CommandFuture<List<Integer>> channelIds = executeAndMap(cmd, response -> response.getInt("cid"));
		CommandFuture<List<Channel>> allChannels = getChannels();

		findByKey(channelIds, allChannels, Channel::getId)
				.forwardSuccess(future)
				.onFailure(transformError(future, 768, Collections.emptyList()));

		return future;
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see Client#getDatabaseId()
	 * @see Permission
	 */
	public CommandFuture<List<Permission>> getChannelClientPermissions(int channelId, int clientDBId) {
		Command cmd = PermissionCommands.channelClientPermList(channelId, clientDBId);
		return executeAndTransform(cmd, Permission::new);
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see Client#getDatabaseId()
	 * @see ChannelGroup#getId()
	 * @see ChannelGroupClient
	 */
	public CommandFuture<List<ChannelGroupClient>> getChannelGroupClients(int channelId, int clientDBId, int groupId) {
		Command cmd = ChannelGroupCommands.channelGroupClientList(channelId, clientDBId, groupId);
		return executeAndTransform(cmd, ChannelGroupClient::new);
	}

	/**
	 * Gets all client / channel ID combinations currently assigned to the specified channel group.
	 *
	 * @param groupId
	 * 		the ID of the channel group whose client / channel assignments should be returned.
	 *
	 * @return a list of combinations of channel ID, client database ID and channel group ID
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 * @see ChannelGroupClient
	 * @see #getChannelGroupClients(int, int, int)
	 */
	public CommandFuture<List<ChannelGroupClient>> getChannelGroupClientsByChannelGroupId(int groupId) {
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see ChannelGroupClient
	 * @see #getChannelGroupClients(int, int, int)
	 */
	public CommandFuture<List<ChannelGroupClient>> getChannelGroupClientsByChannelId(int channelId) {
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see ChannelGroupClient
	 * @see #getChannelGroupClients(int, int, int)
	 */
	public CommandFuture<List<ChannelGroupClient>> getChannelGroupClientsByClientDBId(int clientDBId) {
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 * @see Permission
	 */
	public CommandFuture<List<Permission>> getChannelGroupPermissions(int groupId) {
		Command cmd = PermissionCommands.channelGroupPermList(groupId);
		return executeAndTransform(cmd, Permission::new);
	}

	/**
	 * Gets a list of all channel groups on the selected virtual server.
	 *
	 * @return a list of all channel groups on the virtual server
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup
	 */
	public CommandFuture<List<ChannelGroup>> getChannelGroups() {
		Command cmd = ChannelGroupCommands.channelGroupList();
		return executeAndTransform(cmd, ChannelGroup::new);
	}

	/**
	 * Gets detailed configuration information about the channel specified channel.
	 *
	 * @param channelId
	 * 		the ID of the channel
	 *
	 * @return information about the channel
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see ChannelInfo
	 */
	public CommandFuture<ChannelInfo> getChannelInfo(int channelId) {
		Command cmd = ChannelCommands.channelInfo(channelId);
		return executeAndTransformFirst(cmd, map -> new ChannelInfo(channelId, map));
	}

	/**
	 * Gets a list of all permissions assigned to the specified channel.
	 *
	 * @param channelId
	 * 		the ID of the channel
	 *
	 * @return a list of all permissions assigned to the channel
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see Permission
	 */
	public CommandFuture<List<Permission>> getChannelPermissions(int channelId) {
		Command cmd = PermissionCommands.channelPermList(channelId);
		return executeAndTransform(cmd, Permission::new);
	}

	/**
	 * Gets a list of all channels on the selected virtual server.
	 *
	 * @return a list of all channels on the virtual server
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel
	 */
	public CommandFuture<List<Channel>> getChannels() {
		Command cmd = ChannelCommands.channelList();
		return executeAndTransform(cmd, Channel::new);
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client
	 * @see #getClientsByName(String)
	 */
	public CommandFuture<Client> getClientByNameExact(String name, boolean ignoreCase) {
		String caseName = ignoreCase ? name.toLowerCase(Locale.ROOT) : name;

		return getClients().map(allClients -> {
			for (Client c : allClients) {
				String clientName = ignoreCase ? c.getNickname().toLowerCase(Locale.ROOT) : c.getNickname();
				if (caseName.equals(clientName)) return c;
			}
			return null; // Not found
		});
	}

	/**
	 * Gets a list of clients whose nicknames contain the given search string.
	 *
	 * @param name
	 * 		the name to search
	 *
	 * @return a list of all clients with nicknames matching the search pattern
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 2
	 * @see Client
	 * @see #getClientByNameExact(String, boolean)
	 */
	public CommandFuture<List<Client>> getClientsByName(String name) {
		Command cmd = ClientCommands.clientFind(name);
		CommandFuture<List<Client>> future = new CommandFuture<>();

		CommandFuture<List<Integer>> clientIds = executeAndMap(cmd, response -> response.getInt("clid"));
		CommandFuture<List<Client>> allClients = getClients();

		findByKey(clientIds, allClients, Client::getId)
				.forwardSuccess(future)
				.onFailure(transformError(future, 512, Collections.emptyList()));

		return future;
	}

	/**
	 * Gets information about the client with the specified unique identifier.
	 *
	 * @param clientUId
	 * 		the unique identifier of the client
	 *
	 * @return information about the client
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 2
	 * @see Client#getUniqueIdentifier()
	 * @see ClientInfo
	 */
	public CommandFuture<ClientInfo> getClientByUId(String clientUId) {
		Command cmd = ClientCommands.clientGetIds(clientUId);
		return executeAndReturnIntProperty(cmd, "clid")
				.then(this::getClientInfo);
	}

	/**
	 * Gets information about the client with the specified client ID.
	 *
	 * @param clientId
	 * 		the client ID of the client
	 *
	 * @return information about the client
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see ClientInfo
	 */
	public CommandFuture<ClientInfo> getClientInfo(int clientId) {
		Command cmd = ClientCommands.clientInfo(clientId);
		return executeAndTransformFirst(cmd, map -> new ClientInfo(clientId, map));
	}

	/**
	 * Gets a list of all permissions assigned to the specified client.
	 *
	 * @param clientDBId
	 * 		the database ID of the client
	 *
	 * @return a list of all permissions assigned to the client
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see Permission
	 */
	public CommandFuture<List<Permission>> getClientPermissions(int clientDBId) {
		Command cmd = PermissionCommands.clientPermList(clientDBId);
		return executeAndTransform(cmd, Permission::new);
	}

	/**
	 * Gets a list of all clients on the selected virtual server.
	 *
	 * @return a list of all clients on the virtual server
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client
	 */
	public CommandFuture<List<Client>> getClients() {
		Command cmd = ClientCommands.clientList();
		return executeAndTransform(cmd, Client::new);
	}

	/**
	 * Gets a list of all complaints on the selected virtual server.
	 *
	 * @return a list of all complaints on the virtual server
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Complaint
	 * @see #getComplaints(int)
	 */
	public CommandFuture<List<Complaint>> getComplaints() {
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see Complaint
	 */
	public CommandFuture<List<Complaint>> getComplaints(int clientDBId) {
		Command cmd = ComplaintCommands.complainList(clientDBId);
		return executeAndTransform(cmd, Complaint::new);
	}

	/**
	 * Gets detailed connection information about the selected virtual server.
	 *
	 * @return connection information about the selected virtual server
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ConnectionInfo
	 * @see #getServerInfo()
	 */
	public CommandFuture<ConnectionInfo> getConnectionInfo() {
		Command cmd = VirtualServerCommands.serverRequestConnectionInfo();
		return executeAndTransformFirst(cmd, ConnectionInfo::new);
	}

	/**
	 * Gets a map of all custom client properties and their values
	 * assigned to the client with database ID {@code clientDBId}.
	 *
	 * @param clientDBId
	 * 		the database ID of the target client
	 *
	 * @return a map of the client's custom client property assignments
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see #searchCustomClientProperty(String)
	 * @see #searchCustomClientProperty(String, String)
	 */
	public CommandFuture<Map<String, String>> getCustomClientProperties(int clientDBId) {
		Command cmd = CustomPropertyCommands.customInfo(clientDBId);
		CommandFuture<Map<String, String>> future = cmd.getFuture()
				.map(result -> {
					List<Wrapper> response = result.getResponses();
					Map<String, String> properties = new HashMap<>(response.size());
					for (Wrapper wrapper : response) {
						properties.put(wrapper.get("ident"), wrapper.get("value"));
					}

					return properties;
				});

		commandQueue.enqueueCommand(cmd);
		return future;
	}

	/**
	 * Gets all clients in the database whose last nickname matches the specified name <b>exactly</b>.
	 *
	 * @param name
	 * 		the nickname for the clients to match
	 *
	 * @return a list of all clients with a matching nickname
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1 + n,
	 * where n is the amount of database clients with a matching nickname
	 * @see Client#getNickname()
	 */
	public CommandFuture<List<DatabaseClientInfo>> getDatabaseClientsByName(String name) {
		Command cmd = DatabaseClientCommands.clientDBFind(name, false);

		return executeAndMap(cmd, response -> response.getInt("cldbid"))
				.then(dbClientIds -> {
					Collection<CommandFuture<DatabaseClientInfo>> infoFutures = new ArrayList<>(dbClientIds.size());
					for (int dbClientId : dbClientIds) {
						infoFutures.add(getDatabaseClientInfo(dbClientId));
					}
					return CommandFuture.ofAll(infoFutures);
				});
	}

	/**
	 * Gets information about the client with the specified unique identifier in the server database.
	 *
	 * @param clientUId
	 * 		the unique identifier of the client
	 *
	 * @return the database client or {@code null} if no client was found
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 2
	 * @see Client#getUniqueIdentifier()
	 * @see DatabaseClientInfo
	 */
	public CommandFuture<DatabaseClientInfo> getDatabaseClientByUId(String clientUId) {
		Command cmd = DatabaseClientCommands.clientDBFind(clientUId, true);
		CommandFuture<DatabaseClientInfo> future = cmd.getFuture()
				.then(result -> {
					if (result.getResponses().isEmpty()) {
						return null;
					} else {
						int databaseId = result.getFirstResponse().getInt("cldbid");
						return getDatabaseClientInfo(databaseId);
					}
				});

		commandQueue.enqueueCommand(cmd);
		return future;
	}

	/**
	 * Gets information about the client with the specified database ID in the server database.
	 *
	 * @param clientDBId
	 * 		the database ID of the client
	 *
	 * @return the database client or {@code null} if no client was found
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see DatabaseClientInfo
	 */
	public CommandFuture<DatabaseClientInfo> getDatabaseClientInfo(int clientDBId) {
		Command cmd = DatabaseClientCommands.clientDBInfo(clientDBId);
		return executeAndTransformFirst(cmd, DatabaseClientInfo::new);
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1 + n,
	 * where n = Math.ceil([amount of database clients] / 200)
	 * @see DatabaseClient
	 */
	public CommandFuture<List<DatabaseClient>> getDatabaseClients() {
		Command cmd = DatabaseClientCommands.clientDBList(0, 1, true);

		return executeAndReturnIntProperty(cmd, "count")
				.then(count -> {
					Collection<CommandFuture<List<DatabaseClient>>> futures = new ArrayList<>((count + 199) / 200);
					for (int i = 0; i < count; i += 200) {
						futures.add(getDatabaseClients(i, 200));
					}
					return CommandFuture.ofAll(futures);
				}).map(listOfLists -> listOfLists.stream()
						.flatMap(List::stream)
						.collect(Collectors.toList()));
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see DatabaseClient
	 */
	public CommandFuture<List<DatabaseClient>> getDatabaseClients(int offset, int count) {
		Command cmd = DatabaseClientCommands.clientDBList(offset, count, false);
		return executeAndTransform(cmd, DatabaseClient::new);
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public CommandFuture<FileInfo> getFileInfo(String filePath, int channelId) {
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public CommandFuture<FileInfo> getFileInfo(String filePath, int channelId, String channelPassword) {
		Command cmd = FileCommands.ftGetFileInfo(channelId, channelPassword, filePath);
		return executeAndTransformFirst(cmd, FileInfo::new);
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public CommandFuture<List<FileInfo>> getFileInfos(String[] filePaths, int channelId) {
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public CommandFuture<List<FileInfo>> getFileInfos(String[] filePaths, int channelId, String channelPassword) {
		Command cmd = FileCommands.ftGetFileInfo(channelId, channelPassword, filePaths);
		return executeAndTransform(cmd, FileInfo::new);
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public CommandFuture<List<FileInfo>> getFileInfos(String[] filePaths, int[] channelIds, String[] channelPasswords) {
		Command cmd = FileCommands.ftGetFileInfo(channelIds, channelPasswords, filePaths);
		return executeAndTransform(cmd, FileInfo::new);
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public CommandFuture<List<FileListEntry>> getFileList(String directoryPath, int channelId) {
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public CommandFuture<List<FileListEntry>> getFileList(String directoryPath, int channelId, String channelPassword) {
		Command cmd = FileCommands.ftGetFileList(directoryPath, channelId, channelPassword);
		return executeAndTransform(cmd, FileListEntry::new);
	}

	/**
	 * Gets a list of active or recently active file transfers.
	 *
	 * @return a list of file transfers
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<List<FileTransfer>> getFileTransfers() {
		Command cmd = FileCommands.ftList();
		return executeAndTransform(cmd, FileTransfer::new);
	}

	/**
	 * Displays detailed configuration information about the server instance including
	 * uptime, number of virtual servers online, traffic information, etc.
	 *
	 * @return information about the host
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<HostInfo> getHostInfo() {
		Command cmd = ServerCommands.hostInfo();
		return executeAndTransformFirst(cmd, HostInfo::new);
	}

	/**
	 * Gets a list of all icon files on this virtual server.
	 *
	 * @return a list of all icons
	 */
	public CommandFuture<List<IconFile>> getIconList() {
		return getFileList("/icons/", 0)
				.map(result -> {
					List<IconFile> icons = new ArrayList<>(result.size());
					for (FileListEntry file : result) {
						if (file.isDirectory() || file.isStillUploading()) continue;
						icons.add(new IconFile(file.getMap()));
					}
					return icons;
				});
	}

	/**
	 * Displays the server instance configuration including database revision number,
	 * the file transfer port, default group IDs, etc.
	 *
	 * @return information about the TeamSpeak server instance.
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<InstanceInfo> getInstanceInfo() {
		Command cmd = ServerCommands.instanceInfo();
		return executeAndTransformFirst(cmd, InstanceInfo::new);
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<List<String>> getInstanceLogEntries(int lines) {
		Command cmd = ServerCommands.logView(lines, true);
		return executeAndMap(cmd, response -> response.get("l"));
	}

	/**
	 * Fetches the last 100 log entries from the server log.
	 *
	 * @return a list of up to 100 log entries
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<List<String>> getInstanceLogEntries() {
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Message#getId()
	 * @see #setMessageRead(int)
	 */
	public CommandFuture<String> getOfflineMessage(int messageId) {
		Command cmd = MessageCommands.messageGet(messageId);
		return executeAndReturnStringProperty(cmd, "message");
	}

	/**
	 * Reads the message body of a message. This will not set the read flag, though.
	 *
	 * @param message
	 * 		the message to be read
	 *
	 * @return the body of the message with the specified ID or {@code null} if there was no message with that ID
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Message#getId()
	 * @see #setMessageRead(Message)
	 */
	public CommandFuture<String> getOfflineMessage(Message message) {
		return getOfflineMessage(message.getId());
	}

	/**
	 * Gets a list of all offline messages for the server query.
	 * The returned messages lack their message body, though.
	 * To read the actual message, use {@link #getOfflineMessage(int)} or {@link #getOfflineMessage(Message)}.
	 *
	 * @return a list of all offline messages this server query has received
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<List<Message>> getOfflineMessages() {
		Command cmd = MessageCommands.messageList();
		return executeAndTransform(cmd, Message::new);
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #getPermissionOverview(int, int)
	 */
	public CommandFuture<List<PermissionAssignment>> getPermissionAssignments(String permName) {
		Command cmd = PermissionCommands.permFind(permName);
		CommandFuture<List<PermissionAssignment>> future = new CommandFuture<>();

		executeAndTransform(cmd, PermissionAssignment::new)
				.forwardSuccess(future)
				.onFailure(transformError(future, 2562, Collections.emptyList()));

		return future;
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Integer> getPermissionIdByName(String permName) {
		Command cmd = PermissionCommands.permIdGetByName(permName);
		return executeAndReturnIntProperty(cmd, "permid");
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<int[]> getPermissionIdsByName(String... permNames) {
		Command cmd = PermissionCommands.permIdGetByName(permNames);
		return executeAndReturnIntArray(cmd, "permid");
	}

	/**
	 * Gets a list of all assigned permissions for a client in a specified channel.
	 * If you do not care about channel permissions, set {@code channelId} to {@code 0}.
	 *
	 * @param channelId
	 * 		the ID of the channel
	 * @param clientDBId
	 * 		the database ID of the client to create the overview for
	 *
	 * @return a list of all permission assignments for the client in the specified channel
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see Client#getDatabaseId()
	 */
	public CommandFuture<List<PermissionAssignment>> getPermissionOverview(int channelId, int clientDBId) {
		Command cmd = PermissionCommands.permOverview(channelId, clientDBId);
		return executeAndTransform(cmd, PermissionAssignment::new);
	}

	/**
	 * Displays a list of all permissions, including ID, name and description.
	 *
	 * @return a list of all permissions
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<List<PermissionInfo>> getPermissions() {
		Command cmd = PermissionCommands.permissionList();
		return executeAndTransform(cmd, PermissionInfo::new);
	}

	/**
	 * Displays the current value of the specified permission for this server query instance.
	 *
	 * @param permName
	 * 		the name of the permission
	 *
	 * @return the permission value, usually ranging from 0 to 100
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Integer> getPermissionValue(String permName) {
		Command cmd = PermissionCommands.permGet(permName);
		return executeAndReturnIntProperty(cmd, "permvalue");
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<int[]> getPermissionValues(String... permNames) {
		Command cmd = PermissionCommands.permGet(permNames);
		return executeAndReturnIntArray(cmd, "permvalue");
	}

	/**
	 * Gets a list of all available tokens to join channel or server groups,
	 * including their type and group IDs.
	 *
	 * @return a list of all generated, but still unclaimed privilege keys
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #addPrivilegeKey(PrivilegeKeyType, int, int, String)
	 * @see #usePrivilegeKey(String)
	 */
	public CommandFuture<List<PrivilegeKey>> getPrivilegeKeys() {
		Command cmd = PrivilegeKeyCommands.privilegeKeyList();
		return executeAndTransform(cmd, PrivilegeKey::new);
	}

	/**
	 * Gets a list of all clients in the specified server group.
	 *
	 * @param serverGroupId
	 * 		the ID of the server group for which the clients should be looked up
	 *
	 * @return a list of all clients in the server group
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<List<ServerGroupClient>> getServerGroupClients(int serverGroupId) {
		Command cmd = ServerGroupCommands.serverGroupClientList(serverGroupId);
		return executeAndTransform(cmd, ServerGroupClient::new);
	}

	/**
	 * Gets a list of all clients in the specified server group.
	 *
	 * @param serverGroup
	 * 		the server group for which the clients should be looked up
	 *
	 * @return a list of all clients in the server group
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<List<ServerGroupClient>> getServerGroupClients(ServerGroup serverGroup) {
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 * @see #getServerGroupPermissions(ServerGroup)
	 */
	public CommandFuture<List<Permission>> getServerGroupPermissions(int serverGroupId) {
		Command cmd = PermissionCommands.serverGroupPermList(serverGroupId);
		return executeAndTransform(cmd, Permission::new);
	}

	/**
	 * Gets a list of all permissions assigned to the specified server group.
	 *
	 * @param serverGroup
	 * 		the server group for which the permissions should be looked up
	 *
	 * @return a list of all permissions assigned to the server group
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<List<Permission>> getServerGroupPermissions(ServerGroup serverGroup) {
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<List<ServerGroup>> getServerGroups() {
		Command cmd = ServerGroupCommands.serverGroupList();
		return executeAndTransform(cmd, ServerGroup::new);
	}

	/**
	 * Gets a list of all server groups set for a client.
	 *
	 * @param clientDatabaseId
	 * 		the database ID of the client for which the server groups should be looked up
	 *
	 * @return a list of all server groups set for the client
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 2
	 * @see Client#getDatabaseId()
	 * @see #getServerGroupsByClient(Client)
	 */
	public CommandFuture<List<ServerGroup>> getServerGroupsByClientId(int clientDatabaseId) {
		Command cmd = ServerGroupCommands.serverGroupsByClientId(clientDatabaseId);

		CommandFuture<List<Integer>> serverGroupIds = executeAndMap(cmd, response -> response.getInt("sgid"));
		CommandFuture<List<ServerGroup>> allServerGroups = getServerGroups();

		return findByKey(serverGroupIds, allServerGroups, ServerGroup::getId);
	}

	/**
	 * Gets a list of all server groups set for a client.
	 *
	 * @param client
	 * 		the client for which the server groups should be looked up
	 *
	 * @return a list of all server group set for the client
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 2
	 * @see #getServerGroupsByClientId(int)
	 */
	public CommandFuture<List<ServerGroup>> getServerGroupsByClient(Client client) {
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see VirtualServer#getPort()
	 * @see VirtualServer#getId()
	 */
	public CommandFuture<Integer> getServerIdByPort(int port) {
		Command cmd = VirtualServerCommands.serverIdGetByPort(port);
		return executeAndReturnIntProperty(cmd, "server_id");
	}

	/**
	 * Gets detailed information about the virtual server the server query is currently in.
	 *
	 * @return information about the current virtual server
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<VirtualServerInfo> getServerInfo() {
		Command cmd = VirtualServerCommands.serverInfo();
		return executeAndTransformFirst(cmd, VirtualServerInfo::new);
	}

	/**
	 * Gets the version, build number and platform of the TeamSpeak3 server.
	 *
	 * @return the version information of the server
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Version> getVersion() {
		Command cmd = ServerCommands.version();
		return executeAndTransformFirst(cmd, Version::new);
	}

	/**
	 * Gets a list of all virtual servers including their ID, status, number of clients online, etc.
	 *
	 * @return a list of all virtual servers
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<List<VirtualServer>> getVirtualServers() {
		Command cmd = VirtualServerCommands.serverList();
		return executeAndTransform(cmd, VirtualServer::new);
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<List<String>> getVirtualServerLogEntries(int lines) {
		Command cmd = ServerCommands.logView(lines, false);
		return executeAndMap(cmd, response -> response.get("l"));
	}

	/**
	 * Fetches the last 100 log entries from the currently selected virtual server.
	 * If no virtual server is selected, the entries will be read from the server log instead.
	 *
	 * @return a list of up to 100 log entries
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<List<String>> getVirtualServerLogEntries() {
		return getVirtualServerLogEntries(100);
	}

	/**
	 * Checks whether the client with the specified client ID is online.
	 * <p>
	 * Please note that there is no guarantee that the client will still be
	 * online by the time the next command is executed.
	 * </p>
	 *
	 * @param clientId
	 * 		the ID of the client
	 *
	 * @return {@code true} if the client is online, {@code false} otherwise
	 *
	 * @querycommands 1
	 * @see #getClientInfo(int)
	 */
	public CommandFuture<Boolean> isClientOnline(int clientId) {
		Command cmd = ClientCommands.clientInfo(clientId);
		CommandFuture<Boolean> future = new CommandFuture<>();

		cmd.getFuture()
				.onSuccess(__ -> future.set(true))
				.onFailure(transformError(future, 512, false));

		commandQueue.enqueueCommand(cmd);
		return future;
	}

	/**
	 * Checks whether the client with the specified unique identifier is online.
	 * <p>
	 * Please note that there is no guarantee that the client will still be
	 * online by the time the next command is executed.
	 * </p>
	 *
	 * @param clientUId
	 * 		the unique ID of the client
	 *
	 * @return {@code true} if the client is online, {@code false} otherwise
	 *
	 * @querycommands 1
	 * @see #getClientByUId(String)
	 */
	public CommandFuture<Boolean> isClientOnline(String clientUId) {
		Command cmd = ClientCommands.clientGetIds(clientUId);
		CommandFuture<Boolean> future = cmd.getFuture()
				.map(result -> !result.getResponses().isEmpty());

		commandQueue.enqueueCommand(cmd);
		return future;
	}

	/**
	 * Kicks one or more clients from their current channels.
	 * This will move the kicked clients into the default channel and
	 * won't do anything if the clients are already in the default channel.
	 *
	 * @param clientIds
	 * 		the IDs of the clients to kick
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #kickClientFromChannel(Client...)
	 * @see #kickClientFromChannel(String, int...)
	 */
	public CommandFuture<Void> kickClientFromChannel(int... clientIds) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #kickClientFromChannel(int...)
	 * @see #kickClientFromChannel(String, Client...)
	 */
	public CommandFuture<Void> kickClientFromChannel(Client... clients) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #kickClientFromChannel(int...)
	 * @see #kickClientFromChannel(String, Client...)
	 */
	public CommandFuture<Void> kickClientFromChannel(String message, int... clientIds) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #kickClientFromChannel(Client...)
	 * @see #kickClientFromChannel(String, int...)
	 */
	public CommandFuture<Void> kickClientFromChannel(String message, Client... clients) {
		return kickClients(ReasonIdentifier.REASON_KICK_CHANNEL, message, clients);
	}

	/**
	 * Kicks one or more clients from the server.
	 *
	 * @param clientIds
	 * 		the IDs of the clients to kick
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #kickClientFromServer(Client...)
	 * @see #kickClientFromServer(String, int...)
	 */
	public CommandFuture<Void> kickClientFromServer(int... clientIds) {
		return kickClients(ReasonIdentifier.REASON_KICK_SERVER, null, clientIds);
	}

	/**
	 * Kicks one or more clients from the server.
	 *
	 * @param clients
	 * 		the clients to kick
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #kickClientFromServer(int...)
	 * @see #kickClientFromServer(String, Client...)
	 */
	public CommandFuture<Void> kickClientFromServer(Client... clients) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #kickClientFromServer(int...)
	 * @see #kickClientFromServer(String, Client...)
	 */
	public CommandFuture<Void> kickClientFromServer(String message, int... clientIds) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #kickClientFromServer(Client...)
	 * @see #kickClientFromServer(String, int...)
	 */
	public CommandFuture<Void> kickClientFromServer(String message, Client... clients) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	private CommandFuture<Void> kickClients(ReasonIdentifier reason, String message, Client... clients) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 */
	private CommandFuture<Void> kickClients(ReasonIdentifier reason, String message, int... clientIds) {
		Command cmd = ClientCommands.clientKick(reason, message, clientIds);
		return executeAndReturnError(cmd);
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #logout()
	 */
	public CommandFuture<Void> login(String username, String password) {
		Command cmd = QueryCommands.logIn(username, password);
		return executeAndReturnError(cmd);
	}

	/**
	 * Logs the server query out and deselects the current virtual server.
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #login(String, String)
	 */
	public CommandFuture<Void> logout() {
		Command cmd = QueryCommands.logOut();
		return executeAndReturnError(cmd);
	}

	/**
	 * Moves a channel to a new parent channel specified by its ID.
	 * To move a channel to root level, set {@code channelTargetId} to {@code 0}.
	 * <p>
	 * This will move the channel right below the specified parent channel, above all other child channels.
	 * This command will fail if the channel already has the specified target channel as the parent channel.
	 * </p>
	 *
	 * @param channelId
	 * 		the channel to move
	 * @param channelTargetId
	 * 		the new parent channel for the specified channel
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see #moveChannel(int, int, int)
	 */
	public CommandFuture<Void> moveChannel(int channelId, int channelTargetId) {
		return moveChannel(channelId, channelTargetId, 0);
	}

	/**
	 * Moves a channel to a new parent channel specified by its ID.
	 * To move a channel to root level, set {@code channelTargetId} to {@code 0}.
	 * <p>
	 * The channel will be ordered below the channel with the ID specified by {@code order}.
	 * To move the channel right below the parent channel, set {@code order} to {@code 0}.
	 * </p><p>
	 * Note that you can't re-order a channel without also changing its parent channel with this method.
	 * Use {@link #editChannel(int, ChannelProperty, String)} to change {@link ChannelProperty#CHANNEL_ORDER} instead.
	 * </p>
	 *
	 * @param channelId
	 * 		the channel to move
	 * @param channelTargetId
	 * 		the new parent channel for the specified channel
	 * @param order
	 * 		the channel to sort the specified channel below
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see #moveChannel(int, int)
	 */
	public CommandFuture<Void> moveChannel(int channelId, int channelTargetId, int order) {
		Command cmd = ChannelCommands.channelMove(channelId, channelTargetId, order);
		return executeAndReturnError(cmd);
	}

	/**
	 * Moves a single client into a channel.
	 * <p>
	 * Consider using {@link #moveClients(int[], int)} to move multiple clients.
	 * </p>
	 *
	 * @param clientId
	 * 		the ID of the client to move
	 * @param channelId
	 * 		the ID of the channel to move the client into
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see Channel#getId()
	 */
	public CommandFuture<Void> moveClient(int clientId, int channelId) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code clientIds} is {@code null}
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see Channel#getId()
	 */
	public CommandFuture<Void> moveClients(int[] clientIds, int channelId) {
		return moveClients(clientIds, channelId, null);
	}

	/**
	 * Moves a single client into a channel.
	 * <p>
	 * Consider using {@link #moveClients(Client[], ChannelBase)} to move multiple clients.
	 * </p>
	 *
	 * @param client
	 * 		the client to move, cannot be {@code null}
	 * @param channel
	 * 		the channel to move the client into, cannot be {@code null}
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code client} or {@code channel} is {@code null}
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Void> moveClient(Client client, ChannelBase channel) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code clients} or {@code channel} is {@code null}
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Void> moveClients(Client[] clients, ChannelBase channel) {
		return moveClients(clients, channel, null);
	}

	/**
	 * Moves a single client into a channel using the specified password.
	 * <p>
	 * Consider using {@link #moveClients(int[], int, String)} to move multiple clients.
	 * </p>
	 *
	 * @param clientId
	 * 		the ID of the client to move
	 * @param channelId
	 * 		the ID of the channel to move the client into
	 * @param channelPassword
	 * 		the password of the channel, can be {@code null}
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see Channel#getId()
	 */
	public CommandFuture<Void> moveClient(int clientId, int channelId, String channelPassword) {
		Command cmd = ClientCommands.clientMove(clientId, channelId, channelPassword);
		return executeAndReturnError(cmd);
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
	 * @return a future to track the progress of this command
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code clientIds} is {@code null}
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see Channel#getId()
	 */
	public CommandFuture<Void> moveClients(int[] clientIds, int channelId, String channelPassword) {
		if (clientIds == null) throw new IllegalArgumentException("Client ID array was null");
		if (clientIds.length == 0) return CommandFuture.immediate(null); // Success

		Command cmd = ClientCommands.clientMove(clientIds, channelId, channelPassword);
		return executeAndReturnError(cmd);
	}

	/**
	 * Moves a single client into a channel using the specified password.
	 * <p>
	 * Consider using {@link #moveClients(Client[], ChannelBase, String)} to move multiple clients.
	 * </p>
	 *
	 * @param client
	 * 		the client to move, cannot be {@code null}
	 * @param channel
	 * 		the channel to move the client into, cannot be {@code null}
	 * @param channelPassword
	 * 		the password of the channel, can be {@code null}
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code client} or {@code channel} is {@code null}
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Void> moveClient(Client client, ChannelBase channel, String channelPassword) {
		if (client == null) throw new IllegalArgumentException("Client cannot be null");
		if (channel == null) throw new IllegalArgumentException("Channel cannot be null");

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
	 * @return a future to track the progress of this command
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code clients} or {@code channel} is {@code null}
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Void> moveClients(Client[] clients, ChannelBase channel, String channelPassword) {
		if (clients == null) throw new IllegalArgumentException("Client array cannot be null");
		if (channel == null) throw new IllegalArgumentException("Channel cannot be null");

		int[] clientIds = new int[clients.length];
		for (int i = 0; i < clients.length; i++) {
			clientIds[i] = clients[i].getId();
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #moveFile(String, String, int, int) moveFile to a different channel
	 */
	public CommandFuture<Void> moveFile(String oldPath, String newPath, int channelId) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #moveFile(String, String, int) moveFile within the same channel
	 */
	public CommandFuture<Void> moveFile(String oldPath, String newPath, int oldChannelId, int newChannelId) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #moveFile(String, String, int, String, int, String) moveFile to a different channel
	 */
	public CommandFuture<Void> moveFile(String oldPath, String newPath, int channelId, String channelPassword) {
		Command cmd = FileCommands.ftRenameFile(oldPath, newPath, channelId, channelPassword);
		return executeAndReturnError(cmd);
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #moveFile(String, String, int, String) moveFile within the same channel
	 */
	public CommandFuture<Void> moveFile(String oldPath, String newPath, int oldChannelId, String oldPassword, int newChannelId, String newPassword) {
		Command cmd = FileCommands.ftRenameFile(oldPath, newPath, oldChannelId, oldPassword, newChannelId, newPassword);
		return executeAndReturnError(cmd);
	}

	/**
	 * Moves the server query into a channel.
	 *
	 * @param channelId
	 * 		the ID of the channel to move the server query into
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 */
	public CommandFuture<Void> moveQuery(int channelId) {
		return moveClient(0, channelId, null);
	}

	/**
	 * Moves the server query into a channel.
	 *
	 * @param channel
	 * 		the channel to move the server query into, cannot be {@code null}
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code channel} is {@code null}
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Void> moveQuery(ChannelBase channel) {
		if (channel == null) throw new IllegalArgumentException("Channel cannot be null");

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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 */
	public CommandFuture<Void> moveQuery(int channelId, String channelPassword) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code channel} is {@code null}
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Void> moveQuery(ChannelBase channel, String channelPassword) {
		if (channel == null) throw new IllegalArgumentException("Channel cannot be null");

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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 */
	public CommandFuture<Void> pokeClient(int clientId, String message) {
		Command cmd = ClientCommands.clientPoke(clientId, message);
		return executeAndReturnError(cmd);
	}

	/**
	 * Terminates the connection with the TeamSpeak3 server.
	 * <p>
	 * This command should never be executed by a user of this API,
	 * as it leaves the query in an undefined state. To terminate
	 * a connection regularly, use {@link TS3Query#exit()}.
	 * </p>
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	CommandFuture<Void> quit() {
		Command cmd = QueryCommands.quit();
		return executeAndReturnError(cmd);
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
	 * <li>A client sends a private message to <b>the server query</b></li>
	 * <li>A client uses a privilege key</li>
	 * </ul>
	 * <p>
	 * The limitations to when the query receives notifications about chat events cannot be circumvented.
	 * </p>
	 * To be able to process these events in your application, register an event listener.
	 *
	 * @return whether all commands succeeded or not
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 6
	 * @see #addTS3Listeners(TS3Listener...)
	 */
	public CommandFuture<Void> registerAllEvents() {
		Collection<CommandFuture<Void>> eventFutures = Arrays.asList(
				registerEvent(TS3EventType.SERVER),
				registerEvent(TS3EventType.TEXT_SERVER),
				registerEvent(TS3EventType.CHANNEL, 0),
				registerEvent(TS3EventType.TEXT_CHANNEL, 0),
				registerEvent(TS3EventType.TEXT_PRIVATE),
				registerEvent(TS3EventType.PRIVILEGE_KEY_USED)
		);

		return CommandFuture.ofAll(eventFutures)
				.map(__ -> null); // Return success as Void, not List<Void>
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #addTS3Listeners(TS3Listener...)
	 * @see #registerEvent(TS3EventType, int)
	 * @see #registerAllEvents()
	 */
	public CommandFuture<Void> registerEvent(TS3EventType eventType) {
		if (eventType == TS3EventType.CHANNEL || eventType == TS3EventType.TEXT_CHANNEL) {
			return registerEvent(eventType, 0);
		} else {
			return registerEvent(eventType, -1);
		}
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see #addTS3Listeners(TS3Listener...)
	 * @see #registerAllEvents()
	 */
	public CommandFuture<Void> registerEvent(TS3EventType eventType, int channelId) {
		Command cmd = QueryCommands.serverNotifyRegister(eventType, channelId);
		return executeAndReturnError(cmd);
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands n, one command per TS3EventType
	 * @see #addTS3Listeners(TS3Listener...)
	 * @see #registerEvent(TS3EventType, int)
	 * @see #registerAllEvents()
	 */
	public CommandFuture<Void> registerEvents(TS3EventType... eventTypes) {
		if (eventTypes.length == 0) return CommandFuture.immediate(null); // Success

		Collection<CommandFuture<Void>> registerFutures = new ArrayList<>(eventTypes.length);
		for (TS3EventType type : eventTypes) {
			registerFutures.add(registerEvent(type));
		}

		return CommandFuture.ofAll(registerFutures)
				.map(__ -> null); // Return success as Void, not List<Void>
	}

	/**
	 * Removes the client specified by its database ID from the specified server group.
	 *
	 * @param serverGroupId
	 * 		the ID of the server group
	 * @param clientDatabaseId
	 * 		the database ID of the client
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 * @see Client#getDatabaseId()
	 * @see #removeClientFromServerGroup(ServerGroup, Client)
	 */
	public CommandFuture<Void> removeClientFromServerGroup(int serverGroupId, int clientDatabaseId) {
		Command cmd = ServerGroupCommands.serverGroupDelClient(serverGroupId, clientDatabaseId);
		return executeAndReturnError(cmd);
	}

	/**
	 * Removes the specified client from the specified server group.
	 *
	 * @param serverGroup
	 * 		the server group to remove the client from
	 * @param client
	 * 		the client to remove from the server group
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #removeClientFromServerGroup(int, int)
	 */
	public CommandFuture<Void> removeClientFromServerGroup(ServerGroup serverGroup, Client client) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 * @see #renameChannelGroup(ChannelGroup, String)
	 */
	public CommandFuture<Void> renameChannelGroup(int channelGroupId, String name) {
		Command cmd = ChannelGroupCommands.channelGroupRename(channelGroupId, name);
		return executeAndReturnError(cmd);
	}

	/**
	 * Renames the specified channel group.
	 *
	 * @param channelGroup
	 * 		the channel group to rename
	 * @param name
	 * 		the new name for the channel group
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #renameChannelGroup(int, String)
	 */
	public CommandFuture<Void> renameChannelGroup(ChannelGroup channelGroup, String name) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 * @see #renameServerGroup(ServerGroup, String)
	 */
	public CommandFuture<Void> renameServerGroup(int serverGroupId, String name) {
		Command cmd = ServerGroupCommands.serverGroupRename(serverGroupId, name);
		return executeAndReturnError(cmd);
	}

	/**
	 * Renames the specified server group.
	 *
	 * @param serverGroup
	 * 		the server group to rename
	 * @param name
	 * 		the new name for the server group
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #renameServerGroup(int, String)
	 */
	public CommandFuture<Void> renameServerGroup(ServerGroup serverGroup, String name) {
		return renameChannelGroup(serverGroup.getId(), name);
	}

	/**
	 * Resets all permissions and deletes all server / channel groups. Use carefully.
	 *
	 * @return a token for a new administrator account
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<String> resetPermissions() {
		Command cmd = PermissionCommands.permReset();
		return executeAndReturnStringProperty(cmd, "token");
	}

	/**
	 * Finds all clients that have any value associated with the {@code key} custom client property,
	 * and returns the client's database ID and the key and value of the matching custom property.
	 *
	 * @param key
	 * 		the key to search for, cannot be {@code null}
	 *
	 * @return a list of client database IDs and their matching custom client properties
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see #searchCustomClientProperty(String, String)
	 * @see #getCustomClientProperties(int)
	 */
	public CommandFuture<List<CustomPropertyAssignment>> searchCustomClientProperty(String key) {
		return searchCustomClientProperty(key, "%");
	}

	/**
	 * Finds all clients whose value associated with the {@code key} custom client property matches the
	 * SQL-like pattern {@code valuePattern}, and returns the client's database ID and the key and value
	 * of the matching custom property.
	 * <p>
	 * Patterns are case insensitive. They support the wildcard characters {@code %}, which matches any sequence of
	 * zero or more characters, and {@code _}, which matches exactly one arbitrary character.
	 * </p>
	 *
	 * @param key
	 * 		the key to search for, cannot be {@code null}
	 * @param valuePattern
	 * 		the pattern that values need to match to be included
	 *
	 * @return a list of client database IDs and their matching custom client properties
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see #searchCustomClientProperty(String)
	 * @see #getCustomClientProperties(int)
	 */
	public CommandFuture<List<CustomPropertyAssignment>> searchCustomClientProperty(String key, String valuePattern) {
		if (key == null) throw new IllegalArgumentException("Key cannot be null");

		Command cmd = CustomPropertyCommands.customSearch(key, valuePattern);
		return executeAndTransform(cmd, CustomPropertyAssignment::new);
	}

	/**
	 * Moves the server query into the virtual server with the specified ID.
	 *
	 * @param id
	 * 		the ID of the virtual server
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see VirtualServer#getId()
	 * @see #selectVirtualServerById(int, String)
	 * @see #selectVirtualServerByPort(int)
	 * @see #selectVirtualServer(VirtualServer)
	 */
	public CommandFuture<Void> selectVirtualServerById(int id) {
		return selectVirtualServerById(id, null);
	}

	/**
	 * Moves the server query into the virtual server with the specified ID
	 * and sets the server query's nickname.
	 * <p>
	 * The nickname must be between 3 and 30 UTF-8 bytes long. BB codes will be ignored.
	 * </p>
	 *
	 * @param id
	 * 		the ID of the virtual server
	 * @param nickname
	 * 		the nickname, or {@code null} if the nickname should not be set
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see VirtualServer#getId()
	 * @see #selectVirtualServerById(int)
	 * @see #selectVirtualServerByPort(int, String)
	 * @see #selectVirtualServer(VirtualServer, String)
	 */
	public CommandFuture<Void> selectVirtualServerById(int id, String nickname) {
		Command cmd = QueryCommands.useId(id, nickname);
		return executeAndReturnError(cmd);
	}

	/**
	 * Moves the server query into the virtual server with the specified voice port.
	 *
	 * @param port
	 * 		the voice port of the virtual server
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see VirtualServer#getPort()
	 * @see #selectVirtualServerById(int)
	 * @see #selectVirtualServerByPort(int, String)
	 * @see #selectVirtualServer(VirtualServer)
	 */
	public CommandFuture<Void> selectVirtualServerByPort(int port) {
		return selectVirtualServerByPort(port, null);
	}

	/**
	 * Moves the server query into the virtual server with the specified voice port
	 * and sets the server query's nickname.
	 * <p>
	 * The nickname must be between 3 and 30 UTF-8 bytes long. BB codes will be ignored.
	 * </p>
	 *
	 * @param port
	 * 		the voice port of the virtual server
	 * @param nickname
	 * 		the nickname, or {@code null} if the nickname should not be set
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see VirtualServer#getPort()
	 * @see #selectVirtualServerById(int, String)
	 * @see #selectVirtualServerByPort(int)
	 * @see #selectVirtualServer(VirtualServer, String)
	 */
	public CommandFuture<Void> selectVirtualServerByPort(int port, String nickname) {
		Command cmd = QueryCommands.usePort(port, nickname);
		return executeAndReturnError(cmd);
	}

	/**
	 * Moves the server query into the specified virtual server.
	 *
	 * @param server
	 * 		the virtual server to move into
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #selectVirtualServerById(int)
	 * @see #selectVirtualServerByPort(int)
	 * @see #selectVirtualServer(VirtualServer, String)
	 */
	public CommandFuture<Void> selectVirtualServer(VirtualServer server) {
		return selectVirtualServerById(server.getId());
	}

	/**
	 * Moves the server query into the specified virtual server
	 * and sets the server query's nickname.
	 * <p>
	 * The nickname must be between 3 and 30 UTF-8 bytes long. BB codes will be ignored.
	 * </p>
	 *
	 * @param server
	 * 		the virtual server to move into
	 * @param nickname
	 * 		the nickname, or {@code null} if the nickname should not be set
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #selectVirtualServerById(int, String)
	 * @see #selectVirtualServerByPort(int, String)
	 * @see #selectVirtualServer(VirtualServer)
	 */
	public CommandFuture<Void> selectVirtualServer(VirtualServer server, String nickname) {
		return selectVirtualServerById(server.getId(), nickname);
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getUniqueIdentifier()
	 * @see Message
	 */
	public CommandFuture<Void> sendOfflineMessage(String clientUId, String subject, String message) {
		Command cmd = MessageCommands.messageAdd(clientUId, subject, message);
		return executeAndReturnError(cmd);
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 */
	public CommandFuture<Void> sendTextMessage(TextMessageTargetMode targetMode, int targetId, String message) {
		Command cmd = ClientCommands.sendTextMessage(targetMode.getIndex(), targetId, message);
		return executeAndReturnError(cmd);
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #sendChannelMessage(String)
	 * @see Channel#getId()
	 */
	public CommandFuture<Void> sendChannelMessage(int channelId, String message) {
		return moveQuery(channelId)
				.then(__ -> sendTextMessage(TextMessageTargetMode.CHANNEL, 0, message));
	}

	/**
	 * Sends a text message to the channel the server query is currently in.
	 * Your message may contain BB codes, but its length is limited to 1024 UTF-8 bytes.
	 *
	 * @param message
	 * 		the text message to send
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Void> sendChannelMessage(String message) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #sendServerMessage(String)
	 * @see VirtualServer#getId()
	 */
	public CommandFuture<Void> sendServerMessage(int serverId, String message) {
		return selectVirtualServerById(serverId)
				.then(__ -> sendTextMessage(TextMessageTargetMode.SERVER, 0, message));
	}

	/**
	 * Sends a text message to the virtual server the server query is currently in.
	 * Your message may contain BB codes, but its length is limited to 1024 UTF-8 bytes.
	 *
	 * @param message
	 * 		the text message to send
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Void> sendServerMessage(String message) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 */
	public CommandFuture<Void> sendPrivateMessage(int clientId, String message) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 * @see Channel#getId()
	 * @see Client#getDatabaseId()
	 */
	public CommandFuture<Void> setClientChannelGroup(int groupId, int channelId, int clientDBId) {
		Command cmd = ChannelGroupCommands.setClientChannelGroup(groupId, channelId, clientDBId);
		return executeAndReturnError(cmd);
	}

	/**
	 * Sets the value of the multiple custom client properties for a client.
	 * <p>
	 * If any key present in the map already has a value assigned for this client,
	 * the existing value will be overwritten.
	 * This method does not delete keys not present in the map.
	 * </p><p>
	 * If {@code properties} contains an entry with {@code null} as its key,
	 * that entry will be ignored and no exception will be thrown.
	 * </p>
	 *
	 * @param clientDBId
	 * 		the database ID of the target client
	 * @param properties
	 * 		the map of properties to set, cannot be {@code null}
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands properties.size()
	 * @see Client#getDatabaseId()
	 * @see #setCustomClientProperty(int, String, String)
	 * @see #deleteCustomClientProperty(int, String)
	 */
	public CommandFuture<Void> setCustomClientProperties(int clientDBId, Map<String, String> properties) {
		Collection<CommandFuture<Void>> futures = new ArrayList<>(properties.size());

		for (Map.Entry<String, String> entry : properties.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();

			if (key != null) {
				futures.add(setCustomClientProperty(clientDBId, key, value));
			}
		}

		return CommandFuture.ofAll(futures)
				.map(__ -> null); // Return success as Void, not List<Void>
	}

	/**
	 * Sets the value of the {@code key} custom client property for a client.
	 * <p>
	 * If there is already an assignment of the {@code key} custom client property
	 * for this client, the existing value will be overwritten.
	 * </p>
	 *
	 * @param clientDBId
	 * 		the database ID of the target client
	 * @param key
	 * 		the key of the custom property to set, cannot be {@code null}
	 * @param value
	 * 		the (new) value of the custom property to set
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see #setCustomClientProperties(int, Map)
	 * @see #deleteCustomClientProperty(int, String)
	 */
	public CommandFuture<Void> setCustomClientProperty(int clientDBId, String key, String value) {
		if (key == null) throw new IllegalArgumentException("Key cannot be null");

		Command cmd = CustomPropertyCommands.customSet(clientDBId, key, value);
		return executeAndReturnError(cmd);
	}

	/**
	 * Sets the read flag to {@code true} for a given message. This will not delete the message.
	 *
	 * @param messageId
	 * 		the ID of the message for which the read flag should be set
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #setMessageReadFlag(int, boolean)
	 */
	public CommandFuture<Void> setMessageRead(int messageId) {
		return setMessageReadFlag(messageId, true);
	}

	/**
	 * Sets the read flag to {@code true} for a given message. This will not delete the message.
	 *
	 * @param message
	 * 		the message for which the read flag should be set
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #setMessageRead(int)
	 * @see #setMessageReadFlag(Message, boolean)
	 * @see #deleteOfflineMessage(int)
	 */
	public CommandFuture<Void> setMessageRead(Message message) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #setMessageRead(int)
	 * @see #setMessageReadFlag(Message, boolean)
	 * @see #deleteOfflineMessage(int)
	 */
	public CommandFuture<Void> setMessageReadFlag(int messageId, boolean read) {
		Command cmd = MessageCommands.messageUpdateFlag(messageId, read);
		return executeAndReturnError(cmd);
	}

	/**
	 * Sets the read flag for a given message. This will not delete the message.
	 *
	 * @param message
	 * 		the message for which the read flag should be set
	 * @param read
	 * 		the boolean value to which the read flag should be set
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #setMessageRead(Message)
	 * @see #setMessageReadFlag(int, boolean)
	 * @see #deleteOfflineMessage(int)
	 */
	public CommandFuture<Void> setMessageReadFlag(Message message, boolean read) {
		return setMessageReadFlag(message.getId(), read);
	}

	/**
	 * Sets the nickname of the server query client.
	 * <p>
	 * The nickname must be between 3 and 30 UTF-8 bytes long. BB codes will be ignored.
	 * </p>
	 *
	 * @param nickname
	 * 		the new nickname, may not be {@code null}
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #updateClient(Map)
	 */
	public CommandFuture<Void> setNickname(String nickname) {
		Map<ClientProperty, String> options = Collections.singletonMap(ClientProperty.CLIENT_NICKNAME, nickname);
		return updateClient(options);
	}

	/**
	 * Starts the virtual server with the specified ID.
	 *
	 * @param serverId
	 * 		the ID of the virtual server
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Void> startServer(int serverId) {
		Command cmd = VirtualServerCommands.serverStart(serverId);
		return executeAndReturnError(cmd);
	}

	/**
	 * Starts the specified virtual server.
	 *
	 * @param virtualServer
	 * 		the virtual server to start
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Void> startServer(VirtualServer virtualServer) {
		return startServer(virtualServer.getId());
	}

	/**
	 * Stops the virtual server with the specified ID.
	 *
	 * @param serverId
	 * 		the ID of the virtual server
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Void> stopServer(int serverId) {
		return stopServer(serverId, null);
	}

	/**
	 * Stops the virtual server with the specified ID.
	 *
	 * @param serverId
	 * 		the ID of the virtual server
	 * @param reason
	 * 		the reason message to display to clients when they are disconnected
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Void> stopServer(int serverId, String reason) {
		Command cmd = VirtualServerCommands.serverStop(serverId, reason);
		return executeAndReturnError(cmd);
	}

	/**
	 * Stops the specified virtual server.
	 *
	 * @param virtualServer
	 * 		the virtual server to stop
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Void> stopServer(VirtualServer virtualServer) {
		return stopServer(virtualServer.getId(), null);
	}

	/**
	 * Stops the specified virtual server.
	 *
	 * @param virtualServer
	 * 		the virtual server to stop
	 * @param reason
	 * 		the reason message to display to clients when they are disconnected
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Void> stopServer(VirtualServer virtualServer, String reason) {
		return stopServer(virtualServer.getId(), reason);
	}

	/**
	 * Stops the entire TeamSpeak 3 Server instance by shutting down the process.
	 * <p>
	 * To have permission to use this command, you need to use the server query admin login.
	 * </p>
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Void> stopServerProcess() {
		return stopServerProcess(null);
	}

	/**
	 * Stops the entire TeamSpeak 3 Server instance by shutting down the process.
	 * <p>
	 * To have permission to use this command, you need to use the server query admin login.
	 * </p>
	 *
	 * @param reason
	 * 		the reason message to display to clients when they are disconnected
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Void> stopServerProcess(String reason) {
		Command cmd = ServerCommands.serverProcessStop(reason);
		return executeAndReturnError(cmd);
	}

	/**
	 * Unregisters the server query from receiving any event notifications.
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<Void> unregisterAllEvents() {
		Command cmd = QueryCommands.serverNotifyUnregister();
		return executeAndReturnError(cmd);
	}

	/**
	 * Updates several client properties for this server query instance.
	 *
	 * @param options
	 * 		the map of properties to update
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #updateClient(ClientProperty, String)
	 * @see #editClient(int, Map)
	 */
	public CommandFuture<Void> updateClient(Map<ClientProperty, String> options) {
		Command cmd = ClientCommands.clientUpdate(options);
		return executeAndReturnError(cmd);
	}

	/**
	 * Changes a single client property for this server query instance.
	 * <p>
	 * Note that one can set many properties at once with the overloaded method that
	 * takes a map of client properties and strings.
	 * </p>
	 *
	 * @param property
	 * 		the client property to modify, make sure it is editable
	 * @param value
	 * 		the new value of the property
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #updateClient(Map)
	 * @see #editClient(int, Map)
	 */
	public CommandFuture<Void> updateClient(ClientProperty property, String value) {
		return updateClient(Collections.singletonMap(property, value));
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public CommandFuture<String> updateServerQueryLogin(String loginName) {
		Command cmd = ClientCommands.clientSetServerQueryLogin(loginName);
		return executeAndReturnStringProperty(cmd, "client_login_password");
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #uploadFileDirect(byte[], String, boolean, int, String)
	 */
	public CommandFuture<Void> uploadFile(InputStream dataIn, long dataLength, String filePath, boolean overwrite, int channelId) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #uploadFileDirect(byte[], String, boolean, int, String)
	 */
	public CommandFuture<Void> uploadFile(InputStream dataIn, long dataLength, String filePath, boolean overwrite, int channelId, String channelPassword) {
		FileTransferHelper helper = query.getFileTransferHelper();
		int transferId = helper.getClientTransferId();
		Command cmd = FileCommands.ftInitUpload(transferId, filePath, channelId, channelPassword, dataLength, overwrite);
		CommandFuture<Void> future = new CommandFuture<>();

		executeAndTransformFirst(cmd, FileTransferParameters::new).onSuccess(params -> {
			QueryError error = params.getQueryError();
			if (!error.isSuccessful()) {
				future.fail(new TS3CommandFailedException(error, cmd.getName()));
				return;
			}

			try {
				query.getFileTransferHelper().uploadFile(dataIn, dataLength, params);
			} catch (IOException e) {
				future.fail(new TS3FileTransferFailedException("Upload failed", e));
				return;
			}
			future.set(null); // Mark as successful
		}).forwardFailure(future);

		return future;
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #uploadFile(InputStream, long, String, boolean, int)
	 */
	public CommandFuture<Void> uploadFileDirect(byte[] data, String filePath, boolean overwrite, int channelId) {
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
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #uploadFile(InputStream, long, String, boolean, int, String)
	 */
	public CommandFuture<Void> uploadFileDirect(byte[] data, String filePath, boolean overwrite, int channelId, String channelPassword) {
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see IconFile#getIconId()
	 * @see #uploadIconDirect(byte[])
	 * @see #downloadIcon(OutputStream, long)
	 */
	public CommandFuture<Long> uploadIcon(InputStream dataIn, long dataLength) {
		byte[] data;
		try {
			data = FileTransferHelper.readFully(dataIn, dataLength);
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see IconFile#getIconId()
	 * @see #uploadIcon(InputStream, long)
	 * @see #downloadIconDirect(long)
	 */
	public CommandFuture<Long> uploadIconDirect(byte[] data) {
		CommandFuture<Long> future = new CommandFuture<>();

		long iconId = FileTransferHelper.getIconId(data);
		String path = "/icon_" + iconId;

		uploadFileDirect(data, path, false, 0)
				.onSuccess(__ -> future.set(iconId))
				.onFailure(transformError(future, 2050, iconId));

		return future;
	}

	/**
	 * Uses an existing privilege key to join a server or channel group.
	 *
	 * @param token
	 * 		the privilege key to use
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see PrivilegeKey
	 * @see #addPrivilegeKey(PrivilegeKeyType, int, int, String)
	 * @see #usePrivilegeKey(PrivilegeKey)
	 */
	public CommandFuture<Void> usePrivilegeKey(String token) {
		Command cmd = PrivilegeKeyCommands.privilegeKeyUse(token);
		return executeAndReturnError(cmd);
	}

	/**
	 * Uses an existing privilege key to join a server or channel group.
	 *
	 * @param privilegeKey
	 * 		the privilege key to use
	 *
	 * @return a future to track the progress of this command
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see PrivilegeKey
	 * @see #addPrivilegeKey(PrivilegeKeyType, int, int, String)
	 * @see #usePrivilegeKey(String)
	 */
	public CommandFuture<Void> usePrivilegeKey(PrivilegeKey privilegeKey) {
		return usePrivilegeKey(privilegeKey.getToken());
	}

	/**
	 * Gets information about the current server query instance.
	 *
	 * @return information about the server query instance
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #getClientInfo(int)
	 */
	public CommandFuture<ServerQueryInfo> whoAmI() {
		Command cmd = QueryCommands.whoAmI();
		return executeAndTransformFirst(cmd, ServerQueryInfo::new);
	}

	/**
	 * Checks whether a given {@link TS3Exception} is a {@link TS3CommandFailedException} with the
	 * specified error ID.
	 *
	 * @param exception
	 * 		the exception to check
	 * @param errorId
	 * 		the error ID to match
	 *
	 * @return whether {@code exception} is a {@code TS3CommandFailedException} with error ID {@code errorId}.
	 */
	private static boolean isQueryError(TS3Exception exception, int errorId) {
		if (exception instanceof TS3CommandFailedException) {
			TS3CommandFailedException cfe = (TS3CommandFailedException) exception;
			return (cfe.getError().getId() == errorId);
		} else {
			return false;
		}
	}

	/**
	 * Creates a {@code FailureListener} that checks whether the caught exception is
	 * a {@code TS3CommandFailedException} with error ID {@code errorId}.
	 * <p>
	 * If so, the listener makes {@code future} succeed by setting its result value to an empty
	 * list with element type {@code T}. Else, the caught exception is forwarded to {@code future}.
	 * </p>
	 *
	 * @param future
	 * 		the future to forward the result to
	 * @param errorId
	 * 		the error ID to catch
	 * @param replacement
	 * 		the value to
	 * @param <T>
	 * 		the type of {@code replacement} and element type of {@code future}
	 *
	 * @return a {@code FailureListener} with the described properties
	 */
	private static <T> CommandFuture.FailureListener transformError(CommandFuture<T> future, int errorId, T replacement) {
		return exception -> {
			if (isQueryError(exception, errorId)) {
				future.set(replacement);
			} else {
				future.fail(exception);
			}
		};
	}

	/**
	 * Executes a command and sets the returned future to true if the command succeeded.
	 *
	 * @param command
	 * 		the command to execute
	 *
	 * @return a future to track the progress of this command
	 */
	private CommandFuture<Void> executeAndReturnError(Command command) {
		CommandFuture<Void> future = command.getFuture()
				.map(__ -> null); // Mark as successful

		commandQueue.enqueueCommand(command);
		return future;
	}

	/**
	 * Executes a command, checking for failure and returning a single
	 * {@code String} property from the first response map.
	 *
	 * @param command
	 * 		the command to execute
	 * @param property
	 * 		the name of the property to return
	 *
	 * @return the value of the specified {@code String} property
	 */
	private CommandFuture<String> executeAndReturnStringProperty(Command command, String property) {
		CommandFuture<String> future = command.getFuture()
				.map(result -> result.getFirstResponse().get(property));

		commandQueue.enqueueCommand(command);
		return future;
	}

	/**
	 * Executes a command and returns a single {@code Integer} property from the first response map.
	 *
	 * @param command
	 * 		the command to execute
	 * @param property
	 * 		the name of the property to return
	 *
	 * @return the value of the specified {@code Integer} property
	 */
	private CommandFuture<Integer> executeAndReturnIntProperty(Command command, String property) {
		CommandFuture<Integer> future = command.getFuture()
				.map(result -> result.getFirstResponse().getInt(property));

		commandQueue.enqueueCommand(command);
		return future;
	}

	private CommandFuture<int[]> executeAndReturnIntArray(Command command, String property) {
		CommandFuture<int[]> future = command.getFuture()
				.map(result -> {
					List<Wrapper> responses = result.getResponses();
					int[] values = new int[responses.size()];
					int i = 0;

					for (Wrapper response : responses) {
						values[i++] = response.getInt(property);
					}
					return values;
				});

		commandQueue.enqueueCommand(command);
		return future;
	}

	/**
	 * Executes a command, checks for failure and transforms the first
	 * response map by invoking {@code fn}.
	 *
	 * @param command
	 * 		the command to execute
	 * @param fn
	 * 		the function that creates a new wrapper of type {@code T}
	 * @param <T>
	 * 		the wrapper class the map should be wrapped with
	 *
	 * @return a future of a {@code T} wrapper of the first response map
	 */
	private <T extends Wrapper> CommandFuture<T> executeAndTransformFirst(Command command, Function<Map<String, String>, T> fn) {
		return executeAndMapFirst(command, wrapper -> fn.apply(wrapper.getMap()));
	}

	/**
	 * Executes a command, checks for failure and maps the first
	 * response wrapper by using {@code fn}.
	 *
	 * @param command
	 * 		the command to execute
	 * @param fn
	 * 		a mapping function from {@code Wrapper} to {@code T}
	 * @param <T>
	 * 		the result type of the mapping function {@code fn}
	 *
	 * @return a future of a {@code T}
	 */
	private <T> CommandFuture<T> executeAndMapFirst(Command command, Function<Wrapper, T> fn) {
		CommandFuture<T> future = command.getFuture()
				.map(result -> fn.apply(result.getFirstResponse()));

		commandQueue.enqueueCommand(command);
		return future;
	}

	/**
	 * Executes a command, checks for failure and transforms all
	 * response maps to a wrapper by invoking {@code fn} on each map.
	 *
	 * @param command
	 * 		the command to execute
	 * @param fn
	 * 		the function that creates the new wrappers of type {@code T}
	 * @param <T>
	 * 		the wrapper class the maps should be wrapped with
	 *
	 * @return a future of a list of wrapped response maps
	 */
	private <T extends Wrapper> CommandFuture<List<T>> executeAndTransform(Command command, Function<Map<String, String>, T> fn) {
		return executeAndMap(command, wrapper -> fn.apply(wrapper.getMap()));
	}

	/**
	 * Executes a command, checks for failure and maps all response
	 * wrappers by using {@code fn}.
	 *
	 * @param command
	 * 		the command to execute
	 * @param fn
	 * 		a mapping function from {@code Wrapper} to {@code T}
	 * @param <T>
	 * 		the result type of the mapping function {@code fn}
	 *
	 * @return a future of a list of {@code T}
	 */
	private <T> CommandFuture<List<T>> executeAndMap(Command command, Function<Wrapper, T> fn) {
		CommandFuture<List<T>> future = command.getFuture()
				.map(result -> {
					List<Wrapper> response = result.getResponses();
					List<T> transformed = new ArrayList<>(response.size());
					for (Wrapper wrapper : response) {
						transformed.add(fn.apply(wrapper));
					}

					return transformed;
				});

		commandQueue.enqueueCommand(command);
		return future;
	}

	/**
	 * Computes a sub-list of the list of values produced by {@code valuesFuture} where
	 * each value matches a key in the list of keys produced by {@code keysFuture}.
	 * <p>
	 * The returned future succeeds if {@code keysFuture} and {@code valuesFuture} succeed and
	 * fails if {@code keysFuture} or {@code valuesFuture} fails.
	 * </p><p>
	 * {@code null} keys, {@code null} values, and keys without a matching value are ignored.
	 * If multiple values map to the same key, only the first value is used.
	 * </p><p>
	 * The order of values in the resulting list follows the order of matching keys,
	 * not the order of the original value list.
	 * </p>
	 *
	 * @param keysFuture
	 * 		the future producing a list of keys of type {@code K}
	 * @param valuesFuture
	 * 		the future producing a list of values of type {@code V}
	 * @param keyMapper
	 * 		a function extracting keys from the value type
	 * @param <K>
	 * 		the key type
	 * @param <V>
	 * 		the value type
	 *
	 * @return a future of a list of values of type {@code V}
	 */
	private static <K, V> CommandFuture<List<V>> findByKey(CommandFuture<List<K>> keysFuture, CommandFuture<List<V>> valuesFuture,
	                                                       Function<? super V, ? extends K> keyMapper) {
		CommandFuture<List<V>> future = new CommandFuture<>();

		keysFuture.onSuccess(keys ->
				valuesFuture.onSuccess(values -> {
					Map<K, V> valueMap = values.stream().collect(Collectors.toMap(keyMapper, Function.identity(), (l, r) -> l));
					List<V> foundValues = new ArrayList<>(keys.size());

					for (K key : keys) {
						if (key == null) continue;
						V value = valueMap.get(key);
						if (value == null) continue;
						foundValues.add(value);
					}

					future.set(foundValues);
				}).forwardFailure(future)
		).forwardFailure(future);

		return future;
	}
}
