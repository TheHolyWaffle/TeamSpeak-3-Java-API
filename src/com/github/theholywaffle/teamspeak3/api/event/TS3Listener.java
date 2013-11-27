package com.github.theholywaffle.teamspeak3.api.event;

public interface TS3Listener {

	public void onTextMessage(TextMessageEvent e);

	public void onClientJoin(ClientJoinEvent e);

	public void onClientLeave(ClientLeaveEvent e);

	public void onServerEdit(ServerEditedEvent e);

	public void onChannelEdit(ChannelEditedEvent e);

	public void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e);

	public void onClientMoved(ClientMovedEvent e);

}
