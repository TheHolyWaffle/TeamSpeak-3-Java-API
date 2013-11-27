package com.github.theholywaffle.teamspeak3.api;

public enum HostBannerMode {
	NO_ADJUST(0),
	IGNORE_ASPECT(1),
	KEEP_ASPECT(2), UNKNOWN(-1);

	private int i;

	HostBannerMode(int i) {
		this.i = i;
	}

	public int getIndex() {
		return i;
	}

}
