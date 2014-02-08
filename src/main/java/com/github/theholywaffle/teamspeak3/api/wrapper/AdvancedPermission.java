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
package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.HashMap;

public class AdvancedPermission extends Wrapper{

	public AdvancedPermission(HashMap<String, String> map) {
		super(map);
	}
	
	public int getType(){
		return getInt("t");
	}
	
	public int getId1(){
		return getInt("id1");
	}
	
	public int getId2(){
		return getInt("id2");
	}
	
	public int getPermissionId(){
		return getInt("p");
	}
	
	public int getPermissionValue(){
		return getInt("v");
	}
	
	public boolean isNegated(){
		return getBoolean("n");
	}
	
	public boolean isSkipped(){
		return getBoolean("s");
	}



}
