package com.github.theholywaffle.teamspeak3.api;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Restricts a channel group membership listing. Empty criteria match everything.
 *
 * @param channelId the channel to list memberships in
 * @param clientDatabaseId the client whose memberships to list
 * @param groupId the channel group whose members to list
 */
public record ChannelGroupFilter(OptionalInt channelId, Optional<ClientDatabaseId> clientDatabaseId, OptionalInt groupId) {

	private static final ChannelGroupFilter ALL = new ChannelGroupFilter(OptionalInt.empty(), Optional.empty(), OptionalInt.empty());

	public ChannelGroupFilter {
		Objects.requireNonNull(clientDatabaseId, "clientDatabaseId");
		for (OptionalInt id : new OptionalInt[] {channelId, groupId}) {
			if (id.isPresent() && id.getAsInt() <= 0) throw new IllegalArgumentException("ID must be positive");
		}
	}

	/** @return a filter matching every membership */
	public static ChannelGroupFilter all() { return ALL; }

	public ChannelGroupFilter inChannel(int id) { return new ChannelGroupFilter(OptionalInt.of(id), clientDatabaseId, groupId); }

	public ChannelGroupFilter ofClient(ClientDatabaseId databaseId) { return new ChannelGroupFilter(channelId, Optional.of(databaseId), groupId); }

	public ChannelGroupFilter inGroup(int id) { return new ChannelGroupFilter(channelId, clientDatabaseId, OptionalInt.of(id)); }
}
