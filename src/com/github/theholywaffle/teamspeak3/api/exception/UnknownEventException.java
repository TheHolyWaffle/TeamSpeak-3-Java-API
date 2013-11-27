package com.github.theholywaffle.teamspeak3.api.exception;

public class UnknownEventException extends TS3Exception {

	private static final long serialVersionUID = -9179157153557357715L;

	public UnknownEventException(String event) {
		super(event);
	}

}
