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
package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.api.VirtualServerStatus;

public class ServerQueryInfo extends Wrapper {

	public ServerQueryInfo(HashMap<String, String> map) {
		super(map);
	}
	
	public int getChannelId(){
		return getInt("client_channel_id");
	}
	
	public int getDatabaseId(){
		return getInt("client_database_id");
	}
	
	public int getId(){
		return getInt("client_id");
	}
	
	public String getLoginName(){
		return get("client_login_name");
	}
	
	public String getNickname(){
		return get("client_nickname");
	}
	
	public int getOriginServerId(){
		return getInt("client_origin_server_id");
	}
	
	public String getUniqueIdentifier(){
		return get("client_unique_identifier");
	}
	
	public int getVirtualServerId(){
		return getInt("virtualserver_id");
	}
	
	public int getVirtualServerPort(){
		return getInt("virtualserver_port");
	}
	
	public VirtualServerStatus getVirtualServerStatus(){
		for (VirtualServerStatus s : VirtualServerStatus.values()) {
			if (s.getName().equals(get("virtualserver_status"))) {
				return s;
			}
		}
		return VirtualServerStatus.UNKNOWN;
	}
	
	public String getVirtualServerUniqueIdentifier(){
		return get("virtualserver_unique_identifier");
	}

}
