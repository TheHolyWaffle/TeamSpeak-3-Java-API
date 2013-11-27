package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CComplainDel extends Command {

	public CComplainDel(int targetClientDBId, int fromClientDBId) {
		super("complaindel");
		add(new KeyValueParam("tcldbid",targetClientDBId+""));
		add(new KeyValueParam("fcldbid",fromClientDBId+""));
	}

}