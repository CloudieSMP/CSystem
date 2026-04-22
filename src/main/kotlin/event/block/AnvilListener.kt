package event.block

import io.papermc.paper.datacomponent.DataComponentTypes
import item.crate.CrateItem
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.inventory.meta.Repairable
import org.bukkit.persistence.PersistentDataType.STRING
import util.Keys.CRATE_ITEM
import util.Keys.HELMET_ORIGINAL_MODEL

@Suppress("UnstableApiUsage")
class AnvilListener : Listener {

    @EventHandler
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        handleCosmetic(event)
        handleSweepingEdgeOnHoe(event)
    }

    /**
     * Overlays a plushie / wearable [CrateItem] cosmetic onto a helmet.
     *
     * Slot 0 → any helmet (without an existing cosmetic)
     * Slot 1 → any [CrateItem]
     * Output → helmet with the cosmetic's ITEM_MODEL applied; all cosmetic PDC keys copied flat.
     *
     * Reversal: `/stripcosmetic`
     */
    private fun handleCosmetic(event: PrepareAnvilEvent) {
        val helmet = event.inventory.getItem(0) ?: return
        if (!isHelmet(helmet.type)) return

        val cosmeticStack = event.inventory.getItem(1) ?: return
        val crateItem = CrateItem.resolve(cosmeticStack) ?: return

        // Block if a cosmetic is already applied (CRATE_ITEM on a helmet = cosmetified)
        if (helmet.itemMeta?.persistentDataContainer?.has(CRATE_ITEM, STRING) == true) return

        val result = helmet.clone()

        // Read ITEM_MODEL BEFORE editMeta to avoid mid-transaction key construction issues
        val originalModel = result.getData(DataComponentTypes.ITEM_MODEL)
        val cosmeticModel = cosmeticStack.getData(DataComponentTypes.ITEM_MODEL)
            ?: NamespacedKey("cloudie", crateItem.storedId.lowercase())

        result.editMeta { meta ->
            val pdc = meta.persistentDataContainer

            if (originalModel != null) {
                pdc.set(HELMET_ORIGINAL_MODEL, STRING, originalModel.asString())
            }

            // Copy all cosmetic PDC keys flat onto the helmet — CRATE_ITEM acts as the cosmetic identifier
            cosmeticStack.itemMeta?.persistentDataContainer?.copyTo(pdc, true)
        }

        result.setData(DataComponentTypes.ITEM_MODEL, cosmeticModel)

        event.result = result
        event.view.repairCost = 0
        event.view.repairItemCountCost = 1
        event.view.maximumRepairCost = maxOf(event.view.maximumRepairCost, 0)
    }

    /**
     * Allows Sweeping Edge to be applied to hoes via an anvil.
     * Vanilla forbids this combination; we intercept [PrepareAnvilEvent] and force the result.
     */
    private fun handleSweepingEdgeOnHoe(event: PrepareAnvilEvent) {
        val base = event.inventory.getItem(0) ?: return
        if (base.type !in HarvestReplantListener.HOE_MATERIALS) return

        val addition = event.inventory.getItem(1) ?: return
        val bookMeta = addition.itemMeta as? EnchantmentStorageMeta ?: return

        val sweepingLevel = bookMeta.getStoredEnchantLevel(Enchantment.SWEEPING_EDGE)
        if (sweepingLevel == 0) return

        // Start with whatever result vanilla already computed, or fall back to a clone of the base item.
        val result = event.result?.clone() ?: base.clone()

        val existingLevel = base.getEnchantmentLevel(Enchantment.SWEEPING_EDGE)
        val newLevel = if (existingLevel == sweepingLevel) {
            minOf(existingLevel + 1, Enchantment.SWEEPING_EDGE.maxLevel)
        } else {
            maxOf(existingLevel, sweepingLevel)
        }

        result.addUnsafeEnchantment(Enchantment.SWEEPING_EDGE, newLevel)

        val currentCost = event.view.repairCost
        val extraCost = when (existingLevel) {
            0 -> sweepingLevel * 2
            sweepingLevel -> newLevel * 2
            else -> (newLevel - existingLevel).coerceAtLeast(1) * 2
        }
        val totalCost = maxOf(1, currentCost + extraCost)

        event.view.repairCost = totalCost
        event.view.repairItemCountCost = 1
        event.view.maximumRepairCost = maxOf(event.view.maximumRepairCost, totalCost)

        result.editMeta { meta ->
            val repairableMeta = meta as? Repairable ?: return@editMeta
            val prior = (base.itemMeta as? Repairable)?.repairCost ?: 0
            repairableMeta.repairCost = prior * 2 + 1
        }

        event.result = result
    }

    companion object {
        private val HELMET_MATERIALS = setOf(
            Material.LEATHER_HELMET,
            Material.CHAINMAIL_HELMET,
            Material.IRON_HELMET,
            Material.GOLDEN_HELMET,
            Material.DIAMOND_HELMET,
            Material.NETHERITE_HELMET,
            Material.TURTLE_HELMET,
        )

        fun isHelmet(material: Material): Boolean = material in HELMET_MATERIALS
    }
}

