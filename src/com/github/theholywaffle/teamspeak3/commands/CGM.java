package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CGM extends Command {

	public CGM(String message) {
		super("gm");
		add(new KeyValueParam("msg", message));
	}

}
