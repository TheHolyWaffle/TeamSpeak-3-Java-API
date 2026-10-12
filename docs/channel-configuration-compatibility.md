# Verified channel configuration

> Current scope: TS3 RAW and SSH only. TS6 functionality and tests are removed.
> Any TS6 observations below are retained historical findings from merged PRs,
> not current support claims or release acceptance requirements.

This focused #441 slice verifies TS3 **3.13.8 RAW** and TS3 **3.13.8 SSH** using the existing pinned fixtures. It extends the core
channel CRUD evidence in the [overall inventory](command-compatibility.md).

## Verified operations

`ServerQueryCompatibilityIT.channelConfigurationDomain` creates an owned
semi-permanent channel with explicit unlimited client capacity, inherited family
capacity, Opus Voice and quality 6. It uses server-returned channel/client IDs.
A separate observer query session moves into that channel and subscribes to its
channel notifications before the actor changes configuration.

- Description edits preserve Unicode, pipe and literal backslash escape text.
  The observer receives `ChannelDescriptionEditedEvent` identifying the channel;
  the test reads the description separately because the event only supplies `cid`.
- Setting and clearing a password changes `hasPassword()` and sends
  `ChannelPasswordChangedEvent` to the observer each time. These events also
  contain only `cid`; they do not disclose the password or invoker. Only their
  field names are reported. Password enforcement for regular/voice clients is
  not established by these administrator sessions.
- Setting permanent while leaving semi-permanent enabled fails with
  **775 / `invalid channel flags`** and leaves the channel semi-permanent.
  Explicitly clearing the old flag allows a transition to permanent and back.
- Permanent configuration sets individual capacity 2 and family capacity 3,
  clearing unlimited/inherited flags. Typed getters return those values/flags.
  The same edit sets Opus Music and quality 8. A later edit restores unlimited
  individual capacity and inherited family capacity.
- Quality 11 fails with **2568 / `insufficient client permissions`**, reporting
  `i_channel_create_modify_with_codec_maxquality`. The failed numeric ID is
  **98 on TS3 and 101 on TS6** for these pins; the test resolves it by name from
  each server rather than asserting a universal ID. Quality remains 8 after
  rejection. This tests permission rejection, not the server's parameter
  validation order after elevating permissions.

The test captures live `channelcreate`, `channeledit` and `channelinfo` help,
description event metadata, password event field names, configuration field names
and actual errors in `target/compatibility/<target>-channel-configuration.txt`.
Five-second command/event bounds and a 60-second per-target timeout fail on missing
responses/notifications; no sleeps or retries hide missing delivery. Listener
failure, malformed notification and dropped-event counts must remain zero.
Existing channel cleanup deletes only the owned channel on failure and preserves
the original failure if cleanup also fails. Container disposal bounds fixture
state to the test run.

## Release intent and migration

Internal **`test:`** intent for planned 2.0.0 acceptance; no runtime, public API,
dependency or default changes. Specify capacity modes explicitly instead of
assuming server defaults. When changing between permanent and semi-permanent,
set the desired flag and clear the previous flag in the same edit. Do not infer
that a rejected edit changed state. Resolve `failed_permid` using metadata from
the connected server and use the actual failure message.

Description/password notifications identify a changed channel; fetch current
configuration when its value matters. This slice verifies an observer already
inside the channel with an explicit channel subscription. It makes no promise
about delivery to unsubscribed or out-of-channel sessions. Keep passwords out of
reports and do not interpret an administrator move as proof of password checks.

## Remaining scope and official documentation

These paragraphs record the original slice boundary. The current whole-issue
TS3 evidence and follow-up ownership are in the [acceptance inventory](command-compatibility.md)
and [additional command tests](additional-command-compatibility.md).

Temporary-channel creation/empty deletion, default designation, restart persistence
of semi-permanent channels, password verification/enforcement, client/family limit
enforcement, denied roles, other codecs/quality boundaries, encrypted audio,
banners, delay timers and other property combinations remain unverified. Opus
settings here are ServerQuery metadata only, not voice encoding/audio evidence.
Actual voice-client behavior needs verification with the official TeamSpeak client. TS6 RAW and Web
Query remain unclaimed; other #441 gaps stay in the overall inventory.

Reviewed official [TS6 beta13.1 release notes](https://github.com/teamspeak/teamspeak6-server/releases/tag/v6.0.0-beta13.1),
[JUnit 6.1.3 execution guidance](https://docs.junit.org/6.1.3/writing-tests/parallel-execution.html)
and [Failsafe verify](https://maven.apache.org/surefire/maven-failsafe-plugin/usage.html).
The pinned release notes concern server-stop fixes; live help and executed
assertions establish this channel subset. Tests keep the existing sequential
fixture execution rather than enabling concurrent mutation of shared channels.
