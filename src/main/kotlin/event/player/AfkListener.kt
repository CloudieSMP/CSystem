package event.player

import library.AfkHelper
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import io.papermc.paper.event.player.AsyncChatEvent
import plugin

class AfkListener : Listener {

    companion object {
        private const val AFK_COMMAND = "/afk"
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onJoin(event: PlayerJoinEvent) {
        AfkHelper.initPlayer(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        AfkHelper.cleanup(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMove(event: PlayerMoveEvent) {
        // Only trigger on actual position change, not just head rotation
        if (event.from.blockX != event.to.blockX ||
            event.from.blockY != event.to.blockY ||
            event.from.blockZ != event.to.blockZ
        ) {
            AfkHelper.recordActivity(event.player)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.hand == EquipmentSlot.HAND) {
            AfkHelper.recordActivity(event.player)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInteractEntity(event: PlayerInteractEntityEvent) {
        if (event.hand == EquipmentSlot.HAND) {
            AfkHelper.recordActivity(event.player)
        }
    }

    // AsyncChatEvent fires off the main thread. Update lastActivity directly (ConcurrentHashMap is
    // thread-safe), and only dispatch to the main thread when the player was actually AFK, to
    // avoid the overhead of a scheduled task on every chat message.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        AfkHelper.recordActivityTimestamp(player)
        if (AfkHelper.isAfk(player)) {
            Bukkit.getScheduler().runTask(plugin, Runnable { AfkHelper.clearAfkState(player) })
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        // Don't count /afk (with or without args) as activity — it's handled directly in the command
        val cmd = event.message.trim()
        if (!cmd.equals(AFK_COMMAND, ignoreCase = true) && !cmd.startsWith("$AFK_COMMAND ", ignoreCase = true)) {
            AfkHelper.recordActivity(event.player)
        }
    }
}

