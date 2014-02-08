/*******************************************************************************
 * Copyright (c) 2014 Bert De Geyter (https://github.com/TheHolyWaffle).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Bert De Geyter (https://github.com/TheHolyWaffle)
 ******************************************************************************/
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
