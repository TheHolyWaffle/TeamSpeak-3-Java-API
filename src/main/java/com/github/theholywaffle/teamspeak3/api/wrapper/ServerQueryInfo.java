package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;
import com.github.theholywaffle.teamspeak3.api.VirtualServerStatus;
import java.util.Map;
import java.util.Optional;

/**
 * A class containing information about a server query, returned by the
 * API methods {@link TS3Api#whoAmI()} and {@link TS3ApiAsync#whoAmI()}.
 */
public class ServerQueryInfo extends Wrapper {

    /**
     * Creates a new {@code ServerQueryInfo} from the information present in the provided map.
     *
     * @param map
     * 		the map containing the key-value pairs abstracted by this object
     */
    public ServerQueryInfo(Map<String, String> map) {
        super(map);
    }

    /**
     * Gets the ID of the channel the server query is currently in.
     *
     * @return the ID of the current channel
     */
    public int getChannelId() {
        return requireInt("client_channel_id");
    }

    /**
     * Gets the ID of the server query in the database.
     * <p>
     * In case this server query account was created by a client,
     * its database ID will be identical to the client's database ID.
     * </p>
     *
     * @return the server query's database ID, or empty before login (TeamSpeak sends {@code 0})
     */
    public Optional<ClientDatabaseId> getDatabaseId() {
        return findDatabaseId(ClientProperty.CLIENT_DATABASE_ID);
    }

    /**
     * Gets the client ID of the server query.
     *
     * @return the server query's client ID; {@code 0} until a virtual server is selected
     */
    public ClientId getId() {
        return requireClientId("client_id");
    }

    /**
     * Gets the username that was used when logging the query in (using a username-password combination).
     * <p>
     * This username was set when creating the server query login and doesn't have to be related to
     * the client who created the server query login.
     * </p>
     *
     * @return the username used when logging this query in, or empty before login
     *
     * @see TS3Api#login(String, String) TS3Api#login(username, password) - logging in server queries
     */
    public Optional<String> getLoginName() {
        return findText(ClientProperty.CLIENT_LOGIN_NAME);
    }

    /**
     * Gets the nickname currently used by the server query.
     * Unless explicitly set, the nickname will be formatted like
     * <pre><i>username</i> from <i>ip</i>:<i>port</i></pre>
     * <p>
     * Nicknames are only assigned after a virtual server has been selected.
     * Until then, TeamSpeak sends an empty nickname.
     * </p>
     *
     * @return the current nickname of the server query
     */
    public String getNickname() {
        return require(ClientProperty.CLIENT_NICKNAME);
    }

    /**
     * Gets the ID of the virtual server on which the server query login was created.
     * <p>
     * This method will return {@code 0} (the ID of the template server) if a server query is
     * not logged in or using the {@code serveradmin} login.
     * </p>
     *
     * @return the ID of the virtual server this server query belongs to
     */
    public int getOriginServerId() {
        return requireInt("client_origin_server_id");
    }

    /**
     * Gets the unique identifier of the server query.
     * <p>
     * In case this server query account was created by a client,
     * its unique ID will be identical to the client's unique ID.
     * </p>
     *
     * @return the server query's unique ID
     */
    public String getUniqueIdentifier() {
        return require(ClientProperty.CLIENT_UNIQUE_IDENTIFIER);
    }

    /**
     * Gets the ID of the currently selected virtual server.
     * <p>
     * If used on a non-commercial TeamSpeak instance which can only host 1 virtual server,
     * this ID will always be 1.
     * <br>
     * If no virtual server has been selected yet, this method will return {@code 0}.
     * </p>
     *
     * @return the ID of the current virtual server or {@code 0} if none is selected
     */
    public int getVirtualServerId() {
        return requireInt(VirtualServerProperty.VIRTUALSERVER_ID);
    }

    /**
     * Gets the port used by the currently selected virtual server.
     * <p>
     * If no virtual server has been selected yet, this method will return {@code 0}.
     * </p>
     *
     * @return the port used by the current virtual server or {@code 0} if none is selected
     */
    public int getVirtualServerPort() {
        return requireInt(VirtualServerProperty.VIRTUALSERVER_PORT);
    }

    /**
     * Gets the status of the currently selected virtual server.
     * <p>
     * If no virtual server has been selected yet, TeamSpeak reports the status {@code unknown},
     * which maps to {@link VirtualServerStatus#UNKNOWN}.
     * </p>
     *
     * @return the status of the current virtual server
     */
    public VirtualServerStatus getVirtualServerStatus() {
        return VirtualServerStatus.fromName(require(VirtualServerProperty.VIRTUALSERVER_STATUS));
    }

    /**
     * Gets the unique identifier of the currently selected virtual server.
     * <p>
     * If no virtual server has been selected yet, TeamSpeak sends an empty value.
     * </p>
     *
     * @return the unique ID of the current virtual server
     */
    public String getVirtualServerUniqueIdentifier() {
        return require(VirtualServerProperty.VIRTUALSERVER_UNIQUE_IDENTIFIER);
    }
}
