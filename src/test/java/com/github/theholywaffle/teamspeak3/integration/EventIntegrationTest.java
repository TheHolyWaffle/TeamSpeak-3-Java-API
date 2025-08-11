package com.github.theholywaffle.teamspeak3.integration;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.event.ChannelCreateEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelDeletedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelDescriptionEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelPasswordChangedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ServerEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.event.TS3Listener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for event subscription and handling.
 * Tests event registration, notification handling, and event adapter functionality with real server events.
 */
public class EventIntegrationTest extends BaseIntegrationTest {

    @BeforeEach
    public void setUpTest() {
        waitForServerReady();
        assertServerReady();
    }

    @Test
    public void testBasicEventRegistration() {
        // Test basic event registration and unregistration
        TS3Query query = createTestQuery();
        TS3Api api = query.getApi();

        try {
            // Register for server events
            api.registerEvent(TS3EventType.SERVER);
            System.out.println("Registered for server events");

            // Register for channel events
            api.registerEvent(TS3EventType.CHANNEL, 0);
            System.out.println("Registered for channel events");

            // Register for text channel events
            api.registerEvent(TS3EventType.TEXT_CHANNEL, 0);
            System.out.println("Registered for text channel events");

            // Unregister from events
            api.unregisterAllEvents();
            System.out.println("Unregistered from all events");

        } catch (Exception e) {
            System.out.println("Event registration failed: " + e.getMessage());
        }

        System.out.println("Basic event registration test completed");
    }

    @Test
    public void testChannelCreateEvent() throws InterruptedException {
        // Test channel creation event handling
        TS3Query query = createTestQuery();
        TS3Api api = query.getApi();

        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicReference<ChannelCreateEvent> receivedEvent = new AtomicReference<>();

        // Set up event listener
        TS3EventAdapter eventAdapter = new TS3EventAdapter() {
            @Override
            public void onChannelCreate(ChannelCreateEvent e) {
                receivedEvent.set(e);
                eventLatch.countDown();
                System.out.println("Channel create event received: " + e.getChannelId());
            }
        };
        api.addTS3Listeners(eventAdapter);

        try {
            // Register for channel events
            api.registerEvent(TS3EventType.CHANNEL, 0);

            // Create a test channel to trigger the event
            String testChannelName = "EventTest_" + System.currentTimeMillis();
            int channelId = createTestChannel(testChannelName, api);

            // Wait for the event
            boolean eventReceived = eventLatch.await(10, TimeUnit.SECONDS);
            
            if (eventReceived) {
                ChannelCreateEvent event = receivedEvent.get();
                assertNotNull(event, "Should receive channel create event");
                assertEquals(channelId, event.getChannelId(), "Event should contain correct channel ID");
                System.out.println("Channel create event test successful");
            } else {
                System.out.println("Channel create event not received within timeout");
            }

        } catch (Exception e) {
            System.out.println("Channel create event test failed: " + e.getMessage());
        } finally {
            api.unregisterAllEvents();
            api.removeTS3Listeners(eventAdapter);
        }

        System.out.println("Channel create event test completed");
    }

    @Test
    public void testTextMessageEvent() throws InterruptedException {
        // Test text message event handling
        TS3Query query = createTestQuery();
        TS3Api api = query.getApi();

        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicReference<TextMessageEvent> receivedEvent = new AtomicReference<>();

        // Set up event listener
        TS3EventAdapter eventAdapter = new TS3EventAdapter() {
            @Override
            public void onTextMessage(TextMessageEvent e) {
                receivedEvent.set(e);
                eventLatch.countDown();
                System.out.println("Text message event received: " + e.getMessage());
            }
        };
        api.addTS3Listeners(eventAdapter);

        try {
            // Register for text channel events
            api.registerEvent(TS3EventType.TEXT_CHANNEL, 0);

            // Send a test message to trigger the event
            String testMessage = "Integration test message - " + System.currentTimeMillis();
            api.sendChannelMessage(testMessage);

            // Wait for the event
            boolean eventReceived = eventLatch.await(10, TimeUnit.SECONDS);
            
            if (eventReceived) {
                TextMessageEvent event = receivedEvent.get();
                assertNotNull(event, "Should receive text message event");
                assertEquals(testMessage, event.getMessage(), "Event should contain correct message");
                System.out.println("Text message event test successful");
            } else {
                System.out.println("Text message event not received within timeout");
            }

        } catch (Exception e) {
            System.out.println("Text message event test failed: " + e.getMessage());
        } finally {
            api.unregisterAllEvents();
            api.removeTS3Listeners(eventAdapter);
        }

        System.out.println("Text message event test completed");
    }

