package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ServerGroupCommandsTest {

	@Test
	public void serverGroupAdd_ValidName() {
		String expected = "servergroupadd name=TestGroup";
		assertEquals(expected, ServerGroupCommands.serverGroupAdd("TestGroup", null).toString());
	}

	@Test
	public void serverGroupAdd_WithType() {
		String expected = "servergroupadd name=TestGroup type=0";
		assertEquals(expected, ServerGroupCommands.serverGroupAdd("TestGroup", PermissionGroupDatabaseType.TEMPLATE).toString());
	}

	@Test
	public void serverGroupAdd_WithRegularType() {
		String expected = "servergroupadd name=TestGroup type=1";
		assertEquals(expected, ServerGroupCommands.serverGroupAdd("TestGroup", PermissionGroupDatabaseType.REGULAR).toString());
	}

	@Test
	public void serverGroupAdd_WithQueryType() {
		String expected = "servergroupadd name=TestGroup type=2";
		assertEquals(expected, ServerGroupCommands.serverGroupAdd("TestGroup", PermissionGroupDatabaseType.QUERY).toString());
	}

	@Test
	public void serverGroupAdd_NullNameException() {
		assertThrows(IllegalArgumentException.class, () -> ServerGroupCommands.serverGroupAdd(null, null));
	}

	@Test
	public void serverGroupAdd_EmptyNameException() {
		assertThrows(IllegalArgumentException.class, () -> ServerGroupCommands.serverGroupAdd("", null));
	}

	@Test
	public void serverGroupAdd_SpecialCharactersInName() {
		String expected = "servergroupadd name=Test\\sGroup\\s[VIP]";
		assertEquals(expected, ServerGroupCommands.serverGroupAdd("Test Group [VIP]", null).toString());
	}

	@Test
	public void serverGroupAdd_LongGroupName() {
		String longName = "This is a very long server group name that contains multiple words and should be properly encoded";
		String expected = "servergroupadd name=This\\sis\\sa\\svery\\slong\\sserver\\sgroup\\sname\\sthat\\scontains\\smultiple\\swords\\sand\\sshould\\sbe\\sproperly\\sencoded";
		assertEquals(expected, ServerGroupCommands.serverGroupAdd(longName, null).toString());
	}

	@Test
	public void serverGroupAddClient_ValidParameters() {
		String expected = "servergroupaddclient sgid=5 cldbid=100";
		assertEquals(expected, ServerGroupCommands.serverGroupAddClient(5, 100).toString());
	}

	@Test
	public void serverGroupAddClient_ZeroValues() {
		String expected = "servergroupaddclient sgid=0 cldbid=0";
		assertEquals(expected, ServerGroupCommands.serverGroupAddClient(0, 0).toString());
	}

	@Test
	public void serverGroupAddClient_NegativeValues() {
		String expected = "servergroupaddclient sgid=-1 cldbid=-2";
		assertEquals(expected, ServerGroupCommands.serverGroupAddClient(-1, -2).toString());
	}

	@Test
	public void serverGroupAddClient_LargeValues() {
		String expected = "servergroupaddclient sgid=999999 cldbid=888888";
		assertEquals(expected, ServerGroupCommands.serverGroupAddClient(999999, 888888).toString());
	}

	@Test
	public void serverGroupClientList_ValidGroupId() {
		String expected = "servergroupclientlist sgid=5 -names";
		assertEquals(expected, ServerGroupCommands.serverGroupClientList(5).toString());
	}

	@Test
	public void serverGroupClientList_ZeroGroupId() {
		String expected = "servergroupclientlist sgid=0 -names";
		assertEquals(expected, ServerGroupCommands.serverGroupClientList(0).toString());
	}

	@Test
	public void serverGroupClientList_NegativeGroupId() {
		String expected = "servergroupclientlist sgid=-1 -names";
		assertEquals(expected, ServerGroupCommands.serverGroupClientList(-1).toString());
	}

	@Test
	public void serverGroupCopy_SourceToTargetId() {
		String expected = "servergroupcopy ssgid=5 tsgid=10 name=name type=0";
		assertEquals(expected, ServerGroupCommands.serverGroupCopy(5, 10, PermissionGroupDatabaseType.TEMPLATE).toString());
	}

	@Test
	public void serverGroupCopy_SourceToNewGroup() {
		String expected = "servergroupcopy ssgid=5 tsgid=0 name=NewGroup type=1";
		assertEquals(expected, ServerGroupCommands.serverGroupCopy(5, "NewGroup", PermissionGroupDatabaseType.REGULAR).toString());
	}

	@Test
	public void serverGroupCopy_NullTypeException() {
		assertThrows(IllegalArgumentException.class, () -> ServerGroupCommands.serverGroupCopy(5, 10, null));
	}

	@Test
	public void serverGroupCopy_NullNameException() {
		assertThrows(IllegalArgumentException.class, () -> ServerGroupCommands.serverGroupCopy(5, (String) null, PermissionGroupDatabaseType.TEMPLATE));
	}

	@Test
	public void serverGroupCopy_EmptyNameException() {
		assertThrows(IllegalArgumentException.class, () -> ServerGroupCommands.serverGroupCopy(5, "", PermissionGroupDatabaseType.TEMPLATE));
	}

	@Test
	public void serverGroupCopy_SpecialCharactersInName() {
		String expected = "servergroupcopy ssgid=5 tsgid=0 name=New\\sGroup\\s[Admin] type=2";
		assertEquals(expected, ServerGroupCommands.serverGroupCopy(5, "New Group [Admin]", PermissionGroupDatabaseType.QUERY).toString());
	}

	@Test
	public void serverGroupDel_WithoutForce() {
		String expected = "servergroupdel sgid=5 force=0";
		assertEquals(expected, ServerGroupCommands.serverGroupDel(5, false).toString());
	}

	@Test
	public void serverGroupDel_WithForce() {
		String expected = "servergroupdel sgid=10 force=1";
		assertEquals(expected, ServerGroupCommands.serverGroupDel(10, true).toString());
	}

	@Test
	public void serverGroupDel_ZeroGroupId() {
		String expected = "servergroupdel sgid=0 force=0";
		assertEquals(expected, ServerGroupCommands.serverGroupDel(0, false).toString());
	}

	@Test
	public void serverGroupDel_NegativeGroupId() {
		String expected = "servergroupdel sgid=-1 force=1";
		assertEquals(expected, ServerGroupCommands.serverGroupDel(-1, true).toString());
	}

	@Test
	public void serverGroupDelClient_ValidParameters() {
		String expected = "servergroupdelclient sgid=5 cldbid=100";
		assertEquals(expected, ServerGroupCommands.serverGroupDelClient(5, 100).toString());
	}

	@Test
	public void serverGroupDelClient_ZeroValues() {
		String expected = "servergroupdelclient sgid=0 cldbid=0";
		assertEquals(expected, ServerGroupCommands.serverGroupDelClient(0, 0).toString());
	}

	@Test
	public void serverGroupDelClient_NegativeValues() {
		String expected = "servergroupdelclient sgid=-1 cldbid=-2";
		assertEquals(expected, ServerGroupCommands.serverGroupDelClient(-1, -2).toString());
	}

	@Test
	public void serverGroupDelClient_LargeValues() {
		String expected = "servergroupdelclient sgid=999999 cldbid=888888";
		assertEquals(expected, ServerGroupCommands.serverGroupDelClient(999999, 888888).toString());
	}

	@Test
	public void serverGroupList() {
		String expected = "servergrouplist";
		assertEquals(expected, ServerGroupCommands.serverGroupList().toString());
	}

	@Test
	public void serverGroupRename_ValidParameters() {
		String expected = "servergrouprename sgid=5 name=NewName";
		assertEquals(expected, ServerGroupCommands.serverGroupRename(5, "NewName").toString());
	}

	@Test
	public void serverGroupRename_NullNameException() {
		assertThrows(IllegalArgumentException.class, () -> ServerGroupCommands.serverGroupRename(5, null));
	}

	@Test
	public void serverGroupRename_EmptyNameException() {
		assertThrows(IllegalArgumentException.class, () -> ServerGroupCommands.serverGroupRename(5, ""));
	}

	@Test
	public void serverGroupRename_SpecialCharactersInName() {
		String expected = "servergrouprename sgid=5 name=Admin\\sGroup\\s[VIP]";
		assertEquals(expected, ServerGroupCommands.serverGroupRename(5, "Admin Group [VIP]").toString());
	}

	@Test
	public void serverGroupRename_ZeroGroupId() {
		String expected = "servergrouprename sgid=0 name=TestName";
		assertEquals(expected, ServerGroupCommands.serverGroupRename(0, "TestName").toString());
	}

	@Test
	public void serverGroupRename_NegativeGroupId() {
		String expected = "servergrouprename sgid=-1 name=TestName";
		assertEquals(expected, ServerGroupCommands.serverGroupRename(-1, "TestName").toString());
	}

	@Test
	public void serverGroupsByClientId_ValidClientDBId() {
		String expected = "servergroupsbyclientid cldbid=100";
		assertEquals(expected, ServerGroupCommands.serverGroupsByClientId(100).toString());
	}

	@Test
	public void serverGroupsByClientId_ZeroClientDBId() {
		String expected = "servergroupsbyclientid cldbid=0";
		assertEquals(expected, ServerGroupCommands.serverGroupsByClientId(0).toString());
	}

	@Test
	public void serverGroupsByClientId_NegativeClientDBId() {
		String expected = "servergroupsbyclientid cldbid=-1";
		assertEquals(expected, ServerGroupCommands.serverGroupsByClientId(-1).toString());
	}

	@Test
	public void serverGroupsByClientId_LargeClientDBId() {
		String expected = "servergroupsbyclientid cldbid=999999";
		assertEquals(expected, ServerGroupCommands.serverGroupsByClientId(999999).toString());
	}

	@Test
	public void serverGroupsByClientId_MaxIntClientDBId() {
		String expected = "servergroupsbyclientid cldbid=" + Integer.MAX_VALUE;
		assertEquals(expected, ServerGroupCommands.serverGroupsByClientId(Integer.MAX_VALUE).toString());
	}

	@Test
	public void serverGroupAddClient_MaxIntValues() {
		String expected = "servergroupaddclient sgid=" + Integer.MAX_VALUE + " cldbid=" + (Integer.MAX_VALUE - 1);
		assertEquals(expected, ServerGroupCommands.serverGroupAddClient(Integer.MAX_VALUE, Integer.MAX_VALUE - 1).toString());
	}

	@Test
	public void serverGroupDelClient_MaxIntValues() {
		String expected = "servergroupdelclient sgid=" + Integer.MAX_VALUE + " cldbid=" + (Integer.MAX_VALUE - 1);
		assertEquals(expected, ServerGroupCommands.serverGroupDelClient(Integer.MAX_VALUE, Integer.MAX_VALUE - 1).toString());
	}

	@Test
	public void serverGroupClientList_MaxIntGroupId() {
		String expected = "servergroupclientlist sgid=" + Integer.MAX_VALUE + " -names";
		assertEquals(expected, ServerGroupCommands.serverGroupClientList(Integer.MAX_VALUE).toString());
	}

	@Test
	public void serverGroupDel_MaxIntGroupId() {
		String expected = "servergroupdel sgid=" + Integer.MAX_VALUE + " force=1";
		assertEquals(expected, ServerGroupCommands.serverGroupDel(Integer.MAX_VALUE, true).toString());
	}

	@Test
	public void serverGroupRename_MaxIntGroupId() {
		String expected = "servergrouprename sgid=" + Integer.MAX_VALUE + " name=TestName";
		assertEquals(expected, ServerGroupCommands.serverGroupRename(Integer.MAX_VALUE, "TestName").toString());
	}

	@Test
	public void serverGroupAdd_NameWithQuotes() {
		String expected = "servergroupadd name=Group\\swith\\s\"quotes\"";
		assertEquals(expected, ServerGroupCommands.serverGroupAdd("Group with \"quotes\"", null).toString());
	}

	@Test
	public void serverGroupAdd_NameWithAmpersands() {
		String expected = "servergroupadd name=Group\\s&\\sName";
		assertEquals(expected, ServerGroupCommands.serverGroupAdd("Group & Name", null).toString());
	}

	@Test
	public void serverGroupCopy_NameWithForwardSlashes() {
		String expected = "servergroupcopy ssgid=5 tsgid=0 name=Group\\/Name type=0";
		assertEquals(expected, ServerGroupCommands.serverGroupCopy(5, "Group/Name", PermissionGroupDatabaseType.TEMPLATE).toString());
	}

	@Test
	public void serverGroupRename_NameWithPipes() {
		String expected = "servergrouprename sgid=5 name=Group\\pName";
		assertEquals(expected, ServerGroupCommands.serverGroupRename(5, "Group|Name").toString());
	}
}
