package event.player

import library.AfkHelper
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInputEvent
import org.bukkit.event.player.PlayerJoinEvent
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

    @EventHandler(priority = EventPriority.MONITOR)
    fun onInput(event: PlayerInputEvent) {
        val input = event.input
        // Only count deliberate key presses — knockback and forced teleports never trigger this
        if (input.isForward || input.isBackward || input.isLeft || input.isRight ||
            input.isJump || input.isSneak || input.isSprint) {
            AfkHelper.recordActivity(event.player)
        }
    }
}

