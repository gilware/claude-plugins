# Bumpwise for Claude Code

With this plugin, Claude Code can check whether a change to a Java or Kotlin library breaks the API of its last
published release, and tell you which version comes next. Before it upgrades a dependency in any Java or Kotlin
project, it can also find the places in your code that the new version breaks. It is free, also for commercial use.

The `bumpwise` MCP server provides two tools, `check_api_compatibility` and `check_dependency_upgrade`, and the
`release_check` prompt. Two skills tell Claude when to use them and what to do with the answer: `api-check` after a
change to a public API or before a release, and `dependency-upgrade` before a version change or when you review a
Dependabot or Renovate pull request.

## Install

You need Java 17 or later.

```
/plugin marketplace add gilware/claude-plugins
/plugin install bumpwise@gilware
```

The first time it starts, the plugin downloads the Bumpwise command line (about 3.6 MB) from gilware.com, checks
its SHA-256 and saves it as `~/.local/share/bumpwise/bumpwise.jar` (`%LOCALAPPDATA%\Bumpwise\bumpwise.jar` on
Windows). If you already installed the command line with the installer, the plugin uses that jar, and replaces
it with the version the plugin needs when it is older. To use a jar of your own, set `BUMPWISE_JAR`; the plugin
never replaces that one.

If the installer also set up Bumpwise for Claude Code, remove that setup, or Claude will see the tool twice:

```
claude mcp remove -s user bumpwise
```

## Use

Ask Claude something like "Did my change break the API of the last release?" or "Can I upgrade Guava to 33.0?",
or type `/bumpwise:api-check` or `/bumpwise:dependency-upgrade`.

Documentation: https://gilware.com/bumpwise/cli/

## License

The plugin files are under the MIT license. The Bumpwise command line that the plugin downloads is free for any use
under its own terms: https://gilware.com/legal/cli-license/. Bumpwise runs on your computer and sends nothing to
Gilware.
