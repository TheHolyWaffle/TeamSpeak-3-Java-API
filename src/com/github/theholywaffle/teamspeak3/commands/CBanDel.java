package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CBanDel extends Command{

	public CBanDel(int banId) {
		super("bandel");
		add(new KeyValueParam("banid",banId));
	}

}
