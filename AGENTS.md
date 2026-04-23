# AGENTS.md — Cloudie SMP System Plugin

A Paper 1.21.11 plugin for Cloudie SMP Season 10, written in Kotlin. It acts as the all-in-one server management plugin: commands, chat, crates, homes, mail, resource packs, and Discord integration.

## Build & Run

```bash
./gradlew shadowJar          # Build fat JAR → build/libs/csystem-INDEV-<hash>-all.jar
./gradlew runServer          # Spin up a local Paper test server under run/
```

- Version is auto-derived from the short git commit hash (`INDEV-<hash>`).
- The shadow JAR **must** be used (not the plain JAR) — Cloud, Configurate, and FastBoard are relocated into `moe.oof.csystem.shade.*`.
- Relocated packages: `org.incendo`, `org.spongepowered`, `fr.mrmicky`.
- Java toolchain: **JVM 25**.

## Architecture

```
CSystem.kt             — JavaPlugin entry point; wires events, Cloud command manager, command confirmation, config
Config.kt              — Spongepowered Configurate data class (mapped from src/main/resources/config.yml)
command/               — One class per command, all discovered via Cloud's annotationParser.parseContainers()
event/                 — Bukkit event listeners (player/, block/, entity/)
library/               — Stateful singletons: HomeStorage, MailStorage, CrateRollStatsStorage, CardPullCounterStorage, VanishHelper, AfkHelper, LiveHelper, NoSleepHelper, HelpHelper, PlayerListNameHelper
item/                  — Enums for rarities/types; crate/, booster/, binder/, treasurebag/ sub-packages
util/                  — Extensions, Keys registry, resource pack/webhook helpers, UI windows, Sounds
chat/                  — MiniMessage formatting, notifications, ChatUtility broadcasts
```

## Adding a Command

1. Create a class in `command/` annotated with `@CommandContainer`.
2. Annotate methods with `@Command`, `@Permission`, `@CommandDescription`.
3. Use `css.requirePlayer()` (extension in `util/CommandSourceStackExtensions.kt`) to guard player-only commands.
4. No registration needed — `annotationParser.parseContainers()` in `CSystem.kt` auto-discovers all `@CommandContainer` classes via kapt.
5. Declare the permission node in `src/main/resources/paper-plugin.yml` and add it to the appropriate group.
6. Add the command's help entry to `library/HelpHelper.kt` (either `commands` map for player commands or `staffCommands` map for staff commands).

Example skeleton:
```kotlin
@Suppress("unused")
@CommandContainer
class MyCommand {
    @Command("mycommand")
    @CommandDescription("Does something cool.")
    @Permission("cloudie.cmd.mycommand")
    fun run(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        player.sendMessage(Formatting.allTags.deserialize("<cloudiecolor>Hello!"))
    }
}
```

## Text Formatting

Always use `Formatting.allTags` for trusted/system messages and `Formatting.restrictedTags` for player-input messages.

