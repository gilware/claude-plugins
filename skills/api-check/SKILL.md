---
name: api-check
description: Check a Java or Kotlin library's change against the API of its last published release with Bumpwise, before committing or releasing. Use after changing public classes, methods, fields or Kotlin declarations of a published library (Maven or Gradle), when asked whether a change breaks users, which version comes next (major/minor/patch), or to prepare a release or changelog.
---

# Check the API against the last release

Bumpwise compares the library's compiled classes with the version published on Maven Central (or the repository
the project uses) and says whether the change breaks existing users, which semantic version is needed, and what
the compatible alternative is. Use the `check_api_compatibility` tool of the `bumpwise` MCP server.

## When

- After editing the public API of a library that is published: public or protected classes, methods, fields,
  constructors, Kotlin parameters and defaults, sealed hierarchies.
- Before committing such a change, and before a release ("what is the next version?", "prepare the release",
  "write the changelog").
- Not for applications or code that is not published as a library. If the tool says there is no published
  release yet, say so and stop.

## How

1. Build the library the way the project is usually built (its README, `CLAUDE.md`, CI file or wrapper):
   compiling is enough, tests are not needed. The tool reads compiled classes, so build again after each edit.
2. Call `check_api_compatibility`. All arguments are optional: pass `project` for a module folder in a
   multi-module build, `exclude` for packages that are not public API (for example `com.acme.internal`), and
   `includeChangelog: true` when preparing a release.
3. Read the result to the user in a few lines: the verdict, the required bump and the next version.

## Acting on the result

- **Breaking changes found and not intended:** change the code to the compatible alternative the tool gives
  (for example keep the old method, mark it `@Deprecated`, and delegate to the new one), build, and check again
  until it reports no breaking change.
- **Breaking on purpose:** say that the release needs a new major version, and list what users must change.
- **Declared version too low:** say which version the project should have. Change the version in the build
  file only when the user asks.
- **Packages reported that are internal:** suggest `exclude`, or an `exclude` list in `.bumpwise.yml` so the
  whole team gets the same result.

Never commit, tag or publish on your own because the check passed.

## Without the MCP server

If the tool is not available, the same check runs from the terminal when the `bumpwise` command is installed
(https://gilware.com/bumpwise/cli/): `bumpwise check --format markdown`. Exit code 0 means compatible, 1 means
breaking changes, 2 means it could not check.
