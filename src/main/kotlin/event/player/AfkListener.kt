package event.player

import library.AfkHelper
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent

class AfkListener : Listener {

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
}

