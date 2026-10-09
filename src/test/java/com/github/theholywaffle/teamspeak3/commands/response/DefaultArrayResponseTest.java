package com.github.theholywaffle.teamspeak3.commands.response;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DefaultArrayResponseTest {
	@ParameterizedTest
	@NullAndEmptySource
	void emptyResponseHasNoRows(String raw) {
		assertTrue(DefaultArrayResponse.parse(raw).getResponses().isEmpty());
		assertEquals(Map.of(), DefaultArrayResponse.parse(raw).getFirstResponse().getMap());
	}

	static Stream<Arguments> fields() {
		return Stream.of(
			Arguments.of("name=", Map.of("name", "")),
			Arguments.of("flag", Map.of("flag", "")),
			Arguments.of("  name= flag  ", Map.of("name", "", "flag", "")),
			Arguments.of("name=a=b", Map.of("name", "a=b")),
			Arguments.of("name=old name=new", Map.of("name", "new")),
			Arguments.of("name=日本語😀", Map.of("name", "日本語😀")),
			Arguments.of("name=a\\pb\\sc\\\\n", Map.of("name", "a|b c\\n")),
			Arguments.of("a\\sb=\\x", Map.of("a b", "\\x")));
	}

	@ParameterizedTest
	@MethodSource("fields")
	void fieldsDecodeAfterSplitting(String raw, Map<String, String> expected) {
		assertEquals(expected, DefaultArrayResponse.parse(raw).getFirstResponse().getMap());
	}

	static Stream<Arguments> rows() {
		return Stream.of(
			Arguments.of("cid=7 name=first|name=second|cid=9 name=third", List.of(
				Map.of("cid", "7", "name", "first"), Map.of("cid", "7", "name", "second"), Map.of("cid", "9", "name", "third"))),
			Arguments.of("cid=7 name=first|name=|flag", List.of(
				Map.of("cid", "7", "name", "first"), Map.of("cid", "7", "name", ""), Map.of("cid", "7", "name", "first", "flag", ""))),
			Arguments.of("cid=7|cid=9|name=third", List.of(Map.of("cid", "7"), Map.of("cid", "9"), Map.of("cid", "7", "name", "third"))),
			Arguments.of("cid=7||", List.of(Map.of("cid", "7"), Map.of("cid", "7"), Map.of("cid", "7"))),
			Arguments.of("|name=second", List.of(Map.of(), Map.of("name", "second"))));
	}

	@ParameterizedTest
	@MethodSource("rows")
	void arrayRowsInheritFirstRowSharedFields(String raw, List<Map<String, String>> expected) {
		assertEquals(expected, DefaultArrayResponse.parse(raw).getResponses().stream().map(r -> r.getMap()).toList());
	}

	static Stream<Arguments> errors() {
		return Stream.of(Arguments.of("error id=0 msg=ok", 0, "ok", true),
			Arguments.of("error id=1281 msg=database\\sempty\\sresult\\sset", 1281, "database empty result set", true),
			Arguments.of("error id=256 msg=bad\\scommand extra_msg=\\\\n failed_permid=42", 256, "bad command", false),
			Arguments.of("error id=1 msg=", 1, "", false));
	}

	@ParameterizedTest
	@MethodSource("errors")
	void errorLines(String raw, int id, String message, boolean successful) {
		var error = DefaultArrayResponse.parseError(raw);
		assertEquals(id, error.getId());
		assertEquals(message, error.getMessage());
		assertEquals(successful, error.isSuccessful());
		if (id == 256) {
			assertEquals("\\n", error.getExtraMessage());
			assertEquals(42, error.getFailedPermissionId());
		}
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"error", "id=0", "error msg=ok", "error id=x", "error id=", "error id=-1", "error id=2147483648"})
	void malformedErrorIsRejected(String raw) {
		assertThrows(IllegalArgumentException.class, () -> DefaultArrayResponse.parseError(raw));
	}
}
