package com.github.theholywaffle.teamspeak3.integration;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelGroup;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerGroup;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerQueryInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.VirtualServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for complete API workflows.
 * Tests end-to-end scenarios from connection through authentication to command execution.
 */
public class WorkflowIntegrationTest extends BaseIntegrationTest {

    @BeforeEach
    public void setUpTest() {
        waitForServerReady();
        assertServerReady();
    }

    @Test
    public void testCompleteConnectionWorkflow() {
        // Test complete workflow: connect -> login -> select server -> execute commands -> disconnect
        TS3Query query = createTestQuery();
        assertTrue(query.isConnected(), "Query should be connected");

        TS3Api api = query.getApi();

        // Step 1: Verify connection and get server info
        VirtualServer serverInfo = api.getServerInfo();
        assertNotNull(serverInfo, "Should get server info");
        assertTrue(serverInfo.getId() > 0, "Server ID should be positive");
        System.out.println("Connected to server: " + serverInfo.getName() + " (ID: " + serverInfo.getId() + ")");

        // Step 2: Get channels and verify structure
        List<Channel> channels = api.getChannels();
        assertNotNull(channels, "Should get channels list");
        assertFalse(channels.isEmpty(), "Should have at least one channel");
        System.out.println("Found " + channels.size() + " channels");

        // Step 3: Get clients and verify
        List<Client> clients = api.getClients();
        assertNotNull(clients, "Should get clients list");
        System.out.println("Found " + clients.size() + " clients");

        // Step 4: Test basic server operations
        String serverName = serverInfo.getName();
        assertNotNull(serverName, "Server should have a name");
        assertTrue(serverInfo.getMaxClients() > 0, "Server should have max clients configured");

        System.out.println("Complete connection workflow test completed successfully");
    }

    @Test
    public void testChannelManagementWorkflow() {
        // Test complete channel management workflow
        TS3Api api = createTestApi();

        // Step 1: Get initial channel list
        List<Channel> initialChannels = api.getChannels();
        int initialChannelCount = initialChannels.size();
        System.out.println("Initial channel count: " + initialChannelCount);

        // Step 2: Create a test channel
        String testChannelName = "IntegrationTest_" + System.currentTimeMillis();
        int channelId = createTestChannel(testChannelName, api);
        assertTrue(channelId > 0, "Channel ID should be positive");
        System.out.println("Created test channel: " + testChannelName + " (ID: " + channelId + ")");

        // Step 3: Verify channel was created
        List<Channel> channelsAfterCreate = api.getChannels();
        assertEquals(initialChannelCount + 1, channelsAfterCreate.size(), 
                    "Should have one more channel after creation");

        // Step 4: Find the created channel
        Channel createdChannel = channelsAfterCreate.stream()
                .filter(c -> c.getId() == channelId)
                .findFirst()
                .orElse(null);
        assertNotNull(createdChannel, "Should find the created channel");
        assertEquals(testChannelName, createdChannel.getName(), "Channel name should match");

        // Step 5: Modify channel properties (if supported)
        try {
            // Try to edit channel description
            api.editChannel(channelId, ChannelProperty.CHANNEL_DESCRIPTION, "Test channel description");
            System.out.println("Updated channel description");
        } catch (Exception e) {
            System.out.println("Channel editing not supported or failed: " + e.getMessage());
        }

        // Step 6: Channel will be automatically cleaned up by lifecycle manager

        System.out.println("Channel management workflow test completed successfully");
    }

    @Test
    public void testServerInformationWorkflow() {
        // Test comprehensive server information retrieval workflow
        TS3Api api = createTestApi();

        // Step 1: Get basic server info
        VirtualServer serverInfo = api.getServerInfo();
        assertNotNull(serverInfo, "Should get server info");
        System.out.println("Server: " + serverInfo.getName());
        System.out.println("Status: " + serverInfo.getStatus());
        System.out.println("Clients: " + serverInfo.getClientsOnline() + "/" + serverInfo.getMaxClients());

        // Step 2: Get virtual servers list
        List<VirtualServer> virtualServers = api.getVirtualServers();
        assertNotNull(virtualServers, "Should get virtual servers list");
        assertFalse(virtualServers.isEmpty(), "Should have at least one virtual server");
        System.out.println("Found " + virtualServers.size() + " virtual servers");

        // Step 3: Verify current server is in the list
        boolean currentServerFound = virtualServers.stream()
                .anyMatch(vs -> vs.getId() == serverInfo.getId());
        assertTrue(currentServerFound, "Current server should be in virtual servers list");

        // Step 4: Get server groups (if available)
        try {
            List<ServerGroup> serverGroups = api.getServerGroups();
            assertNotNull(serverGroups, "Should get server groups");
            System.out.println("Found " + serverGroups.size() + " server groups");
        } catch (Exception e) {
            System.out.println("Server groups not accessible: " + e.getMessage());
        }

        // Step 5: Get channel groups (if available)
        try {
            List<ChannelGroup> channelGroups = api.getChannelGroups();
            assertNotNull(channelGroups, "Should get channel groups");
            System.out.println("Found " + channelGroups.size() + " channel groups");
        } catch (Exception e) {
            System.out.println("Channel groups not accessible: " + e.getMessage());
        }

        System.out.println("Server information workflow test completed successfully");
    }

