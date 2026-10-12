package com.github.theholywaffle.teamspeak3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/** Caller-owned streams recording whether the library closed or flushed them. */
final class CallerStreams {
    private CallerStreams() { }

    static class Input extends ByteArrayInputStream {
        boolean closed;
        Input(byte[] bytes) { super(bytes); }
        @Override public void close() { closed = true; }
    }

    static class Output extends ByteArrayOutputStream {
        boolean closed, flushed;
        @Override public void close() { closed = true; }
        @Override public void flush() { flushed = true; }
    }
}
