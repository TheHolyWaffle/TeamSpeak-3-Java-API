package com.github.theholywaffle.teamspeak3.integration;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages the lifecycle of test resources including connections, channels, and clients
 * for integration testing. Provides cleanup capabilities to ensure tests don't interfere
 * with each other.
 */
public class TestLifecycleManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TestLifecycleManager.class);
    
    private final ConcurrentMap<String, TS3Query> activeQueries = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<Integer>> testChannels = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<Integer>> testClients = new ConcurrentHashMap<>();
    private final String serverHost;
    private final int serverQueryPort;
    private final String serverQueryUsername;
    private final String serverQueryPassword;
    
    /**
     * Creates a new test lifecycle manager.
     * 
     * @param serverHost the TeamSpeak server host
     * @param serverQueryPort the server query port
     * @param serverQueryUsername the server query username
     * @param serverQueryPassword the server query password
     */
    public TestLifecycleManager(String serverHost, int serverQueryPort, 
                               String serverQueryUsername, String serverQueryPassword) {
        this.serverHost = serverHost;
        this.serverQueryPort = serverQueryPort;
        this.serverQueryUsername = serverQueryUsername;
        this.serverQueryPassword = serverQueryPassword;
    }
    
    /**
     * Creates a new TS3Query connection for testing.
     * The connection is automatically tracked and will be cleaned up.
     * 
     * @param testName the name of the test (for tracking purposes)
     * @return a configured and connected TS3Query instance
     * @throws Exception if connection fails
     */
    public TS3Query createTestQuery(String testName) throws Exception {
        logger.debug("Creating test query for: {}", testName);
        
        TS3Config config = new TS3Config();
        config.setHost(serverHost);
        config.setQueryPort(serverQueryPort);
        config.setLoginCredentials(serverQueryUsername, serverQueryPassword);
        config.setEnableCommunicationsLogging(true);
        
        TS3Query query = new TS3Query(config);
        query.connect();
        
        // Select the first virtual server (default)
        TS3Api api = query.getApi();
        api.selectVirtualServerById(1);
        
        activeQueries.put(testName, query);
        logger.debug("Test query created and connected for: {}", testName);
        
        return query;
    }
    
    /**
     * Creates a test channel and tracks it for cleanup.
     * 
     * @param testName the name of the test
     * @param channelName the name of the channel to create
     * @param api the TS3Api instance to use
     * @return the channel ID
     */
    public int createTestChannel(String testName, String channelName, TS3Api api) {
        logger.debug("Creating test channel '{}' for test: {}", channelName, testName);
        
        int channelId = api.createChannel(channelName, null);
        
        testChannels.computeIfAbsent(testName, k -> new ArrayList<>()).add(channelId);
        logger.debug("Test channel '{}' created with ID {} for test: {}", channelName, channelId, testName);
        
        return channelId;
    }
    
    /**
     * Tracks a client ID for cleanup.
     * 
     * @param testName the name of the test
     * @param clientId the client ID to track
     */
    public void trackTestClient(String testName, int clientId) {
        testClients.computeIfAbsent(testName, k -> new ArrayList<>()).add(clientId);
        logger.debug("Tracking test client {} for test: {}", clientId, testName);
    }
    
    /**
     * Cleans up all resources for a specific test.
     * 
     * @param testName the name of the test to clean up
     */
    public void cleanupTest(String testName) {
        logger.debug("Cleaning up resources for test: {}", testName);
        
        TS3Query query = activeQueries.get(testName);
        if (query != null && query.isConnected()) {
            try {
                TS3Api api = query.getApi();
                
                // Clean up test channels
                List<Integer> channels = testChannels.get(testName);
                if (channels != null) {
                    for (Integer channelId : channels) {
                        try {
                            api.deleteChannel(channelId);
                            logger.debug("Deleted test channel {} for test: {}", channelId, testName);
                        } catch (Exception e) {
                            logger.warn("Failed to delete test channel {} for test {}: {}", 
                                      channelId, testName, e.getMessage());
                        }
                    }
                }
                
                // Clean up test clients (kick them)
                List<Integer> clients = testClients.get(testName);
                if (clients != null) {
                    for (Integer clientId : clients) {
                        try {
                            api.kickClientFromServer("Test cleanup", clientId);
                            logger.debug("Kicked test client {} for test: {}", clientId, testName);
                        } catch (Exception e) {
                            logger.warn("Failed to kick test client {} for test {}: {}", 
                                      clientId, testName, e.getMessage());
                        }
                    }
                }
                
            } catch (Exception e) {
                logger.warn("Error during resource cleanup for test {}: {}", testName, e.getMessage());
            }
        }
        
        // Remove from tracking maps
        testChannels.remove(testName);
        testClients.remove(testName);
        
        logger.debug("Resource cleanup completed for test: {}", testName);
    }
    
    /**
     * Disconnects and cleans up a specific query connection.
     * 
     * @param testName the name of the test
     */
    public void disconnectTestQuery(String testName) {
        TS3Query query = activeQueries.remove(testName);
        if (query != null) {
            try {
                if (query.isConnected()) {
                    query.exit();
                }
                logger.debug("Disconnected test query for: {}", testName);
            } catch (Exception e) {
                logger.warn("Error disconnecting test query for {}: {}", testName, e.getMessage());
            }
        }
    }
    
    /**
     * Cleans up all active resources and connections.
     * Should be called during test suite teardown.
     */
    public void cleanupAll() {
        logger.debug("Cleaning up all test resources");
        
        // Clean up all test resources
        for (String testName : new ArrayList<>(activeQueries.keySet())) {
            cleanupTest(testName);
            disconnectTestQuery(testName);
        }
        
        activeQueries.clear();
        testChannels.clear();
        testClients.clear();
        
        logger.debug("All test resources cleaned up");
    }
    
    /**
     * Gets the number of active query connections.
     * 
     * @return the number of active connections
     */
    public int getActiveConnectionCount() {
        return activeQueries.size();
    }
    
    /**
     * Checks if a test has active resources.
     * 
     * @param testName the name of the test
     * @return true if the test has active resources
     */
    public boolean hasActiveResources(String testName) {
        return activeQueries.containsKey(testName) || 
               testChannels.containsKey(testName) || 
               testClients.containsKey(testName);
    }
    
    /**
     * Waits for the server to be ready for connections.
     * Performs basic connectivity checks.
     * 
     * @throws Exception if the server is not ready
     */
    public void waitForServerReady() throws Exception {
        logger.debug("Waiting for server to be ready");
        
        int maxAttempts = 30;
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            try {
                TS3Config config = new TS3Config();
                config.setHost(serverHost);
                config.setQueryPort(serverQueryPort);
                config.setLoginCredentials(serverQueryUsername, serverQueryPassword);
                config.setCommandTimeout(2000); // Short timeout for readiness check
                
                TS3Query testQuery = new TS3Query(config);
                testQuery.connect();
                
                // Try to get server info to verify connection
                TS3Api api = testQuery.getApi();
                api.getServerInfo();
                
                testQuery.exit();
                logger.debug("Server is ready after {} attempts", attempt + 1);
                return;
                
            } catch (Exception e) {
                attempt++;
                if (attempt >= maxAttempts) {
                    throw new Exception("Server not ready after " + maxAttempts + " attempts: " + e.getMessage(), e);
                }
                
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new Exception("Interrupted while waiting for server", ie);
                }
            }
        }
    }
}
