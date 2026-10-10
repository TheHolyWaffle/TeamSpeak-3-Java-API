#!/usr/bin/env bash
# Local/CI non-publishing signed staging with a disposable key. No real credentials.
set -euo pipefail
keydir=$(mktemp -d)
trap 'gpgconf --homedir "$keydir" --kill all >/dev/null 2>&1 || true; rm -rf "$keydir"' EXIT
chmod 700 "$keydir"
gpg --homedir "$keydir" --batch --pinentry-mode loopback --passphrase rehearsal \
  --quick-generate-key 'CI rehearsal <rehearsal@example.invalid>' rsa3072 sign 1d >/dev/null 2>&1
gpg --homedir "$keydir" --batch --armor --export > "$keydir/expected-public.asc"
gpg --homedir "$keydir" --batch --pinentry-mode loopback --passphrase rehearsal \
  --armor --export-secret-keys > "$keydir/secret.asc"
bash .github/scripts/derive-signing-public-key.sh "$keydir/secret.asc" "$keydir/public.asc"
cmp "$keydir/expected-public.asc" "$keydir/public.asc"
export JRELEASER_GPG_PUBLIC_KEY="$keydir/public.asc"
export JRELEASER_GPG_SECRET_KEY="$keydir/secret.asc"
export JRELEASER_GPG_PASSPHRASE=rehearsal
export JRELEASER_GITHUB_TOKEN=unused-dry-run
export JRELEASER_MAVENCENTRAL_CENTRAL_USERNAME=unused-dry-run
export JRELEASER_MAVENCENTRAL_CENTRAL_PASSWORD=unused-dry-run
./mvnw -B -ntp -Pcentral-staging deploy
./mvnw -B -ntp jreleaser:deploy -Djreleaser.dry.run=true
./mvnw -B -ntp jreleaser:release -Djreleaser.dry.run=true \
  -Djreleaser.config.file=jreleaser-github.yml
cmp target/release-notes.md target/jreleaser/release/CHANGELOG.md
# Verify signatures independently of JReleaser and assert the required artifacts.
version=$(./mvnw -q -ntp org.apache.maven.plugins:maven-help-plugin:3.5.2:evaluate \
  -Dexpression=project.version -DforceStdout)
base="target/staging-deploy/com/github/theholywaffle/teamspeak3-api/$version/teamspeak3-api-$version"
for suffix in .pom .jar -sources.jar -javadoc.jar; do
  test -s "$base$suffix"
  test -s "$base$suffix.asc"
  gpg --homedir "$keydir" --batch --verify "$base$suffix.asc" "$base$suffix" >/dev/null 2>&1
done

# A bundle without sources must fail Central rules before any upload.
mv "$base-sources.jar" "$keydir/sources.jar"
if ./mvnw -B -ntp jreleaser:deploy -Djreleaser.dry.run=true > target/missing-source.log 2>&1; then
  mv "$keydir/sources.jar" "$base-sources.jar"
  echo 'JReleaser accepted a bundle without sources' >&2
  exit 1
fi
mv "$keydir/sources.jar" "$base-sources.jar"
grep -Ei 'sources.*(missing|not found)|missing.*sources' target/missing-source.log
