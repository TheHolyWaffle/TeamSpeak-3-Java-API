package com.github.theholywaffle.teamspeak3.api;

public enum VirtualServerStatus {
    ONLINE("online"),
    OFFLINE("offline"),
    DEPLOY_RUNNING("deploy running"),
    BOOTING_UP("booting up"),
    SHUTTING_DOWN("shutting down"),
    VIRTUAL_ONLINE("virtual online"),
    UNKNOWN("unknown");

    private static final VirtualServerStatus[] VALUES = values();

    private final String name;

    VirtualServerStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * @param name the ServerQuery status name
     * @return the matching constant, or {@link #UNKNOWN} for a status this library does not know
     */
    public static VirtualServerStatus fromName(String name) {
        for (VirtualServerStatus value : VALUES) {
            if (value != UNKNOWN && value.name.equals(name)) return value;
        }
        return UNKNOWN;
    }
}
