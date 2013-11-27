package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CClientSetServerQueryLogin extends Command {

	public CClientSetServerQueryLogin(String username) {
		super("clientsetserverquerylogin");
		add(new KeyValueParam("client_login_name",username));
	}



}
