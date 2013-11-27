package com.github.theholywaffle.teamspeak3.api;

public enum TextMessageTargetMode {

	CLIENT(1),
	CHANNEL(2),
	SERVER(3);

	private int i;

	TextMessageTargetMode(int i) {
		this.i = i;
	}

	public int getIndex() {
		return i;
	}

}
