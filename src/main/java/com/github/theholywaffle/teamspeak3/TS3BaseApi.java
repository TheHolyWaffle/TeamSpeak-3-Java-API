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

import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerQueryInfo;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;
import java.util.Map;

/**
 * Synchronous API shared by {@link TS3Query} and {@link TS3WebQuery}: one domain API per administrative
 * area, for example {@code api.channels().list()}, plus operations on this query client. A
 * {@link TS3Query} returns the {@link TS3Api} subtype, which adds login and virtual server selection.
 * <p>
 * Every method blocks until the server answers, then returns the result or throws a
 * {@link com.github.theholywaffle.teamspeak3.api.exception.TS3Exception}:
 * </p>
 * <ul>
 * <li>{@link TS3CommandFailedException}: the server rejected the command;</li>
 * <li>{@link com.github.theholywaffle.teamspeak3.api.exception.TS3TransportException}: the connection is unavailable;</li>
 * <li>{@link com.github.theholywaffle.teamspeak3.api.exception.TS3TimeoutException}: a configured deadline expired;</li>
 * <li>{@link com.github.theholywaffle.teamspeak3.api.exception.TS3QueueFullException}: command capacity is exhausted;</li>
 * <li>{@link com.github.theholywaffle.teamspeak3.api.exception.TS3UnsupportedOperationException}: the transport or configuration does not allow the operation.</li>
 * </ul>
 * <p>
 * {@link com.github.theholywaffle.teamspeak3.api.exception.TS3Exception#isOutcomeUnknown()} tells whether a failed command
 * may still have executed. Lookups that may legitimately find nothing return an empty
 * {@link java.util.Optional} or list; no method returns {@code null} or {@code -1} to signal absence or failure.
 * Waiting is uninterruptible; it preserves the thread's interrupt flag.
 * </p><p>
 * Over Web Query, event registration and file operations fail with
 * {@link com.github.theholywaffle.teamspeak3.api.exception.TS3UnsupportedOperationException} without
 * sending a request.
 * </p>
 *
 * @see TS3BaseApiAsync The asynchronous version of the API
 */
public class TS3BaseApi {

    private final TS3BaseApiAsync asyncApi;
    private final ApiKeyApi apiKeys;
    private final BanApi bans;
    private final ChannelApi channels;
    private final ChannelGroupApi channelGroups;
    private final ClientApi clients;
    private final ComplaintApi complaints;
    private final DatabaseClientApi databaseClients;
    private final EventApi events;
    private final FileApi files;
    private final InstanceApi instance;
    private final MessageApi messages;
    private final PermissionApi permissions;
    private final PrivilegeKeyApi privilegeKeys;
    private final QueryLoginApi queryLogins;
    private final ServerGroupApi serverGroups;
    private final VirtualServerApi servers;

    TS3BaseApi(TS3BaseApiAsync asyncApi) {
        this.asyncApi = asyncApi;
        apiKeys = new ApiKeyApi(asyncApi.apiKeys());
        bans = new BanApi(asyncApi.bans());
        channels = new ChannelApi(asyncApi.channels());
        channelGroups = new ChannelGroupApi(asyncApi.channelGroups());
        clients = new ClientApi(asyncApi.clients());
        complaints = new ComplaintApi(asyncApi.complaints());
        databaseClients = new DatabaseClientApi(asyncApi.databaseClients());
        events = new EventApi(asyncApi.events());
        files = new FileApi(asyncApi.files());
        instance = new InstanceApi(asyncApi.instance());
        messages = new MessageApi(asyncApi.messages());
        permissions = new PermissionApi(asyncApi.permissions());
        privilegeKeys = new PrivilegeKeyApi(asyncApi.privilegeKeys());
        queryLogins = new QueryLoginApi(asyncApi.queryLogins());
        serverGroups = new ServerGroupApi(asyncApi.serverGroups());
        servers = new VirtualServerApi(asyncApi.servers());
    }

    /** @return Web Query API key administration */
    public ApiKeyApi apiKeys() {
        return apiKeys;
    }

    /** @return ban rule administration */
    public BanApi bans() {
        return bans;
    }

    /** @return channel administration */
    public ChannelApi channels() {
        return channels;
    }

    /** @return channel group administration */
    public ChannelGroupApi channelGroups() {
        return channelGroups;
    }

    /** @return administration of connected clients */
    public ClientApi clients() {
        return clients;
    }

    /** @return complaint administration */
    public ComplaintApi complaints() {
        return complaints;
    }

    /** @return administration of clients in the virtual server database */
    public DatabaseClientApi databaseClients() {
        return databaseClients;
    }

    /** @return server notification registration and local listeners */
    public EventApi events() {
        return events;
    }

    /** @return file repository access, transfers and icons */
    public FileApi files() {
        return files;
    }

    /** @return server instance administration */
    public InstanceApi instance() {
        return instance;
    }

    /** @return text and offline messages */
    public MessageApi messages() {
        return messages;
    }

    /** @return permission metadata and assignments */
    public PermissionApi permissions() {
        return permissions;
    }

    /** @return privilege key administration */
    public PrivilegeKeyApi privilegeKeys() {
        return privilegeKeys;
    }

    /** @return ServerQuery login administration */
    public QueryLoginApi queryLogins() {
        return queryLogins;
    }

    /** @return server group administration */
    public ServerGroupApi serverGroups() {
        return serverGroups;
    }

    /** @return virtual server lifecycle and administration of the selected virtual server */
    public VirtualServerApi servers() {
        return servers;
    }

    /**
     * Executes one already-encoded ServerQuery command using the normal bounded queue.
     *
     * @param line one command without control characters
     * @return the decoded rows and original response text
     * @throws IllegalArgumentException if the command line is invalid
     * @throws TS3CommandFailedException if the server rejects the command
     * @see TS3ApiAsync#executeRawCommand(String)
     */
    public DefaultArrayResponse executeRawCommand(String line) {
        return CommandFutures.join(asyncApi.executeRawCommand(line));
    }

    /** @return this query client's session: login, selected server, channel and IDs */
    public ServerQueryInfo whoAmI() {
        return CommandFutures.join(asyncApi.whoAmI());
    }

    /**
     * Changes this query client's nickname on the selected virtual server.
     *
     * @param nickname the new nickname
     */
    public void setNickname(String nickname) {
        CommandFutures.join(asyncApi.setNickname(nickname));
    }

    /**
     * Changes properties of this query client. Use {@link ClientApi#edit(com.github.theholywaffle.teamspeak3.api.ClientId, Map)} for other clients.
     *
     * @param properties the properties to change
     */
    public void updateClient(Map<ClientProperty, String> properties) {
        CommandFutures.join(asyncApi.updateClient(properties));
    }

    /**
     * Moves this query client to another channel.
     *
     * @param channelId the target channel
     */
    public void moveQuery(int channelId) {
        CommandFutures.join(asyncApi.moveQuery(channelId));
    }

    /**
     * Moves this query client to a password-protected channel.
     *
     * @param channelId the target channel
     * @param channelPassword the channel password, or {@code null}
     */
    public void moveQuery(int channelId, String channelPassword) {
        CommandFutures.join(asyncApi.moveQuery(channelId, channelPassword));
    }
}
