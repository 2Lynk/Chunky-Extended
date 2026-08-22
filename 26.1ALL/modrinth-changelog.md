## New in-game menu

No more memorising commands — run **`/cemenu`** to open a clean settings screen and
manage everything with clicks:

- **Toggles** for the mod, auto-pause on join, auto-continue on leave, the scheduler,
  and skip-while-players-online.
- A **timezone picker** you cycle through (no typing).
- The **full schedule editor**: see your rules, **Remove** any of them, and add new
  ones with **Hour/Minute sliders**, a pause/continue switch, and **Mon–Sun day
  buttons** (plus All / None) — so a rule can never be entered wrong.

The menu lives on the client and talks to the server in the background, so your
settings always stay in sync. Editing requires operator permissions; everyone else
sees it read-only. The existing `/chunky-extend` commands still work exactly as before.

## Now powered by CodxLib

Chunky Extended now uses **CodxLib**, a small companion library shared across my mods.
Its update checking now runs through CodxLib, so you get a console notice on server
start and a heads-up for operators when they join — and the `/chunkyextend` update
command works just like before.

### New required dependency
Chunky Extended now needs **CodxLib** installed alongside it. It's listed under
**Dependencies**, so most launchers will install it for you automatically.

Everything else works exactly as before, and your settings carry over.
