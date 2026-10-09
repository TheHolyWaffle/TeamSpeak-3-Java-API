package com.github.theholywaffle.teamspeak3.commands;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandEncodingTest {
	static Stream<Arguments> escapes() {
		return Stream.of(
			Arguments.of("\\", "\\\\"), Arguments.of(" ", "\\s"),
			Arguments.of("/", "\\/"), Arguments.of("|", "\\p"),
			Arguments.of("\b", "\\b"), Arguments.of("\f", "\\f"),
			Arguments.of("\n", "\\n"), Arguments.of("\r", "\\r"),
			Arguments.of("\t", "\\t"), Arguments.of("\u0007", "\\a"),
			Arguments.of("\u000b", "\\v"));
	}

	@ParameterizedTest
	@MethodSource("escapes")
	void everyProtocolEscape(String plain, String encoded) {
		assertEquals(encoded, CommandEncoding.encode(plain));
		assertEquals(plain, CommandEncoding.decode(encoded));
		// A literal escape must not be decoded a second time.
		assertEquals(encoded, CommandEncoding.decode(CommandEncoding.encode(encoded)));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "日本語 😀 café", "\\n\\s\\p\\b\\f\\r\\t\\a\\v\\/", "\\\\n", "end\\", "a=1|b=two words\n\\n"})
	void roundTrip(String plain) {
		assertEquals(plain, CommandEncoding.decode(CommandEncoding.encode(plain)));
	}

	@ParameterizedTest
	@ValueSource(strings = {"\\", "end\\", "\\x", "\\u1234", "a\\?b"})
	void malformedOrUnknownEscapesArePreserved(String raw) {
		assertEquals(raw, CommandEncoding.decode(raw));
	}
}
