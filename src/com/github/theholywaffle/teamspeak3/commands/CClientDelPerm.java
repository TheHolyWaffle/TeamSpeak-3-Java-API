package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CClientDelPerm extends Command{

	public CClientDelPerm(int clientDBId, String permName) {
		super("clientdelperm");
		add(new KeyValueParam("cldbid",clientDBId+""));
		add(new KeyValueParam("permsid",permName));
	}

}
