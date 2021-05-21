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
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3FileTransferFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
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

	private final TS3ApiAsync asyncApi;

	/**
	 * Creates a new synchronous API object for the given {@code TS3Query}.
	 * <p>
	 * <b>Usually, this constructor should not be called.</b> Use {@link TS3Query#getApi()} instead.
	 * </p>
	 *
	 * @param asyncApi
	 * 		the asynchronous version of the API this class routes its method calls through
	 */
	TS3Api(TS3ApiAsync asyncApi) {
		this.asyncApi = asyncApi;
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
	public int addBan(String ip, String name, String uid, long timeInSeconds, String reason) {
		return asyncApi.addBan(ip, name, uid, timeInSeconds, reason).getUninterruptibly();
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
	public int addBan(String ip, String name, String uid, String myTSId, long timeInSeconds, String reason) {
		return asyncApi.addBan(ip, name, uid, myTSId, timeInSeconds, reason).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see Client#getDatabaseId()
	 * @see Permission
	 */
	public void addChannelClientPermission(int channelId, int clientDBId, String permName, int permValue) {
		asyncApi.addChannelClientPermission(channelId, clientDBId, permName, permValue).getUninterruptibly();
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
	public int addChannelGroup(String name) {
		return asyncApi.addChannelGroup(name).getUninterruptibly();
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
	public int addChannelGroup(String name, PermissionGroupDatabaseType type) {
		return asyncApi.addChannelGroup(name, type).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 * @see Permission
	 */
	public void addChannelGroupPermission(int groupId, String permName, int permValue) {
		asyncApi.addChannelGroupPermission(groupId, permName, permValue).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see Permission
	 */
	public void addChannelPermission(int channelId, String permName, int permValue) {
		asyncApi.addChannelPermission(channelId, permName, permValue).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see Permission
	 */
	public void addClientPermission(int clientDBId, String permName, int value, boolean skipped) {
		asyncApi.addClientPermission(clientDBId, permName, value, skipped).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 * @see Client#getDatabaseId()
	 */
	public void addClientToServerGroup(int groupId, int clientDatabaseId) {
		asyncApi.addClientToServerGroup(groupId, clientDatabaseId).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see Complaint#getMessage()
	 */
	public void addComplaint(int clientDBId, String message) {
		asyncApi.addComplaint(clientDBId, message).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroupType
	 * @see Permission
	 */
	public void addPermissionToAllServerGroups(ServerGroupType type, String permName, int value, boolean negated, boolean skipped) {
		asyncApi.addPermissionToAllServerGroups(type, permName, value, negated, skipped).getUninterruptibly();
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
	public String addPrivilegeKey(PrivilegeKeyType type, int groupId, int channelId, String description) {
		return asyncApi.addPrivilegeKey(type, groupId, channelId, description).getUninterruptibly();
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
	public String addPrivilegeKeyChannelGroup(int channelGroupId, int channelId, String description) {
		return asyncApi.addPrivilegeKeyChannelGroup(channelGroupId, channelId, description).getUninterruptibly();
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
	public String addPrivilegeKeyServerGroup(int serverGroupId, String description) {
		return asyncApi.addPrivilegeKeyServerGroup(serverGroupId, description).getUninterruptibly();
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
	public int addServerGroup(String name) {
		return asyncApi.addServerGroup(name).getUninterruptibly();
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
	public int addServerGroup(String name, PermissionGroupDatabaseType type) {
		return asyncApi.addServerGroup(name, type).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 * @see Permission
	 */
	public void addServerGroupPermission(int groupId, String permName, int value, boolean negated, boolean skipped) {
		asyncApi.addServerGroupPermission(groupId, permName, value, negated, skipped).getUninterruptibly();
	}

	/**
	 * Creates a server query login with name {@code loginName} for the client specified by {@code clientDBId}
	 * on the currently selected virtual server and returns the password of the created login.
	 * If the client already had a server query login, the existing login will be deleted and replaced.
	 * <p>
	 * Moreover, this method can be used to create new <i>global</i> server query logins that are not tied to any
	 * particular virtual server or client. To create such a server query login, make sure no virtual server is
	 * selected (e.g. use {@code selectVirtualServerById(0)}) and call this method with {@code clientDBId = 0}.
	 * </p>
	 *
	 * @param loginName
	 * 		the name of the server query login to add
	 * @param clientDBId
	 * 		the database ID of the client for which a server query login should be created
	 *
	 * @return an object containing the password of the new server query login
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #deleteServerQueryLogin(int)
	 * @see #getServerQueryLogins()
	 * @see #updateServerQueryLogin(String)
	 */
	public CreatedQueryLogin addServerQueryLogin(String loginName, int clientDBId) {
		return asyncApi.addServerQueryLogin(loginName, clientDBId).getUninterruptibly();
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
		asyncApi.addTS3Listeners(listeners);
	}

	/**
	 * Bans a client with a given client ID for a given time.
	 * <p>
	 * Please note that this will create up to three separate ban rules,
	 * one for the targeted client's IP address, one for their unique identifier,
	 * and potentially one more entry for their "myTeamSpeak" ID, if available.
	 * </p><p>
	 * <i>Exception:</i> If the banned client connects via a loopback address
	 * (i.e. {@code 127.0.0.1} or {@code localhost}), no IP ban is created.
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
	public int[] banClient(int clientId, long timeInSeconds) {
		return asyncApi.banClient(clientId, timeInSeconds).getUninterruptibly();
	}

	/**
	 * Bans a client with a given client ID for a given time for the specified reason.
	 * <p>
	 * Please note that this will create up to three separate ban rules,
	 * one for the targeted client's IP address, one for their unique identifier,
	 * and potentially one more entry for their "myTeamSpeak" ID, if available.
	 * </p><p>
	 * <i>Exception:</i> If the banned client connects via a loopback address
	 * (i.e. {@code 127.0.0.1} or {@code localhost}), no IP ban is created.
	 * </p>
	 *
	 * @param clientId
	 * 		the ID of the client
	 * @param timeInSeconds
	 * 		the duration of the ban in seconds. 0 equals a permanent ban
	 * @param reason
	 * 		the reason for the ban, can be null
	 *
	 * @return an array containing the IDs of the created ban entries
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #addBan(String, String, String, long, String)
	 */
	public int[] banClient(int clientId, long timeInSeconds, String reason) {
		return asyncApi.banClient(clientId, timeInSeconds, reason).getUninterruptibly();
	}

	/**
	 * Bans a client with a given client ID permanently for the specified reason.
	 * <p>
	 * Please note that this will create up to three separate ban rules,
	 * one for the targeted client's IP address, one for their unique identifier,
	 * and potentially one more entry for their "myTeamSpeak" ID, if available.
	 * </p><p>
	 * <i>Exception:</i> If the banned client connects via a loopback address
	 * (i.e. {@code 127.0.0.1} or {@code localhost}), no IP ban is created.
	 * </p>
	 *
	 * @param clientId
	 * 		the ID of the client
	 * @param reason
	 * 		the reason for the ban, can be null
	 *
	 * @return an array containing the IDs of the created ban entries
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #addBan(String, String, String, long, String)
	 */
	public int[] banClient(int clientId, String reason) {
		return asyncApi.banClient(clientId, reason).getUninterruptibly();
	}

	/**
	 * Bans multiple clients by their client ID for a given time for the specified reason.
	 * <p>
	 * Please note that this will create up to three separate ban rules for each client,
	 * one for the targeted client's IP address, one for their unique identifier,
	 * and potentially one more entry for their "myTeamSpeak" ID, if available.
	 * </p><p>
	 * <i>Exception:</i> If the banned client connects via a loopback address
	 * (i.e. {@code 127.0.0.1} or {@code localhost}), no IP ban is created.
	 * </p><p>
	 * <i>Exception:</i> If two or more clients are connecting from the
	 * same IP address, only one IP ban entry for that IP will be created.
	 * </p>
	 *
	 * @param clientIds
	 * 		the IDs of the clients to be banned
	 * @param timeInSeconds
	 * 		the duration of the ban in seconds. 0 equals a permanent ban
	 * @param reason
	 * 		the reason for the ban, can be null
	 * @param continueOnError
	 * 		if true, continue to the next client if banning one client fails, else do not create any bans on error
	 *
	 * @return an array containing the IDs of the created ban entries
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #addBan(String, String, String, long, String)
	 */
	public int[] banClients(int[] clientIds, long timeInSeconds, String reason, boolean continueOnError) {
		return asyncApi.banClients(clientIds, timeInSeconds, reason, continueOnError).getUninterruptibly();
	}

	/**
	 * Sends a text message to all clients on all virtual servers.
	 * These messages will appear to clients in the tab for server messages.
	 *
	 * @param message
	 * 		the message to be sent
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public void broadcast(String message) {
		asyncApi.broadcast(message).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 */
	public void copyChannelGroup(int sourceGroupId, int targetGroupId, PermissionGroupDatabaseType type) {
		asyncApi.copyChannelGroup(sourceGroupId, targetGroupId, type).getUninterruptibly();
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
	public int copyChannelGroup(int sourceGroupId, String targetName, PermissionGroupDatabaseType type) {
		return asyncApi.copyChannelGroup(sourceGroupId, targetName, type).getUninterruptibly();
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
	public int copyServerGroup(int sourceGroupId, int targetGroupId, PermissionGroupDatabaseType type) {
		return asyncApi.copyServerGroup(sourceGroupId, targetGroupId, type).getUninterruptibly();
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
	public int copyServerGroup(int sourceGroupId, String targetName, PermissionGroupDatabaseType type) {
		return asyncApi.copyServerGroup(sourceGroupId, targetName, type).getUninterruptibly();
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
	public int createChannel(String name, Map<ChannelProperty, String> options) {
		return asyncApi.createChannel(name, options).getUninterruptibly();
	}

	/**
	 * Creates a new directory on the file repository in the specified channel.
	 *
	 * @param directoryPath
	 * 		the path to the directory that should be created
	 * @param channelId
	 * 		the ID of the channel the directory should be created in
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public void createFileDirectory(String directoryPath, int channelId) {
		asyncApi.createFileDirectory(directoryPath, channelId).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public void createFileDirectory(String directoryPath, int channelId, String channelPassword) {
		asyncApi.createFileDirectory(directoryPath, channelId, channelPassword).getUninterruptibly();
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
	public CreatedVirtualServer createServer(String name, Map<VirtualServerProperty, String> options) {
		return asyncApi.createServer(name, options).getUninterruptibly();
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
	public Snapshot createServerSnapshot() {
		return asyncApi.createServerSnapshot().getUninterruptibly();
	}

	/**
	 * Deletes all active ban rules from the server. Use with caution.
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public void deleteAllBans() {
		asyncApi.deleteAllBans().getUninterruptibly();
	}

	/**
	 * Deletes all complaints about the client with specified database ID from the server.
	 *
	 * @param clientDBId
	 * 		the database ID of the client
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see Complaint
	 */
	public void deleteAllComplaints(int clientDBId) {
		asyncApi.deleteAllComplaints(clientDBId).getUninterruptibly();
	}

	/**
	 * Deletes the ban rule with the specified ID from the server.
	 *
	 * @param banId
	 * 		the ID of the ban to delete
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Ban#getId()
	 */
	public void deleteBan(int banId) {
		asyncApi.deleteBan(banId).getUninterruptibly();
	}

	/**
	 * Deletes an existing channel specified by its ID, kicking all clients out of the channel.
	 *
	 * @param channelId
	 * 		the ID of the channel to delete
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see #deleteChannel(int, boolean)
	 * @see #kickClientFromChannel(String, int...)
	 */
	public void deleteChannel(int channelId) {
		asyncApi.deleteChannel(channelId).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see #kickClientFromChannel(String, int...)
	 */
	public void deleteChannel(int channelId, boolean force) {
		asyncApi.deleteChannel(channelId, force).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see Client#getDatabaseId()
	 * @see Permission#getName()
	 */
	public void deleteChannelClientPermission(int channelId, int clientDBId, String permName) {
		asyncApi.deleteChannelClientPermission(channelId, clientDBId, permName).getUninterruptibly();
	}

	/**
	 * Removes the channel group with the given ID.
	 *
	 * @param groupId
	 * 		the ID of the channel group
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 */
	public void deleteChannelGroup(int groupId) {
		asyncApi.deleteChannelGroup(groupId).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 */
	public void deleteChannelGroup(int groupId, boolean force) {
		asyncApi.deleteChannelGroup(groupId, force).getUninterruptibly();
	}

	/**
	 * Removes a permission from the channel group with the given ID.
	 *
	 * @param groupId
	 * 		the ID of the channel group
	 * @param permName
	 * 		the name of the permission to revoke
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 * @see Permission#getName()
	 */
	public void deleteChannelGroupPermission(int groupId, String permName) {
		asyncApi.deleteChannelGroupPermission(groupId, permName).getUninterruptibly();
	}

	/**
	 * Removes a permission from the channel with the given ID.
	 *
	 * @param channelId
	 * 		the ID of the channel
	 * @param permName
	 * 		the name of the permission to revoke
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see Permission#getName()
	 */
	public void deleteChannelPermission(int channelId, String permName) {
		asyncApi.deleteChannelPermission(channelId, permName).getUninterruptibly();
	}

	/**
	 * Removes a permission from a client.
	 *
	 * @param clientDBId
	 * 		the database ID of the client
	 * @param permName
	 * 		the name of the permission to revoke
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see Permission#getName()
	 */
	public void deleteClientPermission(int clientDBId, String permName) {
		asyncApi.deleteClientPermission(clientDBId, permName).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Complaint
	 * @see Client#getDatabaseId()
	 */
	public void deleteComplaint(int targetClientDBId, int fromClientDBId) {
		asyncApi.deleteComplaint(targetClientDBId, fromClientDBId).getUninterruptibly();
	}

	/**
	 * Removes the {@code key} custom client property from a client.
	 *
	 * @param clientDBId
	 * 		the database ID of the target client
	 * @param key
	 * 		the key of the custom property to delete, cannot be {@code null}
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 */
	public void deleteCustomClientProperty(int clientDBId, String key) {
		asyncApi.deleteCustomClientProperty(clientDBId, key).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see #getDatabaseClientInfo(int)
	 * @see DatabaseClientInfo
	 */
	public void deleteDatabaseClientProperties(int clientDBId) {
		asyncApi.deleteDatabaseClientProperties(clientDBId).getUninterruptibly();
	}

	/**
	 * Deletes a file or directory from the file repository in the specified channel.
	 *
	 * @param filePath
	 * 		the path to the file or directory
	 * @param channelId
	 * 		the ID of the channel the file or directory resides in
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public void deleteFile(String filePath, int channelId) {
		asyncApi.deleteFile(filePath, channelId).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public void deleteFile(String filePath, int channelId, String channelPassword) {
		asyncApi.deleteFile(filePath, channelId, channelPassword).getUninterruptibly();
	}

	/**
	 * Deletes multiple files or directories from the file repository in the specified channel.
	 *
	 * @param filePaths
	 * 		the paths to the files or directories
	 * @param channelId
	 * 		the ID of the channel the file or directory resides in
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public void deleteFiles(String[] filePaths, int channelId) {
		asyncApi.deleteFiles(filePaths, channelId).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 */
	public void deleteFiles(String[] filePaths, int channelId, String channelPassword) {
		asyncApi.deleteFiles(filePaths, channelId, channelPassword).getUninterruptibly();
	}

	/**
	 * Deletes an icon from the icon directory in the file repository.
	 *
	 * @param iconId
	 * 		the ID of the icon to delete
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see IconFile#getIconId()
	 */
	public void deleteIcon(long iconId) {
		asyncApi.deleteIcon(iconId).getUninterruptibly();
	}

	/**
	 * Deletes multiple icons from the icon directory in the file repository.
	 *
	 * @param iconIds
	 * 		the IDs of the icons to delete
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see IconFile#getIconId()
	 */
	public void deleteIcons(long... iconIds) {
		asyncApi.deleteIcons(iconIds).getUninterruptibly();
	}

	/**
	 * Deletes the offline message with the specified ID.
	 *
	 * @param messageId
	 * 		the ID of the offline message to delete
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Message#getId()
	 */
	public void deleteOfflineMessage(int messageId) {
		asyncApi.deleteOfflineMessage(messageId).getUninterruptibly();
	}

	/**
	 * Removes a specified permission from all server groups of the type specified by {@code type} on all virtual servers.
	 *
	 * @param type
	 * 		the kind of server group this permission should be removed from
	 * @param permName
	 * 		the name of the permission to remove
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroupType
	 * @see Permission#getName()
	 */
	public void deletePermissionFromAllServerGroups(ServerGroupType type, String permName) {
		asyncApi.deletePermissionFromAllServerGroups(type, permName).getUninterruptibly();
	}

	/**
	 * Deletes the privilege key with the given token.
	 *
	 * @param token
	 * 		the token of the privilege key
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see PrivilegeKey
	 */
	public void deletePrivilegeKey(String token) {
		asyncApi.deletePrivilegeKey(token).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see VirtualServer#getId()
	 * @see #stopServer(int)
	 */
	public void deleteServer(int serverId) {
		asyncApi.deleteServer(serverId).getUninterruptibly();
	}

	/**
	 * Deletes the server group with the specified ID, even if the server group still contains clients.
	 *
	 * @param groupId
	 * 		the ID of the server group
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 */
	public void deleteServerGroup(int groupId) {
		asyncApi.deleteServerGroup(groupId).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 */
	public void deleteServerGroup(int groupId, boolean force) {
		asyncApi.deleteServerGroup(groupId, force).getUninterruptibly();
	}

	/**
	 * Removes a permission from the server group with the given ID.
	 *
	 * @param groupId
	 * 		the ID of the server group
	 * @param permName
	 * 		the name of the permission to revoke
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 * @see Permission#getName()
	 */
	public void deleteServerGroupPermission(int groupId, String permName) {
		asyncApi.deleteServerGroupPermission(groupId, permName).getUninterruptibly();
	}

	/**
	 * Deletes the server query login with the specified client database ID.
	 * <p>
	 * If you only know the name of the server query login, use {@link #getServerQueryLoginsByName(String)} first.
	 * </p>
	 *
	 * @param clientDBId
	 * 		the client database ID of the server query login (usually the ID of the associated client)
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #addServerQueryLogin(String, int)
	 * @see #getServerQueryLogins()
	 * @see #updateServerQueryLogin(String)
	 */
	public void deleteServerQueryLogin(int clientDBId) {
		asyncApi.deleteServerQueryLogin(clientDBId).getUninterruptibly();
	}

	/**
	 * Restores the selected virtual servers configuration using the data from a
	 * previously created server snapshot.
	 *
	 * @param snapshot
	 * 		the snapshot to restore
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #createServerSnapshot()
	 */
	public void deployServerSnapshot(Snapshot snapshot) {
		asyncApi.deployServerSnapshot(snapshot).getUninterruptibly();
	}

	/**
	 * Restores the configuration of the selected virtual server using the data from a
	 * previously created server snapshot.
	 *
	 * @param snapshot
	 * 		the snapshot to restore
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #createServerSnapshot()
	 */
	public void deployServerSnapshot(String snapshot) {
		asyncApi.deployServerSnapshot(snapshot).getUninterruptibly();
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
	public long downloadFile(OutputStream dataOut, String filePath, int channelId) {
		return asyncApi.downloadFile(dataOut, filePath, channelId).getUninterruptibly();
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
	public long downloadFile(OutputStream dataOut, String filePath, int channelId, String channelPassword) {
		return asyncApi.downloadFile(dataOut, filePath, channelId, channelPassword).getUninterruptibly();
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
	public byte[] downloadFileDirect(String filePath, int channelId) {
		return asyncApi.downloadFileDirect(filePath, channelId).getUninterruptibly();
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
	public byte[] downloadFileDirect(String filePath, int channelId, String channelPassword) {
		return asyncApi.downloadFileDirect(filePath, channelId, channelPassword).getUninterruptibly();
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
	public long downloadIcon(OutputStream dataOut, long iconId) {
		return asyncApi.downloadIcon(dataOut, iconId).getUninterruptibly();
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
	public byte[] downloadIconDirect(long iconId) {
		return asyncApi.downloadIconDirect(iconId).getUninterruptibly();
	}

	/**
	 * Changes a channel's configuration using the given properties.
	 *
	 * @param channelId
	 * 		the ID of the channel to edit
	 * @param options
	 * 		the map of properties to modify
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 */
	public void editChannel(int channelId, Map<ChannelProperty, String> options) {
		asyncApi.editChannel(channelId, options).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see #editChannel(int, Map)
	 */
	public void editChannel(int channelId, ChannelProperty property, String value) {
		asyncApi.editChannel(channelId, property, value).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #updateClient(Map)
	 */
	public void editClient(int clientId, Map<ClientProperty, String> options) {
		asyncApi.editClient(clientId, options).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #editClient(int, Map)
	 * @see #updateClient(Map)
	 */
	public void editClient(int clientId, ClientProperty property, String value) {
		asyncApi.editClient(clientId, property, value).getUninterruptibly();
	}

	/**
	 * Changes a client's database settings using given properties.
	 *
	 * @param clientDBId
	 * 		the database ID of the client to edit
	 * @param options
	 * 		the map of properties to modify
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see DatabaseClientInfo
	 * @see Client#getDatabaseId()
	 */
	public void editDatabaseClient(int clientDBId, Map<ClientProperty, String> options) {
		asyncApi.editDatabaseClient(clientDBId, options).getUninterruptibly();
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
	 * @throws IllegalArgumentException
	 * 		if {@code property} is not changeable
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerInstanceProperty#isChangeable()
	 */
	public void editInstance(ServerInstanceProperty property, String value) {
		asyncApi.editInstance(property, value).getUninterruptibly();
	}

	/**
	 * Changes the configuration of the selected virtual server using given properties.
	 *
	 * @param options
	 * 		the map of properties to edit
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see VirtualServerProperty
	 */
	public void editServer(Map<VirtualServerProperty, String> options) {
		asyncApi.editServer(options).getUninterruptibly();
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
	public List<Ban> getBans() {
		return asyncApi.getBans().getUninterruptibly();
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
	public List<Binding> getBindings() {
		return asyncApi.getBindings().getUninterruptibly();
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
	public Channel getChannelByNameExact(String name, boolean ignoreCase) {
		return asyncApi.getChannelByNameExact(name, ignoreCase).getUninterruptibly();
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
	public List<Channel> getChannelsByName(String name) {
		return asyncApi.getChannelsByName(name).getUninterruptibly();
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
	public List<Permission> getChannelClientPermissions(int channelId, int clientDBId) {
		return asyncApi.getChannelClientPermissions(channelId, clientDBId).getUninterruptibly();
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
	public List<ChannelGroupClient> getChannelGroupClients(int channelId, int clientDBId, int groupId) {
		return asyncApi.getChannelGroupClients(channelId, clientDBId, groupId).getUninterruptibly();
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
	public List<ChannelGroupClient> getChannelGroupClientsByChannelGroupId(int groupId) {
		return asyncApi.getChannelGroupClientsByChannelGroupId(groupId).getUninterruptibly();
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
	public List<ChannelGroupClient> getChannelGroupClientsByChannelId(int channelId) {
		return asyncApi.getChannelGroupClientsByChannelId(channelId).getUninterruptibly();
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
	public List<ChannelGroupClient> getChannelGroupClientsByClientDBId(int clientDBId) {
		return asyncApi.getChannelGroupClientsByClientDBId(clientDBId).getUninterruptibly();
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
	public List<Permission> getChannelGroupPermissions(int groupId) {
		return asyncApi.getChannelGroupPermissions(groupId).getUninterruptibly();
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
	public List<ChannelGroup> getChannelGroups() {
		return asyncApi.getChannelGroups().getUninterruptibly();
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
	public ChannelInfo getChannelInfo(int channelId) {
		return asyncApi.getChannelInfo(channelId).getUninterruptibly();
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
	public List<Permission> getChannelPermissions(int channelId) {
		return asyncApi.getChannelPermissions(channelId).getUninterruptibly();
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
	public List<Channel> getChannels() {
		return asyncApi.getChannels().getUninterruptibly();
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
	public Client getClientByNameExact(String name, boolean ignoreCase) {
		return asyncApi.getClientByNameExact(name, ignoreCase).getUninterruptibly();
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
	public List<Client> getClientsByName(String name) {
		return asyncApi.getClientsByName(name).getUninterruptibly();
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
	public ClientInfo getClientByUId(String clientUId) {
		return asyncApi.getClientByUId(clientUId).getUninterruptibly();
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
	public ClientInfo getClientInfo(int clientId) {
		return asyncApi.getClientInfo(clientId).getUninterruptibly();
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
	public List<Permission> getClientPermissions(int clientDBId) {
		return asyncApi.getClientPermissions(clientDBId).getUninterruptibly();
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
	public List<Client> getClients() {
		return asyncApi.getClients().getUninterruptibly();
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
	public List<Complaint> getComplaints() {
		return asyncApi.getComplaints().getUninterruptibly();
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
	public List<Complaint> getComplaints(int clientDBId) {
		return asyncApi.getComplaints(clientDBId).getUninterruptibly();
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
	public ConnectionInfo getConnectionInfo() {
		return asyncApi.getConnectionInfo().getUninterruptibly();
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
	public Map<String, String> getCustomClientProperties(int clientDBId) {
		return asyncApi.getCustomClientProperties(clientDBId).getUninterruptibly();
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
	public List<DatabaseClientInfo> getDatabaseClientsByName(String name) {
		return asyncApi.getDatabaseClientsByName(name).getUninterruptibly();
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
	public DatabaseClientInfo getDatabaseClientByUId(String clientUId) {
		return asyncApi.getDatabaseClientByUId(clientUId).getUninterruptibly();
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
	public DatabaseClientInfo getDatabaseClientInfo(int clientDBId) {
		return asyncApi.getDatabaseClientInfo(clientDBId).getUninterruptibly();
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
	public List<DatabaseClient> getDatabaseClients() {
		return asyncApi.getDatabaseClients().getUninterruptibly();
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
	public List<DatabaseClient> getDatabaseClients(int offset, int count) {
		return asyncApi.getDatabaseClients(offset, count).getUninterruptibly();
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
	public FileInfo getFileInfo(String filePath, int channelId) {
		return asyncApi.getFileInfo(filePath, channelId).getUninterruptibly();
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
	public FileInfo getFileInfo(String filePath, int channelId, String channelPassword) {
		return asyncApi.getFileInfo(filePath, channelId, channelPassword).getUninterruptibly();
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
	public List<FileInfo> getFileInfos(String[] filePaths, int channelId) {
		return asyncApi.getFileInfos(filePaths, channelId).getUninterruptibly();
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
	public List<FileInfo> getFileInfos(String[] filePaths, int channelId, String channelPassword) {
		return asyncApi.getFileInfos(filePaths, channelId, channelPassword).getUninterruptibly();
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
	public List<FileInfo> getFileInfos(String[] filePaths, int[] channelIds, String[] channelPasswords) {
		return asyncApi.getFileInfos(filePaths, channelIds, channelPasswords).getUninterruptibly();
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
	public List<FileListEntry> getFileList(String directoryPath, int channelId) {
		return asyncApi.getFileList(directoryPath, channelId).getUninterruptibly();
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
	public List<FileListEntry> getFileList(String directoryPath, int channelId, String channelPassword) {
		return asyncApi.getFileList(directoryPath, channelId, channelPassword).getUninterruptibly();
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
	public List<FileTransfer> getFileTransfers() {
		return asyncApi.getFileTransfers().getUninterruptibly();
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
	public HostInfo getHostInfo() {
		return asyncApi.getHostInfo().getUninterruptibly();
	}

	/**
	 * Gets a list of all icon files on this virtual server.
	 *
	 * @return a list of all icons
	 */
	public List<IconFile> getIconList() {
		return asyncApi.getIconList().getUninterruptibly();
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
	public InstanceInfo getInstanceInfo() {
		return asyncApi.getInstanceInfo().getUninterruptibly();
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
	public List<String> getInstanceLogEntries(int lines) {
		return asyncApi.getInstanceLogEntries(lines).getUninterruptibly();
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
	public List<String> getInstanceLogEntries() {
		return asyncApi.getInstanceLogEntries().getUninterruptibly();
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
	public String getOfflineMessage(int messageId) {
		return asyncApi.getOfflineMessage(messageId).getUninterruptibly();
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
	public String getOfflineMessage(Message message) {
		return asyncApi.getOfflineMessage(message).getUninterruptibly();
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
	public List<Message> getOfflineMessages() {
		return asyncApi.getOfflineMessages().getUninterruptibly();
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
	public List<PermissionAssignment> getPermissionAssignments(String permName) {
		return asyncApi.getPermissionAssignments(permName).getUninterruptibly();
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
	public int getPermissionIdByName(String permName) {
		return asyncApi.getPermissionIdByName(permName).getUninterruptibly();
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
	public int[] getPermissionIdsByName(String... permNames) {
		return asyncApi.getPermissionIdsByName(permNames).getUninterruptibly();
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
	public List<PermissionAssignment> getPermissionOverview(int channelId, int clientDBId) {
		return asyncApi.getPermissionOverview(channelId, clientDBId).getUninterruptibly();
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
	public List<PermissionInfo> getPermissions() {
		return asyncApi.getPermissions().getUninterruptibly();
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
	public int getPermissionValue(String permName) {
		return asyncApi.getPermissionValue(permName).getUninterruptibly();
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
	public int[] getPermissionValues(String... permNames) {
		return asyncApi.getPermissionValues(permNames).getUninterruptibly();
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
	public List<PrivilegeKey> getPrivilegeKeys() {
		return asyncApi.getPrivilegeKeys().getUninterruptibly();
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
	public List<ServerGroupClient> getServerGroupClients(int serverGroupId) {
		return asyncApi.getServerGroupClients(serverGroupId).getUninterruptibly();
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
	public List<ServerGroupClient> getServerGroupClients(ServerGroup serverGroup) {
		return asyncApi.getServerGroupClients(serverGroup).getUninterruptibly();
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
	public List<Permission> getServerGroupPermissions(int serverGroupId) {
		return asyncApi.getServerGroupPermissions(serverGroupId).getUninterruptibly();
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
	public List<Permission> getServerGroupPermissions(ServerGroup serverGroup) {
		return asyncApi.getServerGroupPermissions(serverGroup).getUninterruptibly();
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
	public List<ServerGroup> getServerGroups() {
		return asyncApi.getServerGroups().getUninterruptibly();
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
	public List<ServerGroup> getServerGroupsByClientId(int clientDatabaseId) {
		return asyncApi.getServerGroupsByClientId(clientDatabaseId).getUninterruptibly();
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
	public List<ServerGroup> getServerGroupsByClient(Client client) {
		return asyncApi.getServerGroupsByClient(client).getUninterruptibly();
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
	public int getServerIdByPort(int port) {
		return asyncApi.getServerIdByPort(port).getUninterruptibly();
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
	public VirtualServerInfo getServerInfo() {
		return asyncApi.getServerInfo().getUninterruptibly();
	}

	/**
	 * Gets a list of all server query logins (containing login name, virtual server ID, and client database ID).
	 * If a virtual server is selected, only the server query logins of the selected virtual server are returned.
	 *
	 * @return a list of {@code QueryLogin} objects describing existing server query logins
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #addServerQueryLogin(String, int)
	 * @see #deleteServerQueryLogin(int)
	 * @see #getServerQueryLoginsByName(String)
	 * @see #updateServerQueryLogin(String)
	 */
	public List<QueryLogin> getServerQueryLogins() {
		return asyncApi.getServerQueryLogins().getUninterruptibly();
	}

	/**
	 * Gets a list of all server query logins (containing login name, virtual server ID, and client database ID)
	 * whose login name matches the specified SQL-like pattern.
	 * If a virtual server is selected, only the server query logins of the selected virtual server are returned.
	 *
	 * @param pattern
	 * 		the SQL-like pattern to match the server query login name against
	 *
	 * @return a list of {@code QueryLogin} objects describing existing server query logins
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #addServerQueryLogin(String, int)
	 * @see #deleteServerQueryLogin(int)
	 * @see #getServerQueryLogins()
	 * @see #updateServerQueryLogin(String)
	 */
	public List<QueryLogin> getServerQueryLoginsByName(String pattern) {
		return asyncApi.getServerQueryLoginsByName(pattern).getUninterruptibly();
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
	public Version getVersion() {
		return asyncApi.getVersion().getUninterruptibly();
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
	public List<VirtualServer> getVirtualServers() {
		return asyncApi.getVirtualServers().getUninterruptibly();
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
	public List<String> getVirtualServerLogEntries(int lines) {
		return asyncApi.getVirtualServerLogEntries(lines).getUninterruptibly();
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
	public List<String> getVirtualServerLogEntries() {
		return asyncApi.getVirtualServerLogEntries().getUninterruptibly();
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
	public boolean isClientOnline(int clientId) {
		return asyncApi.isClientOnline(clientId).getUninterruptibly();
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
	public boolean isClientOnline(String clientUId) {
		return asyncApi.isClientOnline(clientUId).getUninterruptibly();
	}

	/**
	 * Kicks one or more clients from their current channels.
	 * This will move the kicked clients into the default channel and
	 * won't do anything if the clients are already in the default channel.
	 *
	 * @param clientIds
	 * 		the IDs of the clients to kick
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #kickClientFromChannel(Client...)
	 * @see #kickClientFromChannel(String, int...)
	 */
	public void kickClientFromChannel(int... clientIds) {
		asyncApi.kickClientFromChannel(clientIds).getUninterruptibly();
	}

	/**
	 * Kicks one or more clients from their current channels.
	 * This will move the kicked clients into the default channel and
	 * won't do anything if the clients are already in the default channel.
	 *
	 * @param clients
	 * 		the clients to kick
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #kickClientFromChannel(int...)
	 * @see #kickClientFromChannel(String, Client...)
	 */
	public void kickClientFromChannel(Client... clients) {
		asyncApi.kickClientFromChannel(clients).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #kickClientFromChannel(int...)
	 * @see #kickClientFromChannel(String, Client...)
	 */
	public void kickClientFromChannel(String message, int... clientIds) {
		asyncApi.kickClientFromChannel(message, clientIds).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #kickClientFromChannel(Client...)
	 * @see #kickClientFromChannel(String, int...)
	 */
	public void kickClientFromChannel(String message, Client... clients) {
		asyncApi.kickClientFromChannel(message, clients).getUninterruptibly();
	}

	/**
	 * Kicks one or more clients from the server.
	 *
	 * @param clientIds
	 * 		the IDs of the clients to kick
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #kickClientFromServer(Client...)
	 * @see #kickClientFromServer(String, int...)
	 */
	public void kickClientFromServer(int... clientIds) {
		asyncApi.kickClientFromServer(clientIds).getUninterruptibly();
	}

	/**
	 * Kicks one or more clients from the server.
	 *
	 * @param clients
	 * 		the clients to kick
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #kickClientFromServer(int...)
	 * @see #kickClientFromServer(String, Client...)
	 */
	public void kickClientFromServer(Client... clients) {
		asyncApi.kickClientFromServer(clients).getUninterruptibly();
	}

	/**
	 * Kicks one or more clients from the server for the specified reason.
	 *
	 * @param message
	 * 		the reason message to display to the clients
	 * @param clientIds
	 * 		the IDs of the clients to kick
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see #kickClientFromServer(int...)
	 * @see #kickClientFromServer(String, Client...)
	 */
	public void kickClientFromServer(String message, int... clientIds) {
		asyncApi.kickClientFromServer(message, clientIds).getUninterruptibly();
	}

	/**
	 * Kicks one or more clients from the server for the specified reason.
	 *
	 * @param message
	 * 		the reason message to display to the clients
	 * @param clients
	 * 		the clients to kick
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #kickClientFromServer(Client...)
	 * @see #kickClientFromServer(String, int...)
	 */
	public void kickClientFromServer(String message, Client... clients) {
		asyncApi.kickClientFromServer(message, clients).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #logout()
	 */
	public void login(String username, String password) {
		asyncApi.login(username, password).getUninterruptibly();
	}

	/**
	 * Logs the server query out and deselects the current virtual server.
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #login(String, String)
	 */
	public void logout() {
		asyncApi.logout().getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see #moveChannel(int, int, int)
	 */
	public void moveChannel(int channelId, int channelTargetId) {
		asyncApi.moveChannel(channelId, channelTargetId).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see #moveChannel(int, int)
	 */
	public void moveChannel(int channelId, int channelTargetId, int order) {
		asyncApi.moveChannel(channelId, channelTargetId, order).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see Channel#getId()
	 */
	public void moveClient(int clientId, int channelId) {
		asyncApi.moveClient(clientId, channelId).getUninterruptibly();
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
	 * @throws IllegalArgumentException
	 * 		if {@code clientIds} is {@code null}
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see Channel#getId()
	 */
	public void moveClients(int[] clientIds, int channelId) {
		asyncApi.moveClients(clientIds, channelId).getUninterruptibly();
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
	 * @throws IllegalArgumentException
	 * 		if {@code client} or {@code channel} is {@code null}
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public void moveClient(Client client, ChannelBase channel) {
		asyncApi.moveClient(client, channel).getUninterruptibly();
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
	 * @throws IllegalArgumentException
	 * 		if {@code clients} or {@code channel} is {@code null}
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public void moveClients(Client[] clients, ChannelBase channel) {
		asyncApi.moveClients(clients, channel).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see Channel#getId()
	 */
	public void moveClient(int clientId, int channelId, String channelPassword) {
		asyncApi.moveClient(clientId, channelId, channelPassword).getUninterruptibly();
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
	 * @throws IllegalArgumentException
	 * 		if {@code clientIds} is {@code null}
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 * @see Channel#getId()
	 */
	public void moveClients(int[] clientIds, int channelId, String channelPassword) {
		asyncApi.moveClients(clientIds, channelId, channelPassword).getUninterruptibly();
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
	 * @throws IllegalArgumentException
	 * 		if {@code client} or {@code channel} is {@code null}
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public void moveClient(Client client, ChannelBase channel, String channelPassword) {
		asyncApi.moveClient(client, channel, channelPassword).getUninterruptibly();
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
	 * @throws IllegalArgumentException
	 * 		if {@code clients} or {@code channel} is {@code null}
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public void moveClients(Client[] clients, ChannelBase channel, String channelPassword) {
		asyncApi.moveClients(clients, channel, channelPassword).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #moveFile(String, String, int, int) moveFile to a different channel
	 */
	public void moveFile(String oldPath, String newPath, int channelId) {
		asyncApi.moveFile(oldPath, newPath, channelId).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #moveFile(String, String, int) moveFile within the same channel
	 */
	public void moveFile(String oldPath, String newPath, int oldChannelId, int newChannelId) {
		asyncApi.moveFile(oldPath, newPath, oldChannelId, newChannelId).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #moveFile(String, String, int, String, int, String) moveFile to a different channel
	 */
	public void moveFile(String oldPath, String newPath, int channelId, String channelPassword) {
		asyncApi.moveFile(oldPath, newPath, channelId, channelPassword).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #moveFile(String, String, int, String) moveFile within the same channel
	 */
	public void moveFile(String oldPath, String newPath, int oldChannelId, String oldPassword, int newChannelId, String newPassword) {
		asyncApi.moveFile(oldPath, newPath, oldChannelId, oldPassword, newChannelId, newPassword).getUninterruptibly();
	}

	/**
	 * Moves the server query into a channel.
	 *
	 * @param channelId
	 * 		the ID of the channel to move the server query into
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 */
	public void moveQuery(int channelId) {
		asyncApi.moveQuery(channelId).getUninterruptibly();
	}

	/**
	 * Moves the server query into a channel.
	 *
	 * @param channel
	 * 		the channel to move the server query into, cannot be {@code null}
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code channel} is {@code null}
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public void moveQuery(ChannelBase channel) {
		asyncApi.moveQuery(channel).getUninterruptibly();
	}

	/**
	 * Moves the server query into a channel using the specified password.
	 *
	 * @param channelId
	 * 		the ID of the channel to move the client into
	 * @param channelPassword
	 * 		the password of the channel, can be {@code null}
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 */
	public void moveQuery(int channelId, String channelPassword) {
		asyncApi.moveQuery(channelId, channelPassword).getUninterruptibly();
	}

	/**
	 * Moves the server query into a channel using the specified password.
	 *
	 * @param channel
	 * 		the channel to move the client into, cannot be {@code null}
	 * @param channelPassword
	 * 		the password of the channel, can be {@code null}
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code channel} is {@code null}
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public void moveQuery(ChannelBase channel, String channelPassword) {
		asyncApi.moveQuery(channel, channelPassword).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 */
	public void pokeClient(int clientId, String message) {
		asyncApi.pokeClient(clientId, message).getUninterruptibly();
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
	void quit() {
		asyncApi.quit().getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 6
	 * @see #addTS3Listeners(TS3Listener...)
	 */
	public void registerAllEvents() {
		asyncApi.registerAllEvents().getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #addTS3Listeners(TS3Listener...)
	 * @see #registerEvent(TS3EventType, int)
	 * @see #registerAllEvents()
	 */
	public void registerEvent(TS3EventType eventType) {
		asyncApi.registerEvent(eventType).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Channel#getId()
	 * @see #addTS3Listeners(TS3Listener...)
	 * @see #registerAllEvents()
	 */
	public void registerEvent(TS3EventType eventType, int channelId) {
		asyncApi.registerEvent(eventType, channelId).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands n, one command per TS3EventType
	 * @see #addTS3Listeners(TS3Listener...)
	 * @see #registerEvent(TS3EventType, int)
	 * @see #registerAllEvents()
	 */
	public void registerEvents(TS3EventType... eventTypes) {
		asyncApi.registerEvents(eventTypes).getUninterruptibly();
	}

	/**
	 * Removes the client specified by its database ID from the specified server group.
	 *
	 * @param serverGroupId
	 * 		the ID of the server group
	 * @param clientDatabaseId
	 * 		the database ID of the client
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 * @see Client#getDatabaseId()
	 * @see #removeClientFromServerGroup(ServerGroup, Client)
	 */
	public void removeClientFromServerGroup(int serverGroupId, int clientDatabaseId) {
		asyncApi.removeClientFromServerGroup(serverGroupId, clientDatabaseId).getUninterruptibly();
	}

	/**
	 * Removes the specified client from the specified server group.
	 *
	 * @param serverGroup
	 * 		the server group to remove the client from
	 * @param client
	 * 		the client to remove from the server group
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #removeClientFromServerGroup(int, int)
	 */
	public void removeClientFromServerGroup(ServerGroup serverGroup, Client client) {
		asyncApi.removeClientFromServerGroup(serverGroup, client).getUninterruptibly();
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
		asyncApi.removeTS3Listeners(listeners);
	}

	/**
	 * Renames the channel group with the specified ID.
	 *
	 * @param channelGroupId
	 * 		the ID of the channel group to rename
	 * @param name
	 * 		the new name for the channel group
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 * @see #renameChannelGroup(ChannelGroup, String)
	 */
	public void renameChannelGroup(int channelGroupId, String name) {
		asyncApi.renameChannelGroup(channelGroupId, name).getUninterruptibly();
	}

	/**
	 * Renames the specified channel group.
	 *
	 * @param channelGroup
	 * 		the channel group to rename
	 * @param name
	 * 		the new name for the channel group
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #renameChannelGroup(int, String)
	 */
	public void renameChannelGroup(ChannelGroup channelGroup, String name) {
		asyncApi.renameChannelGroup(channelGroup, name).getUninterruptibly();
	}

	/**
	 * Renames the server group with the specified ID.
	 *
	 * @param serverGroupId
	 * 		the ID of the server group to rename
	 * @param name
	 * 		the new name for the server group
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ServerGroup#getId()
	 * @see #renameServerGroup(ServerGroup, String)
	 */
	public void renameServerGroup(int serverGroupId, String name) {
		asyncApi.renameServerGroup(serverGroupId, name).getUninterruptibly();
	}

	/**
	 * Renames the specified server group.
	 *
	 * @param serverGroup
	 * 		the server group to rename
	 * @param name
	 * 		the new name for the server group
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #renameServerGroup(int, String)
	 */
	public void renameServerGroup(ServerGroup serverGroup, String name) {
		asyncApi.renameServerGroup(serverGroup, name).getUninterruptibly();
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
	public String resetPermissions() {
		return asyncApi.resetPermissions().getUninterruptibly();
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
	public List<CustomPropertyAssignment> searchCustomClientProperty(String key) {
		return asyncApi.searchCustomClientProperty(key).getUninterruptibly();
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
	public List<CustomPropertyAssignment> searchCustomClientProperty(String key, String valuePattern) {
		return asyncApi.searchCustomClientProperty(key, valuePattern).getUninterruptibly();
	}

	/**
	 * Moves the server query into the virtual server with the specified ID.
	 *
	 * @param id
	 * 		the ID of the virtual server
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see VirtualServer#getId()
	 * @see #selectVirtualServerById(int, String)
	 * @see #selectVirtualServerByPort(int)
	 * @see #selectVirtualServer(VirtualServer)
	 */
	public void selectVirtualServerById(int id) {
		asyncApi.selectVirtualServerById(id).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see VirtualServer#getId()
	 * @see #selectVirtualServerById(int)
	 * @see #selectVirtualServerByPort(int, String)
	 * @see #selectVirtualServer(VirtualServer, String)
	 */
	public void selectVirtualServerById(int id, String nickname) {
		asyncApi.selectVirtualServerById(id, nickname).getUninterruptibly();
	}

	/**
	 * Moves the server query into the virtual server with the specified voice port.
	 *
	 * @param port
	 * 		the voice port of the virtual server
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see VirtualServer#getPort()
	 * @see #selectVirtualServerById(int)
	 * @see #selectVirtualServerByPort(int, String)
	 * @see #selectVirtualServer(VirtualServer)
	 */
	public void selectVirtualServerByPort(int port) {
		asyncApi.selectVirtualServerByPort(port).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see VirtualServer#getPort()
	 * @see #selectVirtualServerById(int, String)
	 * @see #selectVirtualServerByPort(int)
	 * @see #selectVirtualServer(VirtualServer, String)
	 */
	public void selectVirtualServerByPort(int port, String nickname) {
		asyncApi.selectVirtualServerByPort(port, nickname).getUninterruptibly();
	}

	/**
	 * Moves the server query into the specified virtual server.
	 *
	 * @param server
	 * 		the virtual server to move into
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #selectVirtualServerById(int)
	 * @see #selectVirtualServerByPort(int)
	 * @see #selectVirtualServer(VirtualServer, String)
	 */
	public void selectVirtualServer(VirtualServer server) {
		asyncApi.selectVirtualServer(server).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #selectVirtualServerById(int, String)
	 * @see #selectVirtualServerByPort(int, String)
	 * @see #selectVirtualServer(VirtualServer)
	 */
	public void selectVirtualServer(VirtualServer server, String nickname) {
		asyncApi.selectVirtualServer(server, nickname).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getUniqueIdentifier()
	 * @see Message
	 */
	public void sendOfflineMessage(String clientUId, String subject, String message) {
		asyncApi.sendOfflineMessage(clientUId, subject, message).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 */
	public void sendTextMessage(TextMessageTargetMode targetMode, int targetId, String message) {
		asyncApi.sendTextMessage(targetMode, targetId, message).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #sendChannelMessage(String)
	 * @see Channel#getId()
	 */
	public void sendChannelMessage(int channelId, String message) {
		asyncApi.sendChannelMessage(channelId, message).getUninterruptibly();
	}

	/**
	 * Sends a text message to the channel the server query is currently in.
	 * Your message may contain BB codes, but its length is limited to 1024 UTF-8 bytes.
	 *
	 * @param message
	 * 		the text message to send
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public void sendChannelMessage(String message) {
		asyncApi.sendChannelMessage(message).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #sendServerMessage(String)
	 * @see VirtualServer#getId()
	 */
	public void sendServerMessage(int serverId, String message) {
		asyncApi.sendServerMessage(serverId, message).getUninterruptibly();
	}

	/**
	 * Sends a text message to the virtual server the server query is currently in.
	 * Your message may contain BB codes, but its length is limited to 1024 UTF-8 bytes.
	 *
	 * @param message
	 * 		the text message to send
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public void sendServerMessage(String message) {
		asyncApi.sendServerMessage(message).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getId()
	 */
	public void sendPrivateMessage(int clientId, String message) {
		asyncApi.sendPrivateMessage(clientId, message).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see ChannelGroup#getId()
	 * @see Channel#getId()
	 * @see Client#getDatabaseId()
	 */
	public void setClientChannelGroup(int groupId, int channelId, int clientDBId) {
		asyncApi.setClientChannelGroup(groupId, channelId, clientDBId).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands properties.size()
	 * @see Client#getDatabaseId()
	 * @see #setCustomClientProperty(int, String, String)
	 * @see #deleteCustomClientProperty(int, String)
	 */
	public void setCustomClientProperties(int clientDBId, Map<String, String> properties) {
		asyncApi.setCustomClientProperties(clientDBId, properties).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see Client#getDatabaseId()
	 * @see #setCustomClientProperties(int, Map)
	 * @see #deleteCustomClientProperty(int, String)
	 */
	public void setCustomClientProperty(int clientDBId, String key, String value) {
		asyncApi.setCustomClientProperty(clientDBId, key, value).getUninterruptibly();
	}

	/**
	 * Sets the read flag to {@code true} for a given message. This will not delete the message.
	 *
	 * @param messageId
	 * 		the ID of the message for which the read flag should be set
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #setMessageReadFlag(int, boolean)
	 */
	public void setMessageRead(int messageId) {
		asyncApi.setMessageRead(messageId).getUninterruptibly();
	}

	/**
	 * Sets the read flag to {@code true} for a given message. This will not delete the message.
	 *
	 * @param message
	 * 		the message for which the read flag should be set
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #setMessageRead(int)
	 * @see #setMessageReadFlag(Message, boolean)
	 * @see #deleteOfflineMessage(int)
	 */
	public void setMessageRead(Message message) {
		asyncApi.setMessageRead(message).getUninterruptibly();
	}

	/**
	 * Sets the read flag for a given message. This will not delete the message.
	 *
	 * @param messageId
	 * 		the ID of the message for which the read flag should be set
	 * @param read
	 * 		the boolean value to which the read flag should be set
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #setMessageRead(int)
	 * @see #setMessageReadFlag(Message, boolean)
	 * @see #deleteOfflineMessage(int)
	 */
	public void setMessageReadFlag(int messageId, boolean read) {
		asyncApi.setMessageReadFlag(messageId, read).getUninterruptibly();
	}

	/**
	 * Sets the read flag for a given message. This will not delete the message.
	 *
	 * @param message
	 * 		the message for which the read flag should be set
	 * @param read
	 * 		the boolean value to which the read flag should be set
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #setMessageRead(Message)
	 * @see #setMessageReadFlag(int, boolean)
	 * @see #deleteOfflineMessage(int)
	 */
	public void setMessageReadFlag(Message message, boolean read) {
		asyncApi.setMessageReadFlag(message, read).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #updateClient(Map)
	 */
	public void setNickname(String nickname) {
		asyncApi.setNickname(nickname).getUninterruptibly();
	}

	/**
	 * Starts the virtual server with the specified ID.
	 *
	 * @param serverId
	 * 		the ID of the virtual server
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public void startServer(int serverId) {
		asyncApi.startServer(serverId).getUninterruptibly();
	}

	/**
	 * Starts the specified virtual server.
	 *
	 * @param virtualServer
	 * 		the virtual server to start
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public void startServer(VirtualServer virtualServer) {
		asyncApi.startServer(virtualServer).getUninterruptibly();
	}

	/**
	 * Stops the virtual server with the specified ID.
	 *
	 * @param serverId
	 * 		the ID of the virtual server
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public void stopServer(int serverId) {
		asyncApi.stopServer(serverId).getUninterruptibly();
	}

	/**
	 * Stops the virtual server with the specified ID.
	 *
	 * @param serverId
	 * 		the ID of the virtual server
	 * @param reason
	 * 		the reason message to display to clients when they are disconnected
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public void stopServer(int serverId, String reason) {
		asyncApi.stopServer(serverId, reason).getUninterruptibly();
	}

	/**
	 * Stops the specified virtual server.
	 *
	 * @param virtualServer
	 * 		the virtual server to stop
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public void stopServer(VirtualServer virtualServer) {
		asyncApi.stopServer(virtualServer).getUninterruptibly();
	}

	/**
	 * Stops the specified virtual server.
	 *
	 * @param virtualServer
	 * 		the virtual server to stop
	 * @param reason
	 * 		the reason message to display to clients when they are disconnected
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public void stopServer(VirtualServer virtualServer, String reason) {
		asyncApi.stopServer(virtualServer, reason).getUninterruptibly();
	}

	/**
	 * Stops the entire TeamSpeak 3 Server instance by shutting down the process.
	 * <p>
	 * To have permission to use this command, you need to use the server query admin login.
	 * </p>
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public void stopServerProcess() {
		asyncApi.stopServerProcess().getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public void stopServerProcess(String reason) {
		asyncApi.stopServerProcess(reason).getUninterruptibly();
	}

	/**
	 * Unregisters the server query from receiving any event notifications.
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 */
	public void unregisterAllEvents() {
		asyncApi.unregisterAllEvents().getUninterruptibly();
	}

	/**
	 * Updates several client properties for this server query instance.
	 *
	 * @param options
	 * 		the map of properties to update
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #updateClient(ClientProperty, String)
	 * @see #editClient(int, Map)
	 */
	public void updateClient(Map<ClientProperty, String> options) {
		asyncApi.updateClient(options).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see #updateClient(Map)
	 * @see #editClient(int, Map)
	 */
	public void updateClient(ClientProperty property, String value) {
		asyncApi.updateClient(property, value).getUninterruptibly();
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
	 * @see #addServerQueryLogin(String, int)
	 * @see #deleteServerQueryLogin(int)
	 * @see #getServerQueryLogins()
	 */
	public String updateServerQueryLogin(String loginName) {
		return asyncApi.updateServerQueryLogin(loginName).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #uploadFileDirect(byte[], String, boolean, int, String)
	 */
	public void uploadFile(InputStream dataIn, long dataLength, String filePath, boolean overwrite, int channelId) {
		asyncApi.uploadFile(dataIn, dataLength, filePath, overwrite, channelId).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #uploadFileDirect(byte[], String, boolean, int, String)
	 */
	public void uploadFile(InputStream dataIn, long dataLength, String filePath, boolean overwrite, int channelId, String channelPassword) {
		asyncApi.uploadFile(dataIn, dataLength, filePath, overwrite, channelId, channelPassword).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #uploadFile(InputStream, long, String, boolean, int)
	 */
	public void uploadFileDirect(byte[] data, String filePath, boolean overwrite, int channelId) {
		asyncApi.uploadFileDirect(data, filePath, overwrite, channelId).getUninterruptibly();
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
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @throws TS3FileTransferFailedException
	 * 		if the file transfer fails for any reason
	 * @querycommands 1
	 * @see FileInfo#getPath()
	 * @see Channel#getId()
	 * @see #uploadFile(InputStream, long, String, boolean, int, String)
	 */
	public void uploadFileDirect(byte[] data, String filePath, boolean overwrite, int channelId, String channelPassword) {
		asyncApi.uploadFileDirect(data, filePath, overwrite, channelId, channelPassword).getUninterruptibly();
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
	public long uploadIcon(InputStream dataIn, long dataLength) {
		return asyncApi.uploadIcon(dataIn, dataLength).getUninterruptibly();
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
	public long uploadIconDirect(byte[] data) {
		return asyncApi.uploadIconDirect(data).getUninterruptibly();
	}

	/**
	 * Uses an existing privilege key to join a server or channel group.
	 *
	 * @param token
	 * 		the privilege key to use
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see PrivilegeKey
	 * @see #addPrivilegeKey(PrivilegeKeyType, int, int, String)
	 * @see #usePrivilegeKey(PrivilegeKey)
	 */
	public void usePrivilegeKey(String token) {
		asyncApi.usePrivilegeKey(token).getUninterruptibly();
	}

	/**
	 * Uses an existing privilege key to join a server or channel group.
	 *
	 * @param privilegeKey
	 * 		the privilege key to use
	 *
	 * @throws TS3CommandFailedException
	 * 		if the execution of a command fails
	 * @querycommands 1
	 * @see PrivilegeKey
	 * @see #addPrivilegeKey(PrivilegeKeyType, int, int, String)
	 * @see #usePrivilegeKey(String)
	 */
	public void usePrivilegeKey(PrivilegeKey privilegeKey) {
		asyncApi.usePrivilegeKey(privilegeKey).getUninterruptibly();
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
	public ServerQueryInfo whoAmI() {
		return asyncApi.whoAmI().getUninterruptibly();
	}
}
