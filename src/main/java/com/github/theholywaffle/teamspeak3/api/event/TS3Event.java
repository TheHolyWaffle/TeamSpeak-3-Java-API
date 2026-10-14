package com.github.theholywaffle.teamspeak3.api.event;

public interface TS3Event {

    void fire(TS3Listener listener);
}
