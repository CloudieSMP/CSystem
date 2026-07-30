package library

import item.cosmetic.WearableTail
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CosmeticHelper {

    // Map of player UUID → list of tail segment displays + the tracking task
    private data class ActiveTail(val segments: List<ItemDisplay>, val task: BukkitTask)
    private val activeTails = ConcurrentHashMap<UUID, ActiveTail>()

    fun equipTail(player: Player, tail: WearableTail) {
        removeTail(player) // Remove existing tail first
        val segments = tail.spawnSegments(player)
        val task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            if (!player.isOnline) { removeTail(player); return@Runnable }
            tail.tickSegments(player, segments)
        }, 0L, 1L)
        activeTails[player.uniqueId] = ActiveTail(segments, task)
    }

    fun removeTail(player: Player) {
        activeTails.remove(player.uniqueId)?.let { active ->
            active.task.cancel()
            active.segments.forEach { it.remove() }
        }
    }

    fun hasTail(player: Player) = activeTails.containsKey(player.uniqueId)

    fun onDisable() {
        activeTails.values.forEach { active ->
            active.task.cancel()
            active.segments.forEach { it.remove() }
        }
        activeTails.clear()
    }
}