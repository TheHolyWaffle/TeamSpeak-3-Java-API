package com.github.theholywaffle.teamspeak3.api.wrapper;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FileInfoTest {
    @Test void ts3FileAndDirectoryTimestampsUseEpochSeconds() {
        var expected = Instant.parse("2026-10-11T10:00:00Z");
        var fields = Map.of("datetime", Long.toString(expected.getEpochSecond()));
        assertEquals(expected, new FileInfo(fields).getLastModifiedDate());
        assertEquals(expected, new FileListEntry(fields).getLastModifiedDate());
    }
}
