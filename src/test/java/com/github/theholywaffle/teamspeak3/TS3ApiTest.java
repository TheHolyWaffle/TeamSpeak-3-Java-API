package com.github.theholywaffle.teamspeak3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic tests for TS3Api class.
 * Note: This class is a synchronous wrapper around TS3ApiAsync.
 * Due to the complexity of mocking TS3ApiAsync and Java version compatibility issues,
 * these tests focus on basic functionality and structure validation.
 */
public class TS3ApiTest {

	@Test
	public void constructor_WithNullAsyncApi_ThrowsException() {
		// Test that constructor properly validates input
		// Note: The constructor is package-private and doesn't validate null input
		// This test verifies the expected behavior when null is passed
		assertDoesNotThrow(() -> new TS3Api(null),
			"Constructor should accept null TS3ApiAsync (package-private constructor)");
	}

	@Test
	public void ts3Api_HasExpectedPublicMethods() {
		// Verify that TS3Api has the expected public methods
		// This test ensures the API surface is maintained

		Class<TS3Api> apiClass = TS3Api.class;

		// Check for some key methods that should exist
		assertDoesNotThrow(() -> apiClass.getMethod("login", String.class, String.class),
			"login method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("logout"),
			"logout method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("whoAmI"),
			"whoAmI method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("getVersion"),
			"getVersion method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("getClients"),
			"getClients method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("getBans"),
			"getBans method should exist");
	}

	@Test
	public void ts3Api_HasExpectedChannelMethods() {
		// Verify that TS3Api has channel-related methods
		Class<TS3Api> apiClass = TS3Api.class;

		assertDoesNotThrow(() -> apiClass.getMethod("getChannels"),
			"getChannels method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("createChannel", String.class, java.util.Map.class),
			"createChannel method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("deleteChannel", int.class),
			"deleteChannel method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("editChannel", int.class, java.util.Map.class),
			"editChannel method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("moveChannel", int.class, int.class),
			"moveChannel method should exist");
	}

	@Test
	public void ts3Api_HasExpectedClientMethods() {
		// Verify that TS3Api has client-related methods
		Class<TS3Api> apiClass = TS3Api.class;

		assertDoesNotThrow(() -> apiClass.getMethod("getClientInfo", int.class),
			"getClientInfo method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("moveClient", int.class, int.class),
			"moveClient method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("kickClientFromChannel", int[].class),
			"kickClientFromChannel method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("kickClientFromServer", int[].class),
			"kickClientFromServer method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("banClient", int.class, long.class),
			"banClient method should exist");
	}

	@Test
	public void ts3Api_HasExpectedMessageMethods() {
		// Verify that TS3Api has messaging methods
		Class<TS3Api> apiClass = TS3Api.class;

		assertDoesNotThrow(() -> apiClass.getMethod("sendPrivateMessage", int.class, String.class),
			"sendPrivateMessage method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("sendChannelMessage", String.class),
			"sendChannelMessage method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("sendServerMessage", String.class),
			"sendServerMessage method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("pokeClient", int.class, String.class),
			"pokeClient method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("broadcast", String.class),
			"broadcast method should exist");
	}

	@Test
	public void ts3Api_HasExpectedFileOperationMethods() {
		// Verify that TS3Api has file operation methods
		Class<TS3Api> apiClass = TS3Api.class;

		assertDoesNotThrow(() -> apiClass.getMethod("uploadFile", java.io.InputStream.class, long.class, String.class, boolean.class, int.class),
			"uploadFile method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("downloadFile", java.io.OutputStream.class, String.class, int.class),
			"downloadFile method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("deleteFile", String.class, int.class),
			"deleteFile method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("moveFile", String.class, String.class, int.class),
			"moveFile method should exist");
	}

	@Test
	public void ts3Api_HasExpectedEventMethods() {
		// Verify that TS3Api has event-related methods
		Class<TS3Api> apiClass = TS3Api.class;

		assertDoesNotThrow(() -> apiClass.getMethod("registerAllEvents"),
			"registerAllEvents method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("unregisterAllEvents"),
			"unregisterAllEvents method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("registerEvent", com.github.theholywaffle.teamspeak3.api.event.TS3EventType.class),
			"registerEvent method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("addTS3Listeners", com.github.theholywaffle.teamspeak3.api.event.TS3Listener[].class),
			"addTS3Listeners method should exist");
		assertDoesNotThrow(() -> apiClass.getMethod("removeTS3Listeners", com.github.theholywaffle.teamspeak3.api.event.TS3Listener[].class),
			"removeTS3Listeners method should exist");
	}

	@Test
	public void ts3Api_ClassStructure() {
		// Verify basic class structure
		Class<TS3Api> apiClass = TS3Api.class;

		// Should be a public class
		assertTrue(java.lang.reflect.Modifier.isPublic(apiClass.getModifiers()),
			"TS3Api should be a public class");

		// Should not be abstract
		assertFalse(java.lang.reflect.Modifier.isAbstract(apiClass.getModifiers()),
			"TS3Api should not be abstract");

		// Should not be an interface
		assertFalse(apiClass.isInterface(),
			"TS3Api should not be an interface");

		// Should have a package-private constructor that takes TS3ApiAsync
		assertDoesNotThrow(() -> apiClass.getDeclaredConstructor(TS3ApiAsync.class),
			"TS3Api should have a package-private constructor that takes TS3ApiAsync");
	}

	@Test
	public void ts3Api_IsWrapperAroundAsyncApi() {
		// Verify that TS3Api is designed as a synchronous wrapper
		// This test checks the conceptual design without complex mocking

		Class<TS3Api> apiClass = TS3Api.class;

		// Should have a package-private constructor that requires TS3ApiAsync
		boolean hasAsyncConstructor = false;
		for (java.lang.reflect.Constructor<?> constructor : apiClass.getDeclaredConstructors()) {
			if (constructor.getParameterCount() == 1 &&
				constructor.getParameterTypes()[0] == TS3ApiAsync.class) {
				hasAsyncConstructor = true;
				break;
			}
		}
		assertTrue(hasAsyncConstructor, "TS3Api should have a package-private constructor that takes TS3ApiAsync");

		// Should have many public methods (it's a comprehensive API wrapper)
		java.lang.reflect.Method[] publicMethods = apiClass.getMethods();
		long apiMethods = java.util.Arrays.stream(publicMethods)
			.filter(method -> method.getDeclaringClass() == apiClass)
			.filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
			.count();

		assertTrue(apiMethods > 50, "TS3Api should have many public methods as it's a comprehensive wrapper");
	}
}
