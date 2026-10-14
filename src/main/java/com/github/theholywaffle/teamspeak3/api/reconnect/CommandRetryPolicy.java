package com.github.theholywaffle.teamspeak3.api.reconnect;

import java.util.Set;

/** Immutable, opt-in replay policy for explicitly selected read-only ServerQuery commands. */
public final class CommandRetryPolicy {
    private static final Set<String> SAFE_READS = Set.of(
            "whoami", "version", "serverinfo", "serverlist", "channellist", "channelinfo", "clientlist", "clientinfo");
    private final int maxRetries;
    private final Set<String> commands;

    private CommandRetryPolicy(int maxRetries, Set<String> commands) {
        this.maxRetries = maxRetries;
        this.commands = commands;
    }

    /** @return a policy that never replays a command with an unknown outcome */
    public static CommandRetryPolicy none() {
        return new CommandRetryPolicy(0, Set.of());
    }

    /**
     * Selects read commands to replay, in their original position, after session restoration.
     * Server error responses are never retried. Mutations and context changes are rejected.
     * @param maxRetries maximum additional sends per command, greater than zero
     * @param commands exact protocol names from the supported read-only set: {@code whoami}, {@code version},
     *     {@code serverinfo}, {@code serverlist}, {@code channellist}, {@code channelinfo}, {@code clientlist}
     *     and {@code clientinfo}
     * @return an immutable policy
     */
    public static CommandRetryPolicy safeReads(int maxRetries, String... commands) {
        if (maxRetries <= 0) throw new IllegalArgumentException("Retry count must be positive");
        Set<String> selected = Set.of(commands);
        if (selected.isEmpty() || !SAFE_READS.containsAll(selected)) {
            throw new IllegalArgumentException("Select only supported read-only commands: " + SAFE_READS);
        }
        return new CommandRetryPolicy(maxRetries, selected);
    }

    /** @return the maximum number of additional sends */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * @param name protocol command name
     * @return whether replay is explicitly enabled
     */
    public boolean allows(String name) {
        return commands.contains(name);
    }
}
