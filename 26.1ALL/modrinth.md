# Chunky Extended

**Pregenerate your world without ever lagging your players.**

Chunky Extended is a lightweight, server-side companion for the popular [Chunky](https://modrinth.com/project/LFJf0Klb) pregenerator. It automatically **pauses Chunky while players are online** and **continues it once the server is empty**, so all that heavy chunk generation only happens when nobody is around to feel it. Add an optional time scheduler and you get full control over exactly when pregeneration runs.

No more choosing between a fully pregenerated world and a smooth experience for your players — Chunky Extended gives you both.

---

## Features

- **Player-aware auto-pause / continue** — Chunky runs only while the server is empty. The moment someone joins, it pauses; when the last player leaves, it resumes.
- **Time scheduler** — set rules like *"continue at 03:00, pause at 08:00, Mon–Fri"* so pregeneration runs overnight. Rules are per-day, with a configurable timezone.
- **In-game menu** — manage everything from a clean GUI, no commands to memorize (see below).
- **Commands too** — full control via `/chunky-extend` if you prefer the console.
- **Drives Chunky directly** — uses Chunky's own `pause` / `continue`, so it stays compatible with however you've configured Chunky.
- **Multi-loader** — one mod for **Fabric**, **Forge**, and **NeoForge** on Minecraft 26.1.2.

---

## How to open the menu

Type the client command:

```text
/cemenu
```

This opens the Chunky Extended control panel, where you can:

- Toggle the mod on/off and the auto-pause / auto-continue behaviour.
- Add and remove schedule rules using simple Hour / Minute sliders and Mon–Sun day toggles — no typing times by hand, so there's nothing to get wrong.
- See the current status and settings at a glance.

> **Who can edit:** server **operators** (gamemaster / permission level 2) and the **singleplayer host** can make changes. Everyone else can open the menu in read-only mode.

The menu syncs live with the server, so what you see is always the server's real state — handy on multiplayer where the settings live server-side.

---

## Commands

Everything in the menu is also available through the `/chunky-extend` command tree:

```text
/chunky-extend enable      # turn auto pause/continue on
/chunky-extend disable     # turn it off
/chunky-extend status      # show current status
/chunky-extend settings    # view settings (e.g. timezone)
/chunky-extend schedule    # add / list / remove scheduled rules
```

---

## Requirements

Chunky Extended needs two dependencies installed alongside it:

- **[Chunky](https://modrinth.com/project/LFJf0Klb)** — the pregenerator it controls.
- **CodxLib** — a small shared library used by my mods (powers the menu, networking, and update checks). It's listed under **Dependencies**, so most launchers install it automatically.

Install all three (Chunky Extended + Chunky + CodxLib) on the **server**. The `/cemenu` GUI is client-side, but the mod itself does its work server-side.

---

## Short description

Server-side automation for the Chunky pregenerator: auto-pause while players are online, auto-continue when the server is empty, plus a scheduler and an in-game menu. Fabric / Forge / NeoForge.
