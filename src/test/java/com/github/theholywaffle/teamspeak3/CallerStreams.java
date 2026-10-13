package com.github.theholywaffle.teamspeak3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.CountDownLatch;

/** Caller-owned streams recording whether the library closed or flushed them. */
final class CallerStreams {
    private CallerStreams() {}

    static class Input extends ByteArrayInputStream {
        boolean closed;

        Input(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    /** Blocks every read until interrupted, counting down {@code entered} first. */
    static class BlockingInput extends InputStream {
        final CountDownLatch entered = new CountDownLatch(1);
        boolean closed;

        @Override
        public int read() throws InterruptedIOException {
            entered.countDown();
            try {
                new CountDownLatch(1).await();
                throw new AssertionError();
            } catch (InterruptedException failure) {
                throw new InterruptedIOException();
            }
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    static class Output extends ByteArrayOutputStream {
        boolean closed, flushed;

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public void flush() {
            flushed = true;
        }
    }
}