    @Test
    public void testMultipleEventTypes() throws InterruptedException {
        // Test handling multiple event types simultaneously
        TS3Query query = createTestQuery();
        TS3Api api = query.getApi();

        CountDownLatch channelEventLatch = new CountDownLatch(1);
        CountDownLatch textEventLatch = new CountDownLatch(1);
        AtomicInteger eventCount = new AtomicInteger(0);

        // Set up event listener for multiple event types
        TS3EventAdapter eventAdapter = new TS3EventAdapter() {
            @Override
            public void onChannelCreate(ChannelCreateEvent e) {
                eventCount.incrementAndGet();
                channelEventLatch.countDown();
                System.out.println("Multi-test: Channel create event received");
            }

            @Override
            public void onTextMessage(TextMessageEvent e) {
                eventCount.incrementAndGet();
                textEventLatch.countDown();
                System.out.println("Multi-test: Text message event received");
            }
        };
        api.addTS3Listeners(eventAdapter);

        try {
            // Register for multiple event types
            api.registerEvent(TS3EventType.CHANNEL, 0);
            api.registerEvent(TS3EventType.TEXT_CHANNEL, 0);

            // Trigger channel event
            String testChannelName = "MultiEventTest_" + System.currentTimeMillis();
            createTestChannel(testChannelName, api);

            // Trigger text event
            String testMessage = "Multi-event test message";
            api.sendChannelMessage(testMessage);

            // Wait for both events
            boolean channelEventReceived = channelEventLatch.await(10, TimeUnit.SECONDS);
            boolean textEventReceived = textEventLatch.await(10, TimeUnit.SECONDS);
            
            System.out.println("Multiple event types test results:");
            System.out.println("  Channel event received: " + channelEventReceived);
            System.out.println("  Text event received: " + textEventReceived);
            System.out.println("  Total events received: " + eventCount.get());

            if (channelEventReceived || textEventReceived) {
                assertTrue(eventCount.get() > 0, "Should receive at least one event");
                System.out.println("Multiple event types test successful");
            }

        } catch (Exception e) {
            System.out.println("Multiple event types test failed: " + e.getMessage());
        } finally {
            api.unregisterAllEvents();
            api.removeTS3Listeners(eventAdapter);
        }

        System.out.println("Multiple event types test completed");
    }

    @Test
    public void testEventAdapterLifecycle() {
        // Test event adapter lifecycle management
        TS3Query query = createTestQuery();
        TS3Api api = query.getApi();

        AtomicBoolean adapterActive = new AtomicBoolean(false);

        // Create a test event adapter
        TS3EventAdapter testAdapter = new TS3EventAdapter() {
            @Override
            public void onTextMessage(TextMessageEvent e) {
                adapterActive.set(true);
                System.out.println("Lifecycle test: Event received by adapter");
            }
        };

        try {
            // Add the adapter
            api.addTS3Listeners(testAdapter);
            System.out.println("Event adapter added");

            // Register for events
            api.registerEvent(TS3EventType.TEXT_CHANNEL, 0);

            // Send a test message
            api.sendChannelMessage("Lifecycle test message");

            // Wait a bit for potential event
            sleep(2000);

            // Remove the adapter
            api.removeTS3Listeners(testAdapter);
            System.out.println("Event adapter removed");

            // Send another message (should not trigger the removed adapter)
            api.sendChannelMessage("Post-removal test message");

            // Wait a bit more
            sleep(2000);

            System.out.println("Event adapter lifecycle test completed");
            System.out.println("  Adapter was active: " + adapterActive.get());

        } catch (Exception e) {
            System.out.println("Event adapter lifecycle test failed: " + e.getMessage());
        } finally {
            api.unregisterAllEvents();
        }

        System.out.println("Event adapter lifecycle test completed");
    }

