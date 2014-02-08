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

import com.github.theholywaffle.teamspeak3.api.ServerGroupType;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CServerGroupAutoAddPerm extends Command {

	public CServerGroupAutoAddPerm(ServerGroupType t, String permName,
			int permValue, boolean permNegated, boolean permSkipped) {
		super("servergroupautoaddperm");
		add(new KeyValueParam("sgtype", t.getIndex()));
		add(new KeyValueParam("permsid", permName));
		add(new KeyValueParam("permvalue", permValue));
		add(new KeyValueParam("permnegated", permNegated ? "1" : "0"));
		add(new KeyValueParam("permskip", permSkipped ? "1" : "0"));
	}

}
