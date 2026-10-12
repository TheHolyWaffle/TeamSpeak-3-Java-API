# Additional TS3 command coverage

Targets: TS3 3.13.8 RAW and SSH using the digest-pinned disposable server.
Run `./mvnw -B -ntp -Pintegration verify` with Java 25 without preview.
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

## Moderation and enforced bans

`RegularClientCompatibilityIT.movesKicksAndClientBans` uses a full UDP client and
independent query actor/observer. Incorrect channel passwords return exact error
781; SHA-1/base64 passwords permit UDP self-moves, while typed query bulk moves take
plaintext. Channel kicks return reason 4, bans reason 6 and server kicks reason 5,
with exact decoded reasons and actor IDs. Ban rules are listed and bulk-deleted.
A fresh client connection fails while an owned nickname ban exists and another
fresh client connects after its removal. The fixture does not expose a ban-specific
error code for this handshake failure. This does not claim reconnecting the same banned UID/IP fixture.
Fixture command failures expose a typed error ID; callback and socket waits are bounded.

Reports are under `target/compatibility/*-file-contents.txt`, `*-permission-modes.txt`,
`*-server-modes.txt`, `*-process-stop.txt`, and `*-regular-actions.txt`.
Server help/output and assertions define the tested operations; arbitrary property
combinations, licensed concurrency and all possible permissions are not implied.

## Restricted accounts, credentials and temporary channels

`RestrictedClientCompatibilityIT.deniedOperationsAndCredentialRotation` authenticates
an owned server-bound query account for the UDP client's persistent identity.
Server-name edits and virtual-server log reads fail with 2568; `failed_permid` is
resolved through administrator metadata by name, and rejected edits leave state
unchanged. Granting the named own-login permission allows `clientsetserverquerylogin`
to replace credentials. The existing session stays usable; old name and old password fail,
new credentials authenticate, and query-login matching uses SQL `%`, not shell `*`. One-row offset/limit windows recover the
owned login and then an empty next page.

`channelKeyCustomMetadataAndTemporaryLifetime` consumes a channel-group key via the
full UDP client, verifies membership, event identity/type/channel and double-encoded
custom-property text. Consuming the used key again fails with exact error 3840.
It creates a temporary channel, moves the client in/out and observes automatic deletion
from a separately subscribed query session; subsequent `channelinfo` returns 768.
Channel notifications require the CHANNEL subscription, not SERVER alone.

`SessionCommandsCompatibilityIT.logoutAndRelogin` verifies nonempty binding-list IP rows and explicit logout clears
server context and login, then same-stream login restores administrator access on
RAW and SSH. All error branches leave framing usable or produce an authentication-
specific connection failure (RAW 520 / SSH UserAuthException), without logging secrets.
