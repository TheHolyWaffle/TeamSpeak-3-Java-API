package com.github.theholywaffle.teamspeak3.api;

public enum TokenType {

	SERVER_GROUP(0),
	CHANNEL_GROUP(1);

	private int i;

	TokenType(int i) {
		this.i = i;
	}

	public int getIndex() {
		return i;
	}

}
