package event

import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import library.VanishHelper
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class ServerListEvent: Listener {
    @EventHandler
    private fun onServerPing(e: PaperServerListPingEvent) {
        e.version = "Cloudie v${Bukkit.getMinecraftVersion()}"
        e.listedPlayers.removeIf { listed ->
            val id = listed.id()
            VanishHelper.isVanished(id)
        }
        e.numPlayers = Bukkit.getOnlinePlayers().size - VanishHelper.vanishedOnlineCount()
    }
}