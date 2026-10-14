package com.github.theholywaffle.teamspeak3.api.event;

/**
 * Interface used to listen to events from a TeamSpeak3 server.
 * <p><i>
 * Directly using this interface requires implementing all listener methods, even if you don't intend to use them.
 * Consider extending {@link TS3EventAdapter} instead.
 * </i></p>
 *
 * @see com.github.theholywaffle.teamspeak3.TS3Query#subscribe(TS3Listener)
 * @see com.github.theholywaffle.teamspeak3.EventApi#subscribe(TS3Listener)
 */
public interface TS3Listener {

    void onTextMessage(TextMessageEvent e);

    void onClientJoin(ClientJoinEvent e);

    void onClientLeave(ClientLeaveEvent e);

    void onServerEdit(ServerEditedEvent e);

    void onChannelEdit(ChannelEditedEvent e);

    void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e);

    void onClientMoved(ClientMovedEvent e);

    void onChannelCreate(ChannelCreateEvent e);

    void onChannelDeleted(ChannelDeletedEvent e);

    void onChannelMoved(ChannelMovedEvent e);

    void onChannelPasswordChanged(ChannelPasswordChangedEvent e);

    void onPrivilegeKeyUsed(PrivilegeKeyUsedEvent e);
}
