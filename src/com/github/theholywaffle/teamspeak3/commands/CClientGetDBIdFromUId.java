package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CClientGetDBIdFromUId extends Command{

	public CClientGetDBIdFromUId(String clientUId) {
		super("clientgetdbidfromuid");
		add(new KeyValueParam("cluid",clientUId));
	}

}
