package event.player

import chat.Formatting.allTags
import item.plushiebox.PlushieBox
import org.bukkit.entity.Player
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
import plugin
import util.Sounds
import util.ui.PlushieBoxWindow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles right-click interaction with the Plushie Box item and protects it
 * from being moved or dropped while its GUI is open — mirroring the same
 * safety guards used by BinderInteract.
 *
 * Also handles shift-clicking crate collectibles from the player's own
 * inventory directly into the box while the GUI is open.
 */
class PlushieBoxInteract : Listener {
    /** Maps UUID → the equipment slot holding the box while the GUI is open. */
    private val openBoxes: MutableMap<UUID, EquipmentSlot> = ConcurrentHashMap()

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
        openBoxes[player.uniqueId] = hand
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
        val player = event.whoClicked as? Player ?: return
        val slot = openBoxes[player.uniqueId] ?: return

        // Shift-click a collectible from the player's own inventory → insert into box
        if (event.isShiftClick && event.clickedInventory == player.inventory) {
            val item = event.currentItem
            if (item != null && !item.isEmpty && PlushieBox.isCrateCollectible(item)) {
                handleShiftInsert(player, slot, event)
                return
            }
        }

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

    private fun handleShiftInsert(player: Player, slot: EquipmentSlot, event: InventoryClickEvent) {
        val item = event.currentItem ?: return
        event.isCancelled = true

        val boxItem = when (slot) {
            EquipmentSlot.HAND -> player.inventory.itemInMainHand
            EquipmentSlot.OFF_HAND -> player.inventory.itemInOffHand
            else -> return
        }

        val plushies = PlushieBox.readPlushies(boxItem).toMutableList()
        var remaining = item.amount

        // Stack into existing similar entries first
        for (existing in plushies) {
            if (remaining <= 0) break
            if (existing.isSimilar(item) && existing.amount < existing.maxStackSize) {
                val canAdd = minOf(remaining, existing.maxStackSize - existing.amount)
                existing.amount += canAdd
                remaining -= canAdd
            }
        }

        // Add remaining as new list entries up to capacity
        while (remaining > 0 && plushies.size < PlushieBox.MAX_CAPACITY) {
            val toAdd = minOf(remaining, item.maxStackSize)
            plushies.add(item.clone().apply { amount = toAdd })
            remaining -= toAdd
        }

        // Update the player's inventory slot to reflect consumed items
        event.currentItem = if (remaining > 0) item.clone().apply { amount = remaining } else null

        PlushieBox.savePlushies(player, slot, plushies)

        if (remaining > 0) {
            player.sendMessage(allTags.deserialize("<red>Plushie Box is full! (${PlushieBox.MAX_CAPACITY} max)"))
            player.playSound(Sounds.INTERFACE_ERROR)
        } else {
            player.playSound(Sounds.INTERFACE_INTERACT)
        }

        // Close and reopen the GUI on the next tick so the content grid reflects the new items
        player.closeInventory()
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            if (player.isOnline) {
                openBoxes[player.uniqueId] = slot
                PlushieBoxWindow.open(player, slot)
            }
        }, 1L)
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        openBoxes.remove(player.uniqueId)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        openBoxes.remove(event.player.uniqueId)
        PlushieBoxWindow.clearFilter(event.player.uniqueId)
    }
}