package com.github.theholywaffle.teamspeak3.api.event;

/**
 * A template class implementing {@link TS3Listener} similar to Swing's event adapters.
 * <p>
 * All methods in this class do nothing, so the user only has to override the interface
 * methods for the events they want to take action on.
 * </p>
 */
public abstract class TS3EventAdapter implements TS3Listener {

    @Override
    public void onTextMessage(TextMessageEvent e) {}

    @Override
    public void onClientJoin(ClientJoinEvent e) {}

    @Override
    public void onClientLeave(ClientLeaveEvent e) {}

    @Override
    public void onServerEdit(ServerEditedEvent e) {}

    @Override
    public void onChannelEdit(ChannelEditedEvent e) {}

    @Override
    public void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e) {}

    @Override
    public void onClientMoved(ClientMovedEvent e) {}

    @Override
    public void onChannelCreate(ChannelCreateEvent e) {}

    @Override
    public void onChannelDeleted(ChannelDeletedEvent e) {}

    @Override
    public void onChannelMoved(ChannelMovedEvent e) {}

    @Override
    public void onChannelPasswordChanged(ChannelPasswordChangedEvent e) {}

    @Override
    public void onPrivilegeKeyUsed(PrivilegeKeyUsedEvent e) {}
}
