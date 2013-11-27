package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CPrivilegeKeyUse extends Command{

	public CPrivilegeKeyUse(String token) {
		super("privilegekeyuse");
		add(new KeyValueParam("token",token));
	}

}
