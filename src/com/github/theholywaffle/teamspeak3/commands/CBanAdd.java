package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CBanAdd extends Command {

	public CBanAdd(String ip, String name, String uid, long timeInSeconds,
			String reason) {
		super("banadd");
		if (ip != null) {
			add(new KeyValueParam("ip", ip));
		}
		if (name != null) {
			add(new KeyValueParam("name", name));
		}
		if (uid != null) {
			add(new KeyValueParam("uid", uid));
		}
		if (timeInSeconds > 0) {
			add(new KeyValueParam("time", timeInSeconds));
		}
		if (reason != null) {
			add(new KeyValueParam("banreason", reason));
		}
	}

}
