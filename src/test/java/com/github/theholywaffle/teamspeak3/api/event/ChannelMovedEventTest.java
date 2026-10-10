package com.github.theholywaffle.teamspeak3.api.event;

import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChannelMovedEventTest {
	@Test void usesNotificationOrderRatherThanChannelInfoProperty() {
		var event = new ChannelMovedEvent(DefaultArrayResponse.parse(
			"cid=3 cpid=2 order=7 invokerid=4 invokername=actor reasonid=1").getFirstResponse());
		assertEquals(3, event.getChannelId());
		assertEquals(2, event.getChannelParentId());
		assertEquals(7, event.getChannelOrder());
		assertEquals(4, event.getInvokerId());
	}

	@Test void distinguishesFirstSiblingFromAbsentOrder() {
		assertEquals(0, new ChannelMovedEvent(DefaultArrayResponse.parse("cid=3 cpid=2 order=0").getFirstResponse()).getChannelOrder());
		assertEquals(-1, new ChannelMovedEvent(DefaultArrayResponse.parse("cid=3 cpid=2").getFirstResponse()).getChannelOrder());
	}
}
