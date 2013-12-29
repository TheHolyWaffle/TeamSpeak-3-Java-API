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

public class VirtualServer extends Wrapper {

	public VirtualServer(HashMap<String, String> map) {
		super(map);
	}	

	public int getId() {
		return getInt("virtualserver_id");
	}

	public int getPort() {
		return getInt("virtualserver_port");
	}

	public VirtualServerStatus getStatus() {
		for (VirtualServerStatus s : VirtualServerStatus.values()) {
			if (s.getName().equals(get("virtualserver_status"))) {
				return s;
			}
		}
		return VirtualServerStatus.UNKNOWN;
	}
	
	public int getClientsOnline(){
		return getInt("virtualserver_clientsonline");
	}
	
	public int getQueryClientsOnline(){
		return getInt("virtualserver_queryclientsonline");
	}
	
	public int getMaxClients(){
		return getInt("virtualserver_maxclients");
	}
	
	public int getUptime(){
		return getInt("virtualserver_uptime"); //in seconds
	}
	
	public String getName(){
		return get("virtualserver_name");
	}
	
	public boolean isAutoStart(){
		return getBoolean("virtualserver_autostart");
	}

}
