package event.player

import item.plushiebox.PlushieBox
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import util.ui.PlushieBoxWindow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles right-click interaction with the Plushie Box item and protects it
 * from being moved or dropped while its GUI is open — mirroring the same
 * safety guards used by BinderInteract.
 */
class PlushieBoxInteract : Listener {
    private val openBoxes: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    @EventHandler(priority = EventPriority.HIGH)
    fun onRightClick(event: PlayerInteractEvent) {
        val action = event.action
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return

        val hand = event.hand ?: return
        val player = event.player
        val item = when (hand) {
            EquipmentSlot.HAND -> player.inventory.itemInMainHand
            EquipmentSlot.OFF_HAND -> player.inventory.itemInOffHand
            else -> return
        }

        if (!PlushieBox.isPlushieBox(item)) return

        event.isCancelled = true
        openBoxes += player.uniqueId
        PlushieBoxWindow.open(player, hand)
    }

    @EventHandler(ignoreCancelled = true)
    fun onDrop(event: PlayerDropItemEvent) {
        if (event.player.uniqueId !in openBoxes) return
        if (PlushieBox.isPlushieBox(event.itemDrop.itemStack)) {
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? org.bukkit.entity.Player ?: return
        if (player.uniqueId !in openBoxes) return

        if (PlushieBox.isPlushieBox(event.currentItem) || PlushieBox.isPlushieBox(event.cursor)) {
            event.isCancelled = true
            return
        }

        // Block number-key swaps that would move the box out of a hotbar slot
        if (event.click == ClickType.NUMBER_KEY) {
            val hotbarSlot = event.hotbarButton
            if (hotbarSlot in 0..8 && PlushieBox.isPlushieBox(player.inventory.getItem(hotbarSlot))) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val player = event.player as? org.bukkit.entity.Player ?: return
        openBoxes.remove(player.uniqueId)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        openBoxes.remove(event.player.uniqueId)
    }
}

