package util.ui

import item.binder.BinderItem
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot

/** Opens the Card Binder GUI via the shared [StorageWindow] engine. */
object BinderWindow {

    fun open(player: Player, slot: EquipmentSlot) {
        val binderItem = when (slot) {
            EquipmentSlot.HAND -> player.inventory.itemInMainHand
            EquipmentSlot.OFF_HAND -> player.inventory.itemInOffHand
            else -> return
        }

        StorageWindow.open(
            player = player,
            title = "<gradient:#5b9df5:#a78bfa><bold>Card Binder</bold></gradient>",
            items = BinderItem.readCards(binderItem).toMutableList(),
            maxCapacity = BinderItem.MAX_CAPACITY,
            canInsert = { BinderItem.isCard(it) },
            onSave = { p, cards -> BinderItem.saveCards(p, slot, cards) },
            capacityLabel = "Cards",
            fullMessage = "Your binder is full! (${BinderItem.MAX_CAPACITY} cards max)",
            removeHint = "Click a card to take it out",
            insertHint = "Hold a card and click an empty slot to insert",
        )
    }
}
