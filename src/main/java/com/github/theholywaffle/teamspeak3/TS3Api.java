package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.VirtualServerId;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;

/**
 * Synchronous API of a RAW or SSH {@link TS3Query}: the shared {@link TS3BaseApi} plus the ServerQuery
 * session, login and virtual server selection.
 * <p>
 * Most commands require a login and a selected virtual server. Pass credentials to {@link TS3Config}
 * or call {@link #login(String, String)}, then {@link #selectVirtualServerById(VirtualServerId)}. Server errors throw
 * {@link TS3CommandFailedException}.
 * </p>
 *
 * @see TS3ApiAsync The asynchronous version of the API
 */
public class TS3Api extends TS3BaseApi {

    private final TS3ApiAsync asyncApi;

    TS3Api(TS3ApiAsync asyncApi) {
        super(asyncApi);
        this.asyncApi = asyncApi;
    }

    /**
     * Logs in to the ServerQuery interface. RAW connections can instead log in automatically
     * with {@link TS3Config#setLoginCredentials(String, String)}; SSH always uses those credentials.
     *
     * @param username the query login name
     * @param password the query login password
     */
    public void login(String username, String password) {
        CommandFutures.join(asyncApi.login(username, password));
    }

    /** Logs out, deselecting the virtual server. */
    public void logout() {
        CommandFutures.join(asyncApi.logout());
    }

    /**
     * Selects the virtual server that subsequent commands apply to.
     *
     * @param id the virtual server ID
     */
    public void selectVirtualServerById(VirtualServerId id) {
        CommandFutures.join(asyncApi.selectVirtualServerById(id));
    }

    /**
     * Selects a virtual server and sets this query client's nickname on it.
     *
     * @param id the virtual server ID
     * @param nickname the nickname, or {@code null} to keep the default
     */
    public void selectVirtualServerById(VirtualServerId id, String nickname) {
        CommandFutures.join(asyncApi.selectVirtualServerById(id, nickname));
    }

    /**
     * Selects the virtual server listening on a voice port.
     *
     * @param port the UDP voice port
     */
    public void selectVirtualServerByPort(int port) {
        CommandFutures.join(asyncApi.selectVirtualServerByPort(port));
    }

    /**
     * Selects the virtual server listening on a voice port and sets this query client's nickname.
     *
     * @param port the UDP voice port
     * @param nickname the nickname, or {@code null} to keep the default
     */
    public void selectVirtualServerByPort(int port, String nickname) {
        CommandFutures.join(asyncApi.selectVirtualServerByPort(port, nickname));
    }

    /**
     * Deselects the virtual server, returning this session to the server instance as right after login,
     * for example to write instance log entries. Sends {@code use sid=0}. Like selecting a server, it is
     * rejected while a reconnect strategy restores a
     * {@link com.github.theholywaffle.teamspeak3.api.reconnect.SessionConfiguration}.
     */
    public void deselectVirtualServer() {
        CommandFutures.join(asyncApi.deselectVirtualServer());
    }
}
