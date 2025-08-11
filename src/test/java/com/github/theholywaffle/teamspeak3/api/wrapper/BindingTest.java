package com.github.theholywaffle.teamspeak3.api.wrapper;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BindingTest {

	@Test
	public void constructor_ValidMap() {
		Map<String, String> map = new HashMap<>();
		map.put("ip", "192.168.1.1");
		
		Binding binding = new Binding(map);
		assertNotNull(binding);
		assertEquals("192.168.1.1", binding.getIp());
	}

	@Test
	public void getIp_ValidIp() {
		Map<String, String> map = new HashMap<>();
		map.put("ip", "10.0.0.1");
		
		Binding binding = new Binding(map);
		assertEquals("10.0.0.1", binding.getIp());
	}

	@Test
	public void getIp_IPv6Address() {
		Map<String, String> map = new HashMap<>();
		map.put("ip", "2001:0db8:85a3:0000:0000:8a2e:0370:7334");
		
		Binding binding = new Binding(map);
		assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", binding.getIp());
	}

	@Test
	public void getIp_LocalhostIp() {
		Map<String, String> map = new HashMap<>();
		map.put("ip", "127.0.0.1");
		
		Binding binding = new Binding(map);
		assertEquals("127.0.0.1", binding.getIp());
	}

	@Test
	public void getIp_WildcardIp() {
		Map<String, String> map = new HashMap<>();
		map.put("ip", "0.0.0.0");
		
		Binding binding = new Binding(map);
		assertEquals("0.0.0.0", binding.getIp());
	}

	@Test
	public void getIp_EmptyIp() {
		Map<String, String> map = new HashMap<>();
		map.put("ip", "");
		
		Binding binding = new Binding(map);
		assertEquals("", binding.getIp());
	}

	@Test
	public void getIp_NullIp() {
		Map<String, String> map = new HashMap<>();
		// ip key not present

		Binding binding = new Binding(map);
		assertEquals("", binding.getIp());
	}

	@Test
	public void getIp_InvalidIpFormat() {
		Map<String, String> map = new HashMap<>();
		map.put("ip", "invalid.ip.address");
		
		Binding binding = new Binding(map);
		assertEquals("invalid.ip.address", binding.getIp());
	}

	@Test
	public void getIp_IpWithPort() {
		Map<String, String> map = new HashMap<>();
		map.put("ip", "192.168.1.1:9987");
		
		Binding binding = new Binding(map);
		assertEquals("192.168.1.1:9987", binding.getIp());
	}

	@Test
	public void binding_EmptyMap() {
		Map<String, String> map = new HashMap<>();

		Binding binding = new Binding(map);
		assertEquals("", binding.getIp());
	}

	@Test
	public void binding_NullMap() {
		// Test that constructor accepts null map (inherited from Wrapper)
		Binding binding = new Binding(null);
		assertNotNull(binding);
		// Note: calling getIp() would throw NPE, but constructor doesn't
	}

	@Test
	public void binding_MultipleEntries() {
		Map<String, String> map = new HashMap<>();
		map.put("ip", "192.168.1.100");
		map.put("other_field", "other_value");
		
		Binding binding = new Binding(map);
		assertEquals("192.168.1.100", binding.getIp());
		// Verify that other fields don't interfere
		assertEquals("other_value", binding.get("other_field"));
	}

	@Test
	public void binding_SpecialCharactersInIp() {
		Map<String, String> map = new HashMap<>();
		map.put("ip", "192.168.1.1/24");
		
		Binding binding = new Binding(map);
		assertEquals("192.168.1.1/24", binding.getIp());
	}

	@Test
	public void binding_WhitespaceInIp() {
		Map<String, String> map = new HashMap<>();
		map.put("ip", " 192.168.1.1 ");
		
		Binding binding = new Binding(map);
		assertEquals(" 192.168.1.1 ", binding.getIp());
	}

//	@Test
//	public void binding_VeryLongIp() {
//		Map<String, String> map = new HashMap<>();
//		String longIp = "192.168.1.1".repeat(10);
//		map.put("ip", longIp);
//
//		Binding binding = new Binding(map);
//		assertEquals(longIp, binding.getIp());
//	}

	@Test
	public void binding_NumericStringIp() {
		Map<String, String> map = new HashMap<>();
		map.put("ip", "123456789");
		
		Binding binding = new Binding(map);
		assertEquals("123456789", binding.getIp());
	}

	@Test
	public void binding_InheritedWrapperMethods() {
		Map<String, String> map = new HashMap<>();
		map.put("ip", "192.168.1.1");
		map.put("test_int", "42");
		map.put("test_boolean", "1");
		
		Binding binding = new Binding(map);
		
		// Test inherited methods from Wrapper
		assertEquals("192.168.1.1", binding.get("ip"));
		assertEquals(42, binding.getInt("test_int"));
		assertTrue(binding.getBoolean("test_boolean"));
	}
}
