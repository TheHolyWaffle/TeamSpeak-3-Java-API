package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class VirtualServerCommandsTest {

	@Test
	public void serverCreate_BasicName() {
		String expected = "servercreate virtualserver_name=TestServer";
		assertEquals(expected, VirtualServerCommands.serverCreate("TestServer", null).toString());
	}

	@Test
	public void serverCreate_WithOptions() {
		Map<VirtualServerProperty, String> options = new HashMap<>();
		options.put(VirtualServerProperty.VIRTUALSERVER_PORT, "9988");
		options.put(VirtualServerProperty.VIRTUALSERVER_MAXCLIENTS, "32");
		
		String result = VirtualServerCommands.serverCreate("TestServer", options).toString();
		assertTrue(result.startsWith("servercreate virtualserver_name=TestServer"));
		assertTrue(result.contains("virtualserver_port=9988"));
		assertTrue(result.contains("virtualserver_maxclients=32"));
	}

	@Test
	public void serverCreate_EmptyOptions() {
		Map<VirtualServerProperty, String> options = new HashMap<>();
		String expected = "servercreate virtualserver_name=TestServer";
		assertEquals(expected, VirtualServerCommands.serverCreate("TestServer", options).toString());
	}

	@Test
	public void serverCreate_NullNameException() {
		assertThrows(IllegalArgumentException.class, () -> VirtualServerCommands.serverCreate(null, null));
	}

	@Test
	public void serverCreate_EmptyNameException() {
		assertThrows(IllegalArgumentException.class, () -> VirtualServerCommands.serverCreate("", null));
	}

	@Test
	public void serverCreate_SpecialCharactersInName() {
		String expected = "servercreate virtualserver_name=Test\\sServer\\s[VIP]";
		assertEquals(expected, VirtualServerCommands.serverCreate("Test Server [VIP]", null).toString());
	}

	@Test
	public void serverDelete_ValidId() {
		String expected = "serverdelete sid=5";
		assertEquals(expected, VirtualServerCommands.serverDelete(5).toString());
	}

	@Test
	public void serverDelete_ZeroId() {
		String expected = "serverdelete sid=0";
		assertEquals(expected, VirtualServerCommands.serverDelete(0).toString());
	}

	@Test
	public void serverDelete_NegativeId() {
		String expected = "serverdelete sid=-1";
		assertEquals(expected, VirtualServerCommands.serverDelete(-1).toString());
	}

	@Test
	public void serverEdit_WithOptions() {
		Map<VirtualServerProperty, String> options = new HashMap<>();
		options.put(VirtualServerProperty.VIRTUALSERVER_NAME, "Updated Server");
		options.put(VirtualServerProperty.VIRTUALSERVER_WELCOMEMESSAGE, "Welcome!");
		
		String result = VirtualServerCommands.serverEdit(options).toString();
		assertTrue(result.startsWith("serveredit"));
		assertTrue(result.contains("virtualserver_name=Updated\\sServer"));
		assertTrue(result.contains("virtualserver_welcomemessage=Welcome!"));
	}

	@Test
	public void serverEdit_EmptyOptions() {
		Map<VirtualServerProperty, String> options = new HashMap<>();
		String expected = "serveredit";
		assertEquals(expected, VirtualServerCommands.serverEdit(options).toString());
	}

	@Test
	public void serverEdit_NullOptions() {
		String expected = "serveredit";
		assertEquals(expected, VirtualServerCommands.serverEdit(null).toString());
	}

	@Test
	public void serverIdGetByPort_ValidPort() {
		String expected = "serveridgetbyport virtualserver_port=9987";
		assertEquals(expected, VirtualServerCommands.serverIdGetByPort(9987).toString());
	}

	@Test
	public void serverIdGetByPort_CustomPort() {
		String expected = "serveridgetbyport virtualserver_port=10000";
		assertEquals(expected, VirtualServerCommands.serverIdGetByPort(10000).toString());
	}

	@Test
	public void serverIdGetByPort_ZeroPort() {
		String expected = "serveridgetbyport virtualserver_port=0";
		assertEquals(expected, VirtualServerCommands.serverIdGetByPort(0).toString());
	}

	@Test
	public void serverInfo() {
		String expected = "serverinfo";
		assertEquals(expected, VirtualServerCommands.serverInfo().toString());
	}

	@Test
	public void serverList() {
		String expected = "serverlist -uid -all";
		assertEquals(expected, VirtualServerCommands.serverList().toString());
	}

	@Test
	public void serverRequestConnectionInfo() {
		String expected = "serverrequestconnectioninfo";
		assertEquals(expected, VirtualServerCommands.serverRequestConnectionInfo().toString());
	}

	@Test
	public void serverSnapshotCreate() {
		String expected = "serversnapshotcreate";
		assertEquals(expected, VirtualServerCommands.serverSnapshotCreate().toString());
	}

	@Test
	public void serverSnapshotDeploy_ValidSnapshot() {
		String snapshot = "version=3 virtualserver_name=Test\\sServer virtualserver_port=9987";
		String expected = "serversnapshotdeploy " + snapshot;
		assertEquals(expected, VirtualServerCommands.serverSnapshotDeploy(snapshot).toString());
	}

	@Test
	public void serverSnapshotDeploy_NullSnapshotException() {
		assertThrows(IllegalArgumentException.class, () -> VirtualServerCommands.serverSnapshotDeploy(null));
	}

	@Test
	public void serverSnapshotDeploy_EmptySnapshotException() {
		assertThrows(IllegalArgumentException.class, () -> VirtualServerCommands.serverSnapshotDeploy(""));
	}

	@Test
	public void serverStart_ValidId() {
		String expected = "serverstart sid=1";
		assertEquals(expected, VirtualServerCommands.serverStart(1).toString());
	}

	@Test
	public void serverStart_ZeroId() {
		String expected = "serverstart sid=0";
		assertEquals(expected, VirtualServerCommands.serverStart(0).toString());
	}

	@Test
	public void serverStart_NegativeId() {
		String expected = "serverstart sid=-1";
		assertEquals(expected, VirtualServerCommands.serverStart(-1).toString());
	}

	@Test
	public void serverStop_WithoutReason() {
		String expected = "serverstop sid=1";
		assertEquals(expected, VirtualServerCommands.serverStop(1, null).toString());
	}

	@Test
	public void serverStop_WithReason() {
		String expected = "serverstop sid=1 reasonmsg=Maintenance\\srestart";
		assertEquals(expected, VirtualServerCommands.serverStop(1, "Maintenance restart").toString());
	}

	@Test
	public void serverStop_EmptyReason() {
		String expected = "serverstop sid=1 reasonmsg=";
		assertEquals(expected, VirtualServerCommands.serverStop(1, "").toString());
	}

	@Test
	public void serverStop_ReasonWithSpecialCharacters() {
		String expected = "serverstop sid=2 reasonmsg=Server\\supdate\\s&\\smaintenance";
		assertEquals(expected, VirtualServerCommands.serverStop(2, "Server update & maintenance").toString());
	}

	@Test
	public void serverCreate_MultipleOptions() {
		Map<VirtualServerProperty, String> options = new HashMap<>();
		options.put(VirtualServerProperty.VIRTUALSERVER_PORT, "9988");
		options.put(VirtualServerProperty.VIRTUALSERVER_MAXCLIENTS, "64");
		options.put(VirtualServerProperty.VIRTUALSERVER_WELCOMEMESSAGE, "Welcome to our server!");
		options.put(VirtualServerProperty.VIRTUALSERVER_PASSWORD, "secret123");
		
		String result = VirtualServerCommands.serverCreate("MyServer", options).toString();
		assertTrue(result.startsWith("servercreate virtualserver_name=MyServer"));
		assertTrue(result.contains("virtualserver_port=9988"));
		assertTrue(result.contains("virtualserver_maxclients=64"));
		assertTrue(result.contains("virtualserver_welcomemessage=Welcome\\sto\\sour\\sserver!"));
		assertTrue(result.contains("virtualserver_password=secret123"));
	}

	@Test
	public void serverEdit_SingleOption() {
		Map<VirtualServerProperty, String> options = new HashMap<>();
		options.put(VirtualServerProperty.VIRTUALSERVER_MAXCLIENTS, "128");
		
		String expected = "serveredit virtualserver_maxclients=128";
		assertEquals(expected, VirtualServerCommands.serverEdit(options).toString());
	}

	@Test
	public void serverSnapshotDeploy_ComplexSnapshot() {
		String snapshot = "version=3 virtualserver_name=Complex\\sServer virtualserver_port=9988 virtualserver_maxclients=100 virtualserver_welcomemessage=Welcome!";
		String expected = "serversnapshotdeploy " + snapshot;
		assertEquals(expected, VirtualServerCommands.serverSnapshotDeploy(snapshot).toString());
	}

	@Test
	public void serverIdGetByPort_HighPort() {
		String expected = "serveridgetbyport virtualserver_port=65535";
		assertEquals(expected, VirtualServerCommands.serverIdGetByPort(65535).toString());
	}

	@Test
	public void serverCreate_LongServerName() {
		String longName = "This is a very long server name that contains multiple words and should be properly encoded";
		String expected = "servercreate virtualserver_name=This\\sis\\sa\\svery\\slong\\sserver\\sname\\sthat\\scontains\\smultiple\\swords\\sand\\sshould\\sbe\\sproperly\\sencoded";
		assertEquals(expected, VirtualServerCommands.serverCreate(longName, null).toString());
	}
}
