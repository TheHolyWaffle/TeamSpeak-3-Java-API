package com.github.theholywaffle.teamspeak3.example;

import com.github.theholywaffle.teamspeak3.EventStatistics;
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
import com.github.theholywaffle.teamspeak3.api.event.PrivilegeKeyUsedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ServerEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3Listener;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;

/**
 * Prints every kind of event a {@link TS3Listener} can receive. Runs until Enter or end of input,
 * then prints the query's event statistics.
 */
public final class EventListenerExample {

    private EventListenerExample() {}

    public static void main(String[] args) {
        ExampleSettings settings = ExampleSettings.fromEnvironment();
        try (TS3Query query = new TS3Query(settings.config())) {
            query.connect();

            TS3Api api = query.getApi();
            api.selectVirtualServerById(settings.virtualServerId());
            api.setNickname(settings.nickname());

            api.events().subscribe(new TS3Listener() {
                @Override
                public void onTextMessage(TextMessageEvent e) {
                    System.out.println("Text message (" + e.getTargetMode() + ") from "
                            + e.getInvokerName().orElse("the server") + ": " + e.getMessage());
                }

                @Override
                public void onClientJoin(ClientJoinEvent e) {
                    System.out.println("Client joined: " + e.getClientNickname());
                }

                @Override
                public void onClientLeave(ClientLeaveEvent e) {
                    System.out.println("Client left: " + e.getClientId());
                }

                @Override
                public void onServerEdit(ServerEditedEvent e) {
                    System.out.println("Server edited by " + e.getInvokerName().orElse("the server"));
                }

                @Override
                public void onChannelEdit(ChannelEditedEvent e) {
                    System.out.println("Channel edited: " + e.getChannelId());
                }

                @Override
                public void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e) {
                    System.out.println("Channel description changed: " + e.getChannelId());
                }

                @Override
                public void onClientMoved(ClientMovedEvent e) {
                    System.out.println("Client " + e.getClientId() + " moved to channel " + e.getTargetChannelId());
                }

                @Override
                public void onChannelCreate(ChannelCreateEvent e) {
                    System.out.println("Channel created: " + e.getChannelId());
                }

                @Override
                public void onChannelDeleted(ChannelDeletedEvent e) {
                    System.out.println("Channel deleted: " + e.getChannelId());
                }

                @Override
                public void onChannelMoved(ChannelMovedEvent e) {
                    System.out.println("Channel moved: " + e.getChannelId());
                }

                @Override
                public void onChannelPasswordChanged(ChannelPasswordChangedEvent e) {
                    System.out.println("Channel password changed: " + e.getChannelId());
                }

                @Override
                public void onPrivilegeKeyUsed(PrivilegeKeyUsedEvent e) {
                    System.out.println("Privilege key used by " + e.getClientUniqueIdentifier());
                }
            });
            api.events().registerAll();
            System.out.println("Listening for server, channel, text and privilege key events.");

            ExampleSettings.awaitStop();
            EventStatistics statistics = query.getEventStatistics();
            System.out.println("Event statistics: " + statistics);
            query.exit();
        }
    }
}
