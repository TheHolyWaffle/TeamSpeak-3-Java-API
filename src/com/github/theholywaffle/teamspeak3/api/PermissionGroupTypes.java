package com.github.theholywaffle.teamspeak3.api;

public enum PermissionGroupTypes {

	SERVER_GROUP(0),
	GLOBAL_CLIENT(1),
	CHANNEL(2),
	CHANNEL_GROUP(3),
	CHANNEL_CLIENT(4);

	private int i;

	PermissionGroupTypes(int i) {
		this.i = i;
	}

	public int getIndex() {
		return i;
	}

}
