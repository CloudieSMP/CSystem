package event.player

import item.crate.CrateMetadataRefresher
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryOpenEvent

class InventoryOpenRefresh : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return
        val topInventory = event.view.topInventory
        if (CrateMetadataRefresher.shouldRefreshTopInventory(topInventory)) {
            CrateMetadataRefresher.refreshInventory(topInventory)
        }

        CrateMetadataRefresher.refreshInventory(player.inventory)
    }
}

