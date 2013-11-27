package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ServerInstanceProperty;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CInstanceEdit extends Command{

	public CInstanceEdit(ServerInstanceProperty p, String value) {
		super("instanceedit");
		add(new KeyValueParam(p.getName(), value));
	}


}
