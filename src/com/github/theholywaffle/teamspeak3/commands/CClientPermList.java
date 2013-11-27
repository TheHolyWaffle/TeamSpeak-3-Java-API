package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;

public class CClientPermList extends Command{

	public CClientPermList(int clientDBId) {
		super("clientpermlist");
		add(new KeyValueParam("cldbid",clientDBId+""));
		add(new OptionParam("permsid"));
	}

}
