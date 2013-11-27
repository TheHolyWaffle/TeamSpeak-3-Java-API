package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CMessageGet extends Command {

	public CMessageGet(int messageId) {
		super("messageget");
		add(new KeyValueParam("msgid",messageId+""));
	}

}
