package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.ReasonIdentifier;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ClientCommandsTest {

	@Test
	public void clientEdit_WithOptions() {
		Map<ClientProperty, String> options = new HashMap<>();
		options.put(ClientProperty.CLIENT_NICKNAME, "NewNickname");
		options.put(ClientProperty.CLIENT_DESCRIPTION, "New Description");
		
		String result = ClientCommands.clientEdit(5, options).toString();
		assertTrue(result.startsWith("clientedit clid=5"));
		assertTrue(result.contains("client_nickname=NewNickname"));
		assertTrue(result.contains("client_description=New\\sDescription"));
	}

	@Test
	public void clientEdit_EmptyOptions() {
		Map<ClientProperty, String> options = new HashMap<>();
		String expected = "clientedit clid=5";
		assertEquals(expected, ClientCommands.clientEdit(5, options).toString());
	}

	@Test
	public void clientEdit_NullOptions() {
		String expected = "clientedit clid=5";
		assertEquals(expected, ClientCommands.clientEdit(5, null).toString());
	}

	@Test
	public void clientFind_ValidPattern() {
		String expected = "clientfind pattern=TestUser";
		assertEquals(expected, ClientCommands.clientFind("TestUser").toString());
	}

	@Test
	public void clientFind_PatternWithSpaces() {
		String expected = "clientfind pattern=Test\\sUser";
		assertEquals(expected, ClientCommands.clientFind("Test User").toString());
	}

	@Test
	public void clientFind_NullPatternException() {
		assertThrows(IllegalArgumentException.class, () -> ClientCommands.clientFind(null));
	}

	@Test
	public void clientFind_EmptyPatternException() {
		assertThrows(IllegalArgumentException.class, () -> ClientCommands.clientFind(""));
	}

	@Test
	public void clientGetDBIdFromUId_ValidUId() {
		String expected = "clientgetdbidfromuid cluid=unique123";
		assertEquals(expected, ClientCommands.clientGetDBIdFromUId("unique123").toString());
	}

	@Test
	public void clientGetDBIdFromUId_NullUIdException() {
		assertThrows(IllegalArgumentException.class, () -> ClientCommands.clientGetDBIdFromUId(null));
	}

	@Test
	public void clientGetDBIdFromUId_EmptyUIdException() {
		assertThrows(IllegalArgumentException.class, () -> ClientCommands.clientGetDBIdFromUId(""));
	}

	@Test
	public void clientGetIds_ValidUId() {
		String expected = "clientgetids cluid=unique456";
		assertEquals(expected, ClientCommands.clientGetIds("unique456").toString());
	}

	@Test
	public void clientGetIds_NullUIdException() {
		assertThrows(IllegalArgumentException.class, () -> ClientCommands.clientGetIds(null));
	}

	@Test
	public void clientGetIds_EmptyUIdException() {
		assertThrows(IllegalArgumentException.class, () -> ClientCommands.clientGetIds(""));
	}

	@Test
	public void clientInfo_ValidClientId() {
		String expected = "clientinfo clid=10";
		assertEquals(expected, ClientCommands.clientInfo(10).toString());
	}

	@Test
	public void clientInfo_ZeroClientId() {
		String expected = "clientinfo clid=0";
		assertEquals(expected, ClientCommands.clientInfo(0).toString());
	}

	@Test
	public void clientKick_SingleClient() {
		String expected = "clientkick reasonid=4 clid=10";
		assertEquals(expected, ClientCommands.clientKick(ReasonIdentifier.REASON_KICK_CHANNEL, null, 10).toString());
	}

	@Test
	public void clientKick_MultipleClients() {
		String expected = "clientkick reasonid=5 clid=10|clid=15|clid=20";
		assertEquals(expected, ClientCommands.clientKick(ReasonIdentifier.REASON_KICK_SERVER, null, 10, 15, 20).toString());
	}

	@Test
	public void clientKick_WithReasonMessage() {
		String expected = "clientkick reasonid=4 reasonmsg=Inappropriate\\sbehavior clid=10";
		assertEquals(expected, ClientCommands.clientKick(ReasonIdentifier.REASON_KICK_CHANNEL, "Inappropriate behavior", 10).toString());
	}

	@Test
	public void clientKick_NullClientIdsException() {
		assertThrows(IllegalArgumentException.class, () -> ClientCommands.clientKick(ReasonIdentifier.REASON_KICK_SERVER, null, (int[]) null));
	}

	@Test
	public void clientKick_EmptyClientIdsException() {
		assertThrows(IllegalArgumentException.class, () -> ClientCommands.clientKick(ReasonIdentifier.REASON_KICK_SERVER, null));
	}

	@Test
	public void clientList_AllOptions() {
		String expected = "clientlist -uid -away -voice -times -groups -info -icon -country -ip -badges -location";
		assertEquals(expected, ClientCommands.clientList().toString());
	}

	@Test
	public void clientMove_SingleClient() {
		String expected = "clientmove clid=5 cid=10";
		assertEquals(expected, ClientCommands.clientMove(5, 10, null).toString());
	}

	@Test
	public void clientMove_SingleClientWithPassword() {
		String expected = "clientmove clid=5 cid=10 cpw=secret";
		assertEquals(expected, ClientCommands.clientMove(5, 10, "secret").toString());
	}

	@Test
	public void clientMove_MultipleClients() {
		int[] clientIds = {5, 10, 15};
		String expected = "clientmove cid=20 clid=5|clid=10|clid=15";
		assertEquals(expected, ClientCommands.clientMove(clientIds, 20, null).toString());
	}

	@Test
	public void clientMove_MultipleClientsWithPassword() {
		int[] clientIds = {5, 10};
		String expected = "clientmove cid=20 cpw=password123 clid=5|clid=10";
		assertEquals(expected, ClientCommands.clientMove(clientIds, 20, "password123").toString());
	}

	@Test
	public void clientMove_NullClientIdsException() {
		assertThrows(IllegalArgumentException.class, () -> ClientCommands.clientMove(null, 10, null));
	}

	@Test
	public void clientMove_EmptyClientIdsException() {
		int[] clientIds = {};
		assertThrows(IllegalArgumentException.class, () -> ClientCommands.clientMove(clientIds, 10, null));
	}

	@Test
	public void clientPoke_WithMessage() {
		String expected = "clientpoke clid=5 msg=Hello\\sthere!";
		assertEquals(expected, ClientCommands.clientPoke(5, "Hello there!").toString());
	}

	@Test
	public void clientPoke_EmptyMessage() {
		String expected = "clientpoke clid=5 msg=";
		assertEquals(expected, ClientCommands.clientPoke(5, "").toString());
	}

	@Test
	public void clientPoke_NullMessage() {
		String expected = "clientpoke clid=5 msg=";
		assertEquals(expected, ClientCommands.clientPoke(5, null).toString());
	}

	@Test
	public void clientSetServerQueryLogin_ValidUsername() {
		String expected = "clientsetserverquerylogin client_login_name=admin";
		assertEquals(expected, ClientCommands.clientSetServerQueryLogin("admin").toString());
	}

	@Test
	public void clientSetServerQueryLogin_EmptyUsername() {
		String expected = "clientsetserverquerylogin client_login_name=";
		assertEquals(expected, ClientCommands.clientSetServerQueryLogin("").toString());
	}

	@Test
	public void clientSetServerQueryLogin_NullUsername() {
		String expected = "clientsetserverquerylogin client_login_name=";
		assertEquals(expected, ClientCommands.clientSetServerQueryLogin(null).toString());
	}

	@Test
	public void clientUpdate_WithOptions() {
		Map<ClientProperty, String> options = new HashMap<>();
		options.put(ClientProperty.CLIENT_NICKNAME, "UpdatedNick");
		options.put(ClientProperty.CLIENT_DESCRIPTION, "Updated Description");
		
		String result = ClientCommands.clientUpdate(options).toString();
		assertTrue(result.startsWith("clientupdate"));
		assertTrue(result.contains("client_nickname=UpdatedNick"));
		assertTrue(result.contains("client_description=Updated\\sDescription"));
	}

	@Test
	public void clientUpdate_EmptyOptions() {
		Map<ClientProperty, String> options = new HashMap<>();
		String expected = "clientupdate";
		assertEquals(expected, ClientCommands.clientUpdate(options).toString());
	}

	@Test
	public void clientUpdate_NullOptions() {
		String expected = "clientupdate";
		assertEquals(expected, ClientCommands.clientUpdate(null).toString());
	}

	@Test
	public void sendTextMessage_ValidMessage() {
		String expected = "sendtextmessage targetmode=1 target=5 msg=Hello\\sWorld!";
		assertEquals(expected, ClientCommands.sendTextMessage(1, 5, "Hello World!").toString());
	}

	@Test
	public void sendTextMessage_EmptyMessage() {
		String expected = "sendtextmessage targetmode=2 target=10 msg=";
		assertEquals(expected, ClientCommands.sendTextMessage(2, 10, "").toString());
	}

	@Test
	public void sendTextMessage_NullMessage() {
		String expected = "sendtextmessage targetmode=3 target=15 msg=";
		assertEquals(expected, ClientCommands.sendTextMessage(3, 15, null).toString());
	}

	@Test
	public void sendTextMessage_SpecialCharacters() {
		String expected = "sendtextmessage targetmode=1 target=5 msg=Hello\\s\"World\"!";
		assertEquals(expected, ClientCommands.sendTextMessage(1, 5, "Hello \"World\"!").toString());
	}

	@Test
	public void clientEdit_SpecialCharactersInNickname() {
		Map<ClientProperty, String> options = new HashMap<>();
		options.put(ClientProperty.CLIENT_NICKNAME, "User [Admin]");

		String result = ClientCommands.clientEdit(5, options).toString();
		assertTrue(result.contains("client_nickname=User\\s[Admin]"));
	}

	@Test
	public void clientKick_SpecialCharactersInReason() {
		String expected = "clientkick reasonid=4 reasonmsg=Bad\\sbehavior\\s&\\sspamming clid=10";
		assertEquals(expected, ClientCommands.clientKick(ReasonIdentifier.REASON_KICK_CHANNEL, "Bad behavior & spamming", 10).toString());
	}
}
