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

/**
 * @param pctChanceToRoll How likely this item is to be rolled as a percentage (Int 0-100)
 * @param amountRange How many of the item should be rolled
 * @param itemStack The actual itemStack of a single bag item
 */
@Suppress("unstableApiUsage")
enum class BagItem(val pctChanceToRoll: Int, val amountRange: IntRange, val itemStack: ItemStack) {
    /** Items related to the Ender Dragon **/
    DRAGON_EGG(25, 1..1,
        ItemStack(Material.DRAGON_EGG)
    ),
    DRAGON_HEAD(10, 1..1,
        ItemStack(Material.DRAGON_HEAD)
    ),
    DRAGON_ELYTRA(100, 1..1,
        ItemStack(Material.ELYTRA).apply {
            val subRarity = SubRarity.getRandomSubRarity()
            val elytraMeta = this.itemMeta
            elytraMeta.displayName(allTags.deserialize("${if (subRarity == SHADOW) "<#0><shadow:${EPIC.colorHex}>" else "<${EPIC.colorHex}>"}${if (subRarity == OBFUSCATED) "<font:alt>" else ""}${PlainTextComponentSerializer.plainText().serialize(this.effectiveName())}").decoration(TextDecoration.ITALIC, false))
            val elytraLore = mutableListOf<String>()
            elytraLore += "<reset><!i><white>${EPIC.rarityGlyph}${if (subRarity != NONE) subRarity.subRarityGlyph else ""}${ItemType.ARMOR.typeGlyph}"
            elytraMeta.lore(
                elytraLore.map { allTags.deserialize(it) }
            )
            when(subRarity) {
                SHINY -> {
                    elytraMeta.setEnchantmentGlintOverride(true)
                    elytraMeta.itemModel = NamespacedKey("cloudie", "elytra_shiny")
                    val equippable = Equippable.equippable(EquipmentSlot.CHEST).assetId(Key.key("cloudie:elytra_shiny")).build()
                    this.setData(DataComponentTypes.EQUIPPABLE, equippable)
                }
                SHADOW -> {
                    elytraMeta.itemModel = NamespacedKey("cloudie", "elytra_shadow")
                    val equippable = Equippable.equippable(EquipmentSlot.CHEST).assetId(Key.key("cloudie:elytra_shadow")).build()
                    this.setData(DataComponentTypes.EQUIPPABLE, equippable)
                }
                OBFUSCATED -> {
                    elytraMeta.itemModel = NamespacedKey("cloudie", "elytra_obf")
                    val equippable = Equippable.equippable(EquipmentSlot.CHEST).assetId(Key.key("cloudie:elytra_obf")).build()
                    this.setData(DataComponentTypes.EQUIPPABLE, equippable)
                }
                else -> {}
            }
            elytraMeta.persistentDataContainer.set(GENERIC_RARITY, STRING, EPIC.rarityName.uppercase())
            elytraMeta.persistentDataContainer.set(GENERIC_SUB_RARITY, STRING, subRarity.name.uppercase())
            this.itemMeta = elytraMeta
        }
    )
}