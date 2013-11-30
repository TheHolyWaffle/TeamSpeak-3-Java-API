package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CServerGroupRename extends Command {

	public CServerGroupRename(int id, String name) {
		super("servergrouprename");
		add(new KeyValueParam("sgid", id));
		add(new KeyValueParam("name", name));
	}

}
