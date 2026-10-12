# Snapshot and instance-edit compatibility

`ServerSnapshotCompatibilityIT.snapshotRoundtrip` uses an owned disposable server
for TS3 3.13.8 RAW, TS3 3.13.8 SSH and TS6 6.0.0-beta13.1 SSH. Run with Java 25
and `./mvnw -B -ntp -Pintegration verify`; no preview or manual steps are used.

It records live snapshot/instance-edit help, edits and restores the instance download
bandwidth setting, and captures a server with Unicode/literal-escape name and welcome
text, a banner URL, a permanent channel description and a named group permission.
After stopping/deleting the original server, the typed snapshot deploy restores all
those values. Restored server/channel/group IDs are discovered from actual output.
The default fixture license allows only one running server; deleting the original
before deployment avoids treating a license limit as snapshot incompatibility.

Snapshots contain identities and credentials and are never written into acceptance
reports. `target/compatibility/<TARGET>-snapshots.txt` records help and assertions.
The group name is kept within the server's size boundary; an oversized test name
initially returned **1541**, `invalid parameter size`, on all three targets.

This is internal `test:` coverage, with no runtime or API change. The operation
acceptance concerns configuration roundtrips, not filesystem backup, transferred
file contents, cross-version import or simultaneous servers beyond licensed limits.
Remaining lifecycle and configuration acceptance is tracked on the same #441 PR.
