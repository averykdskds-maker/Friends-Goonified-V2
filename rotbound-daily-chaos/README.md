# Rotbound Daily Chaos

Server-only NeoForge mod for **Minecraft Java 26.3**, **NeoForge 26.3.0.100**, and **Java 25**. Players do not need the mod installed on their clients.

## Daily roll

At UTC midnight, the first server tick after the date changes randomly selects exactly one of the 12 settings below and advances it by one state. If the server is stopped at midnight, it rolls once when it next starts on a later UTC date. It never performs more than one roll on the same UTC date.

On a fresh config, all settings are set to their first value:

| Setting | State cycle |
|---|---|
| Keep Inventory | DISABLED → ENABLED |
| PvP | DISABLED → ENABLED |
| Enchantments | DISABLED → ENABLED |
| Mob Spawning | DISABLED → ENABLED |
| Regeneration | DISABLED → ENABLED |
| Day/Night Cycle | DISABLED → ENABLED |
| Difficulty | PEACEFUL → EASY → NORMAL → HARD → PEACEFUL |
| Weather | CLEAR → RAIN → THUNDER → CLEAR |
| Command Blocks | DISABLED → ENABLED |
| Fire Tick Delay | NORMAL → FAST → INSTANT → NORMAL |
| Player Interaction | DISABLED → ENABLED |
| Explosion Block Breaking | DISABLED → ENABLED |

A styled chat message is broadcast to all online players and each online player receives the challenge-complete toast sound.

## Persistence

The mod creates:

`config/rotbound_daily_chaos.toml`

It stores each setting's current state and `last_date_toggled_utc`. The saved values are reapplied when the server starts. The config uses NightConfig and is written automatically.

## How the custom settings work

- **Enchantments:** prevents enchanting-table use and blocks anvil operations that add enchantments. Normal repairs and renaming of already-enchanted items remain available.
- **Fire Tick Delay:** NORMAL keeps vanilla timing, FAST halves the scheduled fire tick delay, and INSTANT uses a one-game-tick delay. A Mixin targets Java's actual scheduled fire update interval; this does not change crop growth or other random-tick mechanics. Existing fire ticks already waiting in the world's schedule keep their current due time; the new delay applies when fire schedules its next update.
- **Player Interaction:** blocks player block/item/entity use, attacks, placement and ordinary block-mining interactions while disabled.
- **Explosion Block Breaking:** explosions still occur and can affect entities, but their affected block list is cleared while disabled.

The remaining switches use Minecraft 26.3 game rules, difficulty, and weather commands.

## Install

1. Stop the Rotbound server.
2. Put `rotbound_daily_chaos-1.0.0.jar` in the server's `mods/` folder.
3. Start the server once. It creates `config/rotbound_daily_chaos.toml`.
4. Keep a backup of that TOML file: it contains the current setting states and date guard.

No client installation is required. The mod targets NeoForge 26.3.0.100 specifically.

## Build from source

Use JDK 25 and Gradle 9.2.1:

```bash
gradle --no-daemon clean build
```

The jar is written to `build/libs/rotbound_daily_chaos-1.0.0.jar`. The repository's GitHub Actions workflow performs this build and uploads a `rotbound-daily-chaos` artifact.
