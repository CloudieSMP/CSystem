package event.player

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class PlayerDeath : Listener {
    @EventHandler
    private fun onPostRespawn(e: PlayerPostRespawnEvent) {
        if (e.player.hasPermission("cloudie.group.admin")) {
            e.player.sendOpLevel(2)
        }
    }
}