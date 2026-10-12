package com.github.theholywaffle.teamspeak3;

import java.time.Duration;
import java.util.Objects;

/**
 * Immutable limits for TS3 file sockets, independent of query deadlines. Caller streams remain
 * caller owned and must cooperate with interruption. SSH query encryption does not encrypt files.
 * @param connectTimeout socket connection budget
 * @param readTimeout maximum wait for each socket read
 * @param transferTimeout total data-operation budget, including endpoint resolution and caller I/O
 * @param maxInMemoryBytes maximum allocation for direct downloads and icon stream buffering
 * @param endpointResolver explicit endpoint mapping, used only for file sockets
 */
public record FileTransferConfig(Duration connectTimeout, Duration readTimeout, Duration transferTimeout,
								 int maxInMemoryBytes, FileTransferEndpointResolver endpointResolver) {
	public FileTransferConfig {
		TS3Config.timeout(connectTimeout); TS3Config.timeout(readTimeout); TS3Config.timeout(transferTimeout);
		if (maxInMemoryBytes < 0 || maxInMemoryBytes > Integer.MAX_VALUE - 64)
			throw new IllegalArgumentException("Invalid in-memory file limit");
		Objects.requireNonNull(endpointResolver, "endpointResolver");
	}

	/** @return four-second connect/read, five-minute total budget and 16 MiB in-memory limit */
	public static FileTransferConfig defaults() {
		return new FileTransferConfig(Duration.ofSeconds(4), Duration.ofSeconds(4), Duration.ofMinutes(5),
			16 * 1024 * 1024, FileTransferEndpointResolver.advertised());
	}
}
