package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ServerGroupType;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CServerGroupAutoDelPerm extends Command {

	public CServerGroupAutoDelPerm(ServerGroupType t, String permName) {
		super("servergroupautodelperm");
		add(new KeyValueParam("sgtype", t.getIndex()));
		add(new KeyValueParam("permsid", permName));
	}

}
