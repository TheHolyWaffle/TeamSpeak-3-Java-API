package com.github.theholywaffle.teamspeak3.api.wrapper;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FileInfoTest {

	@Test
	public void constructor_ValidMap() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", "5");
		map.put("name", "/files/document.txt");
		
		FileInfo fileInfo = new FileInfo(map);
		assertNotNull(fileInfo);
		assertEquals(5, fileInfo.getChannelId());
		assertEquals("/files/document.txt", fileInfo.getPath());
	}

	@Test
	public void getChannelId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", "10");
		
		FileInfo fileInfo = new FileInfo(map);
		assertEquals(10, fileInfo.getChannelId());
	}

	@Test
	public void getChannelId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", "0");
		
		FileInfo fileInfo = new FileInfo(map);
		assertEquals(0, fileInfo.getChannelId());
	}

	@Test
	public void getChannelId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", "-1");
		
		FileInfo fileInfo = new FileInfo(map);
		assertEquals(-1, fileInfo.getChannelId());
	}

	@Test
	public void getPath_ValidPath() {
		Map<String, String> map = new HashMap<>();
		map.put("name", "/documents/report.pdf");
		
		FileInfo fileInfo = new FileInfo(map);
		assertEquals("/documents/report.pdf", fileInfo.getPath());
	}

	@Test
	public void getPath_RootPath() {
		Map<String, String> map = new HashMap<>();
		map.put("name", "/");
		
		FileInfo fileInfo = new FileInfo(map);
		assertEquals("/", fileInfo.getPath());
	}

	@Test
	public void getPath_EmptyPath() {
		Map<String, String> map = new HashMap<>();
		map.put("name", "");
		
		FileInfo fileInfo = new FileInfo(map);
		assertEquals("", fileInfo.getPath());
	}

	@Test
	public void getPath_NullPath() {
		Map<String, String> map = new HashMap<>();
		// name key not present
		
		FileInfo fileInfo = new FileInfo(map);
		assertEquals("", fileInfo.getPath());
	}

	@Test
	public void getName_FileWithPath() {
		Map<String, String> map = new HashMap<>();
		map.put("name", "/documents/subfolder/file.txt");
		
		FileInfo fileInfo = new FileInfo(map);
		assertEquals("file.txt", fileInfo.getName());
	}

	@Test
	public void getName_FileInRoot() {
		Map<String, String> map = new HashMap<>();
		map.put("name", "/file.txt");
		
		FileInfo fileInfo = new FileInfo(map);
		assertEquals("file.txt", fileInfo.getName());
	}

	@Test
	public void getName_FileWithoutPath() {
		Map<String, String> map = new HashMap<>();
		map.put("name", "file.txt");
		
		FileInfo fileInfo = new FileInfo(map);
		assertEquals("file.txt", fileInfo.getName());
	}

	@Test
	public void getName_DirectoryWithPath() {
		Map<String, String> map = new HashMap<>();
		map.put("name", "/documents/subfolder/");
		
		FileInfo fileInfo = new FileInfo(map);
		assertEquals("", fileInfo.getName()); // Empty string after last slash
	}

	@Test
	public void getName_EmptyPath() {
		Map<String, String> map = new HashMap<>();
		map.put("name", "");
		
		FileInfo fileInfo = new FileInfo(map);
		assertEquals("", fileInfo.getName());
	}

	@Test
	public void getParentPath_FileWithPath() {
		Map<String, String> map = new HashMap<>();
		map.put("name", "/documents/subfolder/file.txt");
		
		FileInfo fileInfo = new FileInfo(map);
		assertEquals("/documents/subfolder/", fileInfo.getParentPath());
	}

	@Test
	public void getParentPath_FileInRoot() {
		Map<String, String> map = new HashMap<>();
		map.put("name", "/file.txt");
		
		FileInfo fileInfo = new FileInfo(map);
		assertEquals("/", fileInfo.getParentPath());
	}

	@Test
	public void getParentPath_FileWithoutPath() {
		Map<String, String> map = new HashMap<>();
		map.put("name", "file.txt");
		
		FileInfo fileInfo = new FileInfo(map);
		assertEquals("/", fileInfo.getParentPath()); // No slash found, return root
	}

	@Test
	public void getParentPath_RootDirectory() {
		Map<String, String> map = new HashMap<>();
		map.put("name", "/");
		
		FileInfo fileInfo = new FileInfo(map);
		assertEquals("/", fileInfo.getParentPath());
	}

	@Test
	public void getFileSize_ValidSize() {
		Map<String, String> map = new HashMap<>();
		map.put("size", "1048576"); // 1 MB
		
		FileInfo fileInfo = new FileInfo(map);
		assertEquals(1048576L, fileInfo.getFileSize());
	}

	@Test
	public void getFileSize_ZeroSize() {
		Map<String, String> map = new HashMap<>();
		map.put("size", "0");
		
		FileInfo fileInfo = new FileInfo(map);
		assertEquals(0L, fileInfo.getFileSize());
	}

	@Test
	public void getFileSize_LargeSize() {
		Map<String, String> map = new HashMap<>();
		map.put("size", String.valueOf(Long.MAX_VALUE));
		
		FileInfo fileInfo = new FileInfo(map);
		assertEquals(Long.MAX_VALUE, fileInfo.getFileSize());
	}

	@Test
	public void getLastModifiedDate_ValidTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("datetime", "1609459200"); // 2021-01-01 00:00:00 UTC
		
		FileInfo fileInfo = new FileInfo(map);
		Date expectedDate = new Date(1609459200L * 1000);
		assertEquals(expectedDate, fileInfo.getLastModifiedDate());
	}

	@Test
	public void getLastModifiedDate_ZeroTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("datetime", "0");
		
		FileInfo fileInfo = new FileInfo(map);
		Date expectedDate = new Date(0);
		assertEquals(expectedDate, fileInfo.getLastModifiedDate());
	}

	@Test
	public void getLastModifiedDate_NegativeTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("datetime", "-1");
		
		FileInfo fileInfo = new FileInfo(map);
		Date expectedDate = new Date(-1000);
		assertEquals(expectedDate, fileInfo.getLastModifiedDate());
	}

	@Test
	public void getType_AlwaysReturnsOne() {
		Map<String, String> map = new HashMap<>();
		
		FileInfo fileInfo = new FileInfo(map);
		assertEquals(1, fileInfo.getType()); // Always returns 1 for files
	}

	@Test
	public void isFile_AlwaysReturnsTrue() {
		Map<String, String> map = new HashMap<>();
		
		FileInfo fileInfo = new FileInfo(map);
		assertTrue(fileInfo.isFile()); // Always returns true
	}

	@Test
	public void isDirectory_AlwaysReturnsFalse() {
		Map<String, String> map = new HashMap<>();
		
		FileInfo fileInfo = new FileInfo(map);
		assertFalse(fileInfo.isDirectory()); // Always returns false
	}

	@Test
	public void fileInfo_CompleteData() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", "5");
		map.put("name", "/uploads/documents/report.pdf");
		map.put("size", "2097152"); // 2 MB
		map.put("datetime", "1640995200"); // 2022-01-01 00:00:00 UTC
		
		FileInfo fileInfo = new FileInfo(map);
		
		assertEquals(5, fileInfo.getChannelId());
		assertEquals("/uploads/documents/report.pdf", fileInfo.getPath());
		assertEquals("report.pdf", fileInfo.getName());
		assertEquals("/uploads/documents/", fileInfo.getParentPath());
		assertEquals(2097152L, fileInfo.getFileSize());
		assertEquals(new Date(1640995200L * 1000), fileInfo.getLastModifiedDate());
		assertEquals(1, fileInfo.getType());
		assertTrue(fileInfo.isFile());
		assertFalse(fileInfo.isDirectory());
	}

	@Test
	public void fileInfo_EmptyMap() {
		Map<String, String> map = new HashMap<>();
		
		FileInfo fileInfo = new FileInfo(map);
		
		assertEquals(-1, fileInfo.getChannelId()); // Default int value
		assertEquals("", fileInfo.getPath());
		assertEquals("", fileInfo.getName());
		assertEquals("/", fileInfo.getParentPath()); // No slash found, return root
		assertEquals(-1L, fileInfo.getFileSize()); // Default long value
		assertEquals(new Date(-1000), fileInfo.getLastModifiedDate()); // Default long value * 1000
		assertEquals(1, fileInfo.getType()); // Always returns 1
		assertTrue(fileInfo.isFile()); // Always returns true
		assertFalse(fileInfo.isDirectory()); // Always returns false
	}

	@Test
	public void fileInfo_PartialData() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", "3");
		map.put("name", "/image.jpg");
		// Missing size and datetime
		
		FileInfo fileInfo = new FileInfo(map);
		
		assertEquals(3, fileInfo.getChannelId());
		assertEquals("/image.jpg", fileInfo.getPath());
		assertEquals("image.jpg", fileInfo.getName());
		assertEquals("/", fileInfo.getParentPath());
		assertEquals(-1L, fileInfo.getFileSize()); // Default long value
		assertEquals(new Date(-1000), fileInfo.getLastModifiedDate()); // Default long value * 1000
	}

	@Test
	public void fileInfo_AllZeroValues() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", "0");
		map.put("size", "0");
		map.put("datetime", "0");
		
		FileInfo fileInfo = new FileInfo(map);
		
		assertEquals(0, fileInfo.getChannelId());
		assertEquals(0L, fileInfo.getFileSize());
		assertEquals(new Date(0), fileInfo.getLastModifiedDate());
	}

	@Test
	public void fileInfo_AllMaxValues() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", String.valueOf(Integer.MAX_VALUE));
		map.put("size", String.valueOf(Long.MAX_VALUE));
		map.put("datetime", String.valueOf(Long.MAX_VALUE));
		
		FileInfo fileInfo = new FileInfo(map);
		
		assertEquals(Integer.MAX_VALUE, fileInfo.getChannelId());
		assertEquals(Long.MAX_VALUE, fileInfo.getFileSize());
		assertEquals(new Date(Long.MAX_VALUE * 1000), fileInfo.getLastModifiedDate());
	}

	@Test
	public void fileInfo_InheritedWrapperMethods() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", "7");
		map.put("name", "/test.txt");
		map.put("test_field", "test_value");
		
		FileInfo fileInfo = new FileInfo(map);
		
		// Test inherited methods from Wrapper
		assertEquals("7", fileInfo.get("cid"));
		assertEquals("/test.txt", fileInfo.get("name"));
		assertEquals("test_value", fileInfo.get("test_field"));
		assertEquals(7, fileInfo.getInt("cid"));
	}

	@Test
	public void fileInfo_RealWorldScenarios() {
		// Test various real-world file scenarios
		
		// Document file
		Map<String, String> docMap = new HashMap<>();
		docMap.put("cid", "1");
		docMap.put("name", "/shared/documents/meeting_notes.docx");
		docMap.put("size", "524288"); // 512 KB
		docMap.put("datetime", "1672531200"); // 2023-01-01 00:00:00 UTC
		
		FileInfo docFile = new FileInfo(docMap);
		assertEquals(1, docFile.getChannelId());
		assertEquals("/shared/documents/meeting_notes.docx", docFile.getPath());
		assertEquals("meeting_notes.docx", docFile.getName());
		assertEquals("/shared/documents/", docFile.getParentPath());
		assertEquals(524288L, docFile.getFileSize());
		
		// Image file in root
		Map<String, String> imgMap = new HashMap<>();
		imgMap.put("cid", "2");
		imgMap.put("name", "/avatar.png");
		imgMap.put("size", "65536"); // 64 KB
		imgMap.put("datetime", "1672617600"); // 2023-01-02 00:00:00 UTC
		
		FileInfo imgFile = new FileInfo(imgMap);
		assertEquals(2, imgFile.getChannelId());
		assertEquals("/avatar.png", imgFile.getPath());
		assertEquals("avatar.png", imgFile.getName());
		assertEquals("/", imgFile.getParentPath());
		assertEquals(65536L, imgFile.getFileSize());
		
		// File with special characters
		Map<String, String> specialMap = new HashMap<>();
		specialMap.put("cid", "3");
		specialMap.put("name", "/files/special [file] & name.txt");
		specialMap.put("size", "1024");
		specialMap.put("datetime", "1672704000"); // 2023-01-03 00:00:00 UTC
		
		FileInfo specialFile = new FileInfo(specialMap);
		assertEquals(3, specialFile.getChannelId());
		assertEquals("/files/special [file] & name.txt", specialFile.getPath());
		assertEquals("special [file] & name.txt", specialFile.getName());
		assertEquals("/files/", specialFile.getParentPath());
		assertEquals(1024L, specialFile.getFileSize());
	}

	@Test
	public void fileInfo_PathEdgeCases() {
		// Test various path edge cases
		
		// Multiple slashes
		Map<String, String> map1 = new HashMap<>();
		map1.put("name", "//folder//file.txt");
		FileInfo file1 = new FileInfo(map1);
		assertEquals("file.txt", file1.getName());
		assertEquals("//folder//", file1.getParentPath());
		
		// Trailing slash (directory-like)
		Map<String, String> map2 = new HashMap<>();
		map2.put("name", "/folder/subfolder/");
		FileInfo file2 = new FileInfo(map2);
		assertEquals("", file2.getName());
		assertEquals("/folder/subfolder/", file2.getParentPath());
		
		// Single character file
		Map<String, String> map3 = new HashMap<>();
		map3.put("name", "/a");
		FileInfo file3 = new FileInfo(map3);
		assertEquals("a", file3.getName());
		assertEquals("/", file3.getParentPath());
	}
}
