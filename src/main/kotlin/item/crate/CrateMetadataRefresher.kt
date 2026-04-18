package item.crate

import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.event.inventory.InventoryType

object CrateMetadataRefresher {
    fun refreshPlayerInventories(player: Player): Int {
        return refreshInventory(player.inventory) + refreshInventory(player.enderChest)
    }

    fun refresh(item: ItemStack?): ItemStack? {
        return Crate.refresh(item) ?: CrateItem.refresh(item)
    }

    fun shouldRefreshTopInventory(inventory: Inventory): Boolean {
        return inventory.location != null && inventory.type in STORAGE_INVENTORY_TYPES
    }

    fun refreshInventory(inventory: Inventory): Int {
        var updatedSlots = 0

        for (slot in 0 until inventory.size) {
            val current = inventory.getItem(slot) ?: continue
            val refreshed = refresh(current) ?: continue
            inventory.setItem(slot, refreshed)
            updatedSlots++
        }

        return updatedSlots
    }

    private val STORAGE_INVENTORY_TYPES = setOf(
        InventoryType.CHEST,
        InventoryType.ENDER_CHEST,
        InventoryType.SHULKER_BOX,
        InventoryType.BARREL,
        InventoryType.HOPPER,
        InventoryType.DISPENSER,
        InventoryType.DROPPER,
    )
}

