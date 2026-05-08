package event.player

import chat.Formatting
import library.LiveHelper
import library.NoSleepHelper
import library.Translation
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import plugin

class PlayerQuit: Listener {
    @EventHandler
    private fun onQuit(e: PlayerQuitEvent) {
        if(e.player.hasPermission("cloudie.silent.quit")) {
            e.quitMessage(null)
        } else if (e.player.hasPermission("cloudie.group.ghost")) {
            e.quitMessage(null)
        } else {
            e.quitMessage(Formatting.allTags.deserialize(Translation.PlayerMessages.QUIT.replace("%player%", e.player.name)))
        }
        LiveHelper.onPlayerQuit(e.player)
        NoSleepHelper.cleanup(e.player)
        // Refresh tab list count after this player is fully removed (next tick)
        Bukkit.getScheduler().runTask(plugin, Runnable { PlayerJoin.refreshTabListForAll() })
    }
}