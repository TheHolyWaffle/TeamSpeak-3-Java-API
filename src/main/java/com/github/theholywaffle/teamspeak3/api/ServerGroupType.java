/*******************************************************************************
 * Copyright (c) 2014 Bert De Geyter (https://github.com/TheHolyWaffle).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Bert De Geyter (https://github.com/TheHolyWaffle) - initial API and implementation
 ******************************************************************************/
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
