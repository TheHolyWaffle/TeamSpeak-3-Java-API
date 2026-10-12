package com.github.theholywaffle.teamspeak3.api.exception;

/** The ServerQuery connection could not be established or is no longer usable. */
public class TS3TransportException extends TS3Exception {

	private static final long serialVersionUID = 1L;

	public TS3TransportException(String msg) {
		super(msg);
	}

	public TS3TransportException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
