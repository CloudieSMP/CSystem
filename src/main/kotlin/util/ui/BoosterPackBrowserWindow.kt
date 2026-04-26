package util.ui

import chat.Formatting.allTags
import item.booster.BoosterPack
import item.booster.BoosterPreviewEntry
import item.booster.BoosterType
import item.booster.Cards
import item.SubRarity
import item.ItemType
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.entity.Player
import java.util.Locale

object BoosterPackBrowserWindow {
    private val selectorTitle = allTags.deserialize("<gradient:#DF6F69:#823BC6><bold>Booster Packs</bold></gradient>")

    fun openSelector(player: Player) {
        CollectionBrowserWindow.openSelector(
            player = player,
            title = selectorTitle,
            entries = BoosterType.entries,
            itemForEntry = { boosterType -> BoosterPack.create(boosterType).clone() },
            onEntryClick = { clicker, boosterType -> openPreview(clicker, boosterType) },
        )
    }

    private fun openPreview(player: Player, boosterType: BoosterType) {
        CollectionBrowserWindow.openPreview(
            player = player,
            title = boosterType.displayName,
            entries = Cards.previewEntries(boosterType),
            backButtonName = "← Back to Boosters",
            itemForEntry = { entry -> createPreviewCard(entry) },
            onBackClick = { clicker -> openSelector(clicker) },
        )
    }

    private fun createPreviewCard(entry: BoosterPreviewEntry): ItemStack {
        val rarity = entry.rarity.itemRarity
        val card = entry.definition
        val cardChanceText = String.format(Locale.US, "%.2f", entry.cardChancePercent)
        val rarityChanceText = String.format(Locale.US, "%.2f", entry.rarityChancePercent)

        return ItemStack(Material.PAPER).apply {
            editMeta { meta ->
                meta.displayName(allTags.deserialize("<!i><${rarity.colorHex}>${card.displayName}"))
                meta.lore(listOf(
                    allTags.deserialize("<!i><white>${rarity.rarityGlyph}${ItemType.CARD.typeGlyph} <gray>${entry.rarity.name.lowercase().replaceFirstChar { it.uppercase() }}"),
                    allTags.deserialize("<!i><gray>Card chance: <white>$cardChanceText%"),
                    allTags.deserialize("<!i><gray>Rarity bucket chance: <white>$rarityChanceText%"),
                ))
                @Suppress("DEPRECATION")
                meta.setCustomModelData(card.modelDataFor(SubRarity.NONE))
            }
        }
    }
}
