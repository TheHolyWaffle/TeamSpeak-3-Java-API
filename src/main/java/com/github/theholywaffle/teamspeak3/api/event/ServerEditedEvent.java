package com.github.theholywaffle.teamspeak3.api.event;

import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

/**
 * Notification of a virtual-server edit. The payload is partial and may omit
 * changed properties and the reason ID, depending on the server. Check field
 * presence in {@link #getMap()} and fetch server information for current values.
 */
public class ServerEditedEvent extends BaseEvent {

    public ServerEditedEvent(Wrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void fire(TS3Listener listener) {
        listener.onServerEdit(this);
    }
}
