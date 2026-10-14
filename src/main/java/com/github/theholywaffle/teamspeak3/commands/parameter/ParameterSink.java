package com.github.theholywaffle.teamspeak3.commands.parameter;

/** Receives a command's parameters unencoded, for transports that do not send ServerQuery lines. */
public interface ParameterSink {

    /**
     * @param key the parameter name; options are named {@code -option}, positional values have no value
     * @param value the unencoded value, empty for options and positional values
     */
    void put(String key, String value);

    /** Starts the next pipe-separated entry; the first entry also holds the shared parameters. */
    void nextEntry();
}
