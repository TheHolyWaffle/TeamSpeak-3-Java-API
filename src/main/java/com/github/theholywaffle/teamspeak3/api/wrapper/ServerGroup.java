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
package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;

public class ServerGroup extends Wrapper {

	public ServerGroup(HashMap<String, String> map) {
		super(map);
	}

	public int getId() {
		return getInt("sgid");
	}

	public String getName() {
		return get("name");
	}

	public PermissionGroupDatabaseType getType() {
		for (PermissionGroupDatabaseType p : PermissionGroupDatabaseType
				.values()) {
			if (p.getIndex() == getInt("type")) {
				return p;
			}
		}
		return null;
	}

	public int getIconId() {
		return getInt("iconid");
	}

	public int getSaveDb() {
		return getInt("savedb");
	}

	public int getSortId() {
		return getInt("sortid");
	}

	public int getNameMode() {
		return getInt("namemode");
	}

	public int getModifyPower() {
		return getInt("n_modifyp");
	}

	public int getMemberAddPower() {
		return getInt("n_member_addp");
	}

	public int getMemberRemovePower() {
		return getInt("n_member_removep");
	}

}
