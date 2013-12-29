/*******************************************************************************
 * Copyright (c) 2013 Bert De Geyter (https://github.com/TheHolyWaffle).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Bert De Geyter (https://github.com/TheHolyWaffle) - initial API and implementation
 ******************************************************************************/
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
