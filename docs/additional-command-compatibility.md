# Additional TS3 command coverage

Targets: TS3 3.13.8 RAW and SSH using the digest-pinned disposable server.
Run `./gradlew build integrationTest` with Java 25 without preview.
All fixtures are headless and reproducible through Maven and Docker.

## File contents and icons

`FileContentCompatibilityIT.contentsAndTransferCommands` captures live transfer-command help, uses
`FileCommands` through the controlled raw API, and verifies Unicode filename
upload, scalar/bulk file metadata and exact downloaded bytes. It verifies active
transfer IDs/client ownership, stop/delete with socket EOF and empty transfer lists.
A valid PNG icon is uploaded, listed by CRC ID, downloaded byte-for-byte and deleted.
Files, directories and channels are owned by a fresh server and cleaned on failure.
Transfer keys and payloads are never printed in reports.

These compatibility tests use bounded test sockets and explicitly map the
advertised transfer port to Docker's assigned host port. They do not establish
public transfer-helper endpoint resolution, cancellation, resume, large-file or
caller-stream ownership behavior. Those production changes remain #442 scope.

## Permission modes and reset

`PermissionModesCompatibilityIT.groupTypesAutomaticAssignmentsAndReset` verifies template, regular and query
server-group copies and template/regular channel-group copies with named permission
assignments. Query channel-group creation returns 1538, despite live help listing
type 2; it is not a supported group mode. Automatic permissions target an owned group with server-defined
classification metadata, preserving unrelated groups. `permreset` restores defaults,
removes owned custom groups and returns a new privilege key. It also clears the
query's selected virtual-server context: select server 1 again before subsequent
server-specific calls. Keys are checked without printing them.

## Server modes and stopping

`ServerModesCompatibilityIT.restartAndDefaultChannels` changes the default channel,
stops and restarts the virtual server, selects by actual port, and verifies the
permanent/default channel survives while a semi-permanent channel disappears.
A second concurrently running server is rejected with 2816 by the bundled license;
licensed multi-running-server operation is not claimed. `processStop` verifies the
typed instance-stop command terminates the owned server/query within five seconds.

Reports are under `build/compatibility/*-file-contents.txt`, `*-permission-modes.txt`,
`*-server-modes.txt` and `*-process-stop.txt`.
Server help/output and assertions define the tested operations; arbitrary property
combinations, licensed concurrency and all possible permissions are not implied.

## Session commands

`SessionCommandsCompatibilityIT.logoutAndRelogin` verifies nonempty binding-list IP rows and explicit logout clears
server context and login, then same-stream login restores administrator access on
RAW and SSH.

## Remaining regular-client coverage

Regular-client moves, kicks, ban enforcement, persistent client administration,
complaints, custom properties, successful privilege-key consumption and events,
offline-message CRUD, poke/broadcast receipt and temporary-channel deletion after
the last regular client leaves are unverified by the automated integration suite.
Server-bound restricted query accounts, own-login credential replacement and denied
operations also lack current live evidence. These require verification with the
official TeamSpeak client where a regular client is needed; query-only tests do not
establish those behaviors.
