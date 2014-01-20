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
