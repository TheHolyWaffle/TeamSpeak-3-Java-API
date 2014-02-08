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
package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.Date;
import java.util.HashMap;

public class Message extends Wrapper{


	public Message(HashMap<String, String> map) {
		super(map);
	}

	public int getId(){
		return getInt("msgid");
	}
	
	public String getClientUniqueIdentifier(){
		return get("cluid");
	}
	
	public String getSubject(){
		return get("subject");
	}
	
	public Date getReceivedDate(){
		return new Date(getLong("timestamp")*1000);
	}
	
	public boolean hasRead(){
		return getBoolean("flag_read");
	}

}
