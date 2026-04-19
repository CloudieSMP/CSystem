package util.ui

import chat.Formatting.allTags
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

object TrashWindow : Listener {
    private val title = allTags.deserialize("<gray>🗑 <dark_gray><bold>Trash</bold></dark_gray>")
    private val openInventories: MutableSet<Inventory> =
        Collections.newSetFromMap(ConcurrentHashMap())

    fun open(player: Player) {
        val inventory = Bukkit.createInventory(null, 27, title)
        openInventories.add(inventory)
        player.openInventory(inventory)
    }

    @EventHandler
    fun onClose(e: InventoryCloseEvent) {
        if (!openInventories.remove(e.inventory)) return
        e.inventory.clear()
    }
}
