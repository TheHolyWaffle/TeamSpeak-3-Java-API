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

import com.github.theholywaffle.teamspeak3.api.ClientProperty;

public class DatabaseClient extends Wrapper {

	public DatabaseClient(HashMap<String, String> map) {
		super(map);
	}
	
	public int getDatabaseId(){
		return getInt("cldbid");
	}

	public String getUniqueIdentifier(){
		return get(ClientProperty.CLIENT_UNIQUE_IDENTIFIER);
	}
	
	public String getNickname(){
		return get(ClientProperty.CLIENT_NICKNAME);
	}
	
	public Date getCreated(){
		return new Date(getLong(ClientProperty.CLIENT_CREATED)*1000);
	}
	
	public Date getLastConnected(){
		return new Date(getLong(ClientProperty.CLIENT_LASTCONNECTED)*1000);
	}
	
	public int getTotalConnections(){
		return getInt(ClientProperty.CLIENT_TOTALCONNECTIONS);
	}
	
	public String getDescription(){
		return get(ClientProperty.CLIENT_DESCRIPTION);
	}
	
	public String getLastIp(){
		return get("client_lastip");
	}

}
