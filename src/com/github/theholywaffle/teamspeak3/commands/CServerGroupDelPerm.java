package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CServerGroupDelPerm extends Command {

	public CServerGroupDelPerm(int groupId, String permName) {
		super("servergroupdelperm");
		add(new KeyValueParam("sgid", groupId));
		add(new KeyValueParam("permsid", permName));
	}

}
