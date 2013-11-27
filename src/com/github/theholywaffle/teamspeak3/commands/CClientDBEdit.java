package com.github.theholywaffle.teamspeak3.commands;

import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CClientDBEdit extends Command {

	public CClientDBEdit(int clientDBId, HashMap<ClientProperty, String> options) {
		super("clientdbedit");
		add(new KeyValueParam("cldbid",clientDBId+""));
		if (options != null) {
			for (ClientProperty p : options.keySet()) {
				if (p.isChangeable()) {
					add(new KeyValueParam(p.getName(), options.get(p)));
				}
			}
		}
	}

}
