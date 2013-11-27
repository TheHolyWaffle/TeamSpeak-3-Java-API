package com.github.theholywaffle.teamspeak3.api.exception;

public class TS3Exception extends RuntimeException {

	private static final long serialVersionUID = 7167169981592989359L;

	public TS3Exception(String msg) {
		super(msg + " [Please report this exception to the developer!]");
	}

}
