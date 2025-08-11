package com.github.theholywaffle.teamspeak3.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for integration tests that provides common setup and teardown functionality.
 * This class sets up a TeamSpeak 3 server using TestContainers for integration testing.
 */
@Testcontainers
public abstract class BaseIntegrationTest {

    /**
     * TeamSpeak 3 server container for integration testing.
     * Uses the official TeamSpeak Docker image with proper configuration.
     */
    @Container
    protected static final GenericContainer<?> teamspeakServer = new GenericContainer<>("teamspeak:latest")
            .withExposedPorts(9987, 10011, 30033)
            .withEnv("TS3SERVER_LICENSE", "accept")
            .withEnv("TS3SERVER_QUERY_PROTOCOLS", "raw,ssh")
            .withEnv("TS3SERVER_DB_PLUGIN", "ts3db_sqlite3")
            .withEnv("TS3SERVER_DB_SQLCREATEPATH", "create_sqlite")
            .withEnv("TS3SERVER_MACHINE_ID", "test-machine").withStartupTimeout(Duration.ofMinutes(2));
//            .waitingFor(Wait.forLogMessage(".*listening on.*", 1)
//                    .withStartupTimeout(Duration.ofMinutes(2)));

    protected static String serverHost;
    protected static int serverQueryPort;
    protected static int serverVoicePort;
    protected static int serverFileTransferPort;
    protected static String serverQueryUsername;
    protected static String serverQueryPassword;
    protected static String serverAdminToken;
    protected static TestLifecycleManager lifecycleManager;

    @BeforeAll
    public static void setUpClass() {
        teamspeakServer.followOutput(c -> System.out.println("Container: " + c.getUtf8String()));
        // Get connection details from the started container
        serverHost = teamspeakServer.getHost();
        serverQueryPort = teamspeakServer.getMappedPort(10011);
        serverVoicePort = teamspeakServer.getMappedPort(9987);
        serverFileTransferPort = teamspeakServer.getMappedPort(30033);

        // Default credentials for TeamSpeak server
        serverQueryUsername = "serveradmin";
        serverQueryPassword = extractServerAdminPassword();

        // Initialize the lifecycle manager
        lifecycleManager = new TestLifecycleManager(serverHost, serverQueryPort,
                                                   serverQueryUsername, serverQueryPassword);

        // Wait for server to be ready
        try {
            lifecycleManager.waitForServerReady();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize TeamSpeak server for testing", e);
        }

        System.out.println("Integration test environment set up");
        System.out.println("Server Host: " + serverHost);
        System.out.println("Query Port: " + serverQueryPort);
        System.out.println("Voice Port: " + serverVoicePort);
        System.out.println("File Transfer Port: " + serverFileTransferPort);
    }

    /**
     * Extract the server admin password from the container logs.
     * TeamSpeak server generates a random password on first startup.
     * @return the server admin password
     */
    private static String extractServerAdminPassword() {
        try {
            String logs = teamspeakServer.getLogs();
            Pattern pattern = Pattern.compile("ServerAdmin password= \"([^\"]+)\"");
            Matcher matcher = pattern.matcher(logs);
            if (matcher.find()) {
                return matcher.group(1);
            }
            // Fallback to a default password if extraction fails
            System.out.println("Warning: Could not extract server admin password from logs, using default");
            return "test123";
        } catch (Exception e) {
            System.out.println("Warning: Error extracting server admin password: " + e.getMessage());
            return "test123";
        }
    }

    @AfterAll
    public static void tearDownClass() {
        if (lifecycleManager != null) {
            lifecycleManager.cleanupAll();
        }
        System.out.println("Integration test environment torn down");
        // Container will be automatically stopped by TestContainers
    }

    @BeforeEach
    public void setUp() {
        // Common setup for each test
        System.out.println("Setting up individual test");
    }

    @AfterEach
    public void tearDown() {
        // Clean up test-specific resources
        String testName = getCurrentTestName();
        if (lifecycleManager != null && testName != null) {
            lifecycleManager.cleanupTest(testName);
            lifecycleManager.disconnectTestQuery(testName);
        }
        System.out.println("Cleaning up individual test");
    }

