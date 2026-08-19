# Vantage Client

A client-side, single-player-only Fabric utility mod for Minecraft **26.2**
(the "Chaos Cubed" release, current stable as of build time). Provides
entity ESP, an entity info overlay, an adjustable-reach tool, and toggleable
flight, all configurable through an in-game ClickGUI.

`environment` is set to `"client"` in `fabric.mod.json`, so the mod refuses
to load on a dedicated server — there is no server-side jar variant, and
Flight/Reach are additionally hard-gated in code to only ever act on the
**integrated singleplayer server** running in the same process (see the
Javadoc on `ReachManager`/`FlightManager`). Connected to someone else's
server, those two features are inert no-ops; ESP/entity-info are local
rendering only and don't send or alter any packets.

## Before you build

This project was generated without network access, so the exact **Yarn
mappings build number** in `gradle.properties` (`26.2+build.1`) is a
placeholder — verify the real latest build for `26.2` at
<https://fabricmc.net/develop/> or on the Fabric maven
(`https://maven.fabricmc.net/net/fabricmc/yarn/`) and update
`gradle.properties` if it differs. Loader (`0.19.3`), Fabric API
(`0.153.0+26.2`), and Loom (`1.17`) versions were current as of the same
source check — re-verify those too if some time has passed.

There are two places most likely to need a small tweak once you actually
compile against real mappings, both called out in comments in the source:

- **`RenderUtils`** — the custom depth-test-disabled `RenderLayer`
  construction, because Mojang's Blaze3D/RenderPipeline rewrite (tied to the
  26.2 Vulkan backend work) has been actively changing how custom render
  layers are built.
- **`SettingsList`** — the `ElementListWidget.Entry` abstract method
  signatures, which have shifted more than once across recent versions.

Everything else (config, keybinds, ESP category logic, reach/flight
gameplay logic) is written against stable, long-standing Fabric API and
should not need changes.

## Building

```bash
# From the project root:
./gradlew build
```

If you don't have the wrapper jar (`gradle/wrapper/gradle-wrapper.jar`) —
it wasn't generated in this sandbox since it's a binary and requires
network access to fetch — run once with a local Gradle install to create
it, then commit it:

```bash
gradle wrapper --gradle-version 9.5.1
```

or just open the project in IntelliJ IDEA with the Fabric/Minecraft
Development plugins, which will bootstrap Gradle for you.

The built mod jar will be at:

```
build/libs/vantage-client-1.0.0.jar
```

## Installing

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for
   Minecraft **26.2**.
2. Download **Fabric API** for **26.2** and drop it in `.minecraft/mods/`.
3. Copy `vantage-client-1.0.0.jar` into `.minecraft/mods/`.
4. Launch the "fabric-loader-26.2" profile. Vantage Client only activates
   in singleplayer worlds for Reach/Flight; ESP works anywhere but is meant
   for your own single-player use.

## Default keybinds

All keybinds are registered through Minecraft's normal `KeyBinding` system,
so every one of them is rebindable under **Options → Controls → Key
Binds → Vantage Client** — nothing here is hard-coded.

| Action                  | Default key    |
|--------------------------|----------------|
| Open ClickGUI            | `1`            |
| Toggle ESP                | `]`           |
| Toggle Flight              | `V`           |
| Toggle Entity Info        | Unbound        |
| Toggle Reach Modifier      | Unbound        |

## Feature notes

- **ESP** — `EspRenderer` iterates loaded entities once per frame, filters
  by distance and by category (Players/Hostile/Passive/Animals/Villagers/
  Items/Projectiles/Vehicles/Other — each independently toggleable), and
  draws a wireframe box with depth testing disabled so it renders through
  terrain. Distance filtering happens before any GPU work, keeping the
  per-frame cost low even in busy areas.
- **Entity Info** — `EntityInfoRenderer` draws a small camera-facing label
  above each ESP-eligible entity: name, distance, numeric HP, a two-color
  health bar, and optionally the category name — each line independently
  toggleable.
- **Reach** — `ReachManager` edits the `minecraft:block_interaction_range`
  and `minecraft:entity_interaction_range` attributes directly on the
  integrated server's copy of your player (only possible because
  singleplayer runs its "server" in the same JVM as the client). A GUI
  slider plus a numeric-value display show the live value, and a "Reset
  Reach To Vanilla" button restores the exact defaults it captured on
  world load.
- **Flight** — `FlightManager` toggles vanilla's own creative-style flying
  ability on your player (both the client and integrated-server copies)
  and scales `flySpeed` for a horizontal-speed multiplier, with a separate
  vertical multiplier applied as a velocity adjustment while actively
  ascending/descending. It rides vanilla's existing flight physics rather
  than reimplementing movement, so it stays smooth and consistent with
  normal creative-mode flight.
- **ClickGUI** — `ClickGuiScreen` + `SettingsList` implement tabs (ESP,
  Info, Reach, Flight, General, Keybinds) over a scrollable, searchable
  list of vanilla-style toggle buttons, sliders (with live numeric labels
  and per-slider reset), and action buttons.
- **Config** — `ConfigManager` persists everything to
  `.minecraft/config/vantage-client.json` via Gson. A missing, empty, or
  corrupted file is handled gracefully: the corrupt file is backed up to
  `vantage-client.json.bak` and a fresh default config is written in its
  place, rather than crashing the game.
