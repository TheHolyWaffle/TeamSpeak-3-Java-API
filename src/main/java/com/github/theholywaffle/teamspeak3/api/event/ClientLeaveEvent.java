package com.github.theholywaffle.teamspeak3.api.event;

import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import java.time.Duration;
import java.util.Optional;

public class ClientLeaveEvent extends BaseEvent {

    public ClientLeaveEvent(Wrapper wrapper) {
        super(wrapper);
    }

    /** @return the channel the client was in */
    public ChannelId getClientFromId() {
        return requireId("cfid", ChannelId::of);
    }

    /** @return the channel the client moved to, or empty when it left the server ({@code 0}) */
    public Optional<ChannelId> getClientTargetId() {
        return requireOptionalId("ctid", ChannelId::of);
    }

    /**
     * Gets the duration of the ban that removed the client.
     *
     * @return the ban duration with second precision, {@link Duration#ZERO} for a permanent ban,
     *         or empty if the client was not banned
     */
    public Optional<Duration> getBanDuration() {
        return findSeconds("bantime");
    }

    public ClientId getClientId() {
        return requireClientId("clid");
    }

    @Override
    public void fire(TS3Listener listener) {
        listener.onClientLeave(this);
    }
}
