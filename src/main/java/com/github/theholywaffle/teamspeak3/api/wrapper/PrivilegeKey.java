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

import java.util.Date;
import java.util.HashMap;

public class PrivilegeKey extends Wrapper {

	public PrivilegeKey(HashMap<String, String> map) {
		super(map);
	}
	
	public String getToken(){
		return get("token");
	}
	
	public int getType(){
		return getInt("token_type");
	}
	
	public boolean isServerGroupToken(){
		return getType()==0;
	}
	
	public boolean isChannelGroupToken(){
		return !isServerGroupToken();
	}
	
	public int getGroupId(){
		return getInt("token_id1");
	}
	
	public int getChannelId(){
		return getInt("token_id2");
	}
	
	public Date getCreated(){
		return new Date(getLong("token_created")*1000);
	}

	public String getDescription(){
		return get("token_description");
	}
}
