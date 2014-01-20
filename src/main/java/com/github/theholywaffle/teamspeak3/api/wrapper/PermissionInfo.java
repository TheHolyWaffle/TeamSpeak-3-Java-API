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

import java.util.HashMap;

public class PermissionInfo extends Wrapper {

	public PermissionInfo(HashMap<String, String> map) {
		super(map);
	}
	
	public int getId(){
		return getInt("permid");
	}
	
	public String getName(){
		return get("permname");
	}

	public String getDescription(){
		return get("permdesc");
	}
	

}
