package com.github.theholywaffle.teamspeak3.api.wrapper;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BanTest {

	@Test
	public void constructor_ValidMap() {
		Map<String, String> map = new HashMap<>();
		map.put("banid", "123");
		map.put("ip", "192.168.1.100");
		
		Ban ban = new Ban(map);
		assertNotNull(ban);
		assertEquals(123, ban.getId());
		assertEquals("192.168.1.100", ban.getBannedIp());
	}

	@Test
	public void getId_ValidBanId() {
		Map<String, String> map = new HashMap<>();
		map.put("banid", "456");
		
		Ban ban = new Ban(map);
		assertEquals(456, ban.getId());
	}

	@Test
	public void getId_ZeroBanId() {
		Map<String, String> map = new HashMap<>();
		map.put("banid", "0");
		
		Ban ban = new Ban(map);
		assertEquals(0, ban.getId());
	}

	@Test
	public void getId_NegativeBanId() {
		Map<String, String> map = new HashMap<>();
		map.put("banid", "-1");
		
		Ban ban = new Ban(map);
		assertEquals(-1, ban.getId());
	}

	@Test
	public void getBannedIp_ValidIp() {
		Map<String, String> map = new HashMap<>();
		map.put("ip", "10.0.0.1");
		
		Ban ban = new Ban(map);
		assertEquals("10.0.0.1", ban.getBannedIp());
	}

	@Test
	public void getBannedIp_EmptyIp() {
		Map<String, String> map = new HashMap<>();
		map.put("ip", "");
		
		Ban ban = new Ban(map);
		assertEquals("", ban.getBannedIp());
	}

	@Test
	public void getBannedIp_NullIp() {
		Map<String, String> map = new HashMap<>();
		// ip key not present

		Ban ban = new Ban(map);
		assertEquals("", ban.getBannedIp());
	}

	@Test
	public void getBannedName_ValidName() {
		Map<String, String> map = new HashMap<>();
		map.put("name", "BadUser");
		
		Ban ban = new Ban(map);
		assertEquals("BadUser", ban.getBannedName());
	}

	@Test
	public void getBannedName_EmptyName() {
		Map<String, String> map = new HashMap<>();
		map.put("name", "");
		
		Ban ban = new Ban(map);
		assertEquals("", ban.getBannedName());
	}

	@Test
	public void getBannedName_NullName() {
		Map<String, String> map = new HashMap<>();
		// name key not present

		Ban ban = new Ban(map);
		assertEquals("", ban.getBannedName());
	}

	@Test
	public void getBannedUId_ValidUId() {
		Map<String, String> map = new HashMap<>();
		map.put("uid", "unique123456");

		Ban ban = new Ban(map);
		assertEquals("unique123456", ban.getBannedUId());
	}

	@Test
	public void getBannedUId_EmptyUId() {
		Map<String, String> map = new HashMap<>();
		map.put("uid", "");

		Ban ban = new Ban(map);
		assertEquals("", ban.getBannedUId());
	}

	@Test
	public void getBannedUId_NullUId() {
		Map<String, String> map = new HashMap<>();
		// uid key not present

		Ban ban = new Ban(map);
		assertEquals("", ban.getBannedUId());
	}

	@Test
	public void getLastNickname_ValidNickname() {
		Map<String, String> map = new HashMap<>();
		map.put("lastnickname", "LastSeenName");
		
		Ban ban = new Ban(map);
		assertEquals("LastSeenName", ban.getLastNickname());
	}

	@Test
	public void getLastNickname_EmptyNickname() {
		Map<String, String> map = new HashMap<>();
		map.put("lastnickname", "");
		
		Ban ban = new Ban(map);
		assertEquals("", ban.getLastNickname());
	}

	@Test
	public void getLastNickname_NullNickname() {
		Map<String, String> map = new HashMap<>();
		// lastnickname key not present

		Ban ban = new Ban(map);
		assertEquals("", ban.getLastNickname());
	}

	@Test
	public void getCreatedDate_ValidTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("created", "1609459200"); // 2021-01-01 00:00:00 UTC
		
		Ban ban = new Ban(map);
		Date expectedDate = new Date(1609459200L * 1000);
		assertEquals(expectedDate, ban.getCreatedDate());
	}

	@Test
	public void getCreatedDate_ZeroTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("created", "0");
		
		Ban ban = new Ban(map);
		Date expectedDate = new Date(0);
		assertEquals(expectedDate, ban.getCreatedDate());
	}

	@Test
	public void getCreatedDate_NegativeTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("created", "-1");
		
		Ban ban = new Ban(map);
		Date expectedDate = new Date(-1000);
		assertEquals(expectedDate, ban.getCreatedDate());
	}

	@Test
	public void getDuration_ValidDuration() {
		Map<String, String> map = new HashMap<>();
		map.put("duration", "3600"); // 1 hour
		
		Ban ban = new Ban(map);
		assertEquals(3600L, ban.getDuration());
	}

	@Test
	public void getDuration_ZeroDuration() {
		Map<String, String> map = new HashMap<>();
		map.put("duration", "0"); // Permanent ban
		
		Ban ban = new Ban(map);
		assertEquals(0L, ban.getDuration());
	}

	@Test
	public void getDuration_NegativeDuration() {
		Map<String, String> map = new HashMap<>();
		map.put("duration", "-1");
		
		Ban ban = new Ban(map);
		assertEquals(-1L, ban.getDuration());
	}

	@Test
	public void getInvokerName_ValidName() {
		Map<String, String> map = new HashMap<>();
		map.put("invokername", "AdminUser");
		
		Ban ban = new Ban(map);
		assertEquals("AdminUser", ban.getInvokerName());
	}

	@Test
	public void getInvokerName_EmptyName() {
		Map<String, String> map = new HashMap<>();
		map.put("invokername", "");
		
		Ban ban = new Ban(map);
		assertEquals("", ban.getInvokerName());
	}

	@Test
	public void getInvokerName_NullName() {
		Map<String, String> map = new HashMap<>();
		// invokername key not present

		Ban ban = new Ban(map);
		assertEquals("", ban.getInvokerName());
	}

	@Test
	public void getInvokerClientDBId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("invokercldbid", "100");
		
		Ban ban = new Ban(map);
		assertEquals(100, ban.getInvokerClientDBId());
	}

	@Test
	public void getInvokerClientDBId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		map.put("invokercldbid", "0");
		
		Ban ban = new Ban(map);
		assertEquals(0, ban.getInvokerClientDBId());
	}

	@Test
	public void getInvokerClientDBId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		map.put("invokercldbid", "-1");
		
		Ban ban = new Ban(map);
		assertEquals(-1, ban.getInvokerClientDBId());
	}

	@Test
	public void getInvokerUId_ValidUId() {
		Map<String, String> map = new HashMap<>();
		map.put("invokeruid", "admin123456");
		
		Ban ban = new Ban(map);
		assertEquals("admin123456", ban.getInvokerUId());
	}

	@Test
	public void getInvokerUId_EmptyUId() {
		Map<String, String> map = new HashMap<>();
		map.put("invokeruid", "");
		
		Ban ban = new Ban(map);
		assertEquals("", ban.getInvokerUId());
	}

	@Test
	public void getInvokerUId_NullUId() {
		Map<String, String> map = new HashMap<>();
		// invokeruid key not present

		Ban ban = new Ban(map);
		assertEquals("", ban.getInvokerUId());
	}

	@Test
	public void getReason_ValidReason() {
		Map<String, String> map = new HashMap<>();
		map.put("reason", "Spamming and inappropriate behavior");

		Ban ban = new Ban(map);
		assertEquals("Spamming and inappropriate behavior", ban.getReason());
	}

	@Test
	public void getReason_EmptyReason() {
		Map<String, String> map = new HashMap<>();
		map.put("reason", "");

		Ban ban = new Ban(map);
		assertEquals("", ban.getReason());
	}

	@Test
	public void getReason_NullReason() {
		Map<String, String> map = new HashMap<>();
		// reason key not present

		Ban ban = new Ban(map);
		assertEquals("", ban.getReason());
	}

	@Test
	public void getEnforcements_ValidCount() {
		Map<String, String> map = new HashMap<>();
		map.put("enforcements", "5");
		
		Ban ban = new Ban(map);
		assertEquals(5, ban.getEnforcements());
	}

	@Test
	public void getEnforcements_ZeroCount() {
		Map<String, String> map = new HashMap<>();
		map.put("enforcements", "0");
		
		Ban ban = new Ban(map);
		assertEquals(0, ban.getEnforcements());
	}

	@Test
	public void getEnforcements_NegativeCount() {
		Map<String, String> map = new HashMap<>();
		map.put("enforcements", "-1");
		
		Ban ban = new Ban(map);
		assertEquals(-1, ban.getEnforcements());
	}

	@Test
	public void ban_CompleteData() {
		Map<String, String> map = new HashMap<>();
		map.put("banid", "123");
		map.put("ip", "192.168.1.100");
		map.put("name", "BadUser");
		map.put("uid", "unique123456");
		map.put("lastnickname", "LastSeenName");
		map.put("created", "1609459200");
		map.put("duration", "3600");
		map.put("invokername", "AdminUser");
		map.put("invokercldbid", "100");
		map.put("invokeruid", "admin123456");
		map.put("reason", "Spamming");
		map.put("enforcements", "2");
		
		Ban ban = new Ban(map);
		
		assertEquals(123, ban.getId());
		assertEquals("192.168.1.100", ban.getBannedIp());
		assertEquals("BadUser", ban.getBannedName());
		assertEquals("unique123456", ban.getBannedUId());
		assertEquals("LastSeenName", ban.getLastNickname());
		assertEquals(new Date(1609459200L * 1000), ban.getCreatedDate());
		assertEquals(3600L, ban.getDuration());
		assertEquals("AdminUser", ban.getInvokerName());
		assertEquals(100, ban.getInvokerClientDBId());
		assertEquals("admin123456", ban.getInvokerUId());
		assertEquals("Spamming", ban.getReason());
		assertEquals(2, ban.getEnforcements());
	}

	@Test
	public void ban_EmptyMap() {
		Map<String, String> map = new HashMap<>();

		Ban ban = new Ban(map);

		assertEquals(-1, ban.getId()); // Default int value
		assertEquals("", ban.getBannedIp());
		assertEquals("", ban.getBannedName());
		assertEquals("", ban.getBannedUId());
		assertEquals("", ban.getLastNickname());
		assertEquals(new Date(-1000), ban.getCreatedDate()); // Default long value * 1000
		assertEquals(-1L, ban.getDuration()); // Default long value
		assertEquals("", ban.getInvokerName());
		assertEquals(-1, ban.getInvokerClientDBId()); // Default int value
		assertEquals("", ban.getInvokerUId());
		assertEquals("", ban.getReason());
		assertEquals(-1, ban.getEnforcements()); // Default int value
	}
}
