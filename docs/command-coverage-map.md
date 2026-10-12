# Protocol command coverage mapping

Scope: TS3 3.13.8 RAW and SSH. This inventory audits production `*Commands` builders,
not every possible argument combination. Each listed family executes operations
in the indicated Docker tests; expected failures are part of the acceptance boundary.
Typed sync/async facades use these same builders and shared executor. Scripted tests
cover codec, admission, malformed responses, deadlines and reconnect behavior.

| Builder | Protocol commands | Executed integration evidence |
| --- | --- | --- |
| BanCommands | `banadd`, `banclient`, `bandel`, `bandelall`, `banlist` | ServerQueryCompatibilityIT.banRuleDomain; RegularClientCompatibilityIT.movesKicksAndClientBans |
| ChannelCommands | `channelcreate`, `channeldelete`, `channeledit`, `channelfind`, `channelinfo`, `channellist`, `channelmove` | ServerQueryCompatibilityIT.channelDomain/channelConfigurationDomain; RestrictedClientCompatibilityIT.channelKeyCustomMetadataAndTemporaryLifetime; ServerModesCompatibilityIT.restartAndDefaultChannels |
| ChannelGroupCommands | `channelgroupadd`, `channelgroupclientlist`, `channelgroupcopy`, `channelgroupdel`, `channelgrouplist`, `channelgrouprename`, `setclientchannelgroup` | ServerQueryCompatibilityIT.permissionDomain/groupCopyDomain; RegularClientCompatibilityIT.persistentClientAdministration; PermissionModesCompatibilityIT; RestrictedClientCompatibilityIT |
| ClientCommands | `clientedit`, `clientfind`, `clientgetdbidfromuid`, `clientgetids`, `clientinfo`, `clientkick`, `clientlist`, `clientmove`, `clientpoke`, `clientsetserverquerylogin`, `clientupdate`, `sendtextmessage` | ServerQueryCompatibilityIT.clientDomain/messageDomain; RegularClientCompatibilityIT; RestrictedClientCompatibilityIT.deniedOperationsAndCredentialRotation |
| ComplaintCommands | `complainadd`, `complaindel`, `complaindelall`, `complainlist` | RegularClientCompatibilityIT.persistentClientAdministration |
| CustomPropertyCommands | `customdelete`, `custominfo`, `customsearch`, `customset` | RegularClientCompatibilityIT.persistentClientAdministration; RestrictedClientCompatibilityIT.channelKeyCustomMetadataAndTemporaryLifetime |
| DatabaseClientCommands | `clientdbdelete`, `clientdbedit`, `clientdbfind`, `clientdbinfo`, `clientdblist` | RegularClientCompatibilityIT.persistentClientAdministration/regularEventsPrivilegeKeysAndOfflineMessages |
| FileCommands | `ftcreatedir`, `ftdeletefile`, `ftgetfileinfo`, `ftgetfilelist`, `ftinitdownload`, `ftinitupload`, `ftlist`, `ftrenamefile` | ServerQueryCompatibilityIT.fileDomain; FileContentCompatibilityIT.contentsAndTransferCommands |
| MessageCommands | `messageadd`, `messagedel`, `messageget`, `messagelist`, `messageupdateflag` | RegularClientCompatibilityIT.regularEventsPrivilegeKeysAndOfflineMessages |
| PermissionCommands | `channeladdperm`, `channelclientaddperm`, `channelclientdelperm`, `channelclientpermlist`, `channeldelperm`, `channelgroupaddperm`, `channelgroupdelperm`, `channelgrouppermlist`, `channelpermlist`, `clientaddperm`, `clientdelperm`, `clientpermlist`, `permfind`, `permget`, `permidgetbyname`, `permissionlist`, `permoverview`, `permreset`, `servergroupaddperm`, `servergroupautoaddperm`, `servergroupautodelperm`, `servergroupdelperm`, `servergrouppermlist` | ServerQueryCompatibilityIT.permissionDomain/groupCopyDomain; RegularClientCompatibilityIT.persistentClientAdministration; PermissionModesCompatibilityIT.groupTypesAutomaticAssignmentsAndReset; RestrictedClientCompatibilityIT.deniedOperationsAndCredentialRotation |
| PrivilegeKeyCommands | `privilegekeyadd`, `privilegekeydelete`, `privilegekeylist`, `privilegekeyuse` | ServerQueryCompatibilityIT.privilegeKeyDomain (query use rejected516); RegularClientCompatibilityIT and RestrictedClientCompatibilityIT (successful full UDP tokenuse alias) |
| QueryCommands | `login`, `logout`, `quit`, `servernotifyregister`, `servernotifyunregister`, `use`, `whoami` | ServerQueryCompatibilityIT.smoke/messageDomain/reconnectSession/notificationFrames; SessionCommandsCompatibilityIT.logoutAndRelogin; query.exit() |
| QueryLoginCommands | `queryloginadd`, `querylogindel`, `queryloginlist` | ServerQueryCompatibilityIT.queryLoginDomain; RegularClientCompatibilityIT.regularEventsPrivilegeKeysAndOfflineMessages; RestrictedClientCompatibilityIT.deniedOperationsAndCredentialRotation |
| ServerCommands | `bindinglist`, `gm`, `hostinfo`, `instanceedit`, `instanceinfo`, `logview`, `serverprocessstop`, `version` | ServerQueryCompatibilityIT.serverDomain/logDomain; RegularClientCompatibilityIT.regularEventsPrivilegeKeysAndOfflineMessages; ServerSnapshotCompatibilityIT; ServerModesCompatibilityIT.processStop; SessionCommandsCompatibilityIT.logoutAndRelogin (bindinglist) |
| ServerGroupCommands | `servergroupadd`, `servergroupaddclient`, `servergroupclientlist`, `servergroupcopy`, `servergroupdel`, `servergroupdelclient`, `servergrouplist`, `servergrouprename`, `servergroupsbyclientid` | ServerQueryCompatibilityIT.permissionDomain/groupCopyDomain; RegularClientCompatibilityIT; PermissionModesCompatibilityIT |
| VirtualServerCommands | `servercreate`, `serverdelete`, `serveredit`, `serveridgetbyport`, `serverinfo`, `serverlist`, `serverrequestconnectioninfo`, `serversnapshotcreate`, `serversnapshotdeploy`, `serverstart`, `serverstop` | ServerQueryCompatibilityIT.serverDomain/serverLifecycleDomain; ServerSnapshotCompatibilityIT; ServerModesCompatibilityIT |

The inventory contains 122 distinct command names. Controlled raw commands additionally
exercise `help`, `logadd`, `ftstop` and custom-key properties absent from typed builders.
Raw commands retain validation, admission, deadlines, framing and error translation.

Known boundaries: ServerQuery clients cannot consume keys (516); query channel-group
creation is rejected1538 despite help; absent files return2052; deleted clients512;
deleted channels768; credential failure is RAW520 or SSH UserAuthException.
Unsupported Web Query, untested server versions and licensed multi-running-server
configurations are not advertised. TS6-specific code and acceptance have been removed.

Source of syntax: captured live server help in `target/compatibility/`. Exact
version/digest/architecture and test reports accompany CI artifacts. See the
[domain inventory](command-compatibility.md) for behavioral and migration details.
