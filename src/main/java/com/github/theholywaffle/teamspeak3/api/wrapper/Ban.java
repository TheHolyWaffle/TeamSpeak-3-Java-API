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

public class Ban extends Wrapper{

	public Ban(HashMap<String, String> map) {
		super(map);
	}
	
	public int getId(){
		return getInt("banid");
	}
	
	public String getBannedIp(){
		return get("ip");
	}
	
	public String getBannedName(){
		return get("name");
	}
	
	public String getBannedUId(){
		return get("uid");
	}
	
	public String getLastNickname(){
		return get("lastnickname");
	}
	
	public Date getCreatedDate(){
		return new Date(getLong("created")*1000);
	}
	
	public long getDuration(){
		return getLong("duration");
	}
	
	public String getInvokerName(){
		return get("invokername");
	}
	
	public int getInvokerClientDBId(){
		return getInt("invokercldbid");
	}
	
	public String getInvokerUId(){
		return get("invokeruid");
	}
	
	public String getReason(){
		return get("reason");
	}
	
	public int getEnforcements(){
		return getInt("enforcements");
	}
}