Custom MiniMessage tags available:
| Tag | Meaning |
|-----|---------|
| `<cloudiecolor>` | Pink brand colour (#C45889) |
| `<notifcolor>` | Notification red (#DB0060) |
| `<prefix:NAME>` | Unicode glyph prefix (e.g. `admin`, `dev`, `live`, `warning`, `nosleep`) |
| `<skull:PLAYERNAME>` | Player skull glyph |

Hardcoded join/quit message templates live in `library/Translation.kt`.

## Storage Pattern

`HomeStorage`, `MailStorage`, and `CrateRollStatsStorage` all follow the same pattern:
- In-memory `ConcurrentHashMap` cache per player UUID.
- **Async reads** via callback: `HomeStorage.listHomeNamesAsync(uuid) { names -> ... }` (disk I/O is async, callback is rescheduled onto the Bukkit main thread).
- **Sync flush** on plugin disable: `HomeStorage.flushAllSync()`.
- Data files live in `plugins/System/homes/<uuid>.yml`, `plugins/System/mail/<uuid>.yml`, `plugins/System/crate-roll-stats/<uuid>.yml`, etc.
- Call `preload(uuid)` on player join to warm the cache early (done in `event/player/PlayerJoin.kt`).
- `CardPullCounterStorage` is separate: global sync load/save in `plugins/System/card-pulls.yml`.

## Item System

- All `NamespacedKey` values are centralized in `util/Keys.kt`.
- Custom item models use `DataComponentTypes.ITEM_MODEL` with keys like `NamespacedKey("cloudie", "crates/blue")` — the path maps to the server resource pack.
- `ItemRarity` holds display color + Unicode glyph; `CardRarity` extends this with weighted drop probabilities and broadcast behavior.
- Crate items are `Material.PAPER` with food/consumable data components to make them right-click-activatable without placing.
- **Sub-rarities** (`item/SubRarity.kt`): Cards can roll `SHINY`, `SHADOW`, or `OBFUSCATED` variants (each with a Unicode glyph and a `modelDataOffset` applied on top of the base model). Use `SubRarity.getRandomSubRarity()`. Debug weights can be overridden via `SubRarity.setDebugWeights(...)`.
- **Card Registry** (`item/booster/CardRegistry.kt`): The single source of truth for all trading cards. Add or modify cards here — `CardEntry(type, rarity, canHaveSubRarity, allowedBoosters)`. MOB card IDs must match Bukkit `EntityType` keys. After editing the registry, run `/pack export cardmodels` to regenerate resource pack item definitions.
- **Treasure bags** (`item/treasurebag/`): Bundle-based items (`BundleMeta`) created by `TreasureBag.create(type)`. Loot is defined in `BagLootPool` with per-item percentage roll chances and amount ranges.
- **Vending machines**: Entities tagged `vending_machine` (scoreboard tag) are handled by `event/entity/VendingMachineInteract.kt`. Interacting while holding a specific material consumes it and spawns a booster pack or crate as a dropped item.

## Sounds

All gameplay sounds are centralized in `util/Sounds.kt` as `Sound` constants (Adventure API). Use these instead of inline `sound(...)` calls:

```kotlin
player.playSound(Sounds.PLING)
player.playSound(Sounds.SHINY_CATCH)
```

Notable entries: `EPIC_CATCH`, `LEGENDARY_CATCH`, `SHINY_CATCH`, `SHADOW_CATCH`, `OBFUSCATED_CATCH`, `VENDING_MACHINE`, `ERROR_DIDGERIDOO`, `GAMBLING_WHEEL_TICK/STOP`.

## UI Windows

Inventory GUIs use the **Noxcrew Interfaces** library (`util/ui/`) with `CollectionBrowserWindow` as a shared selector/preview engine (used by `CrateBrowserWindow` and `BoosterPackBrowserWindow`) plus `BinderWindow`.
- Listener-backed windows are `GamblingWindow` and `TrashWindow` (`object : Listener`, registered at startup).
- Interface windows keep reactive state in closures/triggers (see `DelegateTrigger` usage in `CollectionBrowserWindow`/`BinderWindow`), while `GamblingWindow` tracks sessions in-memory by player UUID.

## AFK & Tab List

- `library/AfkHelper.kt`: Tracks AFK state per UUID. Call `AfkHelper.recordActivity(player)` on any meaningful input. Idle timeout is configured in `config.afk.idleTimeoutSeconds`; the checker runs every 30 seconds via `startIdleChecker()` (called at startup).
- `library/LiveHelper.kt`: Tracks streamer/live state per UUID. Call `LiveHelper.startLive(player)` / `LiveHelper.stopLive(player)`. Automatically displays a live glyph next to the player's display name and updates the tab list. Players retain their live status for 10 minutes after disconnecting.
- `library/NoSleepHelper.kt`: Tracks which players have the NoSleep tag enabled. Call `NoSleepHelper.setNoSleep(player, bool)`. While any player has NoSleep active, bed interactions are blocked for others.
- `library/PlayerListNameHelper.kt`: Updates the player's tab-list name whenever AFK/Live/NoSleep state changes. AFK → gray name, Live → pink name + `<prefix:live>`, NoSleep → `<prefix:nosleep>` prefix. Call `PlayerListNameHelper.apply(player)` after any state change.

## Config

Config is loaded via Spongepowered Configurate from `src/main/resources/config.yml` into the `Config` data class. Access it via `plugin.config` (the field is named `config` on the `CSystem` class, shadowing `JavaPlugin.getConfig()`). Reload at runtime with `/cloudie reload` (permission `cloudie.cmd.reload`).

## External Integrations

| Integration | Where |
|-------------|-------|
| Discord reports webhook | `util/DiscordWebhook.kt` — Ktor CIO, URL in `config.discord.reportWebhookUrl` |
| Resource pack CDN | `util/ResourcePacker.kt` — downloads & SHA-1 hashes packs on startup; reapplied on join |
| FastBoard (scoreboard) | `fr.mrmicky:fastboard` — relocated |
| Resource pack card model export | `util/MobCardModelExporter.kt` — generates `assets/minecraft/items/paper.json` dispatch entries and texture placeholder PNGs; triggered via `/pack export cardmodels` |

## Top-level Convenience Accessors

`plugin` and `logger` are top-level `val`s (defined in `CSystem.kt`) that delegate to the plugin instance — use them freely anywhere instead of passing the plugin reference around.
