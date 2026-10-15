package com.github.theholywaffle.teamspeak3.api.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.exception.TS3InvalidValueException;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChannelMovedEventTest {
    @Test
    void usesNotificationOrderRatherThanChannelInfoProperty() {
        var event = event("cid=3 cpid=2 order=7 invokerid=4 invokername=actor reasonid=1");
        assertEquals(ChannelId.of(3), event.getChannelId());
        assertEquals(Optional.of(ChannelId.of(2)), event.getChannelParentId());
        assertEquals(Optional.of(ChannelId.of(7)), event.getChannelOrder());
        assertEquals(Optional.of(ClientId.of(4)), event.getInvokerId());
    }

    @Test
    void distinguishesFirstSiblingFromAbsentOrder() {
        assertEquals(Optional.empty(), event("cid=3 cpid=0 order=0").getChannelParentId());
        assertEquals(Optional.empty(), event("cid=3 cpid=2 order=0").getChannelOrder());
        var missing = assertThrows(
                TS3InvalidValueException.class, () -> event("cid=3 cpid=2").getChannelOrder());
        assertEquals("order", missing.getProperty());
        assertEquals(Optional.empty(), missing.getRawValue());
    }

    private static ChannelMovedEvent event(String raw) {
        return new ChannelMovedEvent(
                DefaultArrayResponse.parse(raw).getFirstResponse().orElseThrow());
    }
}
