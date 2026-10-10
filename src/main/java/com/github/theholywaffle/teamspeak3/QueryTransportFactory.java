package com.github.theholywaffle.teamspeak3;

/** Creates fresh, independently closeable transport resources on every connection/reconnect.
 * Factories are reusable and thread safe, and must allocate resources without blocking network I/O.
 */
@FunctionalInterface
public interface QueryTransportFactory {
	/** @param config immutable settings @return a new unopened transport */
	QueryTransport create(QueryConfig config);
	/** @param server server generation @param protocol requested protocol @return whether supported */
	default boolean supports(ServerType server, TS3Query.Protocol protocol) { return true; }
	/** @return true if transport performs login; false to run the shared ServerQuery login command */
	default boolean authenticates() { return false; }
}
