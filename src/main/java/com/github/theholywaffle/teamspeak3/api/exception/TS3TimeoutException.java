package com.github.theholywaffle.teamspeak3.api.exception;

/** A configured command deadline expired. */
public class TS3TimeoutException extends TS3Exception {

    private static final long serialVersionUID = 1L;

    /** The deadline that expired. */
    public enum Phase {
        /** The command waited too long in the send queue and was never written. */
        QUEUE_WAIT,
        /** The command was written but its response did not finish in time; the connection was closed. */
        RESPONSE
    }

    private final Phase phase;
    private final String commandName;

    /**
     * @param phase the expired deadline
     * @param commandName protocol command name, excluding parameters and credentials
     */
    public TS3TimeoutException(Phase phase, String commandName) {
        super(
                phase == Phase.QUEUE_WAIT
                        ? "Command queue wait deadline exceeded for " + commandName
                        : "Command response deadline exceeded for " + commandName
                                + "; it may have executed on the server");
        this.phase = phase;
        this.commandName = commandName;
    }

    /** @return the deadline that expired */
    public Phase getPhase() {
        return phase;
    }

    /** @return the command name whose deadline expired */
    public String getCommandName() {
        return commandName;
    }

    @Override
    public boolean isOutcomeUnknown() {
        return phase == Phase.RESPONSE;
    }
}