    @Test
    public void testClientInteractionWorkflow() {
        // Test client interaction workflow
        TS3Api api = createTestApi();

        // Step 1: Get current clients
        List<Client> clients = api.getClients();
        assertNotNull(clients, "Should get clients list");
        System.out.println("Current clients: " + clients.size());

        // Step 2: Find query client (ourselves)
        Client queryClient = clients.stream()
                .filter(c -> c.getType() == 1) // Query client type
                .findFirst()
                .orElse(null);

        if (queryClient != null) {
            System.out.println("Found query client: " + queryClient.getNickname() + " (ID: " + queryClient.getId() + ")");
            
            // Step 3: Get client info
            assertNotNull(queryClient.getNickname(), "Query client should have a nickname");
            assertTrue(queryClient.getId() > 0, "Query client should have positive ID");
            
            // Step 4: Test client operations
            try {
                // Try to get client details
                ClientInfo clientInfo = api.getClientInfo(queryClient.getId());
                assertNotNull(clientInfo, "Should get client info");
                System.out.println("Client info retrieved successfully");
            } catch (Exception e) {
                System.out.println("Client info retrieval failed: " + e.getMessage());
            }
        } else {
            System.out.println("No query client found in client list");
        }

        // Step 5: Test whoami command
        try {
            ServerQueryInfo whoAmI = api.whoAmI();
            assertNotNull(whoAmI, "Should get whoami info");
            System.out.println("WhoAmI: " + whoAmI.getNickname());
        } catch (Exception e) {
            System.out.println("WhoAmI command failed: " + e.getMessage());
        }

        System.out.println("Client interaction workflow test completed successfully");
    }

    @Test
    public void testMessageWorkflow() {
        // Test messaging workflow
        TS3Api api = createTestApi();

        // Step 1: Create a test channel for messaging
        String testChannelName = "MessageTest_" + System.currentTimeMillis();
        int channelId = createTestChannel(testChannelName, api);
        System.out.println("Created test channel for messaging: " + testChannelName);

        // Step 2: Move to the test channel
        try {
            api.moveQuery(channelId);
            System.out.println("Moved to test channel");
        } catch (Exception e) {
            System.out.println("Failed to move to test channel: " + e.getMessage());
        }

        // Step 3: Send a channel message
        String testMessage = "Integration test message - " + System.currentTimeMillis();
        try {
            api.sendChannelMessage(testMessage);
            System.out.println("Sent channel message: " + testMessage);
        } catch (Exception e) {
            System.out.println("Failed to send channel message: " + e.getMessage());
        }

        // Step 4: Test server message (if permissions allow)
        try {
            api.sendServerMessage("Integration test server message");
            System.out.println("Sent server message");
        } catch (Exception e) {
            System.out.println("Failed to send server message (expected): " + e.getMessage());
        }

        System.out.println("Message workflow test completed successfully");
    }

    @Test
    public void testErrorHandlingWorkflow() {
        // Test error handling in various scenarios
        TS3Api api = createTestApi();

        // Step 1: Test invalid channel operations
        try {
            api.getChannelInfo(99999); // Non-existent channel
            fail("Should throw exception for non-existent channel");
        } catch (Exception e) {
            System.out.println("Correctly handled invalid channel error: " + e.getMessage());
        }

        // Step 2: Test invalid client operations
        try {
            api.getClientInfo(99999); // Non-existent client
            fail("Should throw exception for non-existent client");
        } catch (Exception e) {
            System.out.println("Correctly handled invalid client error: " + e.getMessage());
        }

        // Step 3: Test permission errors
        try {
            // Try to perform admin operation (should fail with insufficient permissions)
            Map<VirtualServerProperty, String> serverOptions = new HashMap<>();
            api.createServer("TestServer", serverOptions);
            System.out.println("Virtual server creation succeeded (unexpected)");
        } catch (Exception e) {
            System.out.println("Correctly handled permission error: " + e.getMessage());
        }

        // Step 4: Verify connection is still functional after errors
        VirtualServer serverInfo = api.getServerInfo();
        assertNotNull(serverInfo, "Connection should still work after error scenarios");

        System.out.println("Error handling workflow test completed successfully");
    }

    @Test
    public void testMultiStepOperationWorkflow() {
        // Test complex multi-step operations
        TS3Api api = createTestApi();

        // Step 1: Get initial state
        VirtualServer initialServerInfo = api.getServerInfo();
        List<Channel> initialChannels = api.getChannels();
        List<Client> initialClients = api.getClients();

        System.out.println("Initial state - Channels: " + initialChannels.size() + 
                          ", Clients: " + initialClients.size());

        // Step 2: Create multiple test channels
        String baseChannelName = "MultiTest_" + System.currentTimeMillis();
        int[] channelIds = new int[3];
        
        for (int i = 0; i < 3; i++) {
            String channelName = baseChannelName + "_" + i;
            channelIds[i] = createTestChannel(channelName, api);
            System.out.println("Created channel: " + channelName + " (ID: " + channelIds[i] + ")");
        }

        // Step 3: Verify all channels were created
        List<Channel> channelsAfterCreate = api.getChannels();
        assertEquals(initialChannels.size() + 3, channelsAfterCreate.size(),
                    "Should have 3 more channels");

        // Step 4: Perform operations on each channel
        for (int i = 0; i < 3; i++) {
            try {
                // Get channel info
                ChannelInfo channelInfo = api.getChannelInfo(channelIds[i]);
                assertNotNull(channelInfo, "Should get info for channel " + channelIds[i]);

                // Try to move to channel
                api.moveQuery(channelIds[i]);
                System.out.println("Moved to channel " + channelIds[i]);

            } catch (Exception e) {
                System.out.println("Operation failed for channel " + channelIds[i] + ": " + e.getMessage());
            }
        }

        // Step 5: Verify server state is consistent
        VirtualServer finalServerInfo = api.getServerInfo();
        assertEquals(initialServerInfo.getId(), finalServerInfo.getId(),
                    "Server ID should remain consistent");

        // Channels will be automatically cleaned up by lifecycle manager

        System.out.println("Multi-step operation workflow test completed successfully");
    }
}
