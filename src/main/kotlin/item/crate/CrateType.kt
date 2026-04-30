package item.crate

import chat.Formatting.allTags
import item.ItemRarity
import item.ItemRarity.*
import item.ItemType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.Color

/**
 * Crates
 * @param crateName The display name of the crate
 * @param crateDescription The description of the crate
 * @param crateNameColor The color of the crate's name
 * @param crateMaterial The item model path used for this crate
 * @param recipeIngredient The material used in the crafting recipe for this crate
 * @param lootPool The loot pool to pull from when generating crate rewards
 */
enum class CrateType(
    private val crateName: String,
    private val crateDescription: String,
    val crateNameColor: String,
    val crateMaterial: String,
    val recipeIngredient: Material,
    val lootPool: CrateLootPool,
) {
    MASTER(
        "Master Crate",
        "A crate containing everything",
    "#FFAA00",
        "crates/gold",
        Material.YELLOW_WOOL,
        CrateLootPool.MASTER,
    ),
    PLUSHIE(
        "Plushie Crate",
        "A crate containing plushies",
        "#FFFFFF",
        "crates/blue_white",
        Material.WHITE_WOOL,
        CrateLootPool.PLUSHIE,
    ),
    BABY(
        "Baby Crate",
        "A crate containing baby plushies",
        "#AAAAAA",
        "crates/light_gray",
        Material.LIGHT_GRAY_WOOL,
        CrateLootPool.BABY,
    ),
    WEARABLES(
        "Wearables Crate",
        "A crate containing wearables",
        "#5555FF",
        "crates/blue",
        Material.BLUE_WOOL,
        CrateLootPool.WEARABLES,
    ),
    PLAYER(
        "Player Crate",
        "A crate containing player plushies",
        "#AA0000",
        "crates/red",
        Material.RED_WOOL,
        CrateLootPool.PLAYER,
    ),
    CHARACTER(
        "Character Crate",
        "A crate containing character plushies",
        "#2A2A2A",
        "crates/black",
        Material.BLACK_WOOL,
        CrateLootPool.CHARACTER,
    ),
    SABINE(
        "Sabine Crate",
        "A crate containing Sabine plushies",
        "#FF55FF",
        "crates/sabine",
        Material.PINK_WOOL,
        CrateLootPool.SABINE_LOOTPOOL,
    ),
    COOKIE(
        "Cookie Crate",
        "A crate containing Cookie plushies",
        "#55FFFF",
        "crates/cookie",
        Material.LIGHT_BLUE_WOOL,
        CrateLootPool.COOKIE_LOOTPOOL,
    );

    val displayName: Component
        get() = Component.text(crateName)
            .color(TextColor.fromHexString(crateNameColor))
            .decoration(TextDecoration.ITALIC, false)
            .decoration(TextDecoration.BOLD, true)

    val loreLines: List<Component>
        get() = listOf(
            allTags.deserialize("<reset><!i><white>${ItemType.CONSUMABLE.typeGlyph}"),
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