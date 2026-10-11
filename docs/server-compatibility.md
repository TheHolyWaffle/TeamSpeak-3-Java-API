# Verified server information and edit notifications

This focused portion of #441 verifies TS3 **3.13.8 RAW**, TS3 **3.13.8 SSH** and
TS6 **6.0.0-beta13.1 SSH** using the existing pinned fixtures. See the
[overall inventory](command-compatibility.md) and [integration setup](integration-tests.md).
It does not complete the server domain or the issue.

## Supported subset and evidence

`ServerQueryCompatibilityIT.serverDomain` verifies these operations on each target:

- `getServerInfo()` supplies the selected server's ID, online status, name,
  unique identifier, port, max clients, version and platform.
- `getVirtualServers()` (`serverlist -uid -all`) includes the fixture server,
  agreeing with its detailed info on ID, name, port, unique identifier, status
  and maximum clients. This does not establish stopped/other-server listings.
- `getServerIdByPort()` resolves the fixture's internal voice port. The port is
  read from server info, not assumed to be a host-mapped Docker endpoint.
- `getHostInfo()` supplies running-server/channel counts, uptime and timestamp;
  `getInstanceInfo()` supplies database version and configured file-transfer
  port; `getConnectionInfo()` supplies nonnegative total packet/byte counters.
  Captured remaining fields are an inventory, not validation of every accessor,
  traffic accounting or file-transfer endpoint reachability.
- `editServer()` changes name, welcome message and host message in one command;
  detailed info returns each exact value, and server list reflects the new name.
  UTF-8, pipes, slash characters, literal backslash escapes and actual
  newline/tab/carriage-return characters are preserved in the exercised fields.
- A separate observer query session subscribed to `SERVER` receives the two
  actor-driven name edits, with exact name and invoker ID/name/UID. The test
  asserts optional-field presence as well as accessor values.
- A nonnumeric maximum-client edit fails, leaves the name unchanged and does not
  add a notification before the next valid edit's event fence. Port lookup for
  an unavailable port returns the actual server error, then valid lookup still
  succeeds. No server selection is changed by this lookup error.
- Name/welcome/host-message values are restored through try-with-resources cleanup
  even after a failure, and successful restoration is read back. Zero malformed
  notifications, listener failures and dropped events are required.

Live help for `serverinfo`, `serverlist`, `serveridgetbyport`, `hostinfo`,
`instanceinfo`, `serverrequestconnectioninfo`, `serveredit` and
`servernotifyregister`, complete decoded info maps, edit event fields and errors
are recorded in `target/compatibility/<target>-servers.txt`. Required Docker CI
uploads these reports even after failures alongside version/build/image/transport
reports. Live help establishes syntax; actual operations establish this subset.

## Observed field differences

Server-edit notifications on TS3 contain **`reasonid=10`**; TS6 omits **`reasonid`**,
so `getReasonId()` returns its existing absent-field default **`-1`**. Both servers
include the changed name and invoker fields but omit welcome and host messages
from the bulk edit notification. A notification is a partial payload, not a full
snapshot or a guarantee that every changed property is present. Fetch server info
for current values; use `getMap()` to distinguish missing fields from defaults.

The captured field inventories for these exact pins differ as follows:

| Response | TS3 fields | TS6 fields | Differences |
| --- | --- | --- | --- |
| `serverinfo` | 100 | 109 | TS3-only `virtualserver_file_storage_class`; ten TS6-only fields below |
| `serverlist -uid -all` | 11 | 15 | TS6 adds `guest_can_create_channels`, `guest_can_create_homebase`, `virtualserver_uuid`, `virtualserver_weblist_enabled` |
| `hostinfo` | 18 | 20 | TS6 adds `virtualservers_total_homebases`, `virtualservers_total_max_homebases` |
| `instanceinfo` | 15 | 16 | TS6 adds `serverinstance_max_homebases` |
| `serverrequestconnectioninfo` | 15 | 15 | No field-name difference observed |

TS6-only `serverinfo` fields observed:

```text
virtualserver_address
virtualserver_administrative_domain
virtualserver_canonical_name
virtualserver_homebase_storage_quota
virtualserver_homebases
virtualserver_max_homebases
virtualserver_mytsid_connect_only
virtualserver_sfu_endpoint
virtualserver_storage_quota
virtualserver_version_sign
```

Tests assert representative presence differences in each response, including the
TS3-only storage class and TS6 storage quota/canonical name/UUID/homebase fields.
Counts and untested field values are observations for the pinned fixtures,
not universal schema constants or claims about guest, homebase, file-transfer,
SFU, signing or canonical-name administration. Unknown fields remain available
through `getMap()` and the controlled raw-command escape hatch.

## Errors and recovery

| Operation | Error ID | Message | Observed recovery |
| --- | --- | --- | --- |
| Nonnumeric `virtualserver_maxclients` edit | 1540 | `convert error` | Subsequent valid name edit succeeds; no extra edit event |
| Port lookup for unavailable port 65535 | 1024 | `invalid serverID` | Selected server remains usable and valid port lookup succeeds |

These operations differ from failed **server selection**, which can clear the
current context as documented in [message compatibility](message-compatibility.md).
Read the actual error metadata and do not infer context behavior from a numeric
code alone. Restricted-account permission failures remain unverified here.

## Migration and release intent

This slice has internal **`test:`** release intent for the planned 2.0.0 acceptance
work. No runtime, public API, default or dependency changes are introduced.
`ServerEditedEvent` Javadoc now documents the partial payload and optional reason.
Consumers must not treat absent reason `-1` as a supplied reason, or an omitted
changed property as an empty new value; check presence and fetch server info.
Existing enum-based edit methods, raw access and getter defaults remain unchanged.

## Remaining scope and official sources

Server start/stop/create/delete, snapshots, stopped/multiple-server behavior,
other configuration combinations, instance edits and their permission failures
remain in #441. Official [TS6 beta13.1 release notes](https://github.com/teamspeak/teamspeak6-server/releases/tag/v6.0.0-beta13.1)
fix virtual-server stop behavior, but these read/edit tests do not claim lifecycle
coverage from that release note. This slice uses serveradmin query sessions,
not voice clients or guest/restricted roles; TS6 RAW, Web Query and other versions
are unclaimed. Field presence does not establish transport reachability or feature
administration support. Reliable transfers/NAT endpoints remain #442 scope.

Official [JUnit 6.1.3](https://docs.junit.org/6.1.3/overview.html) and
[Maven Failsafe verify usage](https://maven.apache.org/surefire/maven-failsafe-plugin/usage.html)
were reviewed for test execution. Recheck current release documentation, help and
output when updating image pins.
