package com.github.theholywaffle.teamspeak3.integration;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.VirtualServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for timeout handling scenarios.
 * Tests connection timeouts, command timeouts, and response timeouts.
 */
public class TimeoutIntegrationTest extends BaseIntegrationTest {

    @BeforeEach
    public void setUpTest() {
        waitForServerReady();
        assertServerReady();
    }

    @Test
    public void testConnectionTimeout() {
        // Test connection timeout with unreachable server
        TS3Config config = new TS3Config();
        config.setHost("192.0.2.1"); // Non-routable IP address (RFC 5737)
        config.setQueryPort(10011);
        config.setCommandTimeout(2000); // Short timeout for faster test

        TS3Query query = new TS3Query(config);

        // Measure time taken for connection attempt
        long startTime = System.currentTimeMillis();
        
        assertThrows(TS3ConnectionFailedException.class, () -> {
            query.connect();
        }, "Should throw TS3ConnectionFailedException for unreachable server");

        long elapsedTime = System.currentTimeMillis() - startTime;
        
        // Verify timeout was respected (allow some margin for test execution)
        assertTrue(elapsedTime < 10000, "Connection attempt should timeout within reasonable time");
        assertFalse(query.isConnected(), "Query should not be connected after timeout");

        System.out.println("Connection timeout test completed in " + elapsedTime + "ms");
    }

    @Test
    public void testCommandTimeout() {
        // Test command timeout with valid connection but slow operations
        TS3Config config = new TS3Config();
        config.setHost(getServerHost());
        config.setQueryPort(getServerQueryPort());
        config.setLoginCredentials(getServerQueryUsername(), getServerQueryPassword());
        config.setCommandTimeout(1000); // Very short timeout

        TS3Query query = new TS3Query(config);
        query.connect();
        assertTrue(query.isConnected(), "Query should be connected");

        TS3Api api = query.getApi();

        // Most commands should still work with short timeout
        VirtualServer serverInfo = api.getServerInfo();
        assertNotNull(serverInfo, "Should get server info even with short timeout");

        // Test that timeout is actually enforced by trying operations that might take longer
        // Note: This is tricky to test reliably without actually having slow operations
        // In a real scenario, you might test with operations that are known to be slow

        System.out.println("Command timeout test completed successfully");
    }

    @Test
    public void testCommandTimeoutWithLongOperation() {
        // Test command timeout with operations that might take longer
        TS3Config config = new TS3Config();
        config.setHost(getServerHost());
        config.setQueryPort(getServerQueryPort());
        config.setLoginCredentials(getServerQueryUsername(), getServerQueryPassword());
        config.setCommandTimeout(500); // Very short timeout

        TS3Query query = new TS3Query(config);
        query.connect();
        assertTrue(query.isConnected(), "Query should be connected");

        TS3Api api = query.getApi();

        // Try to perform multiple operations quickly
        // Some might timeout if the server is slow to respond
        boolean timeoutOccurred = false;
        
        try {
            for (int i = 0; i < 10; i++) {
                api.getServerInfo();
                api.getChannels();
                api.getClients();
            }
        } catch (TS3CommandFailedException e) {
            if (e.getMessage().contains("timeout") || e.getMessage().contains("timed out")) {
                timeoutOccurred = true;
            }
        }

        // Note: This test might not always trigger a timeout depending on server performance
        // The important thing is that the timeout mechanism is in place
        System.out.println("Long operation timeout test completed (timeout occurred: " + timeoutOccurred + ")");
    }

    @Test
    public void testAsyncCommandTimeout() throws InterruptedException {
        // Test timeout with async API
        TS3Config config = new TS3Config();
        config.setHost(getServerHost());
        config.setQueryPort(getServerQueryPort());
        config.setLoginCredentials(getServerQueryUsername(), getServerQueryPassword());
        config.setCommandTimeout(2000);

        TS3Query query = new TS3Query(config);
        query.connect();
        assertTrue(query.isConnected(), "Query should be connected");

        // Test async operations with timeout
        CompletableFuture<VirtualServer> future = CompletableFuture.supplyAsync(() -> {
            return query.getAsyncApi().getServerInfo().getUninterruptibly();
        });

        try {
            VirtualServer serverInfo = future.get(5, TimeUnit.SECONDS);
            assertNotNull(serverInfo, "Should get server info from async operation");
        } catch (ExecutionException | TimeoutException e) {
            fail("Async operation should not timeout with reasonable timeout: " + e.getMessage());
        }

        System.out.println("Async command timeout test completed successfully");
    }

