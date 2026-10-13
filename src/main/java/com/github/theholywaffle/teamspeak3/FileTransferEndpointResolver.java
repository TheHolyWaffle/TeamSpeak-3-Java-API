package com.github.theholywaffle.teamspeak3;

import java.net.InetSocketAddress;
import java.util.Set;

/**
 * Maps only the separate, plaintext TS3 file socket endpoint. Does not change query transport,
 * credentials or transfer keys. Implementations must be thread safe and return promptly.
 */
@FunctionalInterface
public interface FileTransferEndpointResolver {
    /**
     * @param queryHost configured query host, used when the advertised address is unspecified
     * @param advertisedHost complete server address field, possibly empty or comma separated
     * @param advertisedPort server-advertised file port
     * @return reachable file endpoint, including a NAT or Docker mapped port when necessary
     */
    InetSocketAddress resolve(String queryHost, String advertisedHost, int advertisedPort);

    /** @return resolver selecting the first advertised address, falling back for wildcard/empty addresses */
    static FileTransferEndpointResolver advertised() {
        return (queryHost, hosts, port) -> {
            String host = hosts == null ? "" : hosts.split(",", -1)[0].trim();
            if (host.isEmpty()
                    || Set.of("0.0.0.0", "::", "[::]", "0:0:0:0:0:0:0:0").contains(host)) host = queryHost;
            return new InetSocketAddress(host, port);
        };
    }
}
