package com.github.theholywaffle.teamspeak3.api.event;

public enum TS3EventType {
    SERVER("server"),
    CHANNEL("channel"),
    TEXT_SERVER("textserver"),
    TEXT_CHANNEL("textchannel"),
    TEXT_PRIVATE("textprivate"),
    PRIVILEGE_KEY_USED("tokenused");

    private final String name;

    TS3EventType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
