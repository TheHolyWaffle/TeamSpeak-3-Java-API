package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;

public class CClientList extends Command{

	public CClientList() {
		super("clientlist");
		add(new OptionParam("uid"));
		add(new OptionParam("away"));
		add(new OptionParam("voice"));
		add(new OptionParam("times"));
		add(new OptionParam("groups"));
		add(new OptionParam("info"));
		add(new OptionParam("icon"));
		add(new OptionParam("country"));
	}

}
