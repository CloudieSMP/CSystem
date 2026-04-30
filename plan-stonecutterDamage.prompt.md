# Plan: Stonecutter Damage + Blood Particles Listener

Create a new block listener that uses a repeating scheduler task to periodically check if any online player is standing on a `STONECUTTER` block, deal damage to them, and spawn red dust (blood) particles around the block. Wire it into the existing event setup in `CSystem.kt`.

## Steps

1. Create `event/block/StonecutterDamageListener.kt` as a `Listener` class — on `init` (or from a companion-level `start()` call), schedule a repeating `BukkitRunnable` every **10 ticks** (0.5 s) using `Bukkit.getScheduler().runTaskTimer(plugin, ...)`.

2. Inside the task body, iterate `Bukkit.getOnlinePlayers()`, and for each player check if the block at `player.location.subtract(0.0, 1.0, 0.0)` has type `Material.STONECUTTER`.

3. Deal damage by calling `player.damage(1.0)` (half a heart; vanilla invincibility frames will naturally throttle the actual damage ticks).

4. Spawn blood particles by calling `player.world.spawnParticle(Particle.DUST, location, count, offsetX, offsetY, offsetZ, Particle.DustOptions(Color.RED, 1.5f))` centred on the stonecutter block's location — spread a handful of particles (`count = 8`, small XZ offset of `0.4`) so they look like blood spraying out of the cutter.

5. Register the listener in `CSystem.kt` inside `setupEvents()` by adding `server.pluginManager.registerEvents(StonecutterDamageListener(), this)` and add the corresponding `import event.block.StonecutterDamageListener` at the top.

## Further Considerations

1. **Damage amount & tick rate** — `player.damage(1.0)` + 10-tick loop means roughly 1 damage instance per second (invincibility frames are 20 ticks by default, so only the first `damage()` call per second will actually reduce health). If you want faster bleeding, lower the invincibility frames via `player.noDamageTicks = 0` before each call, but that removes all incoming damage protection.
2. **Particle choice** — `Particle.DUST` with `Color.RED` gives the closest "blood" look in vanilla. An alternative is `Particle.BLOCK` with `Material.RED_CONCRETE` for a chunkier splatter aesthetic.
3. **Exempt players** — Consider skipping players in creative/spectator mode (`player.gameMode == GameMode.CREATIVE`) so admins aren't affected while building.