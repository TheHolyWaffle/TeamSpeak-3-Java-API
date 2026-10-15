package com.github.theholywaffle.teamspeak3.api.event;

/**
 * Interface used to listen to events from a TeamSpeak3 server.
 * <p>All callbacks default to no-ops. Implement only the events you need.</p>
 *
 * @see com.github.theholywaffle.teamspeak3.TS3Query#subscribe(TS3Listener)
 * @see com.github.theholywaffle.teamspeak3.EventApi#subscribe(TS3Listener)
 */
public interface TS3Listener {

    default void onTextMessage(TextMessageEvent e) {}

    default void onClientJoin(ClientJoinEvent e) {}

    default void onClientLeave(ClientLeaveEvent e) {}

    default void onServerEdit(ServerEditedEvent e) {}

    default void onChannelEdit(ChannelEditedEvent e) {}

    default void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e) {}

    default void onClientMoved(ClientMovedEvent e) {}

    default void onChannelCreate(ChannelCreateEvent e) {}

    default void onChannelDeleted(ChannelDeletedEvent e) {}

    default void onChannelMoved(ChannelMovedEvent e) {}

    default void onChannelPasswordChanged(ChannelPasswordChangedEvent e) {}

    default void onPrivilegeKeyUsed(PrivilegeKeyUsedEvent e) {}
}
