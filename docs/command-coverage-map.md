# Protocol command coverage mapping

Scope: TS3 3.13.8 RAW and SSH. This inventory audits production `*Commands` builders,
not every possible argument combination. The evidence column identifies executed
Docker tests and current coverage gaps; listing a command does not imply acceptance.
Typed sync/async facades use these same builders and shared executor. Scripted tests
cover codec, admission, malformed responses, deadlines and reconnect behavior.

| Builder | Protocol commands | Integration evidence and gaps |
| --- | --- | --- |
| BanCommands | `banadd`, `banclient`, `bandel`, `bandelall`, `banlist` | ServerQueryCompatibilityIT.banRuleDomain; `banclient` and client enforcement unverified |
| ChannelCommands | `channelcreate`, `channeldelete`, `channeledit`, `channelfind`, `channelinfo`, `channellist`, `channelmove` | ServerQueryCompatibilityIT.channelDomain/channelConfigurationDomain; ServerModesCompatibilityIT.restartAndDefaultChannels; temporary-channel lifetime unverified |
| ChannelGroupCommands | `channelgroupadd`, `channelgroupclientlist`, `channelgroupcopy`, `channelgroupdel`, `channelgrouplist`, `channelgrouprename`, `setclientchannelgroup` | ServerQueryCompatibilityIT.permissionDomain/groupCopyDomain; PermissionModesCompatibilityIT; regular-client membership commands unverified |
| ClientCommands | `clientedit`, `clientfind`, `clientgetdbidfromuid`, `clientgetids`, `clientinfo`, `clientkick`, `clientlist`, `clientmove`, `clientpoke`, `clientsetserverquerylogin`, `clientupdate`, `sendtextmessage` | ServerQueryCompatibilityIT.clientDomain/messageDomain; regular-client UID lookup, kicks, pokes, password/bulk moves and own-login replacement unverified |
| ComplaintCommands | `complainadd`, `complaindel`, `complaindelall`, `complainlist` | Unverified |
| CustomPropertyCommands | `customdelete`, `custominfo`, `customsearch`, `customset` | Unverified |
| DatabaseClientCommands | `clientdbdelete`, `clientdbedit`, `clientdbfind`, `clientdbinfo`, `clientdblist` | Unverified |
| FileCommands | `ftcreatedir`, `ftdeletefile`, `ftgetfileinfo`, `ftgetfilelist`, `ftinitdownload`, `ftinitupload`, `ftlist`, `ftrenamefile` | ServerQueryCompatibilityIT.fileDomain; FileContentCompatibilityIT.contentsAndTransferCommands |
| MessageCommands | `messageadd`, `messagedel`, `messageget`, `messagelist`, `messageupdateflag` | No live integration evidence; OfflineMessageTest covers scripted body extraction |
| PermissionCommands | `channeladdperm`, `channelclientaddperm`, `channelclientdelperm`, `channelclientpermlist`, `channeldelperm`, `channelgroupaddperm`, `channelgroupdelperm`, `channelgrouppermlist`, `channelpermlist`, `clientaddperm`, `clientdelperm`, `clientpermlist`, `permfind`, `permget`, `permidgetbyname`, `permissionlist`, `permoverview`, `permreset`, `servergroupaddperm`, `servergroupautoaddperm`, `servergroupautodelperm`, `servergroupdelperm`, `servergrouppermlist` | ServerQueryCompatibilityIT.permissionDomain/groupCopyDomain; PermissionModesCompatibilityIT.groupTypesAutomaticAssignmentsAndReset; positive client/channel-client permissions and overview unverified |
| PrivilegeKeyCommands | `privilegekeyadd`, `privilegekeydelete`, `privilegekeylist`, `privilegekeyuse` | ServerQueryCompatibilityIT.privilegeKeyDomain (query use rejected516); successful regular-client consumption and events unverified |
| QueryCommands | `login`, `logout`, `quit`, `servernotifyregister`, `servernotifyunregister`, `use`, `whoami` | ServerQueryCompatibilityIT.smoke/messageDomain/reconnectSession/notificationFrames; SessionCommandsCompatibilityIT.logoutAndRelogin; query.exit() |
| QueryLoginCommands | `queryloginadd`, `querylogindel`, `queryloginlist` | ServerQueryCompatibilityIT.queryLoginDomain; server-bound logins and credential replacement unverified |
| ServerCommands | `bindinglist`, `gm`, `hostinfo`, `instanceedit`, `instanceinfo`, `logview`, `serverprocessstop`, `version` | ServerQueryCompatibilityIT.serverDomain/logDomain; ServerSnapshotCompatibilityIT; ServerModesCompatibilityIT.processStop; SessionCommandsCompatibilityIT.logoutAndRelogin (bindinglist); regular-client broadcast receipt unverified |
| ServerGroupCommands | `servergroupadd`, `servergroupaddclient`, `servergroupclientlist`, `servergroupcopy`, `servergroupdel`, `servergroupdelclient`, `servergrouplist`, `servergrouprename`, `servergroupsbyclientid` | ServerQueryCompatibilityIT.permissionDomain/groupCopyDomain; PermissionModesCompatibilityIT; regular-client membership commands unverified |
| VirtualServerCommands | `servercreate`, `serverdelete`, `serveredit`, `serveridgetbyport`, `serverinfo`, `serverlist`, `serverrequestconnectioninfo`, `serversnapshotcreate`, `serversnapshotdeploy`, `serverstart`, `serverstop` | ServerQueryCompatibilityIT.serverDomain/serverLifecycleDomain; ServerSnapshotCompatibilityIT; ServerModesCompatibilityIT |

The inventory contains 122 distinct command names. Controlled raw commands additionally
exercise `help`, `logadd` and `ftstop` absent from typed builders.
Raw commands retain validation, admission, deadlines, framing and error translation.

Known boundaries: ServerQuery clients cannot consume keys (516); query channel-group
creation is rejected1538 despite help; absent files return2052; query-client
permission lookup returns512; deleted channels768; credential failure is RAW520 or SSH UserAuthException.
Unsupported Web Query, untested server versions and licensed multi-running-server
configurations are not advertised. TS6-specific code and acceptance have been removed.

For tested operations, syntax is captured from live server help in `target/compatibility/`. Exact
version/digest/architecture and test reports accompany CI artifacts. See the
[domain inventory](command-compatibility.md) for behavioral and migration details.
