package com.github.theholywaffle.teamspeak3.api;

public enum HostMessageMode {
	LOG(1),
	MODAL(2),
	MODAL_QUIT(3), UNKNOWN(-1);

	private int i;

	HostMessageMode(int i) {
		this.i = i;
	}

	public int getIndex() {
		return i;
	}

}
