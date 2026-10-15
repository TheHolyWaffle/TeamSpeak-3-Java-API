package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.VirtualServerId;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientIdentity;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ClientIdentityCompatibilityIT {
    @ParameterizedTest
    @EnumSource(ServerQueryCompatibilityIT.Target.class)
    @Timeout(120)
    void identitiesAndMissingClients(ServerQueryCompatibilityIT.Target target) throws Exception {
        try (var server = new TeamSpeakContainer()) {
            server.start();
            try (var query = new TS3Query(server.config(target.protocol))) {
                query.connect();
                var api = query.getApi();
                api.selectVirtualServerById(VirtualServerId.of(1));
                var databaseId = api.databaseClients().list().getFirst().getDatabaseId();
                var info = api.databaseClients().info(databaseId);
                var expected =
                        Optional.of(new ClientIdentity(info.getUniqueIdentifier(), databaseId, info.getNickname()));
                assertEquals(expected, api.databaseClients().findIdentityByUniqueId(info.getUniqueIdentifier()));
                assertEquals(
                        expected,
                        query.getAsyncApi()
                                .databaseClients()
                                .findIdentityByDatabaseId(databaseId)
                                .get(5, TimeUnit.SECONDS));
                assertEquals(
                        Optional.empty(), api.databaseClients().findIdentityByUniqueId("AAAAAAAAAAAAAAAAAAAAAAAAAAA="));
                assertEquals(
                        Optional.empty(),
                        query.getAsyncApi()
                                .databaseClients()
                                .findIdentityByDatabaseId(ClientDatabaseId.of(2147483647))
                                .get(5, TimeUnit.SECONDS));
            }
        }
    }
}
