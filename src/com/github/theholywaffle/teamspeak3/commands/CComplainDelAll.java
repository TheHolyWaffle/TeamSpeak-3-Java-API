package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CComplainDelAll extends Command {

	public CComplainDelAll(int clientDBId) {
		super("complaindelall");
		add(new KeyValueParam("tcldbid",clientDBId+""));
	}

}
