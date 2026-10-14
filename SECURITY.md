# Security policy

## Supported versions

| Version | Status |
| --- | --- |
| 2.x (in development on `modernization/2.0`) | Supported once 2.0.0 is published: fixes go into the latest 2.x minor release |
| 1.3.x (current release on `master`) | Fixes until 2.0.0 is published, then end of life |
| 1.2 and older | End of life |

Once 2.0.0 is published, 1.x receives no further releases, including security fixes. Upgrade
with the [migration guide](docs/migration-2.0.md). Within a supported line, only the
newest release is patched; there are no backports to older minors or patches.

A supported version runs on **Java 25 or newer** against the **TeamSpeak 3 server** versions listed in the README's
[compatibility table](README.md#requirements-and-compatibility). TeamSpeak 6, the TS3 WebQuery
and the voice/client protocols are not supported.

## Reporting a vulnerability

**Do not open a public issue, discussion or pull request for a vulnerability.**

Report it privately through GitHub's
[private vulnerability reporting](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/security/advisories/new)
(**Security → Report a vulnerability**). Only the maintainers can see the report. Please include:

- the affected library version (or commit) and Java version;
- the TeamSpeak server version and transport (raw TCP or SSH);
- the impact and how to reproduce it, ideally with a minimal program;
- logs with credentials, privilege keys, host names and IP addresses removed.

The library is maintained by volunteers on a best-effort basis; we aim to acknowledge a report
within 14 days. A fix is prepared in the advisory's temporary private fork and released as a new
version, then the advisory is published, with a CVE where one applies. Reporters are credited
unless they prefer otherwise.

## Scope

In scope: code in this repository and the published `com.github.theholywaffle:teamspeak3-api`
artifacts, for example command or argument injection through parameter encoding, SSH host key
verification bypasses, secrets leaking into logs or exception messages, and release pipeline
weaknesses.

Out of scope, please report upstream instead:

- vulnerabilities in the TeamSpeak server itself: [TeamSpeak](https://teamspeak.com);
- vulnerabilities in a dependency (SSHJ, Bouncy Castle, SLF4J) that this library does not make
  worse: the dependency's project. Dependabot proposes patched versions here.

Known, documented limitations are not vulnerabilities: file transfers use an unencrypted TCP
connection even when the query uses SSH, raw TCP queries send the login in plain text, and
`SshHostKeyPolicy.trustOnFirstUse` trusts the first key it sees. See the
[README](README.md#using-the-api).
