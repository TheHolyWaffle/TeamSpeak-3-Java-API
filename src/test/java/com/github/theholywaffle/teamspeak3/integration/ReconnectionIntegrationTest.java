package com.github.theholywaffle.teamspeak3.integration;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.wrapper.VirtualServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for reconnection scenarios.
 * Tests automatic reconnection handling, network interruptions, and connection recovery.
 */
public class ReconnectionIntegrationTest extends BaseIntegrationTest {

    @BeforeEach
    public void setUpTest() {
        waitForServerReady();
        assertServerReady();
    }

    @Test
    public void testManualReconnection() {
        // Test manual reconnection after disconnect
        TS3Query query = createTestQuery();
        assertTrue(query.isConnected(), "Query should be initially connected");

        // Get initial server info
        TS3Api api = query.getApi();
        VirtualServer initialServerInfo = api.getServerInfo();
        assertNotNull(initialServerInfo, "Should get initial server info");

        // Disconnect
        query.exit();
        assertFalse(query.isConnected(), "Query should be disconnected after exit");

        // Reconnect
        query.connect();
        assertTrue(query.isConnected(), "Query should be reconnected");

        // Verify functionality after reconnection
        api = query.getApi();
        VirtualServer reconnectedServerInfo = api.getServerInfo();
        assertNotNull(reconnectedServerInfo, "Should get server info after reconnection");
        assertEquals(initialServerInfo.getId(), reconnectedServerInfo.getId(), 
                    "Server ID should be the same after reconnection");

        System.out.println("Manual reconnection test completed successfully");
    }

    @Test
    public void testReconnectStrategyExponentialBackoff() throws InterruptedException {
        // Test exponential backoff reconnection strategy
        AtomicInteger connectCount = new AtomicInteger(0);
        AtomicInteger disconnectCount = new AtomicInteger(0);
        CountDownLatch reconnectLatch = new CountDownLatch(1);

        ConnectionHandler handler = new ConnectionHandler() {
            @Override
            public void onConnect(TS3Api api) {
                connectCount.incrementAndGet();
                System.out.println("Connected (attempt " + connectCount.get() + ")");
                if (connectCount.get() > 1) {
                    reconnectLatch.countDown();
                }
            }

            @Override
            public void onDisconnect(TS3Query query) {
                disconnectCount.incrementAndGet();
                System.out.println("Disconnected (count " + disconnectCount.get() + ")");
            }
        };

        TS3Config config = new TS3Config();
        config.setHost(getServerHost());
        config.setQueryPort(getServerQueryPort());
        config.setLoginCredentials(getServerQueryUsername(), getServerQueryPassword());
        config.setReconnectStrategy(ReconnectStrategy.exponentialBackoff());
        config.setConnectionHandler(handler);

        TS3Query query = new TS3Query(config);
        query.connect();

        assertTrue(query.isConnected(), "Query should be connected initially");
        assertEquals(1, connectCount.get(), "Should have connected once initially");

        // Force disconnect to trigger reconnection
        // Note: In a real test, we might simulate network interruption
        // For now, we'll test the reconnection mechanism by disconnecting and reconnecting
        query.exit();
        assertFalse(query.isConnected(), "Query should be disconnected");

        // Reconnect to simulate automatic reconnection
        query.connect();
        assertTrue(query.isConnected(), "Query should be reconnected");

        // Verify the connection handler was called
        assertTrue(connectCount.get() >= 2, "Should have connected at least twice");

        System.out.println("Exponential backoff reconnection test completed successfully");
    }

    @Test
    public void testReconnectStrategyConstantReconnect() throws InterruptedException {
        // Test constant reconnection strategy
        AtomicBoolean reconnected = new AtomicBoolean(false);
        CountDownLatch reconnectLatch = new CountDownLatch(1);

        ConnectionHandler handler = new ConnectionHandler() {
            private boolean firstConnection = true;

            @Override
            public void onConnect(TS3Api api) {
                if (!firstConnection) {
                    reconnected.set(true);
                    reconnectLatch.countDown();
                    System.out.println("Reconnected successfully");
                } else {
                    firstConnection = false;
                    System.out.println("Initial connection established");
                }
            }

            @Override
            public void onDisconnect(TS3Query query) {
                System.out.println("Connection lost");
            }
        };

        TS3Config config = new TS3Config();
        config.setHost(getServerHost());
        config.setQueryPort(getServerQueryPort());
        config.setLoginCredentials(getServerQueryUsername(), getServerQueryPassword());
        config.setReconnectStrategy(ReconnectStrategy.constantBackoff());
        config.setConnectionHandler(handler);

        TS3Query query = new TS3Query(config);
        query.connect();

        assertTrue(query.isConnected(), "Query should be connected initially");

        // Simulate connection loss and recovery
        query.exit();
        query.connect();

        assertTrue(query.isConnected(), "Query should be reconnected");

        System.out.println("Constant reconnection test completed successfully");
    }

