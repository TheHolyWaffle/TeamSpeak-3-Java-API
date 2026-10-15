package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.VirtualServerId;
import java.util.Objects;

/**
 * A newly created virtual server, from {@code servercreate}.
 *
 * @param id the virtual server ID
 * @param port the voice port
 * @param serverAdminToken the privilege key granting the server admin group
 */
public record CreatedVirtualServer(VirtualServerId id, int port, String serverAdminToken) {

    public CreatedVirtualServer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(serverAdminToken, "serverAdminToken");
    }

    /** @throws com.github.theholywaffle.teamspeak3.api.exception.TS3InvalidValueException if a property is missing or malformed */
    public static CreatedVirtualServer from(Wrapper row) {
        return new CreatedVirtualServer(
                row.requireId("sid", VirtualServerId::new), row.requireInt("virtualserver_port"), row.require("token"));
    }
}
