package item.crate

import chat.Formatting.allTags
import item.ItemRarity
import item.ItemRarity.*
import item.ItemType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material

/**
 * Crates
 * @param crateName The display name of the crate
 * @param crateDescription The description of the crate
 * @param crateRarity The rarity of the crate, which determines the color of the crate's name and lore
 * @param crateMaterial The item model path used for this crate
 * @param lootPool The loot pool to pull from when generating crate rewards
 */
enum class CrateType(
    private val crateName: String,
    private val crateDescription: String,
    val crateRarity: ItemRarity,
    val crateMaterial: String,
    val recipeIngredient: Material,
    val lootPool: CrateLootPool,
) {
    MASTER(
        "Master Crate",
        "A crate containing everything",
        LEGENDARY,
        "crates/gold",
        Material.YELLOW_WOOL,
        CrateLootPool.MASTER,
    ),
    PLUSHIE(
        "Plushie Crate",
        "A crate containing plushies",
        COMMON,
        "crates/blue",
        Material.WHITE_WOOL,
        CrateLootPool.PLUSHIE,
    ),
    WEARABLES(
        "Wearables Crate",
        "A crate containing wearables",
        COMMON,
        "crates/default",
        Material.BLUE_WOOL,
        CrateLootPool.WEARABLES,
    ),
    PLAYER(
        "Player Crate",
        "A crate containing player plushies",
        UNCOMMON,
        "crates/default",
        Material.RED_WOOL,
        CrateLootPool.PLAYER,
    ),
    CHARACTER(
        "Character Crate",
        "A crate containing character plushies",
        RARE,
        "crates/default",
        Material.BLACK_WOOL,
        CrateLootPool.CHARACTER,
    );

    val displayName: Component
        get() = Component.text(crateName)
            .color(TextColor.color(crateRarity.color.asRGB()))
            .decoration(TextDecoration.ITALIC, false)
            .decoration(TextDecoration.BOLD, true)

    val loreLines: List<Component>
        get() = listOf(
            allTags.deserialize("<reset><!i><white>${crateRarity.rarityGlyph}${ItemType.CONSUMABLE.typeGlyph}"),
            Component.text(crateDescription)
                .color(TextColor.color(0xFFFF55))
                .decoration(TextDecoration.ITALIC, false),
        )

    val storedId: String
        get() = name

    val recipeKey: String
        get() = "${name.lowercase()}_crate"

    companion object {
        fun fromStoredId(storedId: String?): CrateType? {
            return entries.firstOrNull { it.storedId == storedId }
        }
    }
}