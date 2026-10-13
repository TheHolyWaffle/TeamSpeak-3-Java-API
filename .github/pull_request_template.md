<!--
Title: a Conventional Commit, for example `fix: decode escaped pipes in client lists`.
It becomes the squash commit and decides the release; see CONTRIBUTING.md#pull-request-titles-and-releases.
-->

## Summary

<!-- What changes and why. Reference the issue: "Closes #123" or "Refs #123". -->

## Compatibility impact

<!-- Keep one. Must match the title: `!` or a BREAKING CHANGE: footer for a break. -->
- None (internal: build, CI, tests, docs)
- Backward-compatible fix or feature
- Breaking change to the API, behavior, defaults, runtime support or dependencies

## Migration notes

<!-- Required for a breaking change or changed behavior/defaults: what users must change. Otherwise "None". -->

## Validation

<!-- Commands you ran and their result, for example `./mvnw -B -ntp verify` and `-Pintegration verify`. -->

- [ ] Tests cover the change, and docs, examples and Javadoc are updated where behavior changed
