package com.github.theholywaffle.teamspeak3.api;

public enum ServerGroupType {
	CHANNEL_GUEST(10),
	SERVER_GUEST(15),
	QUERY_GUEST(20),
	CHANNEL_VOICE(25),
	SERVER_NORMAL(30),
	CHANNEL_OPERATOR(35),
	CHANNEL_ADMIN(40),
	SERVER_ADMIN(45),
	QUERY_ADMIN(50);

	private int i;

	ServerGroupType(int i) {
		this.i = i;
	}

	public int getIndex() {
		return i;
	}

}
