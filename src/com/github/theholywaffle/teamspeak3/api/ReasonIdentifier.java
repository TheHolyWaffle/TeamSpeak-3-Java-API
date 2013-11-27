package com.github.theholywaffle.teamspeak3.api;

public enum ReasonIdentifier {

	REASON_KICK_CHANNEL(4),
	REASON_KICK_SERVER(5);

	private int i;

	ReasonIdentifier(int i) {
		this.i = i;
	}

	public int getIndex() {
		return i;
	}

}
