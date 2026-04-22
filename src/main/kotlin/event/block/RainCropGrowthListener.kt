package event.block

import org.bukkit.block.data.Ageable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockGrowEvent
import plugin
import kotlin.random.Random

class RainCropGrowthListener : Listener {

    /**
     * Returns true when there are no blocks above this block in its column,
     * meaning it is genuinely exposed to the open sky.
     */
    private fun isExposedToOpenSky(block: org.bukkit.block.Block): Boolean {
        return block.world.getHighestBlockYAt(block.x, block.z) <= block.y
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockGrow(event: BlockGrowEvent) {
        val block = event.block
        val world = block.world

        // Only boost growth during rain.
        if (!world.hasStorm()) return

        // Only apply to Ageable crops (wheat, carrots, potatoes, etc.).
        val newData = event.newState.blockData as? Ageable ?: return

        // No extra tick needed if the crop is already reaching max age.
        if (newData.age >= newData.maximumAge) return

        // The block must be exposed to open sky for rain to reach it.
        if (!isExposedToOpenSky(block)) return

        // Apply a configurable chance for the bonus growth tick.
        if (Random.nextDouble() >= plugin.config.rainCropGrowth.boostChance) return

        // Capture coordinates before the scheduler call to avoid extra allocations.
        val x = block.x
        val y = block.y
        val z = block.z

        // Schedule the bonus growth one tick after the primary growth resolves.
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            val current = world.getBlockAt(x, y, z)
            val currentData = current.blockData as? Ageable ?: return@Runnable
            if (currentData.age < currentData.maximumAge) {
                currentData.age = currentData.age + 1
                current.blockData = currentData
            }
        }, 1L)
    }
}
