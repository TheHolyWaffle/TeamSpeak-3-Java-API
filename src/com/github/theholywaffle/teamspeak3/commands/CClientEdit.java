package com.github.theholywaffle.teamspeak3.commands;

import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CClientEdit extends Command{

	public CClientEdit(int clientId, HashMap<ClientProperty, String> options) {
		super("clientedit");
		add(new KeyValueParam("clid",clientId+""));
		if (options != null) {
			for (ClientProperty p : options.keySet()) {
				if (p.isChangeable()) {
					add(new KeyValueParam(p.getName(), options.get(p)));
				}
			}
		}
	}

}
