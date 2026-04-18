package item.crate

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Consumable
import io.papermc.paper.datacomponent.item.FoodProperties
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import util.Keys
import util.Sounds.SILENT

@Suppress("unstableApiUsage")
object Crate {
    fun create(type: CrateType, amount: Int = 1): ItemStack {
        return ItemStack(Material.PAPER, amount).apply {
            applyMetadata(this, type)
        }
    }

    fun refresh(item: ItemStack?): ItemStack? {
        if (item == null || item.isEmpty) return null

        val storedId = item.itemMeta
            ?.persistentDataContainer
            ?.get(Keys.CRATE_TYPE, PersistentDataType.STRING)
            ?: return null
        val type = CrateType.fromStoredId(storedId) ?: return null
        val refreshed = create(type, item.amount)

        return if (item.type == refreshed.type && item.itemMeta == refreshed.itemMeta) {
            null
        } else {
            refreshed
        }
    }

    private fun applyMetadata(itemStack: ItemStack, type: CrateType) {
        itemStack.editMeta { meta ->
            meta.displayName(type.displayName)
            meta.lore(type.loreLines)
            meta.persistentDataContainer.set(Keys.CRATE_TYPE, PersistentDataType.STRING, type.storedId)
            meta.persistentDataContainer.set(Keys.GENERIC_RARITY, PersistentDataType.STRING, type.crateRarity.name)
        }
        itemStack.setData(DataComponentTypes.FOOD, FoodProperties.food().nutrition(0).saturation(0f).canAlwaysEat(true).build())
        itemStack.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable().consumeSeconds(1f).hasConsumeParticles(false).sound(SILENT.name()).build())
        itemStack.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey("cloudie", type.crateMaterial))
    }
}