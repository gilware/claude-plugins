---
name: dependency-upgrade
description: Check what a dependency upgrade breaks in a Java or Kotlin project (Maven or Gradle) with Bumpwise, before changing the version, including a BOM such as compose-bom or spring-boot-dependencies. Use when asked to upgrade, bump or update a dependency, to review a Dependabot or Renovate pull request, or when a build fails after a version change with NoSuchMethodError, NoClassDefFoundError or a missing method.
---

# Check a dependency upgrade

Bumpwise compares the version of a dependency the project uses now with the new one, and finds each breaking change
in the project's compiled classes: the places (file:line) that will stop compiling or fail at run time. Use the
`check_dependency_upgrade` tool of the `bumpwise` MCP server.

## How

1. Build the project the way it is usually built, before changing the version: the tool reads the compiled
   classes, and the version the project uses now from `pom.xml`, `build.gradle(.kts)` or
   `gradle/libs.versions.toml`.
2. Call `check_dependency_upgrade` with `dependency` (`GROUP:ARTIFACT`) and `to` (the new version; leave it out for
   the latest release). In a multi-module build, pass `project` for the module, or leave it out to scan every
   module's build output.
3. Tell the user the verdict and the places it lists. Each place names the method, field or class that changed and,
   for a moved class, where it went.

## Acting on the result

- **Safe:** change the version.
- **Breaks places:** change the version and fix each listed place (a moved class: the new name; a removed method:
  look in the new version's API or release notes for the replacement). Build again and run the tests.
- **Marked source only:** compiled code still runs, but recompiling fails; fix it the same way.
- **Raises minSdk** (Android libraries): the new version needs a higher minSdk than the project's, so the build
  fails at the manifest merge. Tell the user; raising the app's minSdk drops older devices, so it is their decision.
  Otherwise stay on the current version. If Bumpwise could not read the project's minSdk, say which minSdk the new
  version needs.
- **A BOM** (`androidx.compose:compose-bom`, Firebase's `com.google.firebase:firebase-bom`,
  `org.springframework.boot:spring-boot-dependencies`): pass the BOM itself as `dependency`. The answer starts with a
  line about the BOM (how many of the project's libraries change version), then gives one result per library it
  moves. Where the build sets its own version of one of those libraries, Bumpwise checks the version the build will
  use. A library marked "cannot tell" (Bumpwise could not read its version, or the build changes how Gradle picks
  versions) needs a check of its own with its `GROUP:ARTIFACT`.
- **Several dependencies changed** (a Dependabot or Renovate update): in a terminal,
  `bumpwise upgrade --changed-since origin/main` checks every version the branch changed.

It does not see uses through reflection or code that is not compiled, and it only checks the dependency itself, not
the other dependencies its new version brings in.

## Without the MCP server

With the `bumpwise` command installed (https://gilware.com/bumpwise/cli/):
`bumpwise upgrade GROUP:ARTIFACT:VERSION`. Exit code 0 means safe, 1 that it breaks places, 2 that it could not tell.
