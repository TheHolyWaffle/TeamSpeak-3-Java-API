package com.github.theholywaffle.teamspeak3.api.event;

import java.util.HashMap;

public interface TS3EventEmitter {

	public void fire(TS3Listener listener, HashMap<String,String> map);

}
