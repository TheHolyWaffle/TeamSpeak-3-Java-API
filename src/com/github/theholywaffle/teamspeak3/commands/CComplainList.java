package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CComplainList extends Command {

	public CComplainList(int clientDBId) {
		super("complainlist");
		if(clientDBId> 0){
			add(new KeyValueParam("tcldbid",clientDBId+""));
		}		
	}

}
