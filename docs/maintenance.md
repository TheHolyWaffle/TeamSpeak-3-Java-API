# Dependency and image maintenance

## TeamSpeak 3 server image

The integration suite, the examples test and the [development launcher](development-server.md)
use one official TS3 image, pinned by tag and manifest digest in
[`src/test/resources/teamspeak3/Dockerfile`](../src/test/resources/teamspeak3/Dockerfile).
`TeamSpeakContainer` reads that `FROM` line; the tag is also the server version the suite
expects. A tag change changes the verified support claim, so `DocumentationTest` fails until the
README support table names the new version.
