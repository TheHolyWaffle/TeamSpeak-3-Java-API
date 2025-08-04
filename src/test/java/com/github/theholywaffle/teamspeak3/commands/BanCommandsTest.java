package com.github.theholywaffle.teamspeak3.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BanCommandsTest {

	@Test
	public void banAdd_WithIP() {
		String expected = "banadd ip=192.168.1.100";
		assertEquals(expected, BanCommands.banAdd("192.168.1.100", null, null, null, 0, null).toString());
	}

	@Test
	public void banAdd_WithName() {
		String expected = "banadd name=BadUser";
		assertEquals(expected, BanCommands.banAdd(null, "BadUser", null, null, 0, null).toString());
	}

	@Test
	public void banAdd_WithUID() {
		String expected = "banadd uid=unique123";
		assertEquals(expected, BanCommands.banAdd(null, null, "unique123", null, 0, null).toString());
	}

	@Test
	public void banAdd_WithAllParameters() {
		String expected = "banadd ip=192.168.1.100 name=BadUser uid=unique123 mytsid=myts123 time=3600 banreason=Spamming";
		assertEquals(expected, BanCommands.banAdd("192.168.1.100", "BadUser", "unique123", "myts123", 3600, "Spamming").toString());
	}

	@Test
	public void banAdd_WithTimeAndReason() {
		String expected = "banadd ip=192.168.1.100 time=7200 banreason=Inappropriate\\sbehavior";
		assertEquals(expected, BanCommands.banAdd("192.168.1.100", null, null, null, 7200, "Inappropriate behavior").toString());
	}

	@Test
	public void banAdd_WithZeroTime() {
		// Zero time should not be included in the command
		String expected = "banadd ip=192.168.1.100";
		assertEquals(expected, BanCommands.banAdd("192.168.1.100", null, null, null, 0, null).toString());
	}

	@Test
	public void banAdd_WithNegativeTime() {
		// Negative time should not be included in the command
		String expected = "banadd ip=192.168.1.100";
		assertEquals(expected, BanCommands.banAdd("192.168.1.100", null, null, null, -1, null).toString());
	}

	@Test
	public void banAdd_AllNullParametersException() {
		assertThrows(IllegalArgumentException.class, () -> BanCommands.banAdd(null, null, null, null, 0, null));
	}

	@Test
	public void banAdd_EmptyStringParameters() {
		// Empty strings should still be included as they are not null
		String expected = "banadd ip= name= uid= mytsid= banreason=";
		assertEquals(expected, BanCommands.banAdd("", "", "", "", 0, "").toString());
	}

	@Test
	public void banClient_SingleClient() {
		int[] clientIds = {5};
		String expected = "banclient clid=5";
		assertEquals(expected, BanCommands.banClient(clientIds, 0, null, false).toString());
	}

	@Test
	public void banClient_MultipleClients() {
		int[] clientIds = {5, 10, 15};
		String expected = "banclient clid=5|clid=10|clid=15";
		assertEquals(expected, BanCommands.banClient(clientIds, 0, null, false).toString());
	}

	@Test
	public void banClient_WithTime() {
		int[] clientIds = {5};
		String expected = "banclient time=3600 clid=5";
		assertEquals(expected, BanCommands.banClient(clientIds, 3600, null, false).toString());
	}

	@Test
	public void banClient_WithReason() {
		int[] clientIds = {5};
		String expected = "banclient banreason=Violation\\sof\\srules clid=5";
		assertEquals(expected, BanCommands.banClient(clientIds, 0, "Violation of rules", false).toString());
	}

	@Test
	public void banClient_WithContinueOnError() {
		int[] clientIds = {5};
		String expected = "banclient -continueonerror clid=5";
		assertEquals(expected, BanCommands.banClient(clientIds, 0, null, true).toString());
	}

	@Test
	public void banClient_WithAllParameters() {
		int[] clientIds = {5, 10};
		String expected = "banclient time=7200 banreason=Multiple\\sviolations -continueonerror clid=5|clid=10";
		assertEquals(expected, BanCommands.banClient(clientIds, 7200, "Multiple violations", true).toString());
	}

	@Test
	public void banClient_WithZeroTime() {
		// Zero time should not be included
		int[] clientIds = {5};
		String expected = "banclient clid=5";
		assertEquals(expected, BanCommands.banClient(clientIds, 0, null, false).toString());
	}

	@Test
	public void banClient_WithNegativeTime() {
		// Negative time should not be included
		int[] clientIds = {5};
		String expected = "banclient clid=5";
		assertEquals(expected, BanCommands.banClient(clientIds, -1, null, false).toString());
	}

	@Test
	public void banClient_EmptyReason() {
		int[] clientIds = {5};
		String expected = "banclient banreason= clid=5";
		assertEquals(expected, BanCommands.banClient(clientIds, 0, "", false).toString());
	}

	@Test
	public void banDel_ValidBanId() {
		String expected = "bandel banid=123";
		assertEquals(expected, BanCommands.banDel(123).toString());
	}

	@Test
	public void banDel_ZeroBanId() {
		String expected = "bandel banid=0";
		assertEquals(expected, BanCommands.banDel(0).toString());
	}

	@Test
	public void banDel_NegativeBanId() {
		String expected = "bandel banid=-1";
		assertEquals(expected, BanCommands.banDel(-1).toString());
	}

	@Test
	public void banDelAll() {
		String expected = "bandelall";
		assertEquals(expected, BanCommands.banDelAll().toString());
	}

	@Test
	public void banList() {
		String expected = "banlist";
		assertEquals(expected, BanCommands.banList().toString());
	}

	@Test
	public void banAdd_SpecialCharactersInReason() {
		String expected = "banadd ip=192.168.1.100 banreason=User\\swas\\s\"very\"\\sbad";
		assertEquals(expected, BanCommands.banAdd("192.168.1.100", null, null, null, 0, "User was \"very\" bad").toString());
	}

	@Test
	public void banAdd_SpecialCharactersInName() {
		String expected = "banadd name=Bad\\sUser\\s[Admin]";
		assertEquals(expected, BanCommands.banAdd(null, "Bad User [Admin]", null, null, 0, null).toString());
	}

	@Test
	public void banClient_LargeClientArray() {
		int[] clientIds = new int[10];
		for (int i = 0; i < 10; i++) {
			clientIds[i] = i + 1;
		}
		String expected = "banclient clid=1|clid=2|clid=3|clid=4|clid=5|clid=6|clid=7|clid=8|clid=9|clid=10";
		assertEquals(expected, BanCommands.banClient(clientIds, 0, null, false).toString());
	}

	@Test
	public void banAdd_MaxTimeValue() {
		String expected = "banadd ip=192.168.1.100 time=" + Long.MAX_VALUE;
		assertEquals(expected, BanCommands.banAdd("192.168.1.100", null, null, null, Long.MAX_VALUE, null).toString());
	}

	@Test
	public void banClient_MaxTimeValue() {
		int[] clientIds = {5};
		String expected = "banclient time=" + Long.MAX_VALUE + " clid=5";
		assertEquals(expected, BanCommands.banClient(clientIds, Long.MAX_VALUE, null, false).toString());
	}
}
