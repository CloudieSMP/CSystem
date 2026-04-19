package util.ui

import chat.Formatting.allTags
import item.crate.Crate
import item.crate.CrateType
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.math.RoundingMode
import java.util.Locale

object CrateBrowserWindow {
    private val fillerPane = ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
    private val selectorTitle = allTags.deserialize("<gradient:#DF6F69:#823BC6><bold>Crates</bold></gradient>")
    private val backButton = ItemStack(Material.ARROW).apply {
        editMeta { meta ->
            meta.displayName(allTags.deserialize("<yellow><bold>Back to Crates"))
        }
    }

    private fun formatChancePercent(itemEffectiveWeight: Double, totalEffectiveWeight: Double): String {
        if (itemEffectiveWeight <= 0.0 || totalEffectiveWeight <= 0.0) return "0.00"
        val percent = itemEffectiveWeight * 100 / totalEffectiveWeight
        val rounded = percent.toBigDecimal().setScale(2, RoundingMode.HALF_UP)
        if (rounded.signum() == 0) {
            val precise = percent.toBigDecimal().setScale(4, RoundingMode.HALF_UP).stripTrailingZeros()
            return precise.toPlainString()
        }
        return rounded.stripTrailingZeros().toPlainString()
    }

    fun openSelector(player: Player) {
        val crateTypes = CrateType.entries

        CollectionBrowserWindow.openSelector(
            player = player,
            title = selectorTitle,
            entries = crateTypes,
            fillerPane = fillerPane,
            itemForEntry = { crateType ->
                Crate.create(crateType).clone().apply {
                    editMeta { meta ->
                        val ingredientName = crateType.recipeIngredient.name
                            .lowercase(Locale.US)
                            .split('_')
                            .joinToString(" ") { part -> part.replaceFirstChar(Char::uppercaseChar) }
                        val updatedLore = (meta.lore() ?: emptyList()) +
                            allTags.deserialize("<!i><gray>Ingredient: <white>$ingredientName")
                        meta.lore(updatedLore)
                    }
                }
            },
            onEntryClick = { clicker, crateType -> openLootPreview(clicker, crateType) },
        )
    }

    private fun openLootPreview(player: Player, crateType: CrateType) {
        val loot = crateType.lootPool.possibleItems
        val totalEffectiveWeight = loot.sumOf { it.effectiveChanceWeight }

        CollectionBrowserWindow.openPreview(
            player = player,
            title = crateType.displayName,
            entries = loot,
            fillerPane = fillerPane,
            backButton = backButton,
            itemForEntry = { crateItem ->
                val chancePercentText = formatChancePercent(crateItem.effectiveChanceWeight, totalEffectiveWeight)

                crateItem.createItemStack().apply {
                    editMeta { meta ->
                        val updatedLore = (meta.lore() ?: emptyList()) +
                            allTags.deserialize("<!i><gray>Chance: <white>$chancePercentText%")
                        meta.lore(updatedLore)
                    }
                }
            },
            onBackClick = { clicker -> openSelector(clicker) },
        )
    }
}
