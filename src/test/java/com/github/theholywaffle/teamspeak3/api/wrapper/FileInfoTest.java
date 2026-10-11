package com.github.theholywaffle.teamspeak3.api.wrapper;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class FileInfoTest {
	@Test
	void timestampUnitIsExplicitAndSecondsRemainTheDefault() {
		var expected = Instant.parse("2026-10-11T10:00:00Z");
		var seconds = new FileInfo(Map.of("datetime", Long.toString(expected.getEpochSecond())));
		var milliseconds = new FileListEntry(Map.of("datetime", Long.toString(expected.toEpochMilli())));
		assertEquals(expected, seconds.getLastModifiedDate().toInstant());
		assertEquals(expected, seconds.getLastModifiedDate(TimeUnit.SECONDS).toInstant());
		assertEquals(expected, milliseconds.getLastModifiedDate(TimeUnit.MILLISECONDS).toInstant());
		assertNotEquals(expected, milliseconds.getLastModifiedDate().toInstant());
	}

	@Test
	void millisecondsRetainTheirFractionalSecondAndRequireAUnit() {
		var expected = Instant.parse("2026-10-11T10:00:00.123Z");
		var info = new FileInfo(Map.of("datetime", Long.toString(expected.toEpochMilli())));
		assertEquals(expected, info.getLastModifiedDate(TimeUnit.MILLISECONDS).toInstant());
		assertThrows(NullPointerException.class, () -> info.getLastModifiedDate(null));
	}
}
