package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.ValueParam;

public class CLogin extends Command {

	public CLogin(String username, String password) {
		super("login");
		add(new ValueParam(username));
		add(new ValueParam(password));
	}

}
