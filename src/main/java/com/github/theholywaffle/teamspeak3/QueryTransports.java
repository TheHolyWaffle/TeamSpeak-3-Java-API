package com.github.theholywaffle.teamspeak3;

/** Built-in TS3 stream transports. */
public enum QueryTransports implements QueryTransportFactory {
    /** TS3 raw TCP, with login in the shared command executor. */
    RAW {
        @Override
        public QueryTransport create(QueryConfig config) {
            return new SocketChannel(config);
        }

        @Override
        public boolean supports(TS3Query.Protocol protocol) {
            return protocol == TS3Query.Protocol.RAW;
        }
    },
    /** Strictly verified SSH, authenticating before opening the query shell. */
    SSH {
        @Override
        public QueryTransport create(QueryConfig config) {
            return new SSHChannel(config);
        }

        @Override
        public boolean supports(TS3Query.Protocol protocol) {
            return protocol == TS3Query.Protocol.SSH;
        }

        @Override
        public boolean authenticates() {
            return true;
        }
    }
}
