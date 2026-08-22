# Chunky Extended

A lightweight automation extension for [Chunky](https://modrinth.com/plugin/chunky) on Fabric, Forge, and NeoForge.

## What it does

Chunky Extended helps you run Chunky pregeneration with less player-facing lag by automating pause/continue behavior.

- Keeps a simple global automation toggle (`/chunky-extend enable|disable`).
- Supports scheduled actions (`pause` / `continue`) with timezone + weekday support.
- Persists settings and rules in `ce.json` across restarts.

> Chunky is required. This mod extends Chunky and does not replace it.

## Typical usage

1. Install Chunky and verify `/chunky` commands work.
2. Install Chunky Extended and restart server.
3. Enable automation:
	 - `/chunky-extend enable`
4. Configure scheduler (optional):
	 - `/chunky-extend settings timezone Europe/Amsterdam`
	 - `/chunky-extend schedule preset nightly mon-fri`
5. Start your normal Chunky job.

## Commands

### Core

- `/chunky-extend`
- `/chunky-extend enable`
- `/chunky-extend disable`
- `/chunky-extend status`
- `/chunky-extend update check`

### Settings

- `/chunky-extend settings`
- `/chunky-extend settings autopause <true|false>`
- `/chunky-extend settings autocontinue <true|false>`
- `/chunky-extend settings scheduler <true|false>`
- `/chunky-extend settings skip-online <true|false>`
- `/chunky-extend settings timezone <ZoneId>`

### Scheduler

- `/chunky-extend schedule enable`
- `/chunky-extend schedule disable`
- `/chunky-extend schedule list`
- `/chunky-extend schedule next`
- `/chunky-extend schedule clear`
- `/chunky-extend schedule remove <id>`
- `/chunky-extend schedule add <HH:mm> <pause|continue> [days]`

### Presets

- `/chunky-extend schedule preset nightly [days]`
	- Adds: `01:00 continue` + `07:00 pause`
- `/chunky-extend schedule preset weekend [days]`
	- Defaults to `sat,sun` if days are omitted
- `/chunky-extend schedule preset window <start> <end> [days]`
	- Adds: `<start> continue` + `<end> pause`

## Day formats

You can use:

- `all`
- `mon,wed,fri`
- `mon-fri`

## Notes

- Duplicate rules are rejected to keep schedules clean.
- Use `/chunky-extend schedule next` to quickly verify your config.
- Use `/chunky-extend update check` to check Modrinth for new releases.