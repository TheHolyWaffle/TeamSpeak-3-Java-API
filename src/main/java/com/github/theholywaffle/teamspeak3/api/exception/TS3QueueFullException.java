package com.github.theholywaffle.teamspeak3.api.exception;

/** Command admission failed before any bytes were written; retry only when capacity is available. */
public class TS3QueueFullException extends TS3Exception {
	private static final long serialVersionUID = 1L;
	public TS3QueueFullException() { super("Command capacity exceeded"); }
}
