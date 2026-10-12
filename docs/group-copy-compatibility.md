# Verified regular group copying

> Current scope: TS3 RAW and SSH only. TS6 functionality and tests are removed.
> Any TS6 observations below are retained historical findings from merged PRs,
> not current support claims or release acceptance requirements.

This #441 slice covers TS3 **3.13.8 RAW** and TS3 **3.13.8 SSH** on the existing pinned fixtures. See the
[overall inventory](command-compatibility.md) and [permission migration](permission-compatibility.md).

## Verified operations and evidence

`ServerQueryCompatibilityIT.groupCopyDomain` creates disposable source and target
server/channel groups. Source permissions use names, and group IDs come from
creation on the connected server. The test verifies:

- New regular server/channel copies have distinct IDs, expected type and names
  preserving Japanese text, pipes, spaces and literal `\s`.
- The selected server permission copies its value and negated/skipped flags;
  the selected channel permission copies its value with false flags.
- Editing the sources leaves the new copies' selected assignments unchanged.
- Copying onto existing targets reproduces the source permission states,
  including implicit server-provided assignments. A target-only permission is
  removed. Permission-state comparison excludes group IDs while retaining name,
  value, negated and skipped flags.
- All six created groups are registered with the existing cleanup resources.
  Every deletion is attempted on failure with original exceptions preserved.
  Final server/channel group-ID sets exactly match the initial sets.
- Copying from removed sources exposes **2560 / `invalid group ID`**, and a later version
  command succeeds.

Live `servergroupcopy` and `channelgroupcopy` help, group/assignment maps,
overwrite return values and errors are captured to
`target/compatibility/<target>-group-copy.txt`. Required Docker CI uploads these
with version/build/image/transport evidence and reports, including on failure.
Existing independent-session event probes run unchanged; this slice claims no
new group notification.

## Return-value repair and migration

Overwriting an existing server group produces a successful status without an
`sgid` row on all three pins. The previous implementation tried to read `sgid`
and returned **-1**. Both sync and async integer-target overloads now return the
requested target ID after success. The new-name overload still reads the created
ID supplied by the server. Failures are propagated rather than returning an ID.
The existing future adapter preserves cancellation propagation and bounded queue
admission; scripted regressions cover empty successful output, sync/async results,
server failure and cancellation before connection with capacity one. A latch-driven
regression pauses an observer after result publication, asserts admission is
still held and another command fails immediately with `TS3QueueFullException`,
then releases the observer and verifies admission/recovery before graceful quit.
This exercises the completion/cleanup ordering deterministically without a
scheduling-dependent sleep. A completed result does not mean all completion
work has returned or queue capacity has been released.

Release intent: **`fix:`**, behavioral correction for planned 2.0.0, without a
source/binary API or dependency change. Callers should interpret the successful
result as the overwritten target ID; it is not a newly created group. Update
workarounds that expected -1. Migration notes apply to the verified combinations,
not every group type or version.

## Fixture boundary and remaining scope

The serveradmin query database ID reported by `whoami` is rejected by
`clientpermlist` with **512 / `invalid clientID`** on these fixtures. The test
records this error and continues; it does not treat it as an empty assignment
list. An online query ID/database ID alone does not establish a persistent client
record suitable for client-permission CRUD. Positive client/channel-client
permission operations need a suitable persistent-client fixture and remain #441
scope. No client identity is fabricated or client-permission state changed by this test.

Template/query types, memberships/automatic assignments, copied icon semantics,
all name constraints, missing targets, self-copy, restricted-role enforcement,
permission overview/reset and other versions remain unverified. Actual voice
clients need separate fixture/manual evidence. TS6 RAW and Web Query are
unclaimed; remaining domains stay in the overall inventory. Transfer reliability
and NAT endpoints belong to #442.

Official [TS6 beta13.1 release notes](https://github.com/teamspeak/teamspeak6-server/releases/tag/v6.0.0-beta13.1),
[JUnit 6.1.3](https://docs.junit.org/6.1.3/overview.html) and
[Failsafe verify](https://maven.apache.org/surefire/maven-failsafe-plugin/usage.html)
were reviewed. Release notes discuss server-stop fixes; copy acceptance comes
from executed help/output and operations.
