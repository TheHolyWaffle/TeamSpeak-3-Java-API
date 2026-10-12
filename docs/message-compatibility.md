# Verified online messages and text subscriptions

> Current scope: TS3 RAW and SSH only. TS6 functionality and tests are removed.
> Any TS6 observations below are retained historical findings from merged PRs,
> not current support claims or release acceptance requirements.

This focused portion of #441 verifies TS3 **3.13.8 RAW**, TS3 **3.13.8 SSH** and
TS6 **6.0.0-beta13.1 SSH**, using the exact image pins and readiness checks in
[integration tests](integration-tests.md). The overall
[command inventory](command-compatibility.md) records the remaining scope.

## Supported subset

`ServerQueryCompatibilityIT.messageDomain` uses two authenticated query sessions.
The actor sends messages and the observer subscribes to `TEXT_SERVER`,
`TEXT_CHANNEL` and `TEXT_PRIVATE`. The test verifies:

- `messages().sendServer(String)` and `messages().sendServer(1, String)` on the selected
  fixture virtual server; `messages().sendChannel(String)` and
  `messages().sendChannel(channelId, String)` in a disposable permanent channel;
  `messages().sendPrivate(clientId, String)` to the observer.
- Raw `sendtextmessage` in all three modes. The recipient ID is used for
  private messages, and an otherwise-invalid target ID is ignored for server and
  current-channel messages, as live help specifies. This is why the 2.0 typed API
  has no target ID for server and channel messages (#443).
- Each received event's exact text, target mode, invoker client ID, nickname and
  unique identifier. Only private events provide `target`, which matches the
  observer's client ID. Server/channel events omit that field and
  `getTargetClientId()` returns `-1`.
- UTF-8, BB-code text, pipe/slash characters, literal backslash escape sequences,
  and actual newline/tab/carriage-return characters survive command encoding
  and notification decoding unchanged. BB-code rendering is not tested.
- Private payloads of 1024 and 1026 UTF-8 bytes arrive intact. This is positive
  evidence for those sizes, not a maximum-size or every-role guarantee.
- `events().unregisterAll()` removes server/channel text subscriptions. After
  re-registering private messages, a private event from the same actor fences
  prior unsubscribed server/channel sends on the same ordered listener. The
  test expects only that private event, rather than relying on a sleep to infer
  absence. The private event also proves that re-registration works.
- Invalid client, channel and virtual-server targets return caller-visible
  server errors. Successful messages continue after explicitly restoring a
  cleared server selection. Zero malformed notifications, listener failures
  and dropped events are required.

Live help for `sendtextmessage`, `servernotifyregister` and
`servernotifyunregister`, decoded event fields and error rows are saved in
`target/compatibility/<target>-messages.txt`. The existing Docker CI uploads these
reports even after failure, alongside version/build/image/transport evidence.
Help is the syntax source; actual operations establish the supported subset.

## Observed behavior and errors

All two TS3 targets supply `targetmode`, `msg`, `invokerid`, `invokername` and
`invokeruid`. Private events additionally supply `target`. No new event property
was observed in this slice. Unknown fields remain accessible through `getMap()`;
missing optional fields are not evidence that a default value came from the server.

| Operation | Error ID | Message |
| --- | --- | --- |
| Out-of-range client ID (`Integer.MAX_VALUE`) | 1540 | `convert error` |
| Send to a nonexistent client | 512 | `invalid clientID` |
| Explicit channel send to a nonexistent channel | 768 | `invalid channelID` |
| Explicit server send to unavailable virtual server ID 65535 | 2816 | `virtualserver limit reached` |
| Send with no virtual server selected | 1024 | `invalid serverID` |

The explicit channel overload first moves the actor query and then sends to its
current channel. The test verifies that the query remains in that channel. The
explicit server overload selects the fixture server before sending. These
convenience methods use two commands, and stop if the move/selection fails.
On these fixtures, the failed selection returns 2816 and leaves virtual server ID
zero selected. A subsequent current-server send fails with 1024. The test explicitly
reselects server 1 and restores the actor nickname before sending again; the query
client ID can change after re-selection. Do not interpret error 2816 alone as a
universal nonexistent-server error or assume that failed selection preserves state.
Cross-virtual-server delivery is not established by selecting server 1 again.

## Migration and release intent

This slice changes tests and documentation, with internal **`test:`** release
intent. It introduces no runtime, source/binary API, default or dependency change.
Existing synchronous methods still delegate to their asynchronous counterparts.

The Javadocs incorrectly promised a universal 1024 UTF-8 byte limit; the live
query probes accepted 1026 bytes intact on every target. They now defer size limits
to the connected server. Consumers should not infer portable acceptance or
rejection from the previous statement. The library adds no client-side truncation
or validation. Query-command counts for explicit channel/server overloads are
corrected from one to two; their existing move/selection side effects remain.
The explicit server-send Javadoc now documents that failed selection can clear the
current virtual server. Callers must explicitly select a known server before further
server-scoped operations after that failure. No automatic restoration or retry is added.

## Limitations and sources

The fixtures use serveradmin query sessions. Voice-client receive/rendering,
voice-client join/leave, guest or restricted-account permissions, larger message
limits, rate limiting, other server versions, and cross-server delivery are not
claimed. Broadcast, offline-message CRUD, pokes, server-edit events, privilege-key
notifications and other message/event operations remain within #441 for later
focused PRs. No voice-client Testcontainers guarantee is introduced.

Official sources reviewed alongside live output:
[TS6 beta13.1 release notes](https://github.com/teamspeak/teamspeak6-server/releases/tag/v6.0.0-beta13.1),
[JUnit 6.1.3](https://docs.junit.org/6.1.3/overview.html),
[Maven Failsafe verify usage](https://maven.apache.org/surefire/maven-failsafe-plugin/usage.html).
Release notes do not substitute for the live message assertions. Rerun this
inventory and review the current release documentation when image pins change.
