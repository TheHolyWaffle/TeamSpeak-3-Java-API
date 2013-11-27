package com.github.theholywaffle.teamspeak3.api;

public enum PermissionGroupDatabaseType {

	TEMPLATE(0),
	REGULAR(1),
	QUERY(2);

	private int i;

	PermissionGroupDatabaseType(int i) {
		this.i = i;
	}

	public int getIndex() {
		return i;
	}

}
