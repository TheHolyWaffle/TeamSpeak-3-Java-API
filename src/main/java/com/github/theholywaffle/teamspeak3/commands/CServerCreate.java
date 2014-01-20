/*******************************************************************************
 * Copyright (c) 2014 Bert De Geyter (https://github.com/TheHolyWaffle).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Bert De Geyter (https://github.com/TheHolyWaffle) - initial API and implementation
 ******************************************************************************/
package com.github.theholywaffle.teamspeak3.commands;

import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CServerCreate extends Command {

	public CServerCreate(String name, HashMap<VirtualServerProperty, String> map) {
		super("servercreate");
		add(new KeyValueParam(
				VirtualServerProperty.VIRTUALSERVER_NAME.getName(), name));
		if (map != null) {
			for (VirtualServerProperty p : map.keySet()) {
				add(new KeyValueParam(p.getName(), map.get(p)));
			}
		}

	}

}
