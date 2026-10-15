package com.github.theholywaffle.teamspeak3.api.event;

import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

public class ChannelDeletedEvent extends BaseEvent {

    public ChannelDeletedEvent(Wrapper wrapper) {
        super(wrapper);
    }

    public ChannelId getChannelId() {
        return requireId(ChannelProperty.CID, ChannelId::of);
    }

    @Override
    public void fire(TS3Listener listener) {
        listener.onChannelDeleted(this);
    }
}
