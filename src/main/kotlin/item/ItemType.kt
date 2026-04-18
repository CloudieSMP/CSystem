package item

enum class ItemType(val typeName : String, val typeGlyph : String) {
    UTILITY("Utility", "\uE200"),
    CONSUMABLE("Consumable", "\uE201"),
    PLUSHIE("Plushie", "\uE202"),
    CARD("Card", "\uE203"),
    ARMOR("Armor", "\uE204")
}