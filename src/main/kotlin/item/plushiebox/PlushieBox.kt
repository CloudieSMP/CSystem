package item.plushiebox

import chat.Formatting.allTags
import io.papermc.paper.datacomponent.DataComponentTypes
import item.crate.CrateItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.persistence.PersistentDataType
import util.Keys

/**
 * The physical Plushie Box item. Stores crate collectibles (plushies, wearables, etc.)
 * in its PDC as serialized ItemStacks. Opening the box via right-click shows a
 * paginated, filterable GUI (PlushieBoxWindow) rather than the vanilla bundle UI.
 */
@Suppress("UnstableApiUsage")
object PlushieBox {
    const val MAX_CAPACITY = 256

    fun create(): ItemStack {
        return ItemStack(Material.PAPER).apply {
            setData(DataComponentTypes.ITEM_MODEL, NamespacedKey("cloudie", "plushie_box"))
            editMeta { meta ->
                meta.displayName(
                    Component.text("Plushie Box")
                        .color(TextColor.color(0xC45889))
                        .decoration(TextDecoration.ITALIC, false)
                        .decoration(TextDecoration.BOLD, true)
                )
                updateLore(meta, 0)
                meta.persistentDataContainer.set(
                    Keys.PLUSHIE_BOX_ITEMS,
                    PersistentDataType.LIST.listTypeFrom(PersistentDataType.BYTE_ARRAY),
                    emptyList(),
                )
            }
        }
    }

    fun isPlushieBox(item: ItemStack?): Boolean {
        if (item == null || item.isEmpty) return false
        return item.itemMeta?.persistentDataContainer?.has(Keys.PLUSHIE_BOX_ITEMS) == true
    }

    /** Returns true if the item is a crate collectible (plushie, wearable, etc.) */
    fun isCrateCollectible(item: ItemStack?): Boolean {
        if (item == null || item.isEmpty) return false
        return CrateItem.resolve(item) != null
    }

    fun readPlushies(item: ItemStack): List<ItemStack> {
        val bytes = item.itemMeta
            ?.persistentDataContainer
            ?.get(Keys.PLUSHIE_BOX_ITEMS, PersistentDataType.LIST.listTypeFrom(PersistentDataType.BYTE_ARRAY))
            ?: return emptyList()
        return bytes.mapNotNull { runCatching { ItemStack.deserializeBytes(it) }.getOrNull() }
    }

    fun savePlushies(player: Player, slot: EquipmentSlot, plushies: List<ItemStack>) {
        val currentItem = when (slot) {
            EquipmentSlot.HAND -> player.inventory.itemInMainHand
            EquipmentSlot.OFF_HAND -> player.inventory.itemInOffHand
            else -> return
        }

        val updated = currentItem.clone()
        updated.editMeta { meta ->
            meta.persistentDataContainer.set(
                Keys.PLUSHIE_BOX_ITEMS,
                PersistentDataType.LIST.listTypeFrom(PersistentDataType.BYTE_ARRAY),
                plushies.map { it.serializeAsBytes() },
            )
            updateLore(meta, plushies.size)
        }

        when (slot) {
            EquipmentSlot.HAND -> player.inventory.setItemInMainHand(updated)
            EquipmentSlot.OFF_HAND -> player.inventory.setItemInOffHand(updated)
        }
    }

    private fun updateLore(meta: org.bukkit.inventory.meta.ItemMeta, count: Int) {
        meta.lore(
            listOf(
                allTags.deserialize("<!i><gray>Collectibles: <white>$count<gray>/$MAX_CAPACITY"),
                allTags.deserialize("<!i><dark_gray>Right-click to browse your collection"),
            )
        )
    }

    /**
     * Registers a shapeless crafting recipe:
     *   any bundle (any colour) + any wood plank → Plushie Box
     */
    fun registerRecipe() {
        val recipe = ShapelessRecipe(NamespacedKey("cloudie", "plushie_box"), create())

        // Use the #minecraft:bundles item tag when available; fall back to plain BUNDLE
        val bundleTag = Bukkit.getTag(Tag.REGISTRY_ITEMS, NamespacedKey.minecraft("bundles"), Material::class.java)
        if (bundleTag != null) {
            recipe.addIngredient(RecipeChoice.MaterialChoice(bundleTag))
        } else {
            recipe.addIngredient(Material.BUNDLE)
        }

        recipe.addIngredient(RecipeChoice.MaterialChoice(Tag.WOOL))
        Bukkit.addRecipe(recipe)
    }

    fun discoverRecipe(player: Player) {
        player.discoverRecipe(NamespacedKey("cloudie", "plushie_box"))
    }
}