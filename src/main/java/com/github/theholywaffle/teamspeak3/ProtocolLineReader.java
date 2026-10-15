package com.github.theholywaffle.teamspeak3;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Retains a partial protocol line across recoverable socket read timeouts. */
final class ProtocolLineReader implements Closeable {
    private final BufferedReader reader;
    private final StringBuilder partialLine = new StringBuilder();

    ProtocolLineReader(InputStream input) {
        reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
    }

    String readLine() throws IOException {
        int ch;
        while ((ch = reader.read()) != -1) {
            if (ch == '\n' || ch == '\r') {
                String line = partialLine.toString();
                partialLine.setLength(0);
                return line;
            }
            partialLine.appendCodePoint(ch);
        }
        // EOF cannot complete an unterminated protocol line.
        partialLine.setLength(0);
        return null;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