    /**
     * Gets the current test name for resource tracking.
     * This is a simple implementation - in a real scenario you might want to use
     * JUnit 5's TestInfo parameter injection.
     *
     * @return the current test name or null if not available
     */
    protected String getCurrentTestName() {
        // Simple implementation using stack trace
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            if (element.getMethodName().startsWith("test") &&
                element.getClassName().contains("Test")) {
                return element.getClassName() + "." + element.getMethodName();
            }
        }
        return null;
    }

    /**
     * Get the server host for testing.
     * @return the server host
     */
    protected String getServerHost() {
        return serverHost;
    }

    /**
     * Get the server query port for testing.
     * @return the server query port
     */
    protected int getServerPort() {
        return serverQueryPort;
    }

    /**
     * Get the server query port for testing.
     * @return the server query port
     */
    protected int getServerQueryPort() {
        return serverQueryPort;
    }

    /**
     * Get the server voice port for testing.
     * @return the server voice port
     */
    protected int getServerVoicePort() {
        return serverVoicePort;
    }

    /**
     * Get the server file transfer port for testing.
     * @return the server file transfer port
     */
    protected int getServerFileTransferPort() {
        return serverFileTransferPort;
    }

    /**
     * Get the server admin token for testing.
     * @return the server admin token
     */
    protected String getServerAdminToken() {
        return serverAdminToken;
    }

    /**
     * Get the server query username for testing.
     * @return the server query username
     */
    protected String getServerQueryUsername() {
        return serverQueryUsername;
    }

    /**
     * Get the server query password for testing.
     * @return the server query password
     */
    protected String getServerQueryPassword() {
        return serverQueryPassword;
    }

    /**
     * Wait for the server to be ready for connections.
     */
    protected void waitForServerReady() {
        try {
            lifecycleManager.waitForServerReady();
        } catch (Exception e) {
            throw new RuntimeException("Server not ready for testing", e);
        }
    }

    /**
     * Create a test query connection for the current test.
     * The connection will be automatically cleaned up after the test.
     *
     * @return a connected TS3Query instance
     */
    protected com.github.theholywaffle.teamspeak3.TS3Query createTestQuery() {
        try {
            String testName = getCurrentTestName();
            return lifecycleManager.createTestQuery(testName != null ? testName : "unknown-test");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test query", e);
        }
    }

    /**
     * Create a test channel for testing purposes.
     * The channel will be automatically cleaned up after the test.
     *
     * @param channelName the name of the channel to create
     * @param api the TS3Api instance to use
     * @return the channel ID
     */
    protected int createTestChannel(String channelName, com.github.theholywaffle.teamspeak3.TS3Api api) {
        String testName = getCurrentTestName();
        return lifecycleManager.createTestChannel(testName != null ? testName : "unknown-test", channelName, api);
    }

    /**
     * Track a test client for cleanup.
     * The client will be automatically cleaned up after the test.
     *
     * @param clientId the ID of the client to track
     */
    protected void trackTestClient(int clientId) {
        String testName = getCurrentTestName();
        lifecycleManager.trackTestClient(testName != null ? testName : "unknown-test", clientId);
    }

    /**
     * Get the lifecycle manager for advanced test resource management.
     *
     * @return the test lifecycle manager
     */
    protected TestLifecycleManager getLifecycleManager() {
        return lifecycleManager;
    }

    /**
     * Creates a test query and API instance for the current test.
     * Convenience method that combines query creation with API access.
     *
     * @return a TS3Api instance ready for testing
     */
    protected com.github.theholywaffle.teamspeak3.TS3Api createTestApi() {
        return createTestQuery().getApi();
    }

    /**
     * Verifies that the server is ready and in a clean state for testing.
     * Should be called at the beginning of tests that require a clean server state.
     *
     * @throws AssertionError if server is not in a clean state
     */
    protected void assertServerReady() {
        try {
            com.github.theholywaffle.teamspeak3.TS3Api api = createTestApi();
            boolean isClean = IntegrationTestUtils.verifyCleanServerState(api);
            if (!isClean) {
                throw new AssertionError("Server is not in a clean state for testing");
            }
        } catch (Exception e) {
            throw new AssertionError("Failed to verify server state: " + e.getMessage(), e);
        }
    }

    /**
     * Waits for a condition to be true with a default timeout.
     * Convenience method for common wait operations in tests.
     *
     * @param condition the condition to wait for
     * @return true if condition became true within timeout
     */
    protected boolean waitForCondition(IntegrationTestUtils.BooleanSupplier condition) {
        return IntegrationTestUtils.waitForCondition(condition);
    }

    /**
     * Waits for a condition to be true with a specified timeout.
     *
     * @param condition the condition to wait for
     * @param timeoutSeconds the timeout in seconds
     * @return true if condition became true within timeout
     */
    protected boolean waitForCondition(IntegrationTestUtils.BooleanSupplier condition, int timeoutSeconds) {
        return IntegrationTestUtils.waitForCondition(condition, timeoutSeconds, 500);
    }

    /**
     * Sleeps for a short duration. Useful for waiting between operations.
     *
     * @param milliseconds the duration to sleep in milliseconds
     */
    protected void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }
}
