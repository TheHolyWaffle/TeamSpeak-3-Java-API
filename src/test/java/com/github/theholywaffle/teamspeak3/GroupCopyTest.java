package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import com.github.theholywaffle.teamspeak3.api.ServerGroupId;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class GroupCopyTest {
    private static TS3Config config(FakeServerQuery peer) {
        return new TS3Config()
                .setHost("127.0.0.1")
                .setQueryPort(peer.port())
                .setFloodRate(TS3Query.FloodRate.UNLIMITED)
                .setConnectionHandler(new ConnectionHandler() {
                    @Override
                    public void onConnect(TS3Api api) {
                        api.whoAmI();
                    }

                    @Override
                    public void onDisconnect(TS3Query query) {}
                });
    }

    @Test
    void overwriteWithoutResponseRowSucceedsAndPreservesFailure() throws Exception {
        try (var peer = new FakeServerQuery(p -> {
                    p.expect("whoami");
                    p.write("clid=1\nerror id=0 msg=ok\n");
                    p.expect("servergroupcopy ssgid=9 tsgid=42 name=name type=1");
                    p.write("error id=0 msg=ok\n");
                    p.expect("servergroupcopy ssgid=9 tsgid=43 name=name type=1");
                    p.write("error id=0 msg=ok\n");
                    p.expect("servergroupcopy ssgid=9 tsgid=44 name=name type=1");
                    p.write("error id=2560 msg=invalid\\sgroup\\sID\n");
                    p.finishQuit();
                });
                var query = new TS3Query(config(peer))) {
            query.connect();
            query.getApi()
                    .serverGroups()
                    .overwrite(ServerGroupId.of(9), ServerGroupId.of(42), PermissionGroupDatabaseType.REGULAR);
            assertNull(query.getAsyncApi()
                    .serverGroups()
                    .overwrite(ServerGroupId.of(9), ServerGroupId.of(43), PermissionGroupDatabaseType.REGULAR)
                    .get(5, TimeUnit.SECONDS));
            var error = assertThrows(
                    TS3CommandFailedException.class,
                    () -> query.getApi()
                            .serverGroups()
                            .overwrite(ServerGroupId.of(9), ServerGroupId.of(44), PermissionGroupDatabaseType.REGULAR));
            assertEquals(2560, error.getError().getId());
            assertEquals("invalid group ID", error.getError().getMessage());
            FutureAssertions.awaitCommandAdmission(query);
            query.exit();
            peer.await();
        }
    }

    @Test
    void cancellingQueuedOverwriteReleasesAdmissionBeforeCallbacks() throws Exception {
        try (var peer = new FakeServerQuery(p -> {
                    p.expect("whoami");
                    p.write("clid=1\nerror id=0 msg=ok\n");
                    p.expect("version");
                    p.write("version=probe build=1 platform=test\nerror id=0 msg=ok\n");
                    p.finishQuit();
                });
                var query = new TS3Query(config(peer).setCommandCapacity(1))) {
            var cancelled = query.getAsyncApi()
                    .serverGroups()
                    .overwrite(ServerGroupId.of(9), ServerGroupId.of(42), PermissionGroupDatabaseType.REGULAR);
            var next = new java.util.concurrent.atomic.AtomicReference<
                    java.util.concurrent.CompletableFuture<com.github.theholywaffle.teamspeak3.api.wrapper.Version>>();
            cancelled.whenComplete(
                    (_, _) -> next.set(query.getAsyncApi().instance().version()));
            assertTrue(cancelled.cancel(false));
            assertNotNull(next.get());
            query.connect();
            assertEquals("probe", next.get().get(5, TimeUnit.SECONDS).version());
            FutureAssertions.awaitCommandAdmission(query);
            query.exit();
            peer.await();
        }
    }

    @Test
    void publishedOverwriteResultDoesNotSignalReleasedAdmission() throws Exception {
        var published = new java.util.concurrent.CountDownLatch(1);
        var releaseObserver = new java.util.concurrent.CountDownLatch(1);
        try (var peer = new FakeServerQuery(p -> {
                    p.expect("whoami");
                    p.write("clid=1\nerror id=0 msg=ok\n");
                    p.expect("servergroupcopy ssgid=9 tsgid=42 name=name type=1");
                    p.write("error id=0 msg=ok\n");
                    p.expect("version");
                    p.write("version=probe build=1 platform=test\nerror id=0 msg=ok\n");
                    p.finishQuit();
                });
                var query = new TS3Query(config(peer).setCommandCapacity(1))) {
            var result = query.getAsyncApi()
                    .serverGroups()
                    .overwrite(ServerGroupId.of(9), ServerGroupId.of(42), PermissionGroupDatabaseType.REGULAR);
            var observer = result.whenComplete((_, _) -> {
                published.countDown();
                try {
                    assertTrue(releaseObserver.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(interrupted);
                }
            });
            try {
                query.connect();
                assertTrue(published.await(5, TimeUnit.SECONDS));
                assertNull(result.get(5, TimeUnit.SECONDS));
                assertEquals(0, query.commandAdmission(true).availablePermits());
                var rejected = query.getAsyncApi().instance().version();
                assertTrue(rejected.isCompletedExceptionally(), "Admission failure must be immediate");
                assertThrows(
                        com.github.theholywaffle.teamspeak3.api.exception.TS3QueueFullException.class,
                        () -> FutureAssertions.read(rejected));
            } finally {
                releaseObserver.countDown();
            }
            observer.get(5, TimeUnit.SECONDS);
            FutureAssertions.awaitCommandAdmission(query);
            assertEquals(
                    "probe",
                    query.getAsyncApi()
                            .instance()
                            .version()
                            .get(5, TimeUnit.SECONDS)
                            .version());
            FutureAssertions.awaitCommandAdmission(query);
            query.exit();
            peer.await();
        }
    }
}
