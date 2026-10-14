# Snapshot and instance-edit compatibility

> Current scope: TS3 RAW and SSH only. TS6 functionality and tests are removed.
> Any TS6 observations below are retained historical findings from merged PRs,
> not current support claims or release acceptance requirements.

`ServerSnapshotCompatibilityIT.snapshotRoundtrip` uses an owned disposable server
for TS3 3.13.8 RAW and TS3 3.13.8 SSH. Run with Java 25
and `./gradlew build integrationTest`; no preview or manual steps are used.

It records live snapshot/instance-edit help, edits and restores the instance download
bandwidth setting, and captures a server with Unicode/literal-escape name and welcome
text, a banner URL, a permanent channel description and a named group permission.
After stopping/deleting the original server, the typed snapshot deploy restores all
those values. Restored server/channel/group IDs are discovered from actual output.
The default fixture license allows only one running server; deleting the original
before deployment avoids treating a license limit as snapshot incompatibility.

Snapshots contain identities and credentials and are never written into acceptance
reports. `build/compatibility/<TARGET>-snapshots.txt` records help and assertions.
The group name is kept within the server's size boundary; an oversized test name
initially returned **1541**, `invalid parameter size`, on both TS3 targets.

This is internal `test:` coverage, with no runtime or API change. The operation
acceptance concerns configuration roundtrips, not filesystem backup, transferred
file contents, cross-version import or simultaneous servers beyond licensed limits.
Remaining lifecycle and configuration acceptance is tracked on the same #441 PR.
