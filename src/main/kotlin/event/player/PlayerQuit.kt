package event.player

import chat.Formatting
import library.LiveHelper
import library.NoSleepHelper
import library.Translation

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class PlayerQuit: Listener {
    @EventHandler
    private fun onQuit(e: PlayerQuitEvent) {
        if(e.player.hasPermission("cloudie.silent.quit")) {
            e.quitMessage(null)
        } else {
            e.quitMessage(Formatting.allTags.deserialize(Translation.PlayerMessages.QUIT.replace("%player%", e.player.name)))

        }
        LiveHelper.onPlayerQuit(e.player)
        NoSleepHelper.cleanup(e.player)
    }
}