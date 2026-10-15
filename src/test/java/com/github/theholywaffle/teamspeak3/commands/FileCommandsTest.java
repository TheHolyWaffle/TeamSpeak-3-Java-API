package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.FileLocation;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FileCommandsTest {

    private static FileLocation inChannel1(String path) {
        return FileLocation.of(ChannelId.of(1), path);
    }

    @Test
    public void fileLocation_InvalidPathException() {
        Assertions.assertThrows(NullPointerException.class, () -> inChannel1(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> inChannel1(""));
        Assertions.assertThrows(NullPointerException.class, () -> FileLocation.ofServer(null));
        Assertions.assertThrows(
                IllegalArgumentException.class, () -> inChannel1("/a").withPath(""));
    }

    @Test
    public void ftCreateDir() {
        final String expected = "ftcreatedir cid=1 cpw= dirname=\\/dir1";
        Assertions.assertEquals(
                expected, FileCommands.ftCreateDir(inChannel1("/dir1")).encoded());
        Assertions.assertEquals(
                expected, FileCommands.ftCreateDir(inChannel1("dir1")).encoded());
    }

    @Test
    public void ftDeleteFile_EmptyListException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileCommands.ftDeleteFile(List.of()));
    }

    @Test
    public void ftDeleteFile_MixedChannelsException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> FileCommands.ftDeleteFile(List.of(inChannel1("/a"), FileLocation.ofServer("/b"))));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> FileCommands.ftDeleteFile(
                        List.of(inChannel1("/a"), inChannel1("/b").withChannelPassword("x"))));
    }

    @Test
    public void ftDeleteFile() {
        final String expected = "ftdeletefile cid=1 cpw= name=\\/dir1|name=\\/dir2";
        Assertions.assertEquals(
                expected,
                FileCommands.ftDeleteFile(List.of(inChannel1("dir1"), inChannel1("/dir2")))
                        .encoded());
        Assertions.assertEquals(
                "ftdeletefile cid=0 cpw= name=\\/icon_5",
                FileCommands.ftDeleteFile(List.of(FileLocation.ofServer("/icon_5")))
                        .encoded());
    }

    @Test
    public void ftGetFileInfo_EmptyListException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileCommands.ftGetFileInfo(List.of()));
    }

    @Test
    public void ftGetFileInfo() {
        final String expected = "ftgetfileinfo cid=1 cpw= name=\\/dir1|cid=2 cpw=pass2 name=\\/dir2";
        Assertions.assertEquals(
                expected,
                FileCommands.ftGetFileInfo(List.of(
                                inChannel1("dir1"),
                                FileLocation.of(ChannelId.of(2), "/dir2").withChannelPassword("pass2")))
                        .encoded());
    }

    @Test
    public void ftGetFileList() {
        final String expected = "ftgetfilelist cid=1 cpw= path=\\/dir1\\/";
        Assertions.assertEquals(
                expected, FileCommands.ftGetFileList(inChannel1("/dir1")).encoded());
        Assertions.assertEquals(
                expected, FileCommands.ftGetFileList(inChannel1("dir1/")).encoded());
    }

    @Test
    public void ftInitDownload() {
        final String expected = "ftinitdownload clientftfid=1 name=\\/dir1 cid=1 cpw= seekpos=0 proto=0";
        Assertions.assertEquals(
                expected, FileCommands.ftInitDownload(1, inChannel1("/dir1"), 0).encoded());
        Assertions.assertEquals(
                expected, FileCommands.ftInitDownload(1, inChannel1("dir1"), 0).encoded());
    }

    @Test
    public void ftInitUpload() {
        final String expected =
                "ftinitupload clientftfid=1 name=\\/dir1 cid=1 cpw= size=1024 overwrite=1 resume=0 proto=0";
        Assertions.assertEquals(
                expected,
                FileCommands.ftInitUpload(1, inChannel1("/dir1"), 1024, true, false)
                        .encoded());
        Assertions.assertEquals(
                expected,
                FileCommands.ftInitUpload(1, inChannel1("dir1"), 1024, true, false)
                        .encoded());
    }

    @Test
    public void ftList() {
        Assertions.assertEquals("ftlist", FileCommands.ftList().encoded());
    }

    @Test
    public void ftRenameFile() {
        final String expected = "ftrenamefile cid=1 cpw= oldname=\\/dir1 newname=\\/dir2";
        Assertions.assertEquals(
                expected,
                FileCommands.ftRenameFile(inChannel1("/dir1"), inChannel1("dir2"))
                        .encoded());
        Assertions.assertEquals(
                expected,
                FileCommands.ftRenameFile(inChannel1("dir1"), inChannel1("/dir2"))
                        .encoded());
    }

    @Test
    public void ftRenameFileAndChannel() {
        final String expected = "ftrenamefile cid=1 cpw= tcid=2 tcpw= oldname=\\/dir1 newname=\\/dir2";
        Assertions.assertEquals(
                expected,
                FileCommands.ftRenameFile(inChannel1("/dir1"), FileLocation.of(ChannelId.of(2), "dir2"))
                        .encoded());
        Assertions.assertEquals(
                "ftrenamefile cid=1 cpw= tcid=0 tcpw= oldname=\\/dir1 newname=\\/dir2",
                FileCommands.ftRenameFile(inChannel1("/dir1"), FileLocation.ofServer("/dir2"))
                        .encoded());
    }
}
