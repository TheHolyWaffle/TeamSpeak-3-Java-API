package com.github.theholywaffle.teamspeak3.api;

public enum LogLevel {

	ERROR(1),
	WARNING(2),
	DEBUG(3),
	INFO(4);

	private int i;

	LogLevel(int i) {
		this.i = i;
	}

	public int getIndex() {
		return i;
	}

}
