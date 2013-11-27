package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CPrivilegeKeyDelete extends Command {

	public CPrivilegeKeyDelete(String token) {
		super("privilegekeydelete");
		add(new KeyValueParam("token",token));
	}

}
