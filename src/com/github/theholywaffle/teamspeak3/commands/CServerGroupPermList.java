package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;

public class CServerGroupPermList extends Command {

	public CServerGroupPermList(int id) {
		super("servergrouppermlist");
		add(new KeyValueParam("sgid", id));
		add(new OptionParam("permsid"));
	}

}
