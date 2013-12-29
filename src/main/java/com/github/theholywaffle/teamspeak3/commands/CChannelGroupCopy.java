/*******************************************************************************
 * Copyright (c) 2013 Bert De Geyter (https://github.com/TheHolyWaffle).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Bert De Geyter (https://github.com/TheHolyWaffle) - initial API and implementation
 ******************************************************************************/
package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CChannelGroupCopy extends Command {

	public CChannelGroupCopy(int sourceGroupId, int targetGroupId,
			PermissionGroupDatabaseType t) {
		this(sourceGroupId, targetGroupId, "name", t);
	}

	public CChannelGroupCopy(int sourceGroupId, String groupName,
			PermissionGroupDatabaseType t) {
		this(sourceGroupId, 0, groupName, t);
	}

	public CChannelGroupCopy(int sourceGroupId, int targetGroupId,
			String groupName, PermissionGroupDatabaseType t) {
		super("channelgroupcopy");
		add(new KeyValueParam("scgid", sourceGroupId));
		add(new KeyValueParam("tcgid", targetGroupId));
		add(new KeyValueParam("name", groupName));
		add(new KeyValueParam("type", t.getIndex()));
	}

}
