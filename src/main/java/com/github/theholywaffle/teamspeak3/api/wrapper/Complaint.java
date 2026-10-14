package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import java.time.Instant;
import java.util.Map;

public class Complaint extends Wrapper {

    public Complaint(Map<String, String> map) {
        super(map);
    }

    public ClientDatabaseId getTargetClientDatabaseId() {
        return requireDatabaseId("tcldbid");
    }

    public String getTargetName() {
        return require("tname");
    }

    public ClientDatabaseId getSourceClientDatabaseId() {
        return requireDatabaseId("fcldbid");
    }

    public String getSourceName() {
        return require("fname");
    }

    public String getMessage() {
        return require("message");
    }

    public Instant getTimestamp() {
        return requireInstant("timestamp");
    }
}
