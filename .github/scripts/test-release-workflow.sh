#!/usr/bin/env bash
# Asserts the publication gates of release.yml and the JReleaser configurations without running
# them: publication needs successful validation and verification, deploys only after the staged
# artifacts are checked, and never overwrites a tag, release or published deployment.
# Usage: test-release-workflow.sh [WORKFLOW] (yq v4 on PATH)
set -euo pipefail
workflow=${1:-.github/workflows/release.yml}
fail() { echo "Release workflow check failed: $*" >&2; exit 1; }
check() { yq -e "$1" "$workflow" > /dev/null 2>&1 || fail "$2"; }
step() { yq "[.jobs.publish.steps[].name] | to_entries | .[] | select(.value == \"$1\") | .key" "$workflow"; }

check '(.on | keys | join(",")) == "workflow_dispatch"' 'only a manual dispatch may start a release'
check '.on.workflow_dispatch.inputs.action.default == "validate"' 'the default action must not publish'
check '.permissions.contents == "read" and (.permissions | length) == 1' 'the default token must be read-only'
check '.concurrency."cancel-in-progress" == false' 'a running release must never be cancelled'
check '.jobs.validate.if == "${{ github.ref == '"'refs/heads/master'"' }}"' 'validation must run only on master'
check '.jobs.verify.needs == "validate"' 'verification must wait for validation'
check '(.jobs.publish.needs | join(",")) == "validate,verify"' 'publication must need validation and verification'
for condition in "needs.validate.result == 'success'" "needs.verify.result == 'success'" "inputs.action != 'validate'"; do
  check ".jobs.publish.if | contains(\"$condition\")" "publication must require $condition"
done
check '.jobs.publish.environment == "release"' 'publication must use the protected release environment'
check '[.jobs[] | select(.permissions.contents == "write")] | length == 1' 'only one job may write contents'
check '.jobs.publish.permissions.contents == "write"' 'only the publication job may write contents'

validate=$(yq '.jobs.validate.steps[] | select(.id == "approval") | .run' "$workflow")
for gate in 'startswith("release-please--")' '"autorelease: pending"' '"CI required"' \
  'git rev-parse "$RELEASE_TAG^{commit}"' '! grep -Fxq "$RELEASE_TAG" target-releases.txt'; do
  grep -qF -- "$gate" <<< "$validate" || fail "validation lacks the gate $gate"
done

recheck=$(step 'Recheck immutable inputs in the restricted release environment')
stage=$(step 'Package the normal library into a local repository')
inspect=$(step 'Verify the staged artifacts carry the approved version')
deploy=$(step 'Sign, validate and deploy through Central Portal')
github=$(step 'Require Central availability before creating the GitHub release')
[[ -n $recheck && -n $stage && -n $inspect && -n $deploy && -n $github ]] || fail 'a publication step is missing'
((recheck < stage && stage < inspect && inspect < deploy && deploy < github)) || fail 'publication steps are out of order'
deploy_run=$(yq ".jobs.publish.steps[$deploy].run" "$workflow")
grep -qF 'check-portal-deployment.sh' <<< "$deploy_run" || fail 'resume-central must check the Portal deployment first'
grep -qF 'JRELEASER_MAVENCENTRAL_CENTRAL_STAGE=PUBLISH' <<< "$deploy_run" || fail 'resume-central must not upload again'

for config in jreleaser.yml jreleaser-github.yml; do
  yq -e '.release.github.overwrite == false and .release.github.update.enabled == false' "$config" > /dev/null ||
    fail "$config may overwrite or update a GitHub release"
done
yq -e '.deploy.maven.mavenCentral.central.active == "RELEASE" and .signing.active == "RELEASE"' jreleaser.yml > /dev/null ||
  fail 'snapshots must never be signed or deployed'
yq -e '.deploy == null and .signing == null' jreleaser-github.yml > /dev/null ||
  fail 'the GitHub-only configuration must not deploy or sign'
echo 'Release workflow gates verified.'
