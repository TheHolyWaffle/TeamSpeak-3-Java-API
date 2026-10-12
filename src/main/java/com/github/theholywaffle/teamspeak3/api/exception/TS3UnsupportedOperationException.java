package com.github.theholywaffle.teamspeak3.api.exception;

/** The operation is not supported by this query's configuration or by the returned data. */
public class TS3UnsupportedOperationException extends TS3Exception {

	private static final long serialVersionUID = 1L;

	public TS3UnsupportedOperationException(String msg) {
		super(msg);
	}
}
