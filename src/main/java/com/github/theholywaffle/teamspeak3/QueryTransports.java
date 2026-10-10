package com.github.theholywaffle.teamspeak3;

/** Built-in stream transports. Both server generations share the same SSH implementation. */
public enum QueryTransports implements QueryTransportFactory {
	/** TS3 raw TCP, with login in the shared command executor. */
	RAW {
		@Override public QueryTransport create(QueryConfig config) { return new SocketChannel(config); }
		@Override public boolean supports(ServerType server, TS3Query.Protocol protocol) {
			return server == ServerType.TS3 && protocol == TS3Query.Protocol.RAW;
		}
	},
	/** Strictly verified SSH, authenticating before opening the query shell. */
	SSH {
		@Override public QueryTransport create(QueryConfig config) { return new SSHChannel(config); }
		@Override public boolean supports(ServerType server, TS3Query.Protocol protocol) { return protocol == TS3Query.Protocol.SSH; }
		@Override public boolean authenticates() { return true; }
	}
}
