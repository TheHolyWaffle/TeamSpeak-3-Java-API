package com.github.theholywaffle.teamspeak3.api.event;

import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

public class ChannelDescriptionEditedEvent extends Wrapper implements TS3Event,
		TS3EventEmitter {

	public ChannelDescriptionEditedEvent() {
		super(null);
	}

	public ChannelDescriptionEditedEvent(HashMap<String, String> map) {
		super(map);
	}

	public int getChannelId() {
		return getInt("cid");
	}

	public void fire(TS3Listener listener, HashMap<String, String> map) {
		listener.onChannelDescriptionChanged(new ChannelDescriptionEditedEvent(
				map));
	}

}
