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

public class CPrivilegeKeyAdd extends Command{

	public CPrivilegeKeyAdd(int type, int groupId, int channelId, String description) {
		super("privilegekeyadd");
		add(new KeyValueParam("tokentype",type+""));
		add(new KeyValueParam("tokenid1",groupId+""));
		add(new KeyValueParam("tokenid2",channelId+""));
		if(description != null){
			add(new KeyValueParam("tokendescription", description));
		}
	}

}
