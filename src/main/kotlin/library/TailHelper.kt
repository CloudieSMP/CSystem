package library

import item.cosmetic.PlayerTail
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter
import logger
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages active tail cosmetics for online players.
 *
 * BetterModel handles the full lifecycle of spawning, syncing, and removing
 * the display entities — TailHelper only needs to track *which* tail is active.
 */
object TailHelper {

    // UUID → currently equipped tail
    private val activeTails = ConcurrentHashMap<UUID, PlayerTail>()

    /**
     * Equips a tail on the player, removing any existing tail first.
     * Returns false if the BetterModel renderer for this tail doesn't exist yet
     * (i.e., the .bbmodel file hasn't been loaded).
     */
    fun equip(player: Player, tail: PlayerTail): Boolean {
        // Remove existing tail first (no-op if none)
        remove(player)

        val renderer = BetterModel.model(tail.modelId).orElse(null) ?: run {
            logger.warning("TailHelper: model '${tail.modelId}' not found in BetterModel — has the .bbmodel file been loaded?")
            return false
        }

        renderer.getOrCreate(BukkitAdapter.adapt(player))
        activeTails[player.uniqueId] = tail
        return true
    }

    /**
     * Removes the player's active tail, if any.
     * Returns true if a tail was actually removed.
     */
    fun remove(player: Player): Boolean {
        val current = activeTails.remove(player.uniqueId) ?: return false

        // Ask BetterModel's registry to remove the tracker for this model
        BetterModel.registry(player.uniqueId).ifPresent { registry ->
            registry.remove(current.modelId)
        }
        return true
    }

    /** Returns the currently equipped tail, or null. */
    fun current(player: Player): PlayerTail? = activeTails[player.uniqueId]

    /** Returns true if the player has any tail equipped. */
    fun has(player: Player): Boolean = activeTails.containsKey(player.uniqueId)

    /** Called on PlayerQuitEvent — cleans up the map entry (BetterModel auto-removes trackers on entity unload). */
    fun onPlayerQuit(player: Player) {
        activeTails.remove(player.uniqueId)
    }
}
