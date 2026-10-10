# Verified permission metadata and group operations

This focused portion of #441 verifies TS3 **3.13.8 RAW**, TS3 **3.13.8 SSH** and
TS6 **6.0.0-beta13.1 SSH** using the pinned fixtures in
[integration tests](integration-tests.md). It does not complete #441.

## Acceptance boundary and evidence

`ServerQueryCompatibilityIT.permissionDomain` executes these operations on every target:

- Enumerate permission names, IDs and descriptions; resolve names individually and
  in a batch; read effective permission values individually and in a batch.
- Create, list, rename and delete ordinary server groups and channel groups,
  including names containing spaces, pipe characters and literal backslash escapes.
- Add, read, update and remove named channel and server-group integer permissions;
  add, read, update and remove named channel-group integer permissions. Verify server-group
  negated/skipped flags and add/read/remove a boolean permission.
- Search assignments by name, identifying the created server group and matching
  its permission ID to the connected server's metadata.
- Reject unknown names with the server's error and successfully execute a subsequent
  command. Look up a TS6-only name on TS6 and reject it on TS3.

The test captures live help for all 21 exercised commands, the complete decoded
permission metadata, selected assignment rows and errors in
`target/compatibility/<target>-permissions.txt`. CI uploads that evidence with
version/build/image/transport reports. Help alone is not an operation guarantee.

The ordinary `permfind` response contains `t`, `id1`, `id2` and `p`, but omits
`v`, `n` and `s` on all three targets. `PermissionAssignment` also represents
`permoverview` responses. Check `getMap()` field presence before interpreting
values or flags; use the corresponding permission-list operation for actual
assignment values. Wrapper defaults do not establish that a property was supplied.
Permission overview itself remains outside this slice's live acceptance boundary.

## Observed server differences and errors

TS3 returns **496** permission names; TS6 returns **510**. All TS3 names occur in
TS6, but **456 shared names have different numeric IDs**. For example:

| Name | TS3 ID | TS6 ID |
| --- | --- | --- |
| `i_channel_needed_join_power` | 140 | 144 |
| `b_channel_create_permanent` | 89 | 92 |

The 14 TS6-only names observed in these pins are:

```text
b_channel_modify_ft_quotas
b_channel_textmessage_delete
b_serverinstance_licensesign_message
b_virtualserver_canonical_name_manage
b_virtualserver_modify_mytsid_connect
i_ft_max_file_size_mb
i_ft_storage_mb_per_client
i_needed_modify_power_channel_modify_ft_quotas
i_needed_modify_power_channel_textmessage_delete
i_needed_modify_power_ft_max_file_size_mb
i_needed_modify_power_ft_storage_mb_per_client
i_needed_modify_power_serverinstance_licensesign_message
i_needed_modify_power_virtualserver_canonical_name_manage
i_needed_modify_power_virtualserver_modify_mytsid_connect
```

These are metadata observations, not verified file, message or administrative
operation support. Names absent from the enums are available through string-based
permission methods. Do not infer permission availability from an enum constant.

An unknown name in `channeladdperm`, `permidgetbyname` or `permfind` produces
`TS3CommandFailedException` with **2562**, **`invalid permission ID`**, on every
target. `getPermissionAssignments` now preserves that error instead of silently
returning an empty list. A valid but unassigned permission's empty-result response
(**1281**) still becomes an empty list through the existing response parser;
this distinction is covered by a scripted regression test.

`PermissionMetadataTest` also verifies that `failed_permid` from an insufficient
permission error (**2568**) can be resolved through server-provided metadata with
arbitrary IDs. That is a scripted protocol check, not a live denied-account claim.
The live fixture uses serveradmin. It does not validate guest permissions,
permission enforcement for every role, or voice-client behavior.

## Migration for 2.0.0

Release intent is **`feat!:`**, an intentional breaking change for the planned
2.0.0 release. `BPermissionType.getId()` and `IPermissionType.getId()` are removed,
including the embedded numeric tables. Enum constants, names and descriptions
remain. Existing mutation overloads that accept enums already send names. String-based
methods use the same names:

```java
api.addChannelPermission(channelId, IPermissionType.I_CHANNEL_NEEDED_JOIN_POWER.getName(), 23);
```

Replace an enum ID lookup with a lookup on the connected server:

```java
// Before: int id = IPermissionType.I_CHANNEL_NEEDED_JOIN_POWER.getId();
int id = api.getPermissionIdByName(IPermissionType.I_CHANNEL_NEEDED_JOIN_POWER.getName());
```

For `PermissionInfo.getId()`, `PermissionAssignment.getId()` and error
`failed_permid`, use `api.getPermissions()` from that same server to resolve names.
Do not reuse numeric IDs across servers or versions. Prefer names for mutations.
For newer permissions, pass a name discovered in that server's metadata:

```java
api.addChannelPermission(channelId, permissionInfo.getName(), value);
```

Handle `TS3CommandFailedException` for unknown names passed to
`getPermissionAssignments`; previously those names were indistinguishable from
valid unassigned permissions. No dependencies or transport defaults change.
The controlled raw-command escape hatch from the channel slice remains available.

## Remaining #441 scope and reviewed sources

Client/channel-client permissions, permission overview, group copy/membership,
automatic groups and permission reset still require live verification. Server,
client, message/event, file-command and administrative domains remain as recorded
in the [overall inventory](command-compatibility.md). Actual voice-client
join/leave needs a client fixture or manual evidence. File-transfer reliability
and endpoint work belong to #442, not this slice.

The current official [TS6 beta13.1 release notes](https://github.com/teamspeak/teamspeak6-server/releases/tag/v6.0.0-beta13.1)
and [server configuration](https://github.com/teamspeak/teamspeak6-server/blob/main/CONFIG.md)
were reviewed alongside actual query help/output. They do not define universal
numeric permission IDs. Test execution follows official
[Maven Failsafe verify usage](https://maven.apache.org/surefire/maven-failsafe-plugin/usage.html);
the existing [JUnit](https://docs.junit.org/6.1.3/overview.html) and
[Testcontainers wait](https://java.testcontainers.org/features/startup_and_waits/)
contracts remain unchanged. Recheck help, release notes and the inventory when pins
change; these results apply to the exact claimed versions and transports.
