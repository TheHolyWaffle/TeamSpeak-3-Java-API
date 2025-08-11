package com.github.theholywaffle.teamspeak3.integration;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import com.github.theholywaffle.teamspeak3.api.wrapper.VirtualServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods for integration testing with TeamSpeak server.
 * Provides common operations and assertions for integration tests.
 */
public class IntegrationTestUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(IntegrationTestUtils.class);
    
    /**
     * Waits for a condition to be true with timeout.
     * 
     * @param condition the condition to check
     * @param timeoutSeconds the timeout in seconds
     * @param intervalMs the check interval in milliseconds
     * @return true if condition became true within timeout
     */
    public static boolean waitForCondition(BooleanSupplier condition, int timeoutSeconds, long intervalMs) {
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                if (condition.getAsBoolean()) {
                    return true;
                }
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                logger.debug("Exception while checking condition: {}", e.getMessage());
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }
    
    /**
     * Waits for a condition to be true with default timeout and interval.
     * 
     * @param condition the condition to check
     * @return true if condition became true within timeout
     */
    public static boolean waitForCondition(BooleanSupplier condition) {
        return waitForCondition(condition, 30, 500);
    }
    
    /**
     * Waits for a channel to exist on the server.
     * 
     * @param api the TS3Api instance
     * @param channelName the name of the channel to wait for
     * @param timeoutSeconds the timeout in seconds
     * @return the channel if found, null otherwise
     */
    public static Channel waitForChannel(TS3Api api, String channelName, int timeoutSeconds) {
        logger.debug("Waiting for channel '{}' to exist", channelName);
        
        boolean found = waitForCondition(() -> {
            List<Channel> channels = api.getChannels();
            return channels.stream().anyMatch(c -> channelName.equals(c.getName()));
        }, timeoutSeconds, 1000);
        
        if (found) {
            List<Channel> channels = api.getChannels();
            Channel channel = channels.stream()
                    .filter(c -> channelName.equals(c.getName()))
                    .findFirst()
                    .orElse(null);
            logger.debug("Channel '{}' found with ID: {}", channelName, channel != null ? channel.getId() : "null");
            return channel;
        }
        
        logger.debug("Channel '{}' not found within {} seconds", channelName, timeoutSeconds);
        return null;
    }
    
    /**
     * Waits for a client to connect to the server.
     * 
     * @param api the TS3Api instance
     * @param nickname the nickname of the client to wait for
     * @param timeoutSeconds the timeout in seconds
     * @return the client if found, null otherwise
     */
    public static Client waitForClient(TS3Api api, String nickname, int timeoutSeconds) {
        logger.debug("Waiting for client '{}' to connect", nickname);
        
        boolean found = waitForCondition(() -> {
            List<Client> clients = api.getClients();
            return clients.stream().anyMatch(c -> nickname.equals(c.getNickname()));
        }, timeoutSeconds, 1000);
        
        if (found) {
            List<Client> clients = api.getClients();
            Client client = clients.stream()
                    .filter(c -> nickname.equals(c.getNickname()))
                    .findFirst()
                    .orElse(null);
            logger.debug("Client '{}' found with ID: {}", nickname, client != null ? client.getId() : "null");
            return client;
        }
        
        logger.debug("Client '{}' not found within {} seconds", nickname, timeoutSeconds);
        return null;
    }
    
    /**
     * Waits for a client to disconnect from the server.
     * 
     * @param api the TS3Api instance
     * @param clientId the ID of the client to wait for disconnection
     * @param timeoutSeconds the timeout in seconds
     * @return true if client disconnected within timeout
     */
    public static boolean waitForClientDisconnect(TS3Api api, int clientId, int timeoutSeconds) {
        logger.debug("Waiting for client {} to disconnect", clientId);
        
        return waitForCondition(() -> {
            List<Client> clients = api.getClients();
            return clients.stream().noneMatch(c -> c.getId() == clientId);
        }, timeoutSeconds, 1000);
    }
    
    /**
     * Gets server information and logs it for debugging.
     *
     * @param api the TS3Api instance
     * @return the virtual server information
     */
    public static VirtualServer getAndLogServerInfo(TS3Api api) {
        VirtualServer server = api.getServerInfo();
        logger.debug("Server Info - Name: {}, Status: {}, Clients: {}/{}",
                    server.getName(),
                    server.getStatus(),
                    server.getClientsOnline(),
                    server.getMaxClients());
        return server;
    }
    
    /**
     * Creates a test channel with a unique name.
     * 
     * @param api the TS3Api instance
     * @param baseName the base name for the channel
     * @return the channel ID
     */
    public static int createUniqueTestChannel(TS3Api api, String baseName) {
        String uniqueName = baseName + "_" + System.currentTimeMillis();
        logger.debug("Creating unique test channel: {}", uniqueName);
        return api.createChannel(uniqueName, null);
    }
    
    /**
     * Sends a test message to a channel and verifies it was sent.
     * 
     * @param api the TS3Api instance
     * @param channelId the channel ID to send message to
     * @param message the message to send
     * @return true if message was sent successfully
     */
    public static boolean sendTestMessage(TS3Api api, int channelId, String message) {
        try {
            // Move to the channel first
            api.moveQuery(channelId);
            
            // Send the message
            api.sendChannelMessage(message);
            
            logger.debug("Test message sent to channel {}: {}", channelId, message);
            return true;
        } catch (Exception e) {
            logger.warn("Failed to send test message to channel {}: {}", channelId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifies that the server is in a clean state for testing.
     * 
     * @param api the TS3Api instance
     * @return true if server is in clean state
     */
    public static boolean verifyCleanServerState(TS3Api api) {
        try {
            VirtualServer server = api.getServerInfo();
            List<Channel> channels = api.getChannels();
            List<Client> clients = api.getClients();
            
            // Log current state
            logger.debug("Server state - Channels: {}, Clients: {}", channels.size(), clients.size());
            
            // Basic checks - server should be running and responsive
            if (server == null) {
                logger.warn("Server info is null");
                return false;
            }
            
            if (channels.isEmpty()) {
                logger.warn("No channels found on server");
                return false;
            }
            
            logger.debug("Server is in clean state for testing");
            return true;
            
        } catch (Exception e) {
            logger.warn("Failed to verify server state: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Functional interface for boolean conditions.
     */
    @FunctionalInterface
    public interface BooleanSupplier {
        boolean getAsBoolean() throws Exception;
    }
    
    /**
     * Sleeps for the specified duration, handling interruption.
     * 
     * @param duration the duration to sleep
     * @param unit the time unit
     */
    public static void sleep(long duration, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(duration));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }
    
    /**
     * Retries an operation with exponential backoff.
     * 
     * @param operation the operation to retry
     * @param maxAttempts the maximum number of attempts
     * @param initialDelayMs the initial delay in milliseconds
     * @return true if operation succeeded within max attempts
     */
    public static boolean retryWithBackoff(BooleanSupplier operation, int maxAttempts, long initialDelayMs) {
        long delay = initialDelayMs;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (operation.getAsBoolean()) {
                    logger.debug("Operation succeeded on attempt {}", attempt);
                    return true;
                }
            } catch (Exception e) {
                logger.debug("Operation failed on attempt {}: {}", attempt, e.getMessage());
            }
            
            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(delay);
                    delay = Math.min(delay * 2, 5000); // Cap at 5 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        
        logger.debug("Operation failed after {} attempts", maxAttempts);
        return false;
    }
}
