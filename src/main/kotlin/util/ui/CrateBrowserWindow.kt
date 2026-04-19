package util.ui

import chat.Formatting.allTags
import item.crate.Crate
import item.crate.CrateType
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.math.BigDecimal
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

    private fun formatChancePercent(itemRollWeight: Int, totalRollWeight: Int): String {
        if (itemRollWeight <= 0 || totalRollWeight <= 0) return "0"

        val percent = BigDecimal.valueOf(itemRollWeight.toLong())
            .multiply(BigDecimal("100"))
            .divide(BigDecimal.valueOf(totalRollWeight.toLong()), 12, RoundingMode.HALF_UP)

        return percent.stripTrailingZeros().toPlainString()
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
        val totalRollWeight = loot.sumOf { it.rollWeight.coerceAtLeast(0) }

        CollectionBrowserWindow.openPreview(
            player = player,
            title = crateType.displayName,
            entries = loot,
            fillerPane = fillerPane,
            backButton = backButton,
            itemForEntry = { crateItem ->
                val itemRollWeight = crateItem.rollWeight.coerceAtLeast(0)
                val chancePercentText = formatChancePercent(itemRollWeight, totalRollWeight)

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
