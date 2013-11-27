package com.github.theholywaffle.teamspeak3.api.exception;

public class UnknownErrorException extends TS3Exception {

	private static final long serialVersionUID = -8458508268312921713L;

	public UnknownErrorException(String error) {
		super(error);
	}

}
