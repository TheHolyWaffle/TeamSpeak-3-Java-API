package com.github.theholywaffle.teamspeak3.api.reconnect;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Query;

/**
 * Observes a query's connections. Callbacks run on query-owned threads and must cooperate with interruption.
 */
public interface ConnectionHandler {

    /**
     * Runs after the login and {@link SessionConfiguration} have been restored and before queued commands
     * continue, within the handshake deadline. A failure fails the connection attempt.
     *
     * @param api an API for initialization commands on this connection
     */
    void onConnect(TS3Api api);

    /**
     * Runs after an unexpected connection loss, not after {@code close()} or {@code exit()}.
     *
     * @param ts3Query the disconnected query
     */
    void onDisconnect(TS3Query ts3Query);
}
