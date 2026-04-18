package item.treasurebag

import chat.Formatting.allTags
import io.papermc.paper.datacomponent.item.Equippable
import io.papermc.paper.datacomponent.DataComponentTypes
import item.ItemRarity.*
import item.ItemType
import item.SubRarity
import item.SubRarity.*
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType.*
import org.bukkit.NamespacedKey
import org.bukkit.inventory.*
import util.Keys.GENERIC_RARITY
import util.Keys.GENERIC_SUB_RARITY

private fun createBagItem(name: String, useDebugSubRarityOverride: Boolean = false): ItemStack {
    val bagItem = BagItem.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
        ?: throw IllegalArgumentException("Unknown bag item: $name")
    return bagItem.createItemStack(useDebugSubRarityOverride)
}

/**
 * @param pctChanceToRoll How likely this item is to be rolled as a percentage (Int 0-100)
 * @param amountRange How many of the item should be rolled
 * @param baseItemStack The base itemStack template for a single bag item
 */
@Suppress("unstableApiUsage")
enum class BagItem(val pctChanceToRoll: Int, val amountRange: IntRange, private val baseItemStack: ItemStack) {
    /** Items related to the Ender Dragon **/
    DRAGON_EGG(25, 1..1,
        ItemStack(Material.DRAGON_EGG)
    ),
    DRAGON_HEAD(10, 1..1,
        ItemStack(Material.DRAGON_HEAD)
    ),
    DRAGON_ELYTRA(100, 1..1, ItemStack(Material.ELYTRA));

    fun createItemStack(useDebugSubRarityOverride: Boolean = false): ItemStack {
        val item = baseItemStack.clone()
        if (this != DRAGON_ELYTRA) return item

        val subRarity = SubRarity.getRandomSubRarity(useDebugSubRarityOverride)
        val elytraMeta = item.itemMeta
        elytraMeta.displayName(
            allTags.deserialize(
                (if (subRarity == SHADOW) "<#0><shadow:${EPIC.colorHex}>" else "<${EPIC.colorHex}>") +
                        (if (subRarity == OBFUSCATED) "<font:alt>" else "") +
                    PlainTextComponentSerializer.plainText().serialize(item.effectiveName())
            ).decoration(TextDecoration.ITALIC, false)
        )
        elytraMeta.lore(
            listOf(allTags.deserialize("<reset><!i><white>${EPIC.rarityGlyph}${if (subRarity != NONE) subRarity.subRarityGlyph else ""}${ItemType.ARMOR.typeGlyph}"))
        )

        when (subRarity) {
            SHINY -> {
                elytraMeta.setEnchantmentGlintOverride(true)
                elytraMeta.itemModel = NamespacedKey("cloudie", "elytra_shiny")
                val equippable = Equippable.equippable(EquipmentSlot.CHEST).assetId(Key.key("cloudie:elytra_shiny")).build()
                item.setData(DataComponentTypes.EQUIPPABLE, equippable)
            }
            SHADOW -> {
                elytraMeta.itemModel = NamespacedKey("cloudie", "elytra_shadow")
                val equippable = Equippable.equippable(EquipmentSlot.CHEST).assetId(Key.key("cloudie:elytra_shadow")).build()
                item.setData(DataComponentTypes.EQUIPPABLE, equippable)
            }
            OBFUSCATED -> {
                elytraMeta.itemModel = NamespacedKey("cloudie", "elytra_obf")
                val equippable = Equippable.equippable(EquipmentSlot.CHEST).assetId(Key.key("cloudie:elytra_obf")).build()
                item.setData(DataComponentTypes.EQUIPPABLE, equippable)
            }
            else -> {}
        }

        elytraMeta.persistentDataContainer.set(GENERIC_RARITY, STRING, EPIC.rarityName.uppercase())
        elytraMeta.persistentDataContainer.set(GENERIC_SUB_RARITY, STRING, subRarity.name.uppercase())
        item.itemMeta = elytraMeta
        return item
    }
}