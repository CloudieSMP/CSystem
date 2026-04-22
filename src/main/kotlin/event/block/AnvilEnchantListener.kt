package event.block

import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.inventory.meta.Repairable

class AnvilEnchantListener : Listener {

    /**
     * Allows Sweeping Edge to be applied to hoes via an anvil.
     * Vanilla forbids this combination; we intercept [PrepareAnvilEvent] and force the result.
     */
    @EventHandler
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        val base = event.inventory.getItem(0) ?: return
        if (base.type !in HarvestReplantListener.HOE_MATERIALS) return

        val addition = event.inventory.getItem(1) ?: return
        val bookMeta = addition.itemMeta as? EnchantmentStorageMeta ?: return

        val sweepingLevel = bookMeta.getStoredEnchantLevel(Enchantment.SWEEPING_EDGE)
        if (sweepingLevel == 0) return

        // Start with whatever result vanilla already computed (other valid enchants may be present),
        // or fall back to a clone of the base item if vanilla rejected the combination entirely.
        val result = event.result?.clone() ?: base.clone()

        val existingLevel = base.getEnchantmentLevel(Enchantment.SWEEPING_EDGE)
        val newLevel = if (existingLevel == sweepingLevel) {
            minOf(existingLevel + 1, Enchantment.SWEEPING_EDGE.maxLevel)
        } else {
            maxOf(existingLevel, sweepingLevel)
        }

        result.addUnsafeEnchantment(Enchantment.SWEEPING_EDGE, newLevel)

        // Keep survival pickup valid by ensuring the anvil has a non-zero level cost.
        // If vanilla already had a cost, we build on top of it; otherwise we provide one.
        val currentCost = event.view.repairCost
        val extraCost = when {
            existingLevel == 0 -> sweepingLevel * 2
            existingLevel == sweepingLevel -> newLevel * 2
            else -> (newLevel - existingLevel).coerceAtLeast(1) * 2
        }
        val totalCost = maxOf(1, currentCost + extraCost)

        event.view.repairCost = totalCost
        event.view.repairItemCountCost = 1
        event.view.maximumRepairCost = maxOf(event.view.maximumRepairCost, totalCost)

        // Vanilla increases the prior-work penalty each time an item is produced in an anvil.
        result.editMeta { meta ->
            val repairableMeta = meta as? Repairable ?: return@editMeta
            val prior = (base.itemMeta as? Repairable)?.getRepairCost() ?: 0
            repairableMeta.setRepairCost(prior * 2 + 1)
        }

        event.result = result
    }
}
