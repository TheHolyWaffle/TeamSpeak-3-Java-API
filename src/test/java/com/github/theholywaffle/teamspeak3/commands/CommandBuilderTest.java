package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.ValueParam;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CommandBuilderTest {

	@Test
	public void constructor_BasicCommandName() {
		CommandBuilder builder = new CommandBuilder("test");
		Command command = builder.build();
		assertEquals("test", command.toString());
	}

	@Test
	public void constructor_WithExpectedParamCount() {
		CommandBuilder builder = new CommandBuilder("test", 5);
		Command command = builder.build();
		assertEquals("test", command.toString());
	}

	@Test
	public void add_SingleParameter() {
		CommandBuilder builder = new CommandBuilder("test");
		builder.add(new KeyValueParam("key", "value"));
		Command command = builder.build();
		assertEquals("test key=value", command.toString());
	}

	@Test
	public void add_MultipleParameters() {
		CommandBuilder builder = new CommandBuilder("test");
		builder.add(new KeyValueParam("key1", "value1"));
		builder.add(new KeyValueParam("key2", "value2"));
		builder.add(new OptionParam("option"));
		Command command = builder.build();
		assertEquals("test key1=value1 key2=value2 -option", command.toString());
	}

	@Test
	public void add_ChainedCalls() {
		Command command = new CommandBuilder("test")
			.add(new KeyValueParam("key1", "value1"))
			.add(new KeyValueParam("key2", "value2"))
			.build();
		assertEquals("test key1=value1 key2=value2", command.toString());
	}

	@Test
	public void addIf_TrueCondition() {
		CommandBuilder builder = new CommandBuilder("test");
		builder.addIf(true, new KeyValueParam("key", "value"));
		Command command = builder.build();
		assertEquals("test key=value", command.toString());
	}

	@Test
	public void addIf_FalseCondition() {
		CommandBuilder builder = new CommandBuilder("test");
		builder.addIf(false, new KeyValueParam("key", "value"));
		Command command = builder.build();
		assertEquals("test", command.toString());
	}

	@Test
	public void addIf_ChainedCalls() {
		Command command = new CommandBuilder("test")
			.addIf(true, new KeyValueParam("key1", "value1"))
			.addIf(false, new KeyValueParam("key2", "value2"))
			.addIf(true, new OptionParam("option"))
			.build();
		assertEquals("test key1=value1 -option", command.toString());
	}

	@Test
	public void addProperties_NullProperties() {
		CommandBuilder builder = new CommandBuilder("test");
		builder.addProperties(null);
		Command command = builder.build();
		assertEquals("test", command.toString());
	}

	@Test
	public void addProperties_EmptyProperties() {
		CommandBuilder builder = new CommandBuilder("test");
		Map<ClientProperty, String> properties = new HashMap<>();
		builder.addProperties(properties);
		Command command = builder.build();
		assertEquals("test", command.toString());
	}

	@Test
	public void addProperties_SingleProperty() {
		CommandBuilder builder = new CommandBuilder("test");
		Map<ClientProperty, String> properties = new HashMap<>();
		properties.put(ClientProperty.CLIENT_NICKNAME, "TestUser");
		builder.addProperties(properties);
		Command command = builder.build();
		assertEquals("test client_nickname=TestUser", command.toString());
	}

	@Test
	public void addProperties_MultipleProperties() {
		CommandBuilder builder = new CommandBuilder("test");
		Map<ClientProperty, String> properties = new HashMap<>();
		properties.put(ClientProperty.CLIENT_NICKNAME, "TestUser");
		properties.put(ClientProperty.CLIENT_DESCRIPTION, "Test Description");
		builder.addProperties(properties);
		Command command = builder.build();
		
		String result = command.toString();
		assertTrue(result.startsWith("test"));
		assertTrue(result.contains("client_nickname=TestUser"));
		assertTrue(result.contains("client_description=Test\\sDescription"));
	}

	@Test
	public void addProperties_NullPropertyException() {
		CommandBuilder builder = new CommandBuilder("test");
		Map<ClientProperty, String> properties = new HashMap<>();
		properties.put(ClientProperty.CLIENT_NICKNAME, "TestUser");
		properties.put(null, "value");
		
		assertThrows(IllegalArgumentException.class, () -> builder.addProperties(properties));
	}

	@Test
	public void addProperties_NonChangeablePropertyException() {
		CommandBuilder builder = new CommandBuilder("test");
		Map<ClientProperty, String> properties = new HashMap<>();
		properties.put(ClientProperty.CLIENT_COUNTRY, "US"); // CLIENT_COUNTRY is not changeable
		
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
			() -> builder.addProperties(properties));
		assertTrue(exception.getMessage().contains("client_country is not changeable"));
	}

	@Test
	public void addProperties_MixedChangeableAndNonChangeable() {
		CommandBuilder builder = new CommandBuilder("test");
		Map<ClientProperty, String> properties = new HashMap<>();
		properties.put(ClientProperty.CLIENT_NICKNAME, "TestUser"); // changeable
		properties.put(ClientProperty.CLIENT_CREATED, "123456789"); // not changeable
		
		assertThrows(IllegalArgumentException.class, () -> builder.addProperties(properties));
	}

	@Test
	public void addProperties_WithSpecialCharacters() {
		CommandBuilder builder = new CommandBuilder("test");
		Map<ClientProperty, String> properties = new HashMap<>();
		properties.put(ClientProperty.CLIENT_NICKNAME, "Test User [VIP]");
		properties.put(ClientProperty.CLIENT_DESCRIPTION, "User with \"special\" privileges");
		builder.addProperties(properties);
		Command command = builder.build();
		
		String result = command.toString();
		assertTrue(result.contains("client_nickname=Test\\sUser\\s[VIP]"));
		assertTrue(result.contains("client_description=User\\swith\\s\"special\"\\sprivileges"));
	}

	@Test
	public void build_EmptyCommand() {
		CommandBuilder builder = new CommandBuilder("empty");
		Command command = builder.build();
		assertEquals("empty", command.toString());
	}

	@Test
	public void build_ComplexCommand() {
		Command command = new CommandBuilder("complex", 5)
			.add(new KeyValueParam("param1", "value1"))
			.addIf(true, new KeyValueParam("param2", "value2"))
			.addIf(false, new KeyValueParam("param3", "value3"))
			.add(new OptionParam("option1"))
			.add(new ValueParam("rawvalue"))
			.build();
		
		assertEquals("complex param1=value1 param2=value2 -option1 rawvalue", command.toString());
	}

	@Test
	public void mixedParameterTypes() {
		Map<ClientProperty, String> properties = new HashMap<>();
		properties.put(ClientProperty.CLIENT_NICKNAME, "TestUser");
		
		Command command = new CommandBuilder("mixed")
			.add(new KeyValueParam("before", "properties"))
			.addProperties(properties)
			.add(new KeyValueParam("after", "properties"))
			.build();
		
		String result = command.toString();
		assertTrue(result.startsWith("mixed"));
		assertTrue(result.contains("before=properties"));
		assertTrue(result.contains("client_nickname=TestUser"));
		assertTrue(result.contains("after=properties"));
	}

	@Test
	public void addProperties_NullValues() {
		CommandBuilder builder = new CommandBuilder("test");
		Map<ClientProperty, String> properties = new HashMap<>();
		properties.put(ClientProperty.CLIENT_NICKNAME, null);
		properties.put(ClientProperty.CLIENT_DESCRIPTION, "");
		builder.addProperties(properties);
		Command command = builder.build();
		
		String result = command.toString();
		assertTrue(result.contains("client_nickname="));
		assertTrue(result.contains("client_description="));
	}

	@Test
	public void addProperties_LargePropertySet() {
		CommandBuilder builder = new CommandBuilder("test");
		Map<ClientProperty, String> properties = new HashMap<>();
		properties.put(ClientProperty.CLIENT_NICKNAME, "User1");
		properties.put(ClientProperty.CLIENT_DESCRIPTION, "Description1");
		properties.put(ClientProperty.CLIENT_ICON_ID, "100");
		properties.put(ClientProperty.CLIENT_IS_CHANNEL_COMMANDER, "1");
		properties.put(ClientProperty.CLIENT_IS_TALKER, "0");
		
		builder.addProperties(properties);
		Command command = builder.build();
		
		String result = command.toString();
		assertTrue(result.startsWith("test"));
		assertTrue(result.contains("client_nickname=User1"));
		assertTrue(result.contains("client_description=Description1"));
		assertTrue(result.contains("client_icon_id=100"));
		assertTrue(result.contains("client_is_channel_commander=1"));
		assertTrue(result.contains("client_is_talker=0"));
	}

	@Test
	public void commandName_WithSpaces() {
		CommandBuilder builder = new CommandBuilder("command with spaces");
		Command command = builder.build();
		assertEquals("command with spaces", command.toString());
	}

	@Test
	public void commandName_Empty() {
		CommandBuilder builder = new CommandBuilder("");
		Command command = builder.build();
		assertEquals("", command.toString());
	}

	@Test
	public void commandName_Null() {
		// CommandBuilder doesn't handle null command names - it throws NPE
		assertThrows(NullPointerException.class, () -> {
			CommandBuilder builder = new CommandBuilder(null);
			Command command = builder.build();
			command.toString();
		});
	}

	@Test
	public void addProperties_PropertyWithForwardSlashes() {
		CommandBuilder builder = new CommandBuilder("test");
		Map<ClientProperty, String> properties = new HashMap<>();
		properties.put(ClientProperty.CLIENT_DESCRIPTION, "path/to/description");
		builder.addProperties(properties);
		Command command = builder.build();
		
		assertTrue(command.toString().contains("client_description=path\\/to\\/description"));
	}

	@Test
	public void addProperties_PropertyWithPipes() {
		CommandBuilder builder = new CommandBuilder("test");
		Map<ClientProperty, String> properties = new HashMap<>();
		properties.put(ClientProperty.CLIENT_DESCRIPTION, "value|with|pipes");
		builder.addProperties(properties);
		Command command = builder.build();
		
		assertTrue(command.toString().contains("client_description=value\\pwith\\ppipes"));
	}

	@Test
	public void addProperties_PropertyWithBackslashes() {
		CommandBuilder builder = new CommandBuilder("test");
		Map<ClientProperty, String> properties = new HashMap<>();
		properties.put(ClientProperty.CLIENT_DESCRIPTION, "value\\with\\backslashes");
		builder.addProperties(properties);
		Command command = builder.build();
		
		assertTrue(command.toString().contains("client_description=value\\\\with\\\\backslashes"));
	}
}
