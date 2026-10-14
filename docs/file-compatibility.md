# Verified file-directory commands

> Current scope: TS3 RAW and SSH only. TS6 functionality and tests are removed.
> Any TS6 observations below are retained historical findings from merged PRs,
> not current support claims or release acceptance requirements.

This focused #441 slice covers TS3 **3.13.8 RAW** and TS3 **3.13.8 SSH** with the existing pinned containers. See the
[overall inventory](command-compatibility.md) and [integration setup](integration-tests.md).
It verifies directory administration through ServerQuery; file payload transfers,
endpoint resolution and reliability remain #442 work.

## Verified subset and evidence

`ServerQueryCompatibilityIT.fileDomain` exercises these operations on each target:

- List a new channel's empty root and query an empty active-transfer list.
  The latter establishes empty-result handling, not active-transfer metadata.
- Create directories through `files().createDirectory` with and without an explicit
  password argument. Paths without a leading slash are normalized by the existing
  command builder. List a root and nested directory, checking name, full/parent
  path, channel ID, directory type, timestamp and inactive-upload status.
- Create, list and delete a directory containing spaces and a pipe. The exact
  name survives protocol encoding. A literal backslash in `/literal\s` is rejected
  by all targets with `2054 / invalid file path`.
- Probe `/directory 日本語 spaces`: TS3 creates it and returns the exact path;
  TS6 rejects this specific name with `2054 / invalid file path`. The common
  CRUD path uses ASCII names. This is an observation of the pinned servers,
  not a universal character policy for every TS6 version or non-ASCII name.
- Create a nested child, rename its parent in the same channel and move that
  directory tree into another channel. Readback verifies updated parent/child
  paths and an empty source root.
- Delete the child through the scalar API and the two remaining directories
  through the bulk API; root/nested listings become empty as expected.
- Request file info for a directory and an absent file, and list an absent
  directory. The actual errors are retained, and later listings/version calls
  succeed on the same session.

The destination channel has a password containing spaces, a pipe and a literal
backslash escape. The test checks that its password flag is set, supplies that
password to directory operations and verifies effective **`b_ft_ignore_password=1`**
by name from the connected server. An incorrect password also permits listing
for this serveradmin session. These checks exercise the overloads and administrator
bypass; they do not establish password enforcement for restricted accounts.

Both channels are disposable and tracked by the existing cleanup resource. It
attempts every channel deletion even after failures and preserves the original
failure through try-with-resources suppression. Sessions also close on failure.
The suite uploads live help, decoded directory rows and error metadata in
`target/compatibility/<target>-files.txt`, alongside version/build/image/transport
and test reports. Help is captured for `ftcreatedir`, `ftgetfilelist`,
`ftgetfileinfo`, `ftrenamefile`, `ftdeletefile` and `ftlist`; executed operations
establish the supported subset rather than help examples alone.

## Fields, errors and migration

Directory list rows on these pins supply `cid`, `path`, `name`, `size`, `datetime`
and `type`. Root/nested paths end with a slash; `FileListEntry.getPath()` combines
that parent path and the name. Type `0` means directory. TS3's `datetime` is epoch **seconds**; TS6's is epoch
**milliseconds**. The live TS3 tests use the seconds getter and require
the date to be within one minute of directory creation. The exercised new
entries have no `incompletesize` field and are not uploading. This does not
establish file-size accounting, timestamp updates, active/paused transfers or
an exhaustive schema for every entry. Unknown fields remain available via
`getMap()` and controlled raw-command access.

| Operation | Error ID | Message |
| --- | --- | --- |
| Create directory with literal backslash | 2054 | `invalid file path` |
| Create the exercised Japanese directory name on TS6 | 2054 | `invalid file path` |
| `files().info` for an existing directory | 2048 | `invalid file name` |
| `files().info` for an absent file, TS3 | 2052 | `file input/output error` |
| `files().info` for an absent file, TS6 (historical) | 2051 | `file not found` |
| List an absent directory, TS3 | 2052 | `file input/output error` |
| List an absent directory, TS6 | 2051 | `file not found` |

Empty directories return empty lists; absent directories fail. Do not treat a
`2052` failure as an empty directory or assume it universally means absence.
`files().info` is a file-only operation here; obtain directory metadata from a
parent listing. Positive file-info and bulk file-info calls remain unverified.

TS3 file and directory timestamps use epoch seconds through the existing
`getLastModifiedDate()` getter. The `TimeUnit` overload added for historical TS6
millisecond responses is removed with TS6 functionality. Remove calls to that
overload and use the existing TS3 getter. This intentional source/binary removal
is included in #477's breaking 2.0 migration notes. There is no unit heuristic or
automatic server-version detection.

Handle actual errors for path names accepted by one version and rejected by
another, and choose names verified on your target server. No automatic renaming,
character replacement or client-side restriction is introduced. Privileged
password bypass must not be interpreted as a guarantee that a supplied password
was checked. Dependencies and existing default behavior are unchanged.

## Remaining scope and official sources

These paragraphs record the original slice boundary. The current whole-issue
TS3 evidence and follow-up ownership are in the [acceptance inventory](command-compatibility.md)
and [additional command tests](additional-command-compatibility.md).

Actual files, payload upload/download, positive scalar/bulk file info, icons,
active-transfer rows, overwrite/resume/stop semantics, cross-server storage
backends, restricted-account failures and further path cases remain unclaimed.
#441 retains the remaining file-command compatibility inventory; #442 owns
transfer reliability and container/NAT endpoint handling. No file-transfer socket
is opened by this slice. Voice clients, TS6 RAW and other versions are not
covered; Web Query rejects file commands ([Web Query](web-query.md#file-transfers)).

Official [TS6 beta13.1 release notes](https://github.com/teamspeak/teamspeak6-server/releases/tag/v6.0.0-beta13.1),
[Java 25 TimeUnit](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/TimeUnit.html),
[JUnit 6.1.3](https://docs.junit.org/6.1.3/overview.html) and
[Failsafe verify usage](https://maven.apache.org/surefire/maven-failsafe-plugin/usage.html)
were reviewed. The release notes discuss virtual-server stop fixes rather than
file-name compatibility. Recheck live help/output and release documentation when
updating the pins; the observed differences above come from executed probes.
