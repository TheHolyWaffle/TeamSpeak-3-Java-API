# Reproducible regular-client compatibility

> Current scope: TS3 RAW and SSH only. TS6 functionality and tests are removed.
> Any TS6 observations below are retained historical findings from merged PRs,
> not current support claims or release acceptance requirements.

Run `./mvnw -B -ntp -Pintegration verify` with Java 25 and Docker. Maven builds
the isolated [full UDP client fixture](../src/test/fixtures/regular-client/README.md),
then each test owns a fresh server, private Docker network and client container.
All ports are dynamic; identities are generated in memory. There are no desktop
automation, installed-client, microphone, personal-account or manual prerequisites.
Fixture dependency resolution needs Maven Central and JitPack network access.

`RegularClientCompatibilityIT` runs both cases against TS3 3.13.8 RAW and TS3 3.13.8
SSH. The UDP fixture supplies the regular client;
ServerQuery observers and administrators use this library over the stated transport.
TS6 RAW, Web Query, audio and official desktop-client feature parity are not claimed.

| Case | Actual assertions |
| --- | --- |
| Persistent administration | UID/online/database lookup, list/pagination, description edits, custom properties/search/update/delete, named client and channel-client permissions, permission overview, channel/server-group membership, complaint creation/list/specific and bulk deletion |
| Events and messages | Subscriptions before connection; regular-client join and leave with matching identity/ID; single-use server-group key consumption and event/membership; server-bound inbox query login; offline message metadata/body/read flags/delete; exact decoded poke and instance broadcast receipt; offline state and database deletion |

Live help and privacy-preserving response/event field inventories are written to
`target/compatibility/<TARGET>-regular-client.txt` and
`<TARGET>-regular-events-messages.txt`. Evidence never records generated identities,
privilege keys, passwords or private message bodies. These are actual operations,
not guarantees inferred from help or query-client notifications.

TS6 joins additionally expose `client_is_streaming` and `client_user_tag` on the
pinned fixture. Unknown fields remain accessible through the event map. Custom
property searches use SQL wildcard `%`, as live help specifies. Deleting a
disconnected client's database record makes `clientdbinfo` fail with error **512**,
`invalid clientID`, on both TS3 targets.

Event probes have bounded queues and deadlines. Overflow and callback exceptions
reach the test thread immediately; deterministic unit tests cover both failure
paths. The UDP fixture authenticates control requests with a random token, limits
request size, bounds protocol commands and closes a timed-out session. These
contracts detect failures at their source rather than relying on eventual sleeps.

## Migration and release intent

This checkpoint has **`fix:`** release intent within planned 2.0.0:

- `getOfflineMessage` now returns its documented `null` for an absent message;
  an explicit empty message body remains `""`. On all pinned servers a deleted
  message returns an empty successful response. The old wrapper default obscured
  that distinction. Consumers should handle `null` before using the body.
- Group/channel creation and group-copy operations require a positive ID in a
  successful response. Missing, malformed, zero or negative IDs now fail with a
  `TS3Exception` naming the command and required property, instead of propagating
  `-1` into a later command. Scripted tests verify the contract and subsequent
  stream usability. This does not diagnose every possible server/transport race.

The headless client dependencies and crypto overlays are test infrastructure only;
they do not enter the published library POM. See the fixture README for the pinned
upstream source, license and isolated dependency limitations. Remaining #441
acceptance work is tracked in the implementation PR and the domain inventory.
