package event.block

import org.bukkit.Material
import org.bukkit.block.data.Ageable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

class HarvestReplantListener : Listener {

    companion object {
        /** Maps a fully-grown crop block material to the seed material used to replant it. */
        private val CROP_SEEDS = mapOf(
            Material.WHEAT to Material.WHEAT_SEEDS,
            Material.CARROTS to Material.CARROT,
            Material.POTATOES to Material.POTATO,
            Material.BEETROOTS to Material.BEETROOT_SEEDS,
            Material.TORCHFLOWER_CROP to Material.TORCHFLOWER_SEEDS,
            Material.NETHER_WART to Material.NETHER_WART,
        )
    }

    @EventHandler(ignoreCancelled = true)
    fun onRightClickCrop(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.hand != EquipmentSlot.HAND) return

        val block = event.clickedBlock ?: return
        val cropMaterial = block.type
        val seedMaterial = CROP_SEEDS[cropMaterial] ?: return

        val ageable = block.blockData as? Ageable ?: return
        if (ageable.age < ageable.maximumAge) return

        event.isCancelled = true

        // Collect natural drops, then try to consume one seed (the crop stays planted).
        val drops = block.getDrops().toMutableList()
        val seedDrop = drops.firstOrNull { it.type == seedMaterial }
        if (seedDrop != null) {
            // Seed available: consume it and reset the crop to age 0 (replant).
            if (seedDrop.amount > 1) seedDrop.amount -= 1 else drops.remove(seedDrop)
            ageable.age = 0
            block.blockData = ageable
        } else {
            // No seed in drops (e.g. unlucky wheat / beetroot roll): harvest only, leave farmland bare.
            block.type = Material.AIR
        }

        // Drop remaining items at the block location.
        val location = block.location.add(0.5, 0.5, 0.5)
        drops.forEach { block.world.dropItem(location, it) }
    }
}
