package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ChannelGroupCommandsTest {

	@Test
	public void channelGroupAdd_ValidName() {
		String expected = "channelgroupadd name=TestGroup";
		assertEquals(expected, ChannelGroupCommands.channelGroupAdd("TestGroup", null).toString());
	}

	@Test
	public void channelGroupAdd_WithType() {
		String expected = "channelgroupadd name=TestGroup type=0";
		assertEquals(expected, ChannelGroupCommands.channelGroupAdd("TestGroup", PermissionGroupDatabaseType.TEMPLATE).toString());
	}

	@Test
	public void channelGroupAdd_NullNameException() {
		assertThrows(IllegalArgumentException.class, () -> ChannelGroupCommands.channelGroupAdd(null, null));
	}

	@Test
	public void channelGroupAdd_EmptyNameException() {
		assertThrows(IllegalArgumentException.class, () -> ChannelGroupCommands.channelGroupAdd("", null));
	}

	@Test
	public void channelGroupAdd_SpecialCharactersInName() {
		String expected = "channelgroupadd name=Test\\sGroup\\s[VIP]";
		assertEquals(expected, ChannelGroupCommands.channelGroupAdd("Test Group [VIP]", null).toString());
	}

	@Test
	public void channelGroupClientList_AllParameters() {
		String expected = "channelgroupclientlist cid=5 cldbid=100 cgid=10";
		assertEquals(expected, ChannelGroupCommands.channelGroupClientList(5, 100, 10).toString());
	}

	@Test
	public void channelGroupClientList_ZeroChannelId() {
		// Zero channel ID should not be included
		String expected = "channelgroupclientlist cldbid=100 cgid=10";
		assertEquals(expected, ChannelGroupCommands.channelGroupClientList(0, 100, 10).toString());
	}

	@Test
	public void channelGroupClientList_ZeroClientDBId() {
		// Zero client DB ID should not be included
		String expected = "channelgroupclientlist cid=5 cgid=10";
		assertEquals(expected, ChannelGroupCommands.channelGroupClientList(5, 0, 10).toString());
	}

	@Test
	public void channelGroupClientList_ZeroGroupId() {
		// Zero group ID should not be included
		String expected = "channelgroupclientlist cid=5 cldbid=100";
		assertEquals(expected, ChannelGroupCommands.channelGroupClientList(5, 100, 0).toString());
	}

	@Test
	public void channelGroupClientList_AllZeros() {
		// All zeros should result in empty parameters
		String expected = "channelgroupclientlist";
		assertEquals(expected, ChannelGroupCommands.channelGroupClientList(0, 0, 0).toString());
	}

	@Test
	public void channelGroupClientList_NegativeValues() {
		// Negative values should not be included
		String expected = "channelgroupclientlist";
		assertEquals(expected, ChannelGroupCommands.channelGroupClientList(-1, -1, -1).toString());
	}

	@Test
	public void channelGroupCopy_SourceToTargetId() {
		String expected = "channelgroupcopy scgid=5 tcgid=10 name=name type=0";
		assertEquals(expected, ChannelGroupCommands.channelGroupCopy(5, 10, PermissionGroupDatabaseType.TEMPLATE).toString());
	}

	@Test
	public void channelGroupCopy_SourceToNewGroup() {
		String expected = "channelgroupcopy scgid=5 tcgid=0 name=NewGroup type=0";
		assertEquals(expected, ChannelGroupCommands.channelGroupCopy(5, "NewGroup", PermissionGroupDatabaseType.TEMPLATE).toString());
	}

	@Test
	public void channelGroupCopy_NullTypeException() {
		assertThrows(IllegalArgumentException.class, () -> ChannelGroupCommands.channelGroupCopy(5, 10, null));
	}

	@Test
	public void channelGroupCopy_NullNameException() {
		assertThrows(IllegalArgumentException.class, () -> ChannelGroupCommands.channelGroupCopy(5, (String) null, PermissionGroupDatabaseType.TEMPLATE));
	}

	@Test
	public void channelGroupCopy_EmptyNameException() {
		assertThrows(IllegalArgumentException.class, () -> ChannelGroupCommands.channelGroupCopy(5, "", PermissionGroupDatabaseType.TEMPLATE));
	}

	@Test
	public void channelGroupCopy_SpecialCharactersInName() {
		String expected = "channelgroupcopy scgid=5 tcgid=0 name=New\\sGroup\\s[Admin] type=0";
		assertEquals(expected, ChannelGroupCommands.channelGroupCopy(5, "New Group [Admin]", PermissionGroupDatabaseType.TEMPLATE).toString());
	}

	@Test
	public void channelGroupDel_WithoutForce() {
		String expected = "channelgroupdel cgid=5 force=0";
		assertEquals(expected, ChannelGroupCommands.channelGroupDel(5, false).toString());
	}

	@Test
	public void channelGroupDel_WithForce() {
		String expected = "channelgroupdel cgid=10 force=1";
		assertEquals(expected, ChannelGroupCommands.channelGroupDel(10, true).toString());
	}

	@Test
	public void channelGroupDel_ZeroGroupId() {
		String expected = "channelgroupdel cgid=0 force=0";
		assertEquals(expected, ChannelGroupCommands.channelGroupDel(0, false).toString());
	}

	@Test
	public void channelGroupDel_NegativeGroupId() {
		String expected = "channelgroupdel cgid=-1 force=1";
		assertEquals(expected, ChannelGroupCommands.channelGroupDel(-1, true).toString());
	}

	@Test
	public void channelGroupList() {
		String expected = "channelgrouplist";
		assertEquals(expected, ChannelGroupCommands.channelGroupList().toString());
	}

	@Test
	public void channelGroupRename_ValidParameters() {
		String expected = "channelgrouprename cgid=5 name=NewName";
		assertEquals(expected, ChannelGroupCommands.channelGroupRename(5, "NewName").toString());
	}

	@Test
	public void channelGroupRename_NullNameException() {
		assertThrows(IllegalArgumentException.class, () -> ChannelGroupCommands.channelGroupRename(5, null));
	}

	@Test
	public void channelGroupRename_EmptyNameException() {
		assertThrows(IllegalArgumentException.class, () -> ChannelGroupCommands.channelGroupRename(5, ""));
	}

	@Test
	public void channelGroupRename_SpecialCharactersInName() {
		String expected = "channelgrouprename cgid=5 name=Admin\\sGroup\\s[VIP]";
		assertEquals(expected, ChannelGroupCommands.channelGroupRename(5, "Admin Group [VIP]").toString());
	}

	@Test
	public void channelGroupRename_ZeroGroupId() {
		String expected = "channelgrouprename cgid=0 name=TestName";
		assertEquals(expected, ChannelGroupCommands.channelGroupRename(0, "TestName").toString());
	}

	@Test
	public void channelGroupRename_NegativeGroupId() {
		String expected = "channelgrouprename cgid=-1 name=TestName";
		assertEquals(expected, ChannelGroupCommands.channelGroupRename(-1, "TestName").toString());
	}

	@Test
	public void setClientChannelGroup_ValidParameters() {
		String expected = "setclientchannelgroup cgid=5 cid=10 cldbid=100";
		assertEquals(expected, ChannelGroupCommands.setClientChannelGroup(5, 10, 100).toString());
	}

	@Test
	public void setClientChannelGroup_ZeroValues() {
		String expected = "setclientchannelgroup cgid=0 cid=0 cldbid=0";
		assertEquals(expected, ChannelGroupCommands.setClientChannelGroup(0, 0, 0).toString());
	}

	@Test
	public void setClientChannelGroup_NegativeValues() {
		String expected = "setclientchannelgroup cgid=-1 cid=-1 cldbid=-1";
		assertEquals(expected, ChannelGroupCommands.setClientChannelGroup(-1, -1, -1).toString());
	}

	@Test
	public void setClientChannelGroup_LargeValues() {
		String expected = "setclientchannelgroup cgid=999999 cid=888888 cldbid=777777";
		assertEquals(expected, ChannelGroupCommands.setClientChannelGroup(999999, 888888, 777777).toString());
	}

	@Test
	public void channelGroupAdd_LongGroupName() {
		String longName = "This is a very long channel group name that contains multiple words and should be properly encoded";
		String expected = "channelgroupadd name=This\\sis\\sa\\svery\\slong\\schannel\\sgroup\\sname\\sthat\\scontains\\smultiple\\swords\\sand\\sshould\\sbe\\sproperly\\sencoded";
		assertEquals(expected, ChannelGroupCommands.channelGroupAdd(longName, null).toString());
	}

	@Test
	public void channelGroupCopy_DifferentTypes() {
		// Test with REGULAR type
		String expected = "channelgroupcopy scgid=5 tcgid=10 name=name type=1";
		assertEquals(expected, ChannelGroupCommands.channelGroupCopy(5, 10, PermissionGroupDatabaseType.REGULAR).toString());
	}

	@Test
	public void channelGroupClientList_SingleParameter() {
		// Test with only channel ID
		String expected = "channelgroupclientlist cid=5";
		assertEquals(expected, ChannelGroupCommands.channelGroupClientList(5, 0, 0).toString());
		
		// Test with only client DB ID
		expected = "channelgroupclientlist cldbid=100";
		assertEquals(expected, ChannelGroupCommands.channelGroupClientList(0, 100, 0).toString());
		
		// Test with only group ID
		expected = "channelgroupclientlist cgid=10";
		assertEquals(expected, ChannelGroupCommands.channelGroupClientList(0, 0, 10).toString());
	}
}