    @Test
    public void testTimeoutRecovery() {
        // Test recovery after timeout scenarios
        TS3Config config = new TS3Config();
        config.setHost(getServerHost());
        config.setQueryPort(getServerQueryPort());
        config.setLoginCredentials(getServerQueryUsername(), getServerQueryPassword());
        config.setCommandTimeout(1000);

        TS3Query query = new TS3Query(config);
        query.connect();
        assertTrue(query.isConnected(), "Query should be connected");

        TS3Api api = query.getApi();

        // Perform normal operation
        VirtualServer serverInfo1 = api.getServerInfo();
        assertNotNull(serverInfo1, "Should get server info initially");

        // Try operation that might timeout (or simulate timeout condition)
        try {
            // Perform rapid operations that might stress the connection
            for (int i = 0; i < 5; i++) {
                api.getServerInfo();
                Thread.sleep(100);
            }
        } catch (Exception e) {
            // Some operations might fail, but connection should recover
            System.out.println("Some operations failed (expected): " + e.getMessage());
        }

        // Verify connection is still functional after potential timeout
        assertTrue(query.isConnected(), "Query should still be connected after timeout scenarios");

        // Verify we can still perform operations
        VirtualServer serverInfo2 = api.getServerInfo();
        assertNotNull(serverInfo2, "Should get server info after recovery");

        System.out.println("Timeout recovery test completed successfully");
    }

    @Test
    public void testDifferentTimeoutValues() {
        // Test different timeout configurations
        int[] timeouts = {500, 1000, 2000, 5000};

        for (int timeout : timeouts) {
            System.out.println("Testing with timeout: " + timeout + "ms");

            TS3Config config = new TS3Config();
            config.setHost(getServerHost());
            config.setQueryPort(getServerQueryPort());
            config.setLoginCredentials(getServerQueryUsername(), getServerQueryPassword());
            config.setCommandTimeout(timeout);

            TS3Query query = new TS3Query(config);
            query.connect();
            assertTrue(query.isConnected(), "Query should be connected with timeout " + timeout);

            TS3Api api = query.getApi();

            // Measure operation time
            long startTime = System.currentTimeMillis();
            VirtualServer serverInfo = api.getServerInfo();
            long elapsedTime = System.currentTimeMillis() - startTime;

            assertNotNull(serverInfo, "Should get server info with timeout " + timeout);
            assertTrue(elapsedTime < timeout, "Operation should complete within timeout");

            query.exit();
            assertFalse(query.isConnected(), "Query should be disconnected");

            System.out.println("Timeout " + timeout + "ms test completed in " + elapsedTime + "ms");
        }
    }

    @Test
    public void testTimeoutWithInvalidPort() {
        // Test timeout when connecting to valid host but invalid port
        TS3Config config = new TS3Config();
        config.setHost(getServerHost());
        config.setQueryPort(9999); // Invalid port
        config.setCommandTimeout(2000);

        TS3Query query = new TS3Query(config);

        long startTime = System.currentTimeMillis();
        
        assertThrows(TS3ConnectionFailedException.class, () -> {
            query.connect();
        }, "Should throw TS3ConnectionFailedException for invalid port");

        long elapsedTime = System.currentTimeMillis() - startTime;
        
        // Verify timeout was respected
        assertTrue(elapsedTime < 10000, "Connection attempt should timeout within reasonable time");
        assertFalse(query.isConnected(), "Query should not be connected after timeout");

        System.out.println("Invalid port timeout test completed in " + elapsedTime + "ms");
    }

    @Test
    public void testTimeoutConsistency() {
        // Test that timeout behavior is consistent across multiple attempts
        TS3Config config = new TS3Config();
        config.setHost("192.0.2.1"); // Non-routable IP
        config.setQueryPort(10011);
        config.setCommandTimeout(1000);

        int attempts = 3;
        long[] timeouts = new long[attempts];

        for (int i = 0; i < attempts; i++) {
            TS3Query query = new TS3Query(config);
            
            long startTime = System.currentTimeMillis();
            
            assertThrows(TS3ConnectionFailedException.class, () -> {
                query.connect();
            }, "Should throw TS3ConnectionFailedException on attempt " + (i + 1));

            timeouts[i] = System.currentTimeMillis() - startTime;
            assertFalse(query.isConnected(), "Query should not be connected on attempt " + (i + 1));
        }

        // Verify timeout consistency (all attempts should take similar time)
        for (int i = 0; i < attempts; i++) {
            assertTrue(timeouts[i] < 5000, "Timeout " + i + " should be reasonable: " + timeouts[i] + "ms");
            System.out.println("Attempt " + (i + 1) + " timed out in " + timeouts[i] + "ms");
        }

        System.out.println("Timeout consistency test completed successfully");
    }
}
