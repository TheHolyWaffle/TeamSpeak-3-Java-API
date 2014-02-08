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

import java.util.HashMap;

public class ServerGroupClient extends Wrapper{

	public ServerGroupClient(HashMap<String, String> map) {
		super(map);
	}
	
	public int getClientDatabaseId(){
		return getInt("cldbid");
	}
	
	public String getNickname(){
		return get("client_nickname");
	}
	
	public String getUniqueIdentifier(){
		return get("client_unique_identifier");
	}

}
