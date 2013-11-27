package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CBanClient extends Command{

	public CBanClient(int clientId, long timeInSeconds, String reason) {
		super("banclient");
		add(new KeyValueParam("clid",clientId));
		if(timeInSeconds >0){
			add(new KeyValueParam("time",timeInSeconds));
		}
		if(reason != null){
			add(new KeyValueParam("banreason",reason));
		}
		
	}

}
