package com.github.theholywaffle.teamspeak3;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

class SocketChannel implements QueryTransport {

    private final Socket socket = new Socket();
    private final QueryConfig config;

    SocketChannel(QueryConfig config) {
        this.config = config;
    }

    @Override
    public void connect(QueryTransport.Connected connection) throws IOException {
        socket.connect(
                new InetSocketAddress(config.getHost(), config.getQueryPort()),
                TS3Config.socketTimeout(config.getConnectTimeout()));
        connection.transportConnected();
        socket.setTcpNoDelay(true);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
