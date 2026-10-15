package com.github.theholywaffle.teamspeak3.api.event;

import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

public class ChannelCreateEvent extends BaseEvent {

    public ChannelCreateEvent(Wrapper wrapper) {
        super(wrapper);
    }

    public ChannelId getChannelId() {
        return requireId(ChannelProperty.CID, ChannelId::of);
    }

    @Override
    public void fire(TS3Listener listener) {
        listener.onChannelCreate(this);
    }
}
