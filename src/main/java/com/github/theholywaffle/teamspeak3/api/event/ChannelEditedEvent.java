package com.github.theholywaffle.teamspeak3.api.event;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

public class ChannelEditedEvent extends BaseEvent {

    public ChannelEditedEvent(Wrapper wrapper) {
        super(wrapper);
    }

    public int getChannelId() {
        return requireInt(ChannelProperty.CID);
    }

    @Override
    public void fire(TS3Listener listener) {
        listener.onChannelEdit(this);
    }
}
