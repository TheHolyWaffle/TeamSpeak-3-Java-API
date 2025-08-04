package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class QueryCommandsTest {

	@Test
	public void logIn_ValidCredentials() {
		final String expected = "login username password";
		assertEquals(expected, QueryCommands.logIn("username", "password").toString());
	}

	@Test
	public void logIn_NullUsernameException() {
		assertThrows(IllegalArgumentException.class, () -> QueryCommands.logIn(null, "password"));
	}

	@Test
	public void logIn_EmptyUsernameException() {
		assertThrows(IllegalArgumentException.class, () -> QueryCommands.logIn("", "password"));
	}

	@Test
	public void logIn_NullPasswordException() {
		assertThrows(IllegalArgumentException.class, () -> QueryCommands.logIn("username", null));
	}

	@Test
	public void logIn_EmptyPasswordException() {
		assertThrows(IllegalArgumentException.class, () -> QueryCommands.logIn("username", ""));
	}

	@Test
	public void logOut() {
		final String expected = "logout";
		assertEquals(expected, QueryCommands.logOut().toString());
	}

	@Test
	public void quit() {
		final String expected = "quit";
		assertEquals(expected, QueryCommands.quit().toString());
	}

	@Test
	public void serverNotifyRegister_WithChannelId() {
		final String expected = "servernotifyregister event=channel id=5";
		assertEquals(expected, QueryCommands.serverNotifyRegister(TS3EventType.CHANNEL, 5).toString());
	}

	@Test
	public void serverNotifyRegister_WithoutChannelId() {
		final String expected = "servernotifyregister event=server";
		assertEquals(expected, QueryCommands.serverNotifyRegister(TS3EventType.SERVER, -1).toString());
	}

	@Test
	public void serverNotifyRegister_NullEventTypeException() {
		assertThrows(IllegalArgumentException.class, () -> QueryCommands.serverNotifyRegister(null, 0));
	}

	@Test
	public void serverNotifyUnregister() {
		final String expected = "servernotifyunregister";
		assertEquals(expected, QueryCommands.serverNotifyUnregister().toString());
	}

	@Test
	public void useId_WithNickname() {
		final String expected = "use sid=1 -virtual client_nickname=TestNick";
		assertEquals(expected, QueryCommands.useId(1, "TestNick").toString());
	}

	@Test
	public void useId_WithoutNickname() {
		final String expected = "use sid=1 -virtual";
		assertEquals(expected, QueryCommands.useId(1, null).toString());
	}

	@Test
	public void usePort_WithNickname() {
		final String expected = "use port=9987 -virtual client_nickname=TestNick";
		assertEquals(expected, QueryCommands.usePort(9987, "TestNick").toString());
	}

	@Test
	public void usePort_WithoutNickname() {
		final String expected = "use port=9987 -virtual";
		assertEquals(expected, QueryCommands.usePort(9987, null).toString());
	}

	@Test
	public void whoAmI() {
		final String expected = "whoami";
		assertEquals(expected, QueryCommands.whoAmI().toString());
	}
}
