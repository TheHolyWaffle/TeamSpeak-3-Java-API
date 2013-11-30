package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CSendTextMessage extends Command {

	public CSendTextMessage(int targetMode, int targetId, String message) {
		super("sendtextmessage");
		add(new KeyValueParam("targetmode", targetMode));
		add(new KeyValueParam("target", targetId));
		add(new KeyValueParam("msg", message));
	}

}
