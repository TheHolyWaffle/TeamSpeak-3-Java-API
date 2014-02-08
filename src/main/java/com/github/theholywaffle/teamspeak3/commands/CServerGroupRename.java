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
package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CServerGroupRename extends Command {

	public CServerGroupRename(int id, String name) {
		super("servergrouprename");
		add(new KeyValueParam("sgid", id));
		add(new KeyValueParam("name", name));
	}

}