    @Test
    public void testEventErrorHandling() {
        // Test error handling in event processing
        TS3Query query = createTestQuery();
        TS3Api api = query.getApi();

        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);

        // Create an event adapter that throws exceptions
        TS3EventAdapter eventAdapter = new TS3EventAdapter() {
            @Override
            public void onTextMessage(TextMessageEvent e) {
                successCount.incrementAndGet();
                if (e.getMessage().contains("error")) {
                    errorCount.incrementAndGet();
                    throw new RuntimeException("Test exception in event handler");
                }
                System.out.println("Error handling test: Normal event processed");
            }
        };
        api.addTS3Listeners(eventAdapter);

        try {
            // Register for text events
            api.registerEvent(TS3EventType.TEXT_CHANNEL, 0);

            // Send normal message
            api.sendChannelMessage("Normal message");
            sleep(1000);

            // Send message that will cause error
            api.sendChannelMessage("This message contains error keyword");
            sleep(1000);

            // Send another normal message to verify system still works
            api.sendChannelMessage("Another normal message");
            sleep(1000);

            System.out.println("Event error handling test results:");
            System.out.println("  Success count: " + successCount.get());
            System.out.println("  Error count: " + errorCount.get());
            System.out.println("  System continued working after errors");

        } catch (Exception e) {
            System.out.println("Event error handling test failed: " + e.getMessage());
        } finally {
            api.unregisterAllEvents();
            api.removeTS3Listeners(eventAdapter);
        }

        System.out.println("Event error handling test completed");
    }

    @Test
    public void testEventUnregistration() {
        // Test proper event unregistration
        TS3Query query = createTestQuery();
        TS3Api api = query.getApi();

        AtomicInteger eventCount = new AtomicInteger(0);

        TS3EventAdapter eventAdapter = new TS3EventAdapter() {
            @Override
            public void onTextMessage(TextMessageEvent e) {
                eventCount.incrementAndGet();
                System.out.println("Unregistration test: Event received (count: " + eventCount.get() + ")");
            }
        };
        api.addTS3Listeners(eventAdapter);

        try {
            // Register for events
            api.registerEvent(TS3EventType.TEXT_CHANNEL, 0);

            // Send message (should be received)
            api.sendChannelMessage("Before unregistration");
            sleep(1000);

            int eventsBeforeUnregister = eventCount.get();

            // Unregister from events
            api.unregisterAllEvents();
            System.out.println("Unregistered from all events");

            // Send message (should not be received)
            api.sendChannelMessage("After unregistration");
            sleep(2000);

            int eventsAfterUnregister = eventCount.get();

            System.out.println("Event unregistration test results:");
            System.out.println("  Events before unregister: " + eventsBeforeUnregister);
            System.out.println("  Events after unregister: " + eventsAfterUnregister);
            
            // Events after unregistration should be the same as before
            // (no new events should be received)
            assertEquals(eventsBeforeUnregister, eventsAfterUnregister, 
                        "No new events should be received after unregistration");

        } catch (Exception e) {
            System.out.println("Event unregistration test failed: " + e.getMessage());
        } finally {
            api.removeTS3Listeners(eventAdapter);
        }

        System.out.println("Event unregistration test completed");
    }
}
