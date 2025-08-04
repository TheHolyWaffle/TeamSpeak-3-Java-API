package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.VirtualServerStatus;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class VirtualServerTest {

	@Test
	public void constructor_ValidMap() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_id", "1");
		map.put("virtualserver_name", "Test Server");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertNotNull(virtualServer);
		assertEquals(1, virtualServer.getId());
		assertEquals("Test Server", virtualServer.getName());
	}

	@Test
	public void getId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_id", "5");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(5, virtualServer.getId());
	}

	@Test
	public void getId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_id", "0");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(0, virtualServer.getId());
	}

	@Test
	public void getId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_id", "-1");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(-1, virtualServer.getId());
	}

	@Test
	public void getPort_ValidPort() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_port", "9987");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(9987, virtualServer.getPort());
	}

	@Test
	public void getPort_ZeroPort() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_port", "0");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(0, virtualServer.getPort());
	}

	@Test
	public void getPort_CustomPort() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_port", "10011");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(10011, virtualServer.getPort());
	}

	@Test
	public void getStatus_Online() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_status", "online");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(VirtualServerStatus.ONLINE, virtualServer.getStatus());
	}

	@Test
	public void getStatus_Offline() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_status", "offline");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(VirtualServerStatus.OFFLINE, virtualServer.getStatus());
	}

	@Test
	public void getStatus_BoottingUp() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_status", "booting up");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(VirtualServerStatus.BOOTING_UP, virtualServer.getStatus());
	}

	@Test
	public void getStatus_ShuttingDown() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_status", "shutting down");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(VirtualServerStatus.SHUTTING_DOWN, virtualServer.getStatus());
	}

	@Test
	public void getStatus_DeployRunning() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_status", "deploy running");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(VirtualServerStatus.DEPLOY_RUNNING, virtualServer.getStatus());
	}

	@Test
	public void getStatus_Unknown() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_status", "invalid_status");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(VirtualServerStatus.UNKNOWN, virtualServer.getStatus());
	}

	@Test
	public void getStatus_EmptyStatus() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_status", "");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(VirtualServerStatus.UNKNOWN, virtualServer.getStatus());
	}

	@Test
	public void getClientsOnline_ValidCount() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_clientsonline", "25");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(25, virtualServer.getClientsOnline());
	}

	@Test
	public void getClientsOnline_ZeroCount() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_clientsonline", "0");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(0, virtualServer.getClientsOnline());
	}

	@Test
	public void getClientsOnline_NegativeCount() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_clientsonline", "-1");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(-1, virtualServer.getClientsOnline());
	}

	@Test
	public void getQueryClientsOnline_ValidCount() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_queryclientsonline", "3");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(3, virtualServer.getQueryClientsOnline());
	}

	@Test
	public void getQueryClientsOnline_ZeroCount() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_queryclientsonline", "0");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(0, virtualServer.getQueryClientsOnline());
	}

	@Test
	public void getMaxClients_ValidMax() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_maxclients", "100");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(100, virtualServer.getMaxClients());
	}

	@Test
	public void getMaxClients_ZeroMax() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_maxclients", "0");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(0, virtualServer.getMaxClients());
	}

	@Test
	public void getMaxClients_UnlimitedMax() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_maxclients", "-1"); // Unlimited
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(-1, virtualServer.getMaxClients());
	}

	@Test
	public void getUniqueIdentifier_ValidUID() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_unique_identifier", "server123456");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals("server123456", virtualServer.getUniqueIdentifier());
	}

	@Test
	public void getUniqueIdentifier_EmptyUID() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_unique_identifier", "");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals("", virtualServer.getUniqueIdentifier());
	}

	@Test
	public void getUniqueIdentifier_NullUID() {
		Map<String, String> map = new HashMap<>();
		// virtualserver_unique_identifier key not present
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals("", virtualServer.getUniqueIdentifier());
	}

	@Test
	public void getUptime_ValidUptime() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_uptime", "3600"); // 1 hour in seconds
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(3600L, virtualServer.getUptime());
	}

	@Test
	public void getUptime_ZeroUptime() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_uptime", "0");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(0L, virtualServer.getUptime());
	}

	@Test
	public void getUptime_LargeUptime() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_uptime", "86400"); // 1 day in seconds
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals(86400L, virtualServer.getUptime());
	}

	@Test
	public void getName_ValidName() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_name", "My TeamSpeak Server");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals("My TeamSpeak Server", virtualServer.getName());
	}

	@Test
	public void getName_EmptyName() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_name", "");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals("", virtualServer.getName());
	}

	@Test
	public void getName_NullName() {
		Map<String, String> map = new HashMap<>();
		// virtualserver_name key not present
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals("", virtualServer.getName());
	}

	@Test
	public void getName_SpecialCharacters() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_name", "Server [EU] & Gaming");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertEquals("Server [EU] & Gaming", virtualServer.getName());
	}

	@Test
	public void isAutoStart_True() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_autostart", "1");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertTrue(virtualServer.isAutoStart());
	}

	@Test
	public void isAutoStart_False() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_autostart", "0");
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertFalse(virtualServer.isAutoStart());
	}

	@Test
	public void isAutoStart_DefaultValue() {
		Map<String, String> map = new HashMap<>();
		// virtualserver_autostart key not present
		
		VirtualServer virtualServer = new VirtualServer(map);
		assertFalse(virtualServer.isAutoStart()); // Default boolean value
	}

	@Test
	public void virtualServer_CompleteData() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_id", "1");
		map.put("virtualserver_port", "9987");
		map.put("virtualserver_status", "online");
		map.put("virtualserver_clientsonline", "25");
		map.put("virtualserver_queryclientsonline", "2");
		map.put("virtualserver_maxclients", "100");
		map.put("virtualserver_unique_identifier", "server123456");
		map.put("virtualserver_uptime", "86400");
		map.put("virtualserver_name", "My TeamSpeak Server");
		map.put("virtualserver_autostart", "1");
		
		VirtualServer virtualServer = new VirtualServer(map);
		
		assertEquals(1, virtualServer.getId());
		assertEquals(9987, virtualServer.getPort());
		assertEquals(VirtualServerStatus.ONLINE, virtualServer.getStatus());
		assertEquals(25, virtualServer.getClientsOnline());
		assertEquals(2, virtualServer.getQueryClientsOnline());
		assertEquals(100, virtualServer.getMaxClients());
		assertEquals("server123456", virtualServer.getUniqueIdentifier());
		assertEquals(86400L, virtualServer.getUptime());
		assertEquals("My TeamSpeak Server", virtualServer.getName());
		assertTrue(virtualServer.isAutoStart());
	}

	@Test
	public void virtualServer_EmptyMap() {
		Map<String, String> map = new HashMap<>();
		
		VirtualServer virtualServer = new VirtualServer(map);
		
		assertEquals(-1, virtualServer.getId()); // Default int value
		assertEquals(-1, virtualServer.getPort()); // Default int value
		assertEquals(VirtualServerStatus.UNKNOWN, virtualServer.getStatus()); // Empty string maps to UNKNOWN
		assertEquals(-1, virtualServer.getClientsOnline()); // Default int value
		assertEquals(-1, virtualServer.getQueryClientsOnline()); // Default int value
		assertEquals(-1, virtualServer.getMaxClients()); // Default int value
		assertEquals("", virtualServer.getUniqueIdentifier());
		assertEquals(-1L, virtualServer.getUptime()); // Default long value
		assertEquals("", virtualServer.getName());
		assertFalse(virtualServer.isAutoStart()); // Default boolean value
	}

	@Test
	public void virtualServer_PartialData() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_id", "2");
		map.put("virtualserver_name", "Partial Server");
		map.put("virtualserver_status", "offline");
		// Missing other fields
		
		VirtualServer virtualServer = new VirtualServer(map);
		
		assertEquals(2, virtualServer.getId());
		assertEquals(-1, virtualServer.getPort()); // Default int value
		assertEquals(VirtualServerStatus.OFFLINE, virtualServer.getStatus());
		assertEquals(-1, virtualServer.getClientsOnline()); // Default int value
		assertEquals(-1, virtualServer.getQueryClientsOnline()); // Default int value
		assertEquals(-1, virtualServer.getMaxClients()); // Default int value
		assertEquals("", virtualServer.getUniqueIdentifier()); // Default string value
		assertEquals(-1L, virtualServer.getUptime()); // Default long value
		assertEquals("Partial Server", virtualServer.getName());
		assertFalse(virtualServer.isAutoStart()); // Default boolean value
	}

	@Test
	public void virtualServer_AllZeroValues() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_id", "0");
		map.put("virtualserver_port", "0");
		map.put("virtualserver_clientsonline", "0");
		map.put("virtualserver_queryclientsonline", "0");
		map.put("virtualserver_maxclients", "0");
		map.put("virtualserver_uptime", "0");
		map.put("virtualserver_autostart", "0");
		
		VirtualServer virtualServer = new VirtualServer(map);
		
		assertEquals(0, virtualServer.getId());
		assertEquals(0, virtualServer.getPort());
		assertEquals(0, virtualServer.getClientsOnline());
		assertEquals(0, virtualServer.getQueryClientsOnline());
		assertEquals(0, virtualServer.getMaxClients());
		assertEquals(0L, virtualServer.getUptime());
		assertFalse(virtualServer.isAutoStart());
	}

	@Test
	public void virtualServer_AllMaxValues() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_id", String.valueOf(Integer.MAX_VALUE));
		map.put("virtualserver_port", String.valueOf(Integer.MAX_VALUE));
		map.put("virtualserver_clientsonline", String.valueOf(Integer.MAX_VALUE));
		map.put("virtualserver_queryclientsonline", String.valueOf(Integer.MAX_VALUE));
		map.put("virtualserver_maxclients", String.valueOf(Integer.MAX_VALUE));
		map.put("virtualserver_uptime", String.valueOf(Long.MAX_VALUE));
		
		VirtualServer virtualServer = new VirtualServer(map);
		
		assertEquals(Integer.MAX_VALUE, virtualServer.getId());
		assertEquals(Integer.MAX_VALUE, virtualServer.getPort());
		assertEquals(Integer.MAX_VALUE, virtualServer.getClientsOnline());
		assertEquals(Integer.MAX_VALUE, virtualServer.getQueryClientsOnline());
		assertEquals(Integer.MAX_VALUE, virtualServer.getMaxClients());
		assertEquals(Long.MAX_VALUE, virtualServer.getUptime());
	}

	@Test
	public void virtualServer_InheritedWrapperMethods() {
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_id", "1");
		map.put("virtualserver_name", "Test Server");
		map.put("test_field", "test_value");
		
		VirtualServer virtualServer = new VirtualServer(map);
		
		// Test inherited methods from Wrapper
		assertEquals("1", virtualServer.get("virtualserver_id"));
		assertEquals("Test Server", virtualServer.get("virtualserver_name"));
		assertEquals("test_value", virtualServer.get("test_field"));
		assertEquals(1, virtualServer.getInt("virtualserver_id"));
	}

	@Test
	public void virtualServer_RealWorldScenario() {
		// Simulate a real-world virtual server scenario
		Map<String, String> map = new HashMap<>();
		map.put("virtualserver_id", "1");
		map.put("virtualserver_port", "9987");
		map.put("virtualserver_status", "online");
		map.put("virtualserver_clientsonline", "42");
		map.put("virtualserver_queryclientsonline", "1");
		map.put("virtualserver_maxclients", "64");
		map.put("virtualserver_unique_identifier", "TeamSpeakServer=");
		map.put("virtualserver_uptime", "2592000"); // 30 days in seconds
		map.put("virtualserver_name", "Gaming Community [EU]");
		map.put("virtualserver_autostart", "1");
		
		VirtualServer virtualServer = new VirtualServer(map);
		
		assertEquals(1, virtualServer.getId());
		assertEquals(9987, virtualServer.getPort());
		assertEquals(VirtualServerStatus.ONLINE, virtualServer.getStatus());
		assertEquals(42, virtualServer.getClientsOnline());
		assertEquals(1, virtualServer.getQueryClientsOnline());
		assertEquals(64, virtualServer.getMaxClients());
		assertEquals("TeamSpeakServer=", virtualServer.getUniqueIdentifier());
		assertEquals(2592000L, virtualServer.getUptime());
		assertEquals("Gaming Community [EU]", virtualServer.getName());
		assertTrue(virtualServer.isAutoStart());
	}
}
