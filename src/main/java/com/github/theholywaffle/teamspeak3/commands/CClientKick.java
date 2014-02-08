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

import com.github.theholywaffle.teamspeak3.api.ReasonIdentifier;
import com.github.theholywaffle.teamspeak3.commands.parameter.ArrayParameter;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CClientKick extends Command {

	public CClientKick( ReasonIdentifier reason, String reasonMessage,int... clientIds) {
		super("clientkick");
		ArrayParameter p = new ArrayParameter();
		for(int id : clientIds){
			p.add(new KeyValueParam("clid",id+""));
		}
		add(p);
		add(new KeyValueParam("reasonid",reason.getIndex()+""));
		if(reasonMessage != null){
			add(new KeyValueParam("reasonmsg",reasonMessage));
		}		
	}

}
