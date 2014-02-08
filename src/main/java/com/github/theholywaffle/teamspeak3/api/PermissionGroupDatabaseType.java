/*******************************************************************************
 * Copyright (c) 2014 Bert De Geyter (https://github.com/TheHolyWaffle).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Bert De Geyter (https://github.com/TheHolyWaffle)
 ******************************************************************************/
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
