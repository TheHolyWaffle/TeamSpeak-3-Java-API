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
package com.github.theholywaffle.teamspeak3.api.event;

import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

public class ClientMovedEvent extends Wrapper implements TS3Event,
		TS3EventEmitter {

	public ClientMovedEvent() {
		super(null);
	}

	public ClientMovedEvent(HashMap<String, String> map) {
		super(map);
	}
	
	public int getClientTargetId(){
		return getInt("ctid");
	}
	
	public int getReasonId(){
		return getInt("reasonid"); 
	}
	
	public int getClientId(){
		return getInt("clid"); 
	}

	public void fire(TS3Listener listener, HashMap<String, String> map) {
		listener.onClientMoved(new ClientMovedEvent(map));		
	}

}
