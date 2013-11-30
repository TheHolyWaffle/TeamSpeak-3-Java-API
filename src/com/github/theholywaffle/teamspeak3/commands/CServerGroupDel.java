package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CServerGroupDel extends Command {
	
	public CServerGroupDel(int id) {
		this(id, true);
	}

	public CServerGroupDel(int id, boolean forced) {
		super("servergroupdel");
		add(new KeyValueParam("sgid", id));
		add(new KeyValueParam("force", forced ? "1" : "0"));
	}

}
