# Bumpwise for Claude Code

With this plugin, Claude Code can check whether a change to a Java or Kotlin library breaks the API of its last
published release, and tell you which version comes next. It is free, also for commercial use.

The plugin has two parts. The `bumpwise` MCP server provides the `check_api_compatibility` tool and the
`release_check` prompt. The `api-check` skill tells Claude when to run the check (after a change to a public API,
before a commit or a release) and what to do with the answer.

## Install

You need Java 17 or later.

```
/plugin marketplace add gilware/claude-plugins
/plugin install bumpwise@gilware
```

The first time it starts, the plugin downloads the Bumpwise command line (about 4 MB) from gilware.com, checks
its SHA-256 and saves it as `~/.local/share/bumpwise/bumpwise.jar` (`%LOCALAPPDATA%\Bumpwise\bumpwise.jar` on
Windows). If you already installed the command line with the installer, the plugin uses that jar.

If the installer also set up Bumpwise for Claude Code, remove that setup, or Claude will see the tool twice:

```
claude mcp remove -s user bumpwise
```

## Use

Ask Claude something like "Did my change break the API of the last release?", or type `/bumpwise:api-check`.

Documentation: https://gilware.com/bumpwise/cli/

## License

The plugin files are under the MIT license. The Bumpwise command line that the plugin downloads is free for any use
under its own terms: https://gilware.com/legal/cli-license/. Bumpwise runs on your computer and sends nothing to
Gilware.
