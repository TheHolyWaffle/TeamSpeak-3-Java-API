package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.ApiKeyScope;
import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.commands.ApiKeyCommands;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
class ApiKeyApiTest {
    private static final String OK = FakeServerQuery.OK;

    @Test
    void encodesRequestsAndMapsTheServerResponses() throws Exception {
        try (var peer = new FakeServerQuery(p -> {
                    // Recorded from TS3 3.13.8.
                    p.expect("apikeyadd scope=read lifetime=14");
                    p.write("apikey=BAsecret id=4 sid=0 cldbid=1 scope=read time_left=1209599"
                            + " created_at=1791972385 expires_at=1793181985\n" + OK);
                    p.expect("apikeyadd scope=manage lifetime=0 cldbid=7");
                    p.write("apikey=BAother id=5 sid=1 cldbid=7 scope=manage time_left=unlimited"
                            + " created_at=1791972385 expires_at=1791972385\n" + OK);
                    p.expect("apikeylist");
                    p.write("error id=1281 msg=database\\sempty\\sresult\\sset\n");
                    p.expect("apikeylist cldbid=7");
                    p.write("id=5 sid=1 cldbid=7 scope=manage time_left=unlimited created_at=1791972385"
                            + " expires_at=1791972385\n" + OK);
                    p.expect("apikeylist cldbid=*");
                    p.write("id=4 sid=0 cldbid=1 scope=read time_left=1209599 created_at=1791972385"
                            + " expires_at=1793181985|id=5 sid=1 cldbid=7 scope=future time_left=unlimited"
                            + " created_at=1791972385 expires_at=1791972385\n" + OK);
                    p.expect("apikeydel id=5");
                    p.write(OK);
                    p.expect("apikeydel id=999");
                    p.write("error id=5123 msg=invalid\\sapikey\\sid\n");
                    p.finishQuit();
                });
                var query = new TS3Query(new TS3Config()
                        .setHost("127.0.0.1")
                        .setQueryPort(peer.port())
                        .setFloodRate(TS3Query.FloodRate.UNLIMITED))) {
            query.connect();
            var keys = query.getApi().apiKeys();

            var expiring = keys.add(ApiKeyScope.READ, Duration.ofDays(14));
            assertEquals(Optional.of("BAsecret"), expiring.getSecret());
            assertFalse(expiring.toString().contains("BAsecret"));
            assertEquals(4, expiring.getId());
            assertEquals(0, expiring.getVirtualServerId());
            assertEquals(ClientDatabaseId.of(1), expiring.getOwner());
            assertEquals(ApiKeyScope.READ, expiring.getScope());
            assertFalse(expiring.isUnlimited());
            assertEquals(Optional.of(Duration.ofSeconds(1209599)), expiring.getTimeLeft());
            assertEquals(Instant.ofEpochSecond(1791972385), expiring.getCreated());
            assertEquals(Optional.of(Instant.ofEpochSecond(1793181985)), expiring.getExpires());

            var unlimited = keys.add(ApiKeyScope.MANAGE, Duration.ZERO, ClientDatabaseId.of(7));
            assertTrue(unlimited.isUnlimited());
            assertEquals(Optional.empty(), unlimited.getTimeLeft());
            assertEquals(Optional.empty(), unlimited.getExpires());
            assertEquals(1, unlimited.getVirtualServerId());

            assertEquals(List.of(), keys.list());
            var owned = keys.list(ClientDatabaseId.of(7));
            assertEquals(Optional.empty(), owned.getFirst().getSecret());
            var all = keys.listAll();
            assertEquals(List.of(4, 5), all.stream().map(key -> key.getId()).toList());
            assertEquals(ApiKeyScope.UNKNOWN, all.get(1).getScope());

            keys.delete(5);
            assertEquals(
                    5123,
                    assertThrows(TS3CommandFailedException.class, () -> keys.delete(999))
                            .getErrorId());
            query.exit();
        }
    }

    @Test
    void rejectsLifetimesAndScopesTheServerCannotRepresent() {
        for (Duration invalid : List.of(Duration.ofDays(-1), Duration.ofHours(36), Duration.ofSeconds(1))) {
            assertThrows(
                    IllegalArgumentException.class, () -> ApiKeyCommands.apiKeyAdd(ApiKeyScope.READ, invalid, null));
        }
        assertThrows(IllegalArgumentException.class, () -> ApiKeyCommands.apiKeyAdd(ApiKeyScope.READ, null, null));
        assertThrows(IllegalArgumentException.class, () -> ApiKeyCommands.apiKeyAdd(null, Duration.ZERO, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> ApiKeyCommands.apiKeyAdd(ApiKeyScope.UNKNOWN, Duration.ZERO, null));
        assertEquals(
                "apikeyadd scope=write lifetime=0",
                ApiKeyCommands.apiKeyAdd(ApiKeyScope.WRITE, Duration.ZERO, null).encoded());
        assertEquals(ApiKeyScope.MANAGE, ApiKeyScope.fromName("manage"));
        assertEquals(ApiKeyScope.UNKNOWN, ApiKeyScope.fromName("unknown"));
    }
}
