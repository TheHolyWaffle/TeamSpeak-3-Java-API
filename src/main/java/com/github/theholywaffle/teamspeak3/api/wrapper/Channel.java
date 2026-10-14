package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import java.util.Map;

public class Channel extends ChannelBase {

    public Channel(Map<String, String> map) {
        super(map);
    }

    @Override
    public int getId() {
        return requireInt(ChannelProperty.CID);
    }

    public int getTotalClientsFamily() {
        return requireInt(ChannelProperty.TOTAL_CLIENTS_FAMILY);
    }

    public int getTotalClients() {
        return requireInt(ChannelProperty.TOTAL_CLIENTS);
    }

    public int getNeededSubscribePower() {
        return requireInt(ChannelProperty.CHANNEL_NEEDED_SUBSCRIBE_POWER);
    }

    /**
     * @return {@code true}, if the channel is empty, {@code false} otherwise.
     */
    public boolean isEmpty() {
        return (getTotalClients() == 0);
    }

    @Override
    public boolean isFamilyEmpty() {
        return (getTotalClientsFamily() == 0);
    }
}