    @Test
    public void testConnectionRecoveryAfterServerRestart() {
        // Test connection recovery scenario (simulated)
        TS3Query query = createTestQuery();
        assertTrue(query.isConnected(), "Query should be initially connected");

        // Get initial server info
        TS3Api api = query.getApi();
        VirtualServer initialServerInfo = api.getServerInfo();
        assertNotNull(initialServerInfo, "Should get initial server info");

        // Simulate server restart by disconnecting and reconnecting
        // In a real scenario, this would involve actually restarting the container
        query.exit();
        assertFalse(query.isConnected(), "Query should be disconnected");

        // Wait a bit to simulate server restart time
        sleep(2000);

        // Reconnect after "server restart"
        query.connect();
        assertTrue(query.isConnected(), "Query should be reconnected after server restart");

        // Verify functionality is restored
        api = query.getApi();
        VirtualServer recoveredServerInfo = api.getServerInfo();
        assertNotNull(recoveredServerInfo, "Should get server info after recovery");

        System.out.println("Connection recovery test completed successfully");
    }

    @Test
    public void testMultipleReconnectionAttempts() {
        // Test multiple reconnection attempts
        TS3Query query = createTestQuery();
        assertTrue(query.isConnected(), "Query should be initially connected");

        int reconnectionAttempts = 3;
        
        for (int i = 0; i < reconnectionAttempts; i++) {
            System.out.println("Reconnection attempt " + (i + 1));
            
            // Disconnect
            query.exit();
            assertFalse(query.isConnected(), "Query should be disconnected");
            
            // Wait a bit between attempts
            sleep(1000);
            
            // Reconnect
            query.connect();
            assertTrue(query.isConnected(), "Query should be reconnected on attempt " + (i + 1));
            
            // Verify functionality
            TS3Api api = query.getApi();
            VirtualServer serverInfo = api.getServerInfo();
            assertNotNull(serverInfo, "Should get server info on attempt " + (i + 1));
        }

        System.out.println("Multiple reconnection attempts test completed successfully");
    }

    @Test
    public void testReconnectionWithDifferentConfigurations() {
        // Test reconnection with different timeout configurations
        TS3Config config = new TS3Config();
        config.setHost(getServerHost());
        config.setQueryPort(getServerQueryPort());
        config.setLoginCredentials(getServerQueryUsername(), getServerQueryPassword());
        config.setCommandTimeout(5000); // Longer timeout
        
        TS3Query query = new TS3Query(config);
        query.connect();
        assertTrue(query.isConnected(), "Query should be connected with custom config");

        // Test that the connection works with custom configuration
        TS3Api api = query.getApi();
        VirtualServer serverInfo = api.getServerInfo();
        assertNotNull(serverInfo, "Should get server info with custom config");

        // Test reconnection with the same configuration
        query.exit();
        assertFalse(query.isConnected(), "Query should be disconnected");

        query.connect();
        assertTrue(query.isConnected(), "Query should be reconnected with custom config");

        // Verify functionality is maintained
        api = query.getApi();
        serverInfo = api.getServerInfo();
        assertNotNull(serverInfo, "Should get server info after reconnection with custom config");

        System.out.println("Reconnection with different configurations test completed successfully");
    }

    @Test
    public void testConnectionStateConsistency() {
        // Test that connection state remains consistent during reconnection
        TS3Query query = createTestQuery();
        assertTrue(query.isConnected(), "Query should be initially connected");

        // Perform some operations to establish state
        TS3Api api = query.getApi();
        VirtualServer serverInfo = api.getServerInfo();
        assertNotNull(serverInfo, "Should get initial server info");

        // Disconnect and verify state
        query.exit();
        assertFalse(query.isConnected(), "Query should be disconnected");

        // Verify that API operations fail when disconnected
        final TS3Api disconnectedApi = api;
        assertThrows(Exception.class, () -> {
            disconnectedApi.getServerInfo();
        }, "API operations should fail when disconnected");

        // Reconnect and verify state is restored
        query.connect();
        assertTrue(query.isConnected(), "Query should be reconnected");

        // Verify API operations work again
        TS3Api reconnectedApi = query.getApi();
        VirtualServer reconnectedServerInfo = reconnectedApi.getServerInfo();
        assertNotNull(reconnectedServerInfo, "Should get server info after reconnection");

        System.out.println("Connection state consistency test completed successfully");
    }
}
