# MOGS Client

**MOGS Client** · Minecraft 1.21.11 · Fabric

A client-side Fabric mod: dark / red / gold ClickGUI, five categories (Combat, Misc, Render, Visuals, Client),
movable HUD elements, and persistent configuration. The Combat category is strictly informational and cosmetic:
you aim, click, move, place, use items and switch equipment yourself.

## Build

Requirements: JDK 21 and Gradle 9.2 or newer (or generate a wrapper once with `gradle wrapper --gradle-version 9.2.0`).

```
gradle build
```

Output: `build/libs/MOGS-Client.jar` (the remapped jar; ignore `MOGS-Client-1.0.0-dev.jar` if present).

Pinned versions are in `gradle.properties` (Minecraft 1.21.11, Fabric Loader 0.19.5, Fabric API 0.141.6+1.21.11,
Loom 1.14.10, Mojang official mappings).

## Install

1. Install Minecraft Java Edition 1.21.11.
2. Install Fabric Loader for 1.21.11.
3. Put Fabric API (1.21.11 build) into `.minecraft/mods`.
4. Put `MOGS-Client.jar` into `.minecraft/mods`.
5. Launch the Fabric 1.21.11 profile and join a world or server.
6. Hold **Esc** and press **Right Shift** to open the ClickGUI (Right Shift is rebindable in Client > ClickGUI > GUI Key). Right Shift or Esc closes it.

No separate launcher, no manual config editing.

## Lunar Client

MOGS is a normal Fabric mod, and Lunar Client loads Fabric mods on 1.21 (Fabric API is bundled with Lunar).

1. In the Lunar launcher choose 1.21.11 with **Fabric** (Lunar + Fabric).
2. Click the gear icon, open the **Mods** tab, and drag `MOGS-Client.jar` into it.
3. Launch and join a world. Hold **Esc** and press **Right Shift** to open MOGS. Because it needs Esc held, it
   does not clash with Lunar's own Right Shift menu.

Lunar says not every Fabric mod works with it. If MOGS misbehaves there, use Lunar's "Vanilla Addon" or the plain Fabric install above.

## Using the GUI

- One panel per category, side by side. Drag a panel by its header to move it, click the header to collapse it,
  scroll over a panel to scroll it. Panel positions are saved.
- Click a module to toggle it. Right click it (or click its arrow) to open its settings; the first setting row
  is its keybind (click it, press a key; Backspace clears, Esc cancels). Module keybinds work while no screen is open.
- Type anywhere to search every module. Esc clears the search, then closes the menu.
- Edit HUD (bottom) lets you drag HUD elements and scroll over them to resize.
- Every module has a "Reset settings" button; Reset Config at the bottom resets everything (click twice to confirm).
- Client > ClickGUI holds the theme colours, GUI scale, animation and tooltip options.

## Configuration

Stored automatically in `.minecraft/config/mogs/config.json`: enabled modules, keybinds, all settings
(including HUD positions and scale, theme colours), GUI position and the last category. It is saved on
GUI close, shortly after any change, and on game exit.

## Modules

Combat: Combat HUD, CPS Counter, Attack Cooldown, Target Info, Target Highlight, Hit Statistics,
Combat Notifications, Weapon Durability, Armor Durability, Totem Counter, Potion Effects, Combat Crosshair, Hit Particles.
Misc: Coordinates, Session Clock, CustomCrosshair, CoordSnapper, WeatherNotifier. Render: Held Item, FullBright.
Visuals: Watermark. Client: ClickGUI, Notifications.

## Adding a module

Create a class extending `Module` (or `CombatModule`), declare settings with `add(...)`, call `initHud(x, y)` if it
draws a HUD element, and add one `register(new YourModule());` line in `ModuleManager.init()`.

## Notes

- No mixins are used. Version-sensitive input calls are isolated in `util/Compat.java`.
- Target Highlight projects the entity box using the FOV option, so it can drift slightly while a dynamic FOV
  effect (sprinting, speed) is active. Purely visual.
- Hit Particles "size" is the spread radius; vanilla particle sprite size is not adjustable.
