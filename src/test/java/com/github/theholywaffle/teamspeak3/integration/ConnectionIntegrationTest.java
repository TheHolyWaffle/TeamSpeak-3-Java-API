package com.github.theholywaffle.teamspeak3.integration;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.VirtualServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for connection functionality.
 * Tests actual connections to a TeamSpeak 3 server running in a TestContainer.
 */
public class ConnectionIntegrationTest extends BaseIntegrationTest {

    @BeforeEach
    public void setUpTest() {
        waitForServerReady();
        assertServerReady();
    }

    @Test
    public void testBasicConnection() {
        // Test basic connection establishment and teardown
        TS3Query query = createTestQuery();

        // Verify connection is established
        assertTrue(query.isConnected(), "Query should be connected");

        // Get API and verify it works
        TS3Api api = query.getApi();
        assertNotNull(api, "API should not be null");

        // Test basic server info retrieval
        VirtualServer serverInfo = api.getServerInfo();
        assertNotNull(serverInfo, "Server info should not be null");
        assertNotNull(serverInfo.getName(), "Server name should not be null");
        assertTrue(serverInfo.getId() > 0, "Server ID should be positive");

        System.out.println("Connected to server: " + serverInfo.getName() + " (ID: " + serverInfo.getId() + ")");
    }

    @Test
    public void testConnectionWithAuthentication() {
        // Test connection with explicit authentication
        TS3Config config = new TS3Config();
        config.setHost(getServerHost());
        config.setQueryPort(getServerQueryPort());
        config.setLoginCredentials(getServerQueryUsername(), getServerQueryPassword());

        TS3Query query = new TS3Query(config);
        query.connect();

        assertTrue(query.isConnected(), "Query should be connected after authentication");

        // Test that we can perform authenticated operations
        TS3Api api = query.getApi();

        // Select virtual server (requires authentication)
        api.selectVirtualServerById(1);

        // Get server info (should work with authentication)
        VirtualServer serverInfo = api.getServerInfo();
        assertNotNull(serverInfo, "Should be able to get server info after authentication");

        // Clean up
        query.exit();
        assertFalse(query.isConnected(), "Query should be disconnected after exit");

        System.out.println("Authentication test completed successfully");
    }

    @Test
    public void testConnectionTimeout() {
        // Test connection timeout with invalid server
        TS3Config config = new TS3Config();
        config.setHost("192.0.2.1"); // Non-routable IP address (RFC 5737)
        config.setQueryPort(10011);
        config.setCommandTimeout(2000); // Short timeout for faster test

        TS3Query query = new TS3Query(config);

        // Should throw connection failed exception due to timeout
        assertThrows(TS3ConnectionFailedException.class, () -> {
            query.connect();
        }, "Should throw TS3ConnectionFailedException for invalid server");

        assertFalse(query.isConnected(), "Query should not be connected after failed connection");

        System.out.println("Connection timeout test completed successfully");
    }

    @Test
    public void testReconnection() {
        // Test manual reconnection after disconnect
        TS3Query query = createTestQuery();
        assertTrue(query.isConnected(), "Query should be initially connected");

        // Disconnect the query
        query.exit();
        assertFalse(query.isConnected(), "Query should be disconnected after exit");

        // Reconnect
        query.connect();
        assertTrue(query.isConnected(), "Query should be reconnected");

        // Verify functionality after reconnection
        TS3Api api = query.getApi();
        VirtualServer serverInfo = api.getServerInfo();
        assertNotNull(serverInfo, "Should be able to get server info after reconnection");

        System.out.println("Reconnection test completed successfully");
    }

    @Test
    public void testConcurrentConnections() throws Exception {
        // Test multiple concurrent connections
        int numberOfConnections = 3;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfConnections);
        List<CompletableFuture<TS3Query>> futures = new ArrayList<>();

        try {
            // Create multiple connections concurrently
            for (int i = 0; i < numberOfConnections; i++) {
                final int connectionId = i;
                CompletableFuture<TS3Query> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        TS3Config config = new TS3Config();
                        config.setHost(getServerHost());
                        config.setQueryPort(getServerQueryPort());
                        config.setLoginCredentials(getServerQueryUsername(), getServerQueryPassword());

                        TS3Query query = new TS3Query(config);
                        query.connect();

                        // Test that the connection works
                        TS3Api api = query.getApi();
                        api.selectVirtualServerById(1);
                        VirtualServer serverInfo = api.getServerInfo();

                        System.out.println("Connection " + connectionId + " established successfully");
                        return query;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to establish connection " + connectionId, e);
                    }
                }, executor);

                futures.add(future);
            }

            // Wait for all connections to complete
            List<TS3Query> queries = new ArrayList<>();
            for (CompletableFuture<TS3Query> future : futures) {
                TS3Query query = future.get(30, TimeUnit.SECONDS);
                assertTrue(query.isConnected(), "Each query should be connected");
                queries.add(query);
            }

            // Clean up all connections
            for (TS3Query query : queries) {
                query.exit();
                assertFalse(query.isConnected(), "Query should be disconnected after exit");
            }

            System.out.println("Concurrent connections test completed successfully");

        } finally {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testConnectionCleanup() {
        // Test proper resource cleanup
        TS3Query query = createTestQuery();
        assertTrue(query.isConnected(), "Query should be connected");

        // Perform some operations to ensure resources are in use
        TS3Api api = query.getApi();
        VirtualServer serverInfo = api.getServerInfo();
        assertNotNull(serverInfo, "Should be able to get server info");

        // Test explicit disconnect
        query.exit();
        assertFalse(query.isConnected(), "Query should be disconnected after exit");

        // Verify that we can't use the API after disconnect
        assertThrows(Exception.class, () -> {
            api.getServerInfo();
        }, "Should not be able to use API after disconnect");

        System.out.println("Connection cleanup test completed successfully");
    }

    @Test
    public void testConnectionWithInvalidCredentials() {
        // Test connection with invalid credentials
        TS3Config config = new TS3Config();
        config.setHost(getServerHost());
        config.setQueryPort(getServerQueryPort());
        config.setLoginCredentials("invalid_user", "invalid_password");

        TS3Query query = new TS3Query(config);

        // Should throw exception due to invalid credentials
        assertThrows(TS3ConnectionFailedException.class, () -> {
            query.connect();
        }, "Should throw TS3ConnectionFailedException for invalid credentials");

        assertFalse(query.isConnected(), "Query should not be connected with invalid credentials");

        System.out.println("Invalid credentials test completed successfully");
    }
}
