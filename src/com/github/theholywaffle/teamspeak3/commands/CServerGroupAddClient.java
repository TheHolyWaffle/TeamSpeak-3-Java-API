package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CServerGroupAddClient extends Command {

	public CServerGroupAddClient(int groupId, int clientDBId) {
		super("servergroupaddclient");
		add(new KeyValueParam("sgid", groupId));
		add(new KeyValueParam("cldbid", clientDBId));
	}

}
