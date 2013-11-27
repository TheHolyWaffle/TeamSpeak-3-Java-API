package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CClientAddPerm extends Command{

	public CClientAddPerm(int clientDBId, String permName,int permValue, boolean permSkipped) {
		super("clientaddperm");
		add(new KeyValueParam("cldbid",clientDBId+""));
		add(new KeyValueParam("permsid",permName));
		add(new KeyValueParam("permvalue",permValue+""));
		add(new KeyValueParam("permskip",permSkipped?"1":"0"));
	}

}
