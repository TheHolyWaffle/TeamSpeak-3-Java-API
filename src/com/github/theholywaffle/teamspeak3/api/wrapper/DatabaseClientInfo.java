package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.Date;
import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.api.ClientProperty;

public class DatabaseClientInfo extends Wrapper {

	public DatabaseClientInfo(HashMap<String, String> map) {
		super(map);
	}

	
	public String getUniqueIdentifier(){
		return get(ClientProperty.CLIENT_UNIQUE_IDENTIFIER);
	}
	
	public String getNickname(){
		return get(ClientProperty.CLIENT_NICKNAME);
	}
	
	public int getDatabaseId(){
		return getInt(ClientProperty.CLIENT_DATABASE_ID);
	}
	
	public Date getCreated(){
		return new Date(getInt(ClientProperty.CLIENT_CREATED)*1000);
	}
	
	public Date getLastConnected(){
		return new Date(getInt(ClientProperty.CLIENT_LASTCONNECTED)*1000);
	}

	public int getTotalConnections(){
		return getInt(ClientProperty.CLIENT_TOTALCONNECTIONS);
	}
	
	public String getAvatar(){
		return get(ClientProperty.CLIENT_FLAG_AVATAR);
	}
	
	public String getDescription(){
		return get(ClientProperty.CLIENT_DESCRIPTION);
	}
	
	public long getMonthlyBytesUploaded(){
		return getLong(ClientProperty.CLIENT_MONTH_BYTES_UPLOADED);
	}
	
	public long getMonthlyBytesDownloaded(){
		return getLong(ClientProperty.CLIENT_MONTH_BYTES_DOWNLOADED);
	}
	
	public long getTotalBytesUploaded(){
		return getLong(ClientProperty.CLIENT_TOTAL_BYTES_UPLOADED);
	}
	
	public long getTotalBytseDownloaded(){
		return getLong(ClientProperty.CLIENT_TOTAL_BYTES_DOWNLOADED);
	}
	
	public long getIconId(){
		return getLong(ClientProperty.CLIENT_ICON_ID);
	}
	
	public String getBase64HashClientUID(){
		return get("client_base64HashClientUID");
	}
	
	public String getLastIp(){
		return get("client_lastip");
	}

}
