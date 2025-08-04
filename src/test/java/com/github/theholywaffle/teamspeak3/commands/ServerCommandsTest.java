package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ServerInstanceProperty;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ServerCommandsTest {

	@Test
	public void bindingList() {
		String expected = "bindinglist";
		assertEquals(expected, ServerCommands.bindingList().toString());
	}

	@Test
	public void gm_ValidMessage() {
		String expected = "gm msg=Hello\\sEveryone!";
		assertEquals(expected, ServerCommands.gm("Hello Everyone!").toString());
	}

	@Test
	public void gm_MessageWithSpecialCharacters() {
		String expected = "gm msg=Server\\swill\\srestart\\sin\\s5\\sminutes!";
		assertEquals(expected, ServerCommands.gm("Server will restart in 5 minutes!").toString());
	}

	@Test
	public void gm_NullMessageException() {
		assertThrows(IllegalArgumentException.class, () -> ServerCommands.gm(null));
	}

	@Test
	public void gm_EmptyMessageException() {
		assertThrows(IllegalArgumentException.class, () -> ServerCommands.gm(""));
	}

	@Test
	public void hostInfo() {
		String expected = "hostinfo";
		assertEquals(expected, ServerCommands.hostInfo().toString());
	}

	@Test
	public void instanceInfo() {
		String expected = "instanceinfo";
		assertEquals(expected, ServerCommands.instanceInfo().toString());
	}

	@Test
	public void instanceEdit_WithOptions() {
		Map<ServerInstanceProperty, String> options = new HashMap<>();
		options.put(ServerInstanceProperty.SERVERINSTANCE_FILETRANSFER_PORT, "30033");
		options.put(ServerInstanceProperty.SERVERINSTANCE_MAX_DOWNLOAD_TOTAL_BANDWIDTH, "65536");
		
		String result = ServerCommands.instanceEdit(options).toString();
		assertTrue(result.startsWith("instanceedit"));
		assertTrue(result.contains("serverinstance_filetransfer_port=30033"));
		assertTrue(result.contains("serverinstance_max_download_total_bandwidth=65536"));
	}

	@Test
	public void instanceEdit_EmptyOptions() {
		Map<ServerInstanceProperty, String> options = new HashMap<>();
		String expected = "instanceedit";
		assertEquals(expected, ServerCommands.instanceEdit(options).toString());
	}

	@Test
	public void instanceEdit_NullOptions() {
		String expected = "instanceedit";
		assertEquals(expected, ServerCommands.instanceEdit(null).toString());
	}

	@Test
	public void logView_DefaultParameters() {
		String expected = "logview";
		assertEquals(expected, ServerCommands.logView(0, false).toString());
	}

	@Test
	public void logView_WithLines() {
		String expected = "logview lines=50";
		assertEquals(expected, ServerCommands.logView(50, false).toString());
	}

	@Test
	public void logView_WithInstance() {
		String expected = "logview instance=1";
		assertEquals(expected, ServerCommands.logView(0, true).toString());
	}

	@Test
	public void logView_WithLinesAndInstance() {
		String expected = "logview lines=25 instance=1";
		assertEquals(expected, ServerCommands.logView(25, true).toString());
	}

	@Test
	public void logView_MaxLines() {
		String expected = "logview lines=100";
		assertEquals(expected, ServerCommands.logView(100, false).toString());
	}

	@Test
	public void logView_TooManyLinesException() {
		assertThrows(IllegalArgumentException.class, () -> ServerCommands.logView(101, false));
	}

	@Test
	public void logView_NegativeLines() {
		// Negative lines should not be included
		String expected = "logview";
		assertEquals(expected, ServerCommands.logView(-1, false).toString());
	}

	@Test
	public void serverProcessStop_WithoutReason() {
		String expected = "serverprocessstop";
		assertEquals(expected, ServerCommands.serverProcessStop(null).toString());
	}

	@Test
	public void serverProcessStop_WithReason() {
		String expected = "serverprocessstop reasonmsg=Maintenance\\srestart";
		assertEquals(expected, ServerCommands.serverProcessStop("Maintenance restart").toString());
	}

	@Test
	public void serverProcessStop_EmptyReason() {
		String expected = "serverprocessstop reasonmsg=";
		assertEquals(expected, ServerCommands.serverProcessStop("").toString());
	}

	@Test
	public void serverProcessStop_ReasonWithSpecialCharacters() {
		String expected = "serverprocessstop reasonmsg=Server\\supdate\\s&\\smaintenance";
		assertEquals(expected, ServerCommands.serverProcessStop("Server update & maintenance").toString());
	}

	@Test
	public void version() {
		String expected = "version";
		assertEquals(expected, ServerCommands.version().toString());
	}

	@Test
	public void instanceEdit_SingleOption() {
		Map<ServerInstanceProperty, String> options = new HashMap<>();
		options.put(ServerInstanceProperty.SERVERINSTANCE_FILETRANSFER_PORT, "30034");
		
		String expected = "instanceedit serverinstance_filetransfer_port=30034";
		assertEquals(expected, ServerCommands.instanceEdit(options).toString());
	}

	@Test
	public void instanceEdit_MultipleOptions() {
		Map<ServerInstanceProperty, String> options = new HashMap<>();
		options.put(ServerInstanceProperty.SERVERINSTANCE_FILETRANSFER_PORT, "30033");
		options.put(ServerInstanceProperty.SERVERINSTANCE_MAX_DOWNLOAD_TOTAL_BANDWIDTH, "131072");
		options.put(ServerInstanceProperty.SERVERINSTANCE_MAX_UPLOAD_TOTAL_BANDWIDTH, "131072");
		
		String result = ServerCommands.instanceEdit(options).toString();
		assertTrue(result.startsWith("instanceedit"));
		assertTrue(result.contains("serverinstance_filetransfer_port=30033"));
		assertTrue(result.contains("serverinstance_max_download_total_bandwidth=131072"));
		assertTrue(result.contains("serverinstance_max_upload_total_bandwidth=131072"));
	}

	@Test
	public void logView_EdgeCaseLines() {
		// Test edge case with exactly 100 lines
		String expected = "logview lines=100 instance=1";
		assertEquals(expected, ServerCommands.logView(100, true).toString());
	}

	@Test
	public void logView_OneLineOnly() {
		String expected = "logview lines=1";
		assertEquals(expected, ServerCommands.logView(1, false).toString());
	}

	@Test
	public void gm_LongMessage() {
		String longMessage = "This is a very long global message that contains multiple words and should be properly encoded when sent to the TeamSpeak server.";
		String expected = "gm msg=This\\sis\\sa\\svery\\slong\\sglobal\\smessage\\sthat\\scontains\\smultiple\\swords\\sand\\sshould\\sbe\\sproperly\\sencoded\\swhen\\ssent\\sto\\sthe\\sTeamSpeak\\sserver.";
		assertEquals(expected, ServerCommands.gm(longMessage).toString());
	}

	@Test
	public void serverProcessStop_LongReason() {
		String longReason = "Scheduled maintenance for server hardware upgrade and software updates";
		String expected = "serverprocessstop reasonmsg=Scheduled\\smaintenance\\sfor\\sserver\\shardware\\supgrade\\sand\\ssoftware\\supdates";
		assertEquals(expected, ServerCommands.serverProcessStop(longReason).toString());
	}
}
