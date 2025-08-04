package com.github.theholywaffle.teamspeak3.api.wrapper;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConnectionInfoTest {

	@Test
	public void constructor_ValidMap() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_filetransfer_bandwidth_sent", "1024");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertNotNull(connectionInfo);
		assertEquals(1024L, connectionInfo.getFiletransferBandwidthSent());
	}

	@Test
	public void getFiletransferBandwidthSent_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_filetransfer_bandwidth_sent", "2048");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(2048L, connectionInfo.getFiletransferBandwidthSent());
	}

	@Test
	public void getFiletransferBandwidthSent_ZeroValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_filetransfer_bandwidth_sent", "0");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(0L, connectionInfo.getFiletransferBandwidthSent());
	}

	@Test
	public void getFiletransferBandwidthSent_NegativeValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_filetransfer_bandwidth_sent", "-1");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(-1L, connectionInfo.getFiletransferBandwidthSent());
	}

	@Test
	public void getFiletransferBandwidthReceived_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_filetransfer_bandwidth_received", "4096");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(4096L, connectionInfo.getFiletransferBandwidthReceived());
	}

	@Test
	public void getFiletransferBandwidthReceived_ZeroValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_filetransfer_bandwidth_received", "0");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(0L, connectionInfo.getFiletransferBandwidthReceived());
	}

	@Test
	public void getFiletransferBytesSent_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_filetransfer_bytes_sent_total", "1048576"); // 1 MB
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(1048576L, connectionInfo.getFiletransferBytesSent());
	}

	@Test
	public void getFiletransferBytesSent_ZeroValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_filetransfer_bytes_sent_total", "0");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(0L, connectionInfo.getFiletransferBytesSent());
	}

	@Test
	public void getFiletransferBytesReceived_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_filetransfer_bytes_received_total", "2097152"); // 2 MB
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(2097152L, connectionInfo.getFiletransferBytesReceived());
	}

	@Test
	public void getFiletransferBytesReceived_ZeroValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_filetransfer_bytes_received_total", "0");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(0L, connectionInfo.getFiletransferBytesReceived());
	}

	@Test
	public void getTotalPacketsSent_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_packets_sent_total", "10000");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(10000L, connectionInfo.getTotalPacketsSent());
	}

	@Test
	public void getTotalPacketsSent_ZeroValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_packets_sent_total", "0");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(0L, connectionInfo.getTotalPacketsSent());
	}

	@Test
	public void getTotalBytesSent_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_bytes_sent_total", "52428800"); // 50 MB
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(52428800L, connectionInfo.getTotalBytesSent());
	}

	@Test
	public void getTotalBytesSent_ZeroValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_bytes_sent_total", "0");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(0L, connectionInfo.getTotalBytesSent());
	}

	@Test
	public void getTotalPacketsReceived_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_packets_received_total", "8000");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(8000L, connectionInfo.getTotalPacketsReceived());
	}

	@Test
	public void getTotalPacketsReceived_ZeroValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_packets_received_total", "0");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(0L, connectionInfo.getTotalPacketsReceived());
	}

	@Test
	public void getTotalBytesReceived_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_bytes_received_total", "104857600"); // 100 MB
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(104857600L, connectionInfo.getTotalBytesReceived());
	}

	@Test
	public void getTotalBytesReceived_ZeroValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_bytes_received_total", "0");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(0L, connectionInfo.getTotalBytesReceived());
	}

	@Test
	public void getBandwidthSentLastSecond_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_bandwidth_sent_last_second_total", "1024");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(1024L, connectionInfo.getBandwidthSentLastSecond());
	}

	@Test
	public void getBandwidthSentLastSecond_ZeroValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_bandwidth_sent_last_second_total", "0");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(0L, connectionInfo.getBandwidthSentLastSecond());
	}

	@Test
	public void getBandwidthSentLastMinute_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_bandwidth_sent_last_minute_total", "61440"); // 60 KB
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(61440L, connectionInfo.getBandwidthSentLastMinute());
	}

	@Test
	public void getBandwidthSentLastMinute_ZeroValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_bandwidth_sent_last_minute_total", "0");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(0L, connectionInfo.getBandwidthSentLastMinute());
	}

	@Test
	public void getBandwidthReceivedLastSecond_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_bandwidth_received_last_second_total", "2048");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(2048L, connectionInfo.getBandwidthReceivedLastSecond());
	}

	@Test
	public void getBandwidthReceivedLastSecond_ZeroValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_bandwidth_received_last_second_total", "0");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(0L, connectionInfo.getBandwidthReceivedLastSecond());
	}

	@Test
	public void getBandwidthReceivedLastMinute_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_bandwidth_received_last_minute_total", "122880"); // 120 KB
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(122880L, connectionInfo.getBandwidthReceivedLastMinute());
	}

	@Test
	public void getBandwidthReceivedLastMinute_ZeroValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_bandwidth_received_last_minute_total", "0");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(0L, connectionInfo.getBandwidthReceivedLastMinute());
	}

	@Test
	public void getConnectedTime_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_connected_time", "300000"); // 5 minutes in milliseconds
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(300000L, connectionInfo.getConnectedTime());
	}

	@Test
	public void getConnectedTime_ZeroValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_connected_time", "0");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(0L, connectionInfo.getConnectedTime());
	}

	@Test
	public void getPacketLoss_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_packetloss_total", "0.05"); // 5% packet loss
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(0.05, connectionInfo.getPacketLoss(), 0.001);
	}

	@Test
	public void getPacketLoss_ZeroValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_packetloss_total", "0.0");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(0.0, connectionInfo.getPacketLoss(), 0.001);
	}

	@Test
	public void getPacketLoss_HighValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_packetloss_total", "0.25"); // 25% packet loss
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(0.25, connectionInfo.getPacketLoss(), 0.001);
	}

	@Test
	public void getPing_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_ping", "25.5"); // 25.5 ms ping
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(25.5, connectionInfo.getPing(), 0.001);
	}

	@Test
	public void getPing_ZeroValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_ping", "0.0");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(0.0, connectionInfo.getPing(), 0.001);
	}

	@Test
	public void getPing_HighValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_ping", "150.75"); // 150.75 ms ping
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		assertEquals(150.75, connectionInfo.getPing(), 0.001);
	}

	@Test
	public void connectionInfo_CompleteData() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_filetransfer_bandwidth_sent", "2048");
		map.put("connection_filetransfer_bandwidth_received", "4096");
		map.put("connection_filetransfer_bytes_sent_total", "1048576");
		map.put("connection_filetransfer_bytes_received_total", "2097152");
		map.put("connection_packets_sent_total", "10000");
		map.put("connection_bytes_sent_total", "52428800");
		map.put("connection_packets_received_total", "8000");
		map.put("connection_bytes_received_total", "104857600");
		map.put("connection_bandwidth_sent_last_second_total", "1024");
		map.put("connection_bandwidth_sent_last_minute_total", "61440");
		map.put("connection_bandwidth_received_last_second_total", "2048");
		map.put("connection_bandwidth_received_last_minute_total", "122880");
		map.put("connection_connected_time", "300000");
		map.put("connection_packetloss_total", "0.05");
		map.put("connection_ping", "25.5");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		
		assertEquals(2048L, connectionInfo.getFiletransferBandwidthSent());
		assertEquals(4096L, connectionInfo.getFiletransferBandwidthReceived());
		assertEquals(1048576L, connectionInfo.getFiletransferBytesSent());
		assertEquals(2097152L, connectionInfo.getFiletransferBytesReceived());
		assertEquals(10000L, connectionInfo.getTotalPacketsSent());
		assertEquals(52428800L, connectionInfo.getTotalBytesSent());
		assertEquals(8000L, connectionInfo.getTotalPacketsReceived());
		assertEquals(104857600L, connectionInfo.getTotalBytesReceived());
		assertEquals(1024L, connectionInfo.getBandwidthSentLastSecond());
		assertEquals(61440L, connectionInfo.getBandwidthSentLastMinute());
		assertEquals(2048L, connectionInfo.getBandwidthReceivedLastSecond());
		assertEquals(122880L, connectionInfo.getBandwidthReceivedLastMinute());
		assertEquals(300000L, connectionInfo.getConnectedTime());
		assertEquals(0.05, connectionInfo.getPacketLoss(), 0.001);
		assertEquals(25.5, connectionInfo.getPing(), 0.001);
	}

	@Test
	public void connectionInfo_EmptyMap() {
		Map<String, String> map = new HashMap<>();
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		
		assertEquals(-1L, connectionInfo.getFiletransferBandwidthSent()); // Default long value
		assertEquals(-1L, connectionInfo.getFiletransferBandwidthReceived()); // Default long value
		assertEquals(-1L, connectionInfo.getFiletransferBytesSent()); // Default long value
		assertEquals(-1L, connectionInfo.getFiletransferBytesReceived()); // Default long value
		assertEquals(-1L, connectionInfo.getTotalPacketsSent()); // Default long value
		assertEquals(-1L, connectionInfo.getTotalBytesSent()); // Default long value
		assertEquals(-1L, connectionInfo.getTotalPacketsReceived()); // Default long value
		assertEquals(-1L, connectionInfo.getTotalBytesReceived()); // Default long value
		assertEquals(-1L, connectionInfo.getBandwidthSentLastSecond()); // Default long value
		assertEquals(-1L, connectionInfo.getBandwidthSentLastMinute()); // Default long value
		assertEquals(-1L, connectionInfo.getBandwidthReceivedLastSecond()); // Default long value
		assertEquals(-1L, connectionInfo.getBandwidthReceivedLastMinute()); // Default long value
		assertEquals(-1L, connectionInfo.getConnectedTime()); // Default long value
		assertEquals(-1.0, connectionInfo.getPacketLoss(), 0.001); // Default double value
		assertEquals(-1.0, connectionInfo.getPing(), 0.001); // Default double value
	}

	@Test
	public void connectionInfo_InheritedWrapperMethods() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_ping", "25.5");
		map.put("test_field", "test_value");
		
		ConnectionInfo connectionInfo = new ConnectionInfo(map);
		
		// Test inherited methods from Wrapper
		assertEquals("25.5", connectionInfo.get("connection_ping"));
		assertEquals("test_value", connectionInfo.get("test_field"));
		assertEquals(25.5, connectionInfo.getDouble("connection_ping"), 0.001);
	}
}
