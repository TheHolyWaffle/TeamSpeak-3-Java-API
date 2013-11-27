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
