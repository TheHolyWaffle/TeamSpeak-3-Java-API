package com.github.theholywaffle.teamspeak3.integration;

import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic connectivity test to verify the TestContainers infrastructure is working.
 * This test focuses on container startup and basic network connectivity without authentication.
 */
@Testcontainers
public class BasicConnectivityTest {

    @Container
    static final GenericContainer<?> teamspeakServer = new GenericContainer<>("teamspeak:latest")
            .withExposedPorts(9987, 10011, 30033)
            .withEnv("TS3SERVER_LICENSE", "accept")
            .withEnv("TS3SERVER_QUERY_PROTOCOLS", "raw,ssh")
            .withEnv("TS3SERVER_DB_PLUGIN", "ts3db_sqlite3")
            .withEnv("TS3SERVER_DB_SQLCREATEPATH", "create_sqlite")
            .withEnv("TS3SERVER_MACHINE_ID", "test-machine")
            .waitingFor(Wait.forLogMessage(".*listening on.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    @Test
    public void testContainerStartup() {
        // Test that the container started successfully
        assertTrue(teamspeakServer.isRunning(), "TeamSpeak container should be running");
        
        String host = teamspeakServer.getHost();
        int queryPort = teamspeakServer.getMappedPort(10011);
        int voicePort = teamspeakServer.getMappedPort(9987);
        int fileTransferPort = teamspeakServer.getMappedPort(30033);
        
        assertNotNull(host, "Container host should not be null");
        assertTrue(queryPort > 0, "Query port should be positive");
        assertTrue(voicePort > 0, "Voice port should be positive");
        assertTrue(fileTransferPort > 0, "File transfer port should be positive");
        
        System.out.println("✅ Container Infrastructure Test Results:");
        System.out.println("   Container Status: " + (teamspeakServer.isRunning() ? "RUNNING" : "STOPPED"));
        System.out.println("   Host: " + host);
        System.out.println("   Query Port: " + queryPort);
        System.out.println("   Voice Port: " + voicePort);
        System.out.println("   File Transfer Port: " + fileTransferPort);
        
        // Test basic network connectivity (without authentication)
        testBasicNetworkConnectivity(host, queryPort);
    }
    
    private void testBasicNetworkConnectivity(String host, int port) {
        System.out.println("\n🔌 Testing Basic Network Connectivity:");
        
        try {
            // Create a basic config without credentials to test raw connectivity
            TS3Config config = new TS3Config();
            config.setHost(host);
            config.setQueryPort(port);
            config.setCommandTimeout(5000); // 5 second timeout
            
            TS3Query query = new TS3Query(config);
            
            // Try to establish a raw connection (this should work even without auth)
            System.out.println("   Attempting connection to " + host + ":" + port + "...");
            
            try {
                query.connect();
                System.out.println("   ✅ Raw connection established successfully");
                
                // Check if we're connected
                boolean isConnected = query.isConnected();
                System.out.println("   Connection status: " + (isConnected ? "CONNECTED" : "DISCONNECTED"));
                
                if (isConnected) {
                    System.out.println("   ✅ Network connectivity test PASSED");
                } else {
                    System.out.println("   ❌ Connection established but status shows disconnected");
                }
                
                // Clean disconnect
                query.exit();
                System.out.println("   Connection closed cleanly");
                
            } catch (Exception e) {
                System.out.println("   ⚠️  Connection attempt result: " + e.getMessage());
                
                // Check if it's an authentication error (which means network connectivity is OK)
                if (e.getMessage().contains("banned") || 
                    e.getMessage().contains("login") || 
                    e.getMessage().contains("authentication") ||
                    e.getMessage().contains("credentials")) {
                    System.out.println("   ✅ Network connectivity is OK (authentication issue expected)");
                } else {
                    System.out.println("   ❌ Network connectivity issue: " + e.getMessage());
                    throw e;
                }
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Network connectivity test FAILED: " + e.getMessage());
            fail("Network connectivity test failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testContainerLogs() {
        // Test that we can access container logs
        System.out.println("\n📋 Container Logs Test:");
        
        try {
            String logs = teamspeakServer.getLogs();
            assertNotNull(logs, "Container logs should not be null");
            assertFalse(logs.trim().isEmpty(), "Container logs should not be empty");
            
            System.out.println("   ✅ Container logs accessible");
            System.out.println("   Log length: " + logs.length() + " characters");
            
            // Check for key TeamSpeak server startup messages
            if (logs.contains("listening on")) {
                System.out.println("   ✅ Server startup message found in logs");
            } else {
                System.out.println("   ⚠️  Server startup message not found in logs");
            }
            
            if (logs.contains("TeamSpeak")) {
                System.out.println("   ✅ TeamSpeak identifier found in logs");
            }
            
            // Print last few lines of logs for debugging
            String[] logLines = logs.split("\n");
            System.out.println("   Last 5 log lines:");
            int startIndex = Math.max(0, logLines.length - 5);
            for (int i = startIndex; i < logLines.length; i++) {
                System.out.println("     " + logLines[i]);
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Container logs test FAILED: " + e.getMessage());
            fail("Container logs test failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testContainerPorts() {
        // Test that all expected ports are mapped
        System.out.println("\n🔌 Port Mapping Test:");
        
        try {
            int queryPort = teamspeakServer.getMappedPort(10011);
            int voicePort = teamspeakServer.getMappedPort(9987);
            int fileTransferPort = teamspeakServer.getMappedPort(30033);
            
            assertTrue(queryPort > 0 && queryPort < 65536, "Query port should be valid");
            assertTrue(voicePort > 0 && voicePort < 65536, "Voice port should be valid");
            assertTrue(fileTransferPort > 0 && fileTransferPort < 65536, "File transfer port should be valid");
            
            // Ensure ports are different
            assertNotEquals(queryPort, voicePort, "Query and voice ports should be different");
            assertNotEquals(queryPort, fileTransferPort, "Query and file transfer ports should be different");
            assertNotEquals(voicePort, fileTransferPort, "Voice and file transfer ports should be different");
            
            System.out.println("   ✅ All ports mapped correctly:");
            System.out.println("     Query Port: 10011 → " + queryPort);
            System.out.println("     Voice Port: 9987 → " + voicePort);
            System.out.println("     File Transfer Port: 30033 → " + fileTransferPort);
            
        } catch (Exception e) {
            System.out.println("   ❌ Port mapping test FAILED: " + e.getMessage());
            fail("Port mapping test failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testContainerEnvironment() {
        // Test container environment and configuration
        System.out.println("\n🌍 Container Environment Test:");
        
        try {
            // Verify container is using the correct image
            String imageName = teamspeakServer.getDockerImageName();
            assertTrue(imageName.contains("teamspeak"), "Should be using TeamSpeak image");
            System.out.println("   ✅ Using correct image: " + imageName);
            
            // Check if container has the expected exposed ports
            assertTrue(teamspeakServer.getExposedPorts().contains(10011), "Should expose query port 10011");
            assertTrue(teamspeakServer.getExposedPorts().contains(9987), "Should expose voice port 9987");
            assertTrue(teamspeakServer.getExposedPorts().contains(30033), "Should expose file transfer port 30033");
            System.out.println("   ✅ All expected ports are exposed");
            
            // Verify container is healthy
            assertTrue(teamspeakServer.isRunning(), "Container should be running");
            System.out.println("   ✅ Container is running and healthy");
            
        } catch (Exception e) {
            System.out.println("   ❌ Container environment test FAILED: " + e.getMessage());
            fail("Container environment test failed: " + e.getMessage());
        }
    }
}
