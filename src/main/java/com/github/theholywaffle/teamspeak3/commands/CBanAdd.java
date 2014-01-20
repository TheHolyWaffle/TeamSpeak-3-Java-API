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
package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CBanAdd extends Command {

	public CBanAdd(String ip, String name, String uid, long timeInSeconds,
			String reason) {
		super("banadd");
		if (ip != null) {
			add(new KeyValueParam("ip", ip));
		}
		if (name != null) {
			add(new KeyValueParam("name", name));
		}
		if (uid != null) {
			add(new KeyValueParam("uid", uid));
		}
		if (timeInSeconds > 0) {
			add(new KeyValueParam("time", timeInSeconds));
		}
		if (reason != null) {
			add(new KeyValueParam("banreason", reason));
		}
	}

}
