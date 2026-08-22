# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Chunky Extended is a server-side Minecraft mod that auto-pauses/continues the [Chunky](https://modrinth.com/project/LFJf0Klb) pregenerator based on player presence and a time scheduler. It runs Chunky only while the server is empty so pregeneration never lags online players.

This directory (`26.1ALL`) is the current **multi-loader** build targeting Minecraft 26.1.2 — one codebase compiled for Fabric, Forge, and NeoForge. The sibling directories (`../1.19`, `../1.20`, `../1.21`, `../26.1`) are older single-loader versions kept for reference/releases; ignore them unless porting back.

> `CONTEXT.md` in this directory is **stale and wrong** — it documents an unrelated "OneBlock / Architectury" setup. Do not trust it. This project does **not** use Architectury.

## Build system

This uses the [mcgradleconventions](https://github.com/) NeoForged-style multi-loader template, **not** Architectury. The structure:

- `buildSrc/src/main/groovy/multiloader-common.gradle` — shared Java/toolchain/`processResources` config (token expansion for manifests). Applied by every subproject.
- `buildSrc/src/main/groovy/multiloader-loader.gradle` — pulls the `common` source + resources into each loader project via the `commonJava`/`commonResources` Gradle configurations, so common sources are **compiled into each loader jar** rather than depended on at runtime.
- `common/` — compiled against **NeoForm (Mojmap names)** via `net.neoforged.moddev`. This is why common code uses Mojang mappings (`CommandSourceStack`, `MinecraftServer`, etc.).
- `fabric/` uses fabric-loom, `forge/` uses ForgeGradle, `neoforge/` uses moddev.

Versions are centralized in `gradle.properties` (`mod_version`, `minecraft_version`, loader versions, etc.) and expanded into `fabric.mod.json` / `mods.toml` / `neoforge.mods.toml` at build time via `processResources`. Edit versions there, never in the manifests.

**Java 25** toolchain is required (`java_version=25`).

### Common commands

Use the Gradle wrapper from this directory. `org.gradle.daemon=false` is set, so each invocation is a cold start.

```bash
./gradlew build                      # build all loaders
./gradlew :fabric:build              # build one loader's jar
./gradlew :common:build :fabric:build :forge:build :neoforge:build

# Run a dev client/server (pick the loader you're testing):
./gradlew :fabric:runClient
./gradlew :neoforge:runServer
./gradlew :forge:runServer           # forge run dirs: forge/run, forge/run-data
```

Built jars land in each loader's `build/libs/`. There is no test suite in this project.

## CodxLib dependency

The mod depends on **CodxLib** (`codx.codxlib:codxlib-*:${codxlib_version}`), a separate companion library, consumed from **mavenLocal** during development (`repositories { mavenLocal() }` in the common convention plugin). Its source lives at `~/Documents/Projects/codxlib` — read it when you need an API you haven't used. CodxLib provides:
- **Update checking** — `UpdateChecker`, `CodxLib`, `ModInfo`.
- **Loader-neutral networking** — `CodxNetwork` (register `CustomPacketPayload` types + handlers in common; CodxLib wires them into each loader and hops to the main thread).
- **Loader-neutral client commands** — `CodxCommands.registerClient(...)`, materialized into each loader's client dispatcher.
- **Styled config UI** — `CodxConfigScreen` (vanilla "pack screen" base) + `CodxWidgets` (toggle / cycle / int+double sliders).

If a build fails resolving `codx.codxlib`, that library must be published to the local Maven repo first (`./gradlew publishToMavenLocal` in the codxlib project). CodxLib is a **required** runtime dependency declared in all three manifests.

## Code architecture

All real logic lives in `common/`. The three loader modules are thin adapters.

- **`ChunkyExtendedCommon`** — the single source of truth. Holds the server reference, registers commands, and exposes lifecycle hooks (`onServerStarted`, `onServerStopping`, `onServerTick`, `onPlayerJoin`, `onPlayerDisconnect`). It drives Chunky by parsing & executing the vanilla `chunky pause` / `chunky continue` commands through the server's command dispatcher (`executeServerCommand`) — there is no direct Chunky API dependency. Also handles JSON state persistence.
- **`ChunkyExtendCommand`** — the `/chunky-extend` Brigadier command tree (enable/disable/status/settings/schedule/update). Built directly on `com.mojang.brigadier`.
- **`schedule/ScheduleRule`** — immutable time+action+days rule, plus all the parsing (`parseTime` `HH:mm`, `parseDays` supporting `mon`, `mon-fri`, `sat,sun`, `all`, `normalizeAction` → `pause`/`continue`).
- **`util/ModUtil`** — **all mutable mod state is static here** (enabled flags, timezone, the rule list, per-day executed-key set). This is process-global singleton state, not per-world.

### Client menu (`/cemenu`) and networking

A client-side GUI (CodxLib-styled) lets users manage everything without commands. Because all settings live in **server-side** `ModUtil`, the menu syncs over `CodxNetwork` rather than reading state locally. Pieces, all in `common/`:

- **`network/CeMenuPackets`** — the payloads (records): `Request` (c→s), `State` (s→c snapshot; rules encoded as `id|HH:mm|action|days` strings), `Settings`, `AddRule`, `RemoveRule` (c→s).
- **`network/CeNetworking`** — `register()` registers all payloads + the serverbound handlers; edits are gated by `canEdit` (op level `GAMEMASTERS`/2, or the singleplayer host). `register()` is called from `ChunkyExtendedCommon.logLoaded`, i.e. during each loader's main entrypoint — **this timing matters** (see below).
- **`network/CeClientHook`** — a server-safe seam. The clientbound `State` handler must be registered in common (so the server can encode/send the type), but it must **not** reference any `net.minecraft.client.*` class at registration time, or the method-reference lambda would classload the screen on a dedicated server and crash. So common registers `CeClientHook::onState` (no client imports); the client installs the real handler via `setHandler`.
- **`client/CeMenuClient`** + **`client/CeMenuScreen`** — client-only (loaded only from client entrypoints, never on a dedicated server). The screen extends `CodxConfigScreen`; time uses Hour/Min sliders and days use Mon–Sun toggle buttons (no free-text → no invalid input). Every edit sends a packet; the server persists and echoes a fresh `State` back, which is the single source of truth.

**Networking load-order rule:** register payloads during the **main entrypoint / `@Mod` constructor**, never in client init. NeoForge/Forge wire payloads on a later setup event (after all constructors); Fabric gathers clientbound receivers during client init (after all main inits). Registering in `logLoaded` satisfies all three.

### Per-loader entrypoints (the only loader-specific code)

`fabric/.../ChunkyExtendedFabric`, `forge/.../ChunkyExtendedForge`, `neoforge/.../ChunkyExtendedNeoForge` each do exactly one thing: subscribe to that loader's server lifecycle / command-registration / player-join-leave / server-tick events and forward them to the matching `ChunkyExtendedCommon` method. When adding a new lifecycle hook, you must wire it in **all three** entrypoints.

Each loader also has a **client entrypoint** (`*Client`) that calls `CeMenuClient.install()` and registers the `/cemenu` client command. Fabric uses a `client` entrypoint in `fabric.mod.json`; NeoForge/Forge call `*Client.init()` from the main constructor gated on `FMLEnvironment` dist.

### State & persistence

- State persists to **`ce.json`** in the server working directory (Gson, pretty-printed). On first load it migrates from the legacy **`ce.txt`** (a plain `true`/`false`) and rewrites as JSON. Persistence happens on every mutating command and on server stop.
- The **scheduler** runs off the server tick: it derives a per-minute key, skips if already handled this minute, optionally skips while players are online, then fires any matching `ScheduleRule` once per day (tracked by `id|date` keys in `ModUtil.executedKeys`, cleared on server stop).
- Timezone is configurable (`/chunky-extend settings timezone <zone>`); invalid zones fall back to system default.

## Conventions

- Common code targets **Mojang mappings** (NeoForm). Don't introduce Yarn/SRG names in `common/`.
- This MC version's mappings differ from older 1.21.x Mojmap in non-obvious ways — verify against the decompiled sources (the NeoForge moddev sources jar at `neoforge/build/moddev/artifacts/minecraft-patched-*-sources.jar`) instead of assuming. Known renames hit while building the menu: `ResourceLocation` → **`net.minecraft.resources.Identifier`** (`Identifier.fromNamespaceAndPath`); permissions are capability-based — no `Entity.hasPermissions(int)`, use `player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))` (`net.minecraft.server.permissions.*`); `ServerPlayer` has no `getServer()` (use `player.level().getServer()`); `isSingleplayerOwner` takes a `NameAndId` (`player.nameAndId()`); screens render via `extractRenderState(GuiGraphicsExtractor, …)`, not `render(GuiGraphics, …)`.
- Keep loader modules dumb — any behavior change belongs in `common/`, with the three entrypoints only forwarding events.
- User-facing chat messages are prefixed `Ce:` / `Ce ...`. Match that style.
- License is CC0-1.0.
