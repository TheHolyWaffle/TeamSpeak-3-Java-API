package com.github.theholywaffle.teamspeak3.api.event;

import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

public class ClientLeaveEvent extends BaseEvent {

    public ClientLeaveEvent(Wrapper wrapper) {
        super(wrapper);
    }

    public int getClientFromId() {
        return requireInt("cfid");
    }

    public int getClientTargetId() {
        return requireInt("ctid");
    }

    public ClientId getClientId() {
        return requireClientId("clid");
    }

    @Override
    public void fire(TS3Listener listener) {
        listener.onClientLeave(this);
    }
}
