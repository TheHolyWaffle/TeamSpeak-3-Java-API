package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;

public class CServerGroupClientList extends Command {

	public CServerGroupClientList(int groupId) {
		super("servergroupclientlist");
		add(new KeyValueParam("sgid",groupId+""));
		add(new OptionParam("names"));
	}

}
