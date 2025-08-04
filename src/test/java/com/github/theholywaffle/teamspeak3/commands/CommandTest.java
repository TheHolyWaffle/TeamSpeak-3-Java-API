package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.Parameter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class CommandTest {

	@Test
	public void constructor_ValidParameters() {
		Collection<Parameter> parameters = Arrays.asList(
			new KeyValueParam("key1", "value1"),
			new KeyValueParam("key2", "value2")
		);
		
		Command command = new Command("testcommand", parameters);
		
		assertNotNull(command);
		assertEquals("testcommand", command.getName());
		assertNotNull(command.getFuture());
	}

	@Test
	public void constructor_EmptyParameters() {
		Collection<Parameter> parameters = new ArrayList<>();
		
		Command command = new Command("testcommand", parameters);
		
		assertNotNull(command);
		assertEquals("testcommand", command.getName());
		assertNotNull(command.getFuture());
	}

	@Test
	public void getName_ReturnsCorrectName() {
		Collection<Parameter> parameters = new ArrayList<>();
		Command command = new Command("mycommand", parameters);
		
		assertEquals("mycommand", command.getName());
	}

	@Test
	public void getFuture_ReturnsNonNullFuture() {
		Collection<Parameter> parameters = new ArrayList<>();
		Command command = new Command("testcommand", parameters);
		
		assertNotNull(command.getFuture());
	}

	@Test
	public void toString_NoParameters() {
		Collection<Parameter> parameters = new ArrayList<>();
		Command command = new Command("testcommand", parameters);
		
		assertEquals("testcommand", command.toString());
	}

	@Test
	public void toString_WithKeyValueParameters() {
		Collection<Parameter> parameters = Arrays.asList(
			new KeyValueParam("key1", "value1"),
			new KeyValueParam("key2", "value2")
		);
		
		Command command = new Command("testcommand", parameters);
		
		assertEquals("testcommand key1=value1 key2=value2", command.toString());
	}

	@Test
	public void toString_WithOptionParameters() {
		Collection<Parameter> parameters = Arrays.asList(
			new OptionParam("option1"),
			new OptionParam("option2")
		);
		
		Command command = new Command("testcommand", parameters);
		
		assertEquals("testcommand -option1 -option2", command.toString());
	}

	@Test
	public void toString_WithMixedParameters() {
		Collection<Parameter> parameters = Arrays.asList(
			new KeyValueParam("key1", "value1"),
			new OptionParam("option1"),
			new KeyValueParam("key2", "value2")
		);
		
		Command command = new Command("testcommand", parameters);
		
		assertEquals("testcommand key1=value1 -option1 key2=value2", command.toString());
	}

	@Test
	public void toString_WithSpecialCharacters() {
		Collection<Parameter> parameters = Arrays.asList(
			new KeyValueParam("message", "Hello World"),
			new KeyValueParam("path", "/some/path")
		);
		
		Command command = new Command("sendtextmessage", parameters);
		
		// Note: The actual encoding of special characters is handled by the Parameter implementations
		String result = command.toString();
		assertTrue(result.startsWith("sendtextmessage"));
		assertTrue(result.contains("message="));
		assertTrue(result.contains("path="));
	}

	@Test
	public void toString_LongCommandName() {
		Collection<Parameter> parameters = Arrays.asList(
			new KeyValueParam("param", "value")
		);
		
		Command command = new Command("verylongcommandnamethatexceedsnormallength", parameters);
		
		assertEquals("verylongcommandnamethatexceedsnormallength param=value", command.toString());
	}

	@Test
	public void toString_EmptyCommandName() {
		Collection<Parameter> parameters = Arrays.asList(
			new KeyValueParam("param", "value")
		);
		
		Command command = new Command("", parameters);
		
		assertEquals(" param=value", command.toString());
	}
}
