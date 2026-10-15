package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.VirtualServerId;
import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;
import com.github.theholywaffle.teamspeak3.api.VirtualServerStatus;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public class VirtualServer extends Wrapper {

    public VirtualServer(Map<String, String> map) {
        super(map);
    }

    public VirtualServerId getId() {
        return requireId(VirtualServerProperty.VIRTUALSERVER_ID, VirtualServerId::new);
    }

    public int getPort() {
        return requireInt(VirtualServerProperty.VIRTUALSERVER_PORT);
    }

    /** @return the status, or {@link VirtualServerStatus#UNKNOWN} for a status this library does not know */
    public VirtualServerStatus getStatus() {
        return VirtualServerStatus.fromName(require(VirtualServerProperty.VIRTUALSERVER_STATUS));
    }

    /** @return the number of connected clients, or empty for a server that is not running */
    public OptionalInt getClientsOnline() {
        return findInt(VirtualServerProperty.VIRTUALSERVER_CLIENTSONLINE);
    }

    /** @return the number of connected query clients, or empty for a server that is not running */
    public OptionalInt getQueryClientsOnline() {
        return findInt(VirtualServerProperty.VIRTUALSERVER_QUERYCLIENTSONLINE);
    }

    /** @return the client limit, or empty for a server that is not running */
    public OptionalInt getMaxClients() {
        return findInt(VirtualServerProperty.VIRTUALSERVER_MAXCLIENTS);
    }

    public String getUniqueIdentifier() {
        return require(VirtualServerProperty.VIRTUALSERVER_UNIQUE_IDENTIFIER);
    }

    /** @return the uptime, or empty for a server that is not running */
    public Optional<Duration> getUptime() {
        return findLong(VirtualServerProperty.VIRTUALSERVER_UPTIME).stream()
                .mapToObj(Duration::ofSeconds)
                .findFirst();
    }

    public String getName() {
        return require(VirtualServerProperty.VIRTUALSERVER_NAME);
    }

    public boolean isAutoStart() {
        return requireBoolean(VirtualServerProperty.VIRTUALSERVER_AUTOSTART);
    }
}
