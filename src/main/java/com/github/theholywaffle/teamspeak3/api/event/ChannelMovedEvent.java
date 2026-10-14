package com.github.theholywaffle.teamspeak3.api.event;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

public class ChannelMovedEvent extends BaseEvent {

    public ChannelMovedEvent(Wrapper wrapper) {
        super(wrapper);
    }

    public int getChannelId() {
        return requireInt(ChannelProperty.CID);
    }

    public int getChannelParentId() {
        return requireInt(ChannelProperty.CPID);
    }

    public int getChannelOrder() {
        return requireInt("order");
    }

    @Override
    public void fire(TS3Listener listener) {
        listener.onChannelMoved(this);
    }
}
