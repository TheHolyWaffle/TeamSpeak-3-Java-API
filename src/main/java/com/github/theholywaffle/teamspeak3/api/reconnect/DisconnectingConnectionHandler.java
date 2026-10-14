package com.github.theholywaffle.teamspeak3.api.reconnect;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisconnectingConnectionHandler implements ConnectionHandler {

    private static final Logger log = LoggerFactory.getLogger(DisconnectingConnectionHandler.class);
    private final ConnectionHandler userConnectionHandler;

    public DisconnectingConnectionHandler(ConnectionHandler userConnectionHandler) {
        this.userConnectionHandler = userConnectionHandler;
    }

    @Override
    public void onConnect(TS3Api api) {
        if (userConnectionHandler != null) {
            userConnectionHandler.onConnect(api);
        }
    }

    @Override
    public void onDisconnect(TS3Query ts3Query) {
        log.error("[Connection] Disconnected from TS3 server");

        if (userConnectionHandler != null) {
            userConnectionHandler.onDisconnect(ts3Query);
        }
    }
}
