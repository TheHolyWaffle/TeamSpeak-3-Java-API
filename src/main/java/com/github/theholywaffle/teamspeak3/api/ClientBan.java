package com.github.theholywaffle.teamspeak3.api;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * A request to ban online clients by their current client IDs.
 *
 * @param clientIds the online client IDs to ban
 * @param duration the ban duration; {@link Duration#ZERO} is permanent
 * @param reason the ban reason, or {@code null}
 * @param continueOnError whether to continue with the remaining clients after one fails
 */
public record ClientBan(List<ClientId> clientIds, Duration duration, String reason, boolean continueOnError) {

	public ClientBan {
		clientIds = List.copyOf(clientIds);
		if (clientIds.isEmpty()) throw new IllegalArgumentException("At least one client ID is required");
		Objects.requireNonNull(duration, "duration");
		if (duration.isNegative()) throw new IllegalArgumentException("Duration must not be negative");
	}

	/** @return a permanent ban without reason for the given clients */
	public static ClientBan of(ClientId... clientIds) {
		return new ClientBan(List.of(clientIds), Duration.ZERO, null, false);
	}

	/** @param banDuration the ban duration, truncated to seconds; {@link Duration#ZERO} is permanent */
	public ClientBan withDuration(Duration banDuration) { return new ClientBan(clientIds, banDuration, reason, continueOnError); }

	public ClientBan withReason(String banReason) { return new ClientBan(clientIds, duration, banReason, continueOnError); }

	public ClientBan continuingOnError() { return new ClientBan(clientIds, duration, reason, true); }
}
