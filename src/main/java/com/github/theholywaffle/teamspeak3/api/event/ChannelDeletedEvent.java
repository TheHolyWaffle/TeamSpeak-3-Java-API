package com.github.theholywaffle.teamspeak3.api.event;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

public class ChannelDeletedEvent extends BaseEvent {

    public ChannelDeletedEvent(Wrapper wrapper) {
        super(wrapper);
    }

    public int getChannelId() {
        return requireInt(ChannelProperty.CID);
    }

    @Override
    public void fire(TS3Listener listener) {
        listener.onChannelDeleted(this);
    }
}
