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
package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.Date;
import java.util.HashMap;

public class Complaint extends Wrapper {

	public Complaint(HashMap<String, String> map) {
		super(map);
	}

	public int getTargetClientDatabaseId() {
		return getInt("tcldbid");
	}

	public String getTargetName() {
		return get("tname");
	}

	public int getSourceClientDatabaseId() {
		return getInt("fcldbid");
	}

	public String getSourceName() {
		return get("fname");
	}

	public String getMessage() {
		return get("message");
	}

	public Date getTimestamp() {
		return new Date(getLong("timestamp") * 1000);
	}

}
