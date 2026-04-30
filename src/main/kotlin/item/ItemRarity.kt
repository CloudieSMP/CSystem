package item

import org.bukkit.Color

enum class  ItemRarity(
    val rarityName: String,
    val rarityGlyph: String,
    val color: Color,
    val colorHex: String,
    val crateWeight: Double,
) {
    SPECIAL("Special", "\uE100", Color.fromRGB(236, 28, 36), "#ec1c24", 0.0),
    COMMON("Common", "\uE101", Color.fromRGB(255, 255, 255), "#ffffff", 47.7125),
    UNCOMMON("Uncommon", "\uE102", Color.fromRGB(14, 209, 69), "#0ed145", 34.0),
    RARE("Rare", "\uE103", Color.fromRGB(0, 168, 243), "#00a8f3", 12.0),
    EPIC("Epic", "\uE104", Color.fromRGB(184, 61, 186), "#b83dba", 5.0),
    LEGENDARY("Legendary", "\uE105", Color.fromRGB(255, 127, 39), "#ff7f27", 1.0),
    MYTHIC("Mythic", "\uE106", Color.fromRGB(255, 51, 116), "#ff3374", 0.2),
    UNREAL("Unreal", "\uE107", Color.fromRGB(134, 102, 230), "#8666e6", 0.05),
    TRANSCENDENT("Transcendent", "\uE108", Color.fromRGB(199, 10, 23), "#c70a17", 0.025),
    CELESTIAL("Celestial", "\uE109", Color.fromRGB(245, 186, 10), "#f5ba0a", 0.0125);
}