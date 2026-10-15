package com.github.theholywaffle.teamspeak3.api.event;

import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import java.util.Optional;

public class ChannelMovedEvent extends BaseEvent {

    public ChannelMovedEvent(Wrapper wrapper) {
        super(wrapper);
    }

    public ChannelId getChannelId() {
        return requireId(ChannelProperty.CID, ChannelId::of);
    }

    /** @return the new parent channel, or empty if the channel moved to the top level */
    public Optional<ChannelId> getChannelParentId() {
        return requireOptionalId(ChannelProperty.CPID, ChannelId::of);
    }

    /** @return the sibling channel the channel is now sorted below, or empty if it is the first */
    public Optional<ChannelId> getChannelOrder() {
        return requireOptionalId("order", ChannelId::of);
    }

    @Override
    public void fire(TS3Listener listener) {
        listener.onChannelMoved(this);
    }
}
