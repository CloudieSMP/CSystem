package event

import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class ServerListEvent: Listener {
    @EventHandler
    private fun onServerPing(e: PaperServerListPingEvent) {
        e.version = "Cloudie v${Bukkit.getMinecraftVersion()}"
    }
}