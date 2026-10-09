package com.github.theholywaffle.teamspeak3;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolLineReaderTest {
	@ParameterizedTest
	@ValueSource(strings = {"\n", "\r", "\r\n", "\n\r"})
	void supportsProtocolLineEndings(String ending) throws Exception {
		try (var reader = new ProtocolLineReader(new ByteArrayInputStream(("name=日本語" + ending + "error id=0" + ending).getBytes(StandardCharsets.UTF_8)))) {
			assertEquals("name=日本語", reader.readLine());
			String line;
			do { line = reader.readLine(); } while ("".equals(line));
			assertEquals("error id=0", line);
			do { line = reader.readLine(); } while ("".equals(line));
			assertNull(line);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"name=partial", "error id=0 msg=ok"})
	void eofDoesNotSupplyMissingTerminator(String partial) throws Exception {
		try (var reader = new ProtocolLineReader(new ByteArrayInputStream(partial.getBytes(StandardCharsets.UTF_8)))) {
			assertNull(reader.readLine());
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {3, 6, 7, 8, 12})
	void recoverableTimeoutDoesNotLosePartialLine(int timeoutOffset) throws Exception {
		// One successful read, one timeout, then the rest: no timing assumptions.
		InputStream input = new InputStream() {
			final byte[] bytes = "name=日本語fragmented\\\\n\nerror id=0\n".getBytes(StandardCharsets.UTF_8);
			int offset;
			boolean timedOut;
			@Override public int read() throws SocketTimeoutException {
				if (offset == timeoutOffset && !timedOut) {
					timedOut = true;
					throw new SocketTimeoutException("scripted recoverable timeout");
				}
				return offset == bytes.length ? -1 : bytes[offset++] & 0xff;
			}
			@Override public int read(byte[] target, int start, int length) throws SocketTimeoutException {
				int next = read();
				if (next == -1) return -1;
				target[start] = (byte) next;
				return 1;
			}
		};
		try (var reader = new ProtocolLineReader(input)) {
			assertThrows(SocketTimeoutException.class, reader::readLine);
			assertEquals("name=日本語fragmented\\\\n", reader.readLine());
			assertEquals("error id=0", reader.readLine());
			assertNull(reader.readLine());
		}
	}
}
