package com.github.theholywaffle.teamspeak3.commands;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FileCommandsTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  
  @Test
  public void ftCreateDir_NullPathException() {
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftCreateDir(null, 1, null);
  }

  @Test
  public void ftCreateDir() {
    final String expected = "ftcreatedir cid=1 cpw= dirname=\\/dir1";
    Assert.assertEquals(expected, FileCommands.ftCreateDir("/dir1", 1, null).toString());
    Assert.assertEquals(expected, FileCommands.ftCreateDir("dir1", 1, null).toString());
  }

  @Test
  public void ftDeleteFile_EmptyFileArrayException() {
    final String[] filePaths = {};
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftDeleteFile(1, null, filePaths);
  }

  @Test
  public void ftDeleteFile_NullFileArrayException() {
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftDeleteFile(1, null, null);
  }

  @Test
  public void ftDeleteFile_NullFilePathException() {
    final String[] filePaths = {null};
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftDeleteFile(1, null, filePaths);
  }

  @Test
  public void ftDeleteFile() {
    final String[] filePaths = {"dir1", "/dir2"};
    final String expected = "ftdeletefile cid=1 cpw= name=\\/dir1|name=\\/dir2";
    Assert.assertEquals(expected, FileCommands.ftDeleteFile(1, null, filePaths).toString());
  }

  @Test
  public void ftGetFileInfo_EmptyFileArrayException() {
    final String[] filePaths = {};
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftGetFileInfo(1, null, filePaths);
  }

  @Test
  public void ftGetFileInfo_NullFileArrayException() {
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftGetFileInfo(1, null, null);
  }

  @Test
  public void ftGetFileInfo_NullFilePathException() {
    final String[] filePaths = {null};
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftGetFileInfo(1, null, filePaths);
  }

  @Test
  public void ftGetFileInfo() {
    final String[] filePaths = {"dir1", "/dir2"};
    final String expected = "ftgetfileinfo cid=1 cpw= name=dir1|cid=1 cpw= name=\\/dir2";
    Assert.assertEquals(expected, FileCommands.ftGetFileInfo(1, null, filePaths).toString());
  }

  @Test
  public void ftGetFileInfoArray_EmptyChannelIDArrayException() {
    final int[] channelIds = {};
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftGetFileInfo(channelIds, null, null);
  }

  @Test
  public void ftGetFileInfoArray_NullChannelIDArrayException() {
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftGetFileInfo(null, null, null);
  }

  @Test
  public void ftGetFileInfoArray_EmptyFileArrayException() {
    final int[] channelIds = {1};
    final String[] filePaths = {};
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftGetFileInfo(channelIds, null, filePaths);
  }

  @Test
  public void ftGetFileInfoArray_NullFileArrayException() {
    final int[] channelIds = {1};
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftGetFileInfo(channelIds, null, null);
  }

  @Test
  public void ftGetFileInfoArray_ChannelIDsLengthFilePathsLengthException() {
    final int[] channelIds = {1};
    final String[] filePaths = {"dir1", "dir2"};
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftGetFileInfo(channelIds, null, filePaths);
  }

  @Test
  public void ftGetFileInfoArray_PasswordsLengthFilePathsLengthException() {
    final int[] channelIds = {1, 2};
    final String[] channelPasswords = {null};
    final String[] filePaths = {"dir1", "dir2"};
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftGetFileInfo(channelIds, channelPasswords, filePaths);
  }

  @Test
  public void ftGetFileInfoArray_NullFilePathException() {
    final int[] channelIds = {1};
    final String[] filePaths = {null};
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftGetFileInfo(channelIds, null, filePaths);
  }

  @Test
  public void ftGetFileInfoArray() {
    final int[] channelIds = {1, 2};
    final String[] channelPasswords = {null, "pass2"};
    final String[] filePaths = {"dir1", "/dir2"};
    final String expected = "ftgetfileinfo cid=1 cpw= name=\\/dir1|cid=2 cpw=pass2 name=\\/dir2";
    Assert.assertEquals(expected, FileCommands.ftGetFileInfo(channelIds, channelPasswords, filePaths).toString());
  }

  @Test
  public void ftGetFileList_NullDirectoryPathException() {
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftGetFileList(null, 1, null);
  }

  @Test
  public void ftGetFileList() {
    final String expected = "ftgetfilelist cid=1 cpw= path=\\/dir1\\/";
    Assert.assertEquals(expected, FileCommands.ftGetFileList("/dir1", 1, null).toString());
    Assert.assertEquals(expected, FileCommands.ftGetFileList("dir1/", 1, null).toString());
  }

  @Test
  public void ftInitDownload_NullDirectoryPathException() {
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftInitDownload(1, null, 1, null);
  }

  @Test
  public void ftInitDownload() {
    final String expected = "ftinitdownload clientftfid=1 name=\\/dir1 cid=1 cpw= seekpos=0 proto=0";
    Assert.assertEquals(expected, FileCommands.ftInitDownload(1, "/dir1", 1, null).toString());
    Assert.assertEquals(expected, FileCommands.ftInitDownload(1, "dir1", 1, null).toString());
  }

  @Test
  public void ftInitUpload_NullDirectoryPathException() {
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftInitUpload(1, null, 1, null, 1024, true);
  }

  @Test
  public void ftInitUpload() {
    final String expected = "ftinitupload clientftfid=1 name=\\/dir1 cid=1 cpw= size=1024 overwrite=1 resume=0 proto=0";
    Assert.assertEquals(expected, FileCommands.ftInitUpload(1, "/dir1", 1, null, 1024, true).toString());
    Assert.assertEquals(expected, FileCommands.ftInitUpload(1, "dir1", 1, null, 1024, true).toString());
  }

  @Test
  public void ftList() {
    final String expected = "ftlist";
    Assert.assertEquals(expected, FileCommands.ftList().toString());
  }

  @Test
  public void ftRenameFile_NullOldPathException() {
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftRenameFile(null, "dir2", 1, null);
  }

  @Test
  public void ftRenameFile_NullNewPathException() {
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftRenameFile("dir1", null, 1, null);
  }

  @Test
  public void ftRenameFile() {
    final String expected = "ftrenamefile cid=1 cpw= oldname=\\/dir1 newname=\\/dir2";
    Assert.assertEquals(expected, FileCommands.ftRenameFile("/dir1", "dir2", 1, null).toString());
    Assert.assertEquals(expected, FileCommands.ftRenameFile("dir1", "/dir2", 1, null).toString());
  }

  @Test
  public void ftRenameFileAndChannel_NullOldPathException() {
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftRenameFile(null, "dir2", 1, null, 2, null);
  }

  @Test
  public void ftRenameFileAndChannel_NullNewPathException() {
    thrown.expect(IllegalArgumentException.class);
    FileCommands.ftRenameFile("dir1", null, 1, null, 2, null);
  }

  @Test
  public void ftRenameFileAndChannel() {
    final String expected = "ftrenamefile cid=1 cpw= tcid=2 tcpw= oldname=\\/dir1 newname=\\/dir2";
    Assert.assertEquals(expected, FileCommands.ftRenameFile("/dir1", "dir2", 1, null, 2, null).toString());
    Assert.assertEquals(expected, FileCommands.ftRenameFile("dir1", "/dir2", 1, null, 2, null).toString());
  }
}
