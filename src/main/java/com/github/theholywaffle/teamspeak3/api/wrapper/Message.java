package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.MessageId;
import java.time.Instant;
import java.util.Map;

public class Message extends Wrapper {

    public Message(Map<String, String> map) {
        super(map);
    }

    public MessageId getId() {
        return requireId("msgid", MessageId::new);
    }

    public String getSenderUniqueIdentifier() {
        return require("cluid");
    }

    public String getSubject() {
        return require("subject");
    }

    public Instant getReceivedDate() {
        return requireInstant("timestamp");
    }

    public boolean hasBeenRead() {
        return requireBoolean("flag_read");
    }
}
