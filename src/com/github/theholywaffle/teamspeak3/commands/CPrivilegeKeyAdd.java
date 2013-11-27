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
