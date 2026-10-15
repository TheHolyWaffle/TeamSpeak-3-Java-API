package com.github.theholywaffle.teamspeak3.api.event;

import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

public class ClientMovedEvent extends BaseEvent {

    public ClientMovedEvent(Wrapper wrapper) {
        super(wrapper);
    }

    /**
     * Gets the ID of the channel the client has moved to.
     *
     * @return the ID of the target channel
     *
     * @see Channel#getId()
     */
    public ChannelId getTargetChannelId() {
        return requireId("ctid", ChannelId::of);
    }

    public ClientId getClientId() {
        return requireClientId("clid");
    }

    @Override
    public void fire(TS3Listener listener) {
        listener.onClientMoved(this);
    }
}
