package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CServerGroupAddPerm extends Command {

	public CServerGroupAddPerm(int groupId, String permName, int permValue,
			boolean isNegated, boolean isSkipped) {
		super("servergroupaddperm");
		add(new KeyValueParam("sgid", groupId));
		add(new KeyValueParam("permsid", permName));
		add(new KeyValueParam("permvalue", permValue));
		add(new KeyValueParam("permnegated", (isNegated ? "1" : "0")));
		add(new KeyValueParam("permskip", (isSkipped ? "1" : "0")));
	}

}
