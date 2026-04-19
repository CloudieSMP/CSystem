package event.player

import event.block.HarvestReplantListener.harvestReplantEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.block.Action
import org.bukkit.inventory.EquipmentSlot


class PlayerInteract : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val hand = event.hand ?: return
        if (event.action == Action.RIGHT_CLICK_BLOCK && hand == EquipmentSlot.HAND) {
            harvestReplantEvent(event)
        }
    }
}
