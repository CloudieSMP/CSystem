package item.crate

import chat.Formatting.allTags
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Equippable
import item.ItemRarity
import item.ItemRarity.*
import item.ItemType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType.STRING
import util.Keys.CRATE_ITEM
import util.Keys.CRATE_ROLL_CHANCE_PERCENT
import util.Keys.CRATE_ROLLED_BY
import util.Keys.GENERIC_RARITY
import java.math.RoundingMode

private fun createDisplayName(displayName: String, rarity: ItemRarity): Component {
    return Component.text(displayName)
        .color(TextColor.color(rarity.color.asRGB()))
        .decoration(TextDecoration.ITALIC, false)
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

private fun createLore(description: String, rarity: ItemRarity, chancePercent: String? = null): List<Component> {
    return buildList {
        add(allTags.deserialize("<!i><white>${rarity.rarityGlyph}${ItemType.PLUSHIE.typeGlyph}"))
        description.split("\n").forEach { line ->
            add(Component.text(line).decoration(TextDecoration.ITALIC, false))
        }
        if (chancePercent != null) {
            add(allTags.deserialize("<!i><gray>Chance: <white>$chancePercent%"))
        }
    }
}

enum class CrateItem(
    private val itemName: String,
    private val rarity: ItemRarity,
    private val itemDescription: String,
    private val modelPath: String,
) {
    DEFAULT("Default Item", CELESTIAL, "This item should never be obtained.\n(it can be though)\nGood luck getting it", "plushies/player/default_wide"),

    // Plushies
    PENGUIN("Penguin Plushie", COMMON, "A cute penguin plushie", "plushies/penguin"),
    MUSHROOM("Mushroom Plushie", COMMON, "A cute mushroom plushie", "plushies/mushroom"),
    BEE("Bee Plushie", COMMON, "A cute bee plushie", "plushies/bee"),
    STAR("Star Plushie", COMMON, "A cute star plushie", "plushies/star"),
    HEART("Heart Plushie", COMMON, "A cute heart plushie", "plushies/heart"),
    COFFEE_CUP("Coffee Cup Plushie", COMMON, "A cute coffee cup plushie", "plushies/coffee_cup"),
    RAMEN_BOWL("Ramen Bowl Plushie", COMMON, "A cute ramen bowl plushie", "plushies/ramen_bowl"),
    AXOLOTL_WILD("Axolotl Plushie (Wild)", COMMON, "A cute wild axolotl plushie", "plushies/axolotl_wild"),
    AXOLOTL_LUCY("Axolotl Plushie (Lucy)", COMMON, "A cute Lucy axolotl plushie", "plushies/axolotl_lucy"),
    AXOLOTL_GOLD("Axolotl Plushie (Gold)", COMMON, "A cute gold axolotl plushie", "plushies/axolotl_gold"),
    AXOLOTL_CYAN("Axolotl Plushie (Cyan)", COMMON, "A cute cyan axolotl plushie", "plushies/axolotl_cyan"),
    AXOLOTL_BLUE("Axolotl Plushie (Blue)", LEGENDARY, "A cute blue axolotl plushie", "plushies/axolotl_blue"),

    // Player plushies
    SEBIANN("Sebiann Plushie", COMMON, "A cute Sebiann plushie", "plushies/player/sebiann"),
    COOKIE("Cookie Plushie", COMMON, "A cute Cookie plushie", "plushies/player/cookie"),
    BEAUVER("Beauver Plushie", COMMON, "A cute Beauver plushie", "plushies/player/beauver"),
    CARSON("Carson Plushie", COMMON, "Some people's kids these days", "plushies/player/carson_wide"),
    LESHY("Leshy Plushie", COMMON, "Blob Blob", "plushies/player/leshy_wide"),
    MAI("Mai Plushie", COMMON, "A cute Mai plushie", "plushies/player/mai_cheerleader_slim"),
    MEGAN("Megan Plushie", COMMON, "A cute Megan plushie", "plushies/player/megan_cheerleader_slim"),
    RIVEN("Riven Plushie", COMMON, "MEOW", "plushies/player/riven_slim"),
    SABINE("Sabine Plushie", COMMON, "Just Cheering you on", "plushies/player/sabine_cheerleader_slim"),
    YANN("Yann Plushie", COMMON, "A cute Yann plushie", "plushies/player/yann_wide"),
    ROAST("Roast Plushie", COMMON, "A cute Roast plushie", "plushies/player/roast_wide"),
    TURTLE("Turtle Plushie", COMMON, "A cute Turtle plushie", "plushies/player/turtle_wide"),

    SEBIANN_CLASSIC("Sebiann Classic Plushie", LEGENDARY, "A classic Sebiann plushie\nA precious collector's item\nExtremely limited!", "plushies/player/sebiann_classic"),
    CARSON_GRAY("Carson Plushie (Gray)", LEGENDARY, "Some people's kids these days", "plushies/player/carson_gray_wide"),

    // Character plushies
    N("N Plushie", COMMON, "N from Pokemon", "plushies/character/n_slim"),
    ASTARION("Astarion Plushie", COMMON, "Careful darling, I bite.", "plushies/character/astarion_wide"),
    BATMAN("Batman Plushie", COMMON, "Batman from DC Comics", "plushies/character/batman_wide"),
    DAZAI("Dazai Plushie", COMMON, "Dazai from Bungou Stray Dogs", "plushies/character/dazai_slim"),
    LEVI("Levi Plushie", COMMON, "Levi from Attack on Titan", "plushies/character/levi_wide"),
    SPARROW("Sparrow Plushie", COMMON, "Sparrow from Identity V", "plushies/character/sparrow_wide"),
    SPIDERMAN("Spiderman Plushie", COMMON, "Spiderman from Marvel Comics", "plushies/character/spiderman_slim"),
    LEONARDO("Leonardo Plushie", COMMON, "Leonardo from Teenage Mutant Ninja Turtles", "plushies/character/blue_turtle_slim"),
    RAPHAEL("Raphael Plushie", COMMON, "Raphael from Teenage Mutant Ninja Turtles", "plushies/character/red_turtle_slim"),
    MICHELANGELO("Michelangelo Plushie", COMMON, "Michelangelo from Teenage Mutant Ninja Turtles", "plushies/character/orange_turtle_slim"),
    DONATELLO("Donatello Plushie", COMMON, "Donatello from Teenage Mutant Ninja Turtles", "plushies/character/purple_turtle_slim"),

    // Wearables
    CAT_EARS("Cat Ears", COMMON, "Cute cat ears", "wearables/cat_ears"),
    DOG_EARS("Dog Ears", COMMON, "Cute dog ears", "wearables/dog_ears"),
    FOX_EARS("Fox Ears", COMMON, "Cute fox ears", "wearables/fox_ears"),
    COOL_GLASSES("Cool Glasses", UNCOMMON, "Stylish cool glasses", "wearables/cool_glasses"),
    HALO("Halo", RARE, "A glowing halo", "wearables/halo"),
    HEART_CROWN("Heart Crown", UNCOMMON, "A crown of hearts", "wearables/heart_crown"),
    ORCHID_CROWN("Orchid Crown", UNCOMMON, "A crown of orchids", "wearables/orchid_crown"),
    KING_CROWN("King Crown", UNCOMMON, "A majestic king's crown", "wearables/king_crown"),
    HEART_GLASSES("Heart Glasses", COMMON, "Glasses with heart lenses", "wearables/heart_glasses"),
    STRAWHAT("Strawhat", COMMON, "A simple Mugiwara", "wearables/strawhat"),;

    val storedId: String
        get() = name

    val rollWeight: Double
        get() = rarity.crateWeight

    val effectiveChanceWeight: Double
        get() = rollWeight.coerceAtLeast(0.0)

    fun createItemStack(amount: Int = 1): ItemStack {
        return buildItemStack(amount)
    }

    fun createItemStack(crateType: CrateType, rolledBy: String? = null, amount: Int = 1): ItemStack {
        val pool = crateType.lootPool.possibleItems
        val totalEffectiveWeight = pool.sumOf { it.effectiveChanceWeight }
        val chancePercent = formatChancePercent(this.effectiveChanceWeight, totalEffectiveWeight)

        return buildItemStack(amount, chancePercent, rolledBy)
    }

    private fun buildItemStack(amount: Int, chancePercent: String? = null, rolledBy: String? = null): ItemStack {
        return ItemStack(Material.PAPER, amount).apply {
            applyMetadata(this, chancePercent, rolledBy)
        }
    }

    @Suppress("UnstableApiUsage")
    private fun applyMetadata(itemStack: ItemStack, chancePercent: String? = null, rolledBy: String? = null) {
        itemStack.editMeta { meta ->
            meta.displayName(createDisplayName(itemName, rarity))
            meta.lore(createLore(itemDescription, rarity, chancePercent))
            meta.persistentDataContainer.set(CRATE_ITEM, STRING, storedId)
            meta.persistentDataContainer.set(GENERIC_RARITY, STRING, rarity.name)
            chancePercent?.let { meta.persistentDataContainer.set(CRATE_ROLL_CHANCE_PERCENT, STRING, it) }
            rolledBy?.let { meta.persistentDataContainer.set(CRATE_ROLLED_BY, STRING, it) }
        }
        itemStack.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey("cloudie", modelPath))
        itemStack.setData(DataComponentTypes.EQUIPPABLE, Equippable.equippable(EquipmentSlot.HEAD).build())
    }

    companion object {
        private val byStoredId = entries.associateBy(CrateItem::storedId)
        private val byLegacyModelPath = entries.associateBy { it.modelPath }

        fun fromStoredId(storedId: String?): CrateItem? {
            return storedId?.let(byStoredId::get)
        }

        fun resolve(item: ItemStack?): CrateItem? {
            if (item == null || item.isEmpty) return null

            val meta = item.itemMeta ?: return null
            fromStoredId(meta.persistentDataContainer.get(CRATE_ITEM, STRING))?.let { return it }

            val itemModel = meta.itemModel ?: return null
            if (itemModel.namespace != "cloudie") return null

            return byLegacyModelPath[itemModel.key]
        }

        fun refresh(item: ItemStack?): ItemStack? {
            val current = item ?: return null
            val resolved = resolve(current) ?: return null
            val currentMeta = current.itemMeta ?: return null
            val existingChance = currentMeta.persistentDataContainer.get(CRATE_ROLL_CHANCE_PERCENT, STRING)
            val existingRolledBy = currentMeta.persistentDataContainer.get(CRATE_ROLLED_BY, STRING)

            val refreshed = if (existingChance != null || existingRolledBy != null) {
                resolved.buildItemStack(current.amount, existingChance, existingRolledBy)
            } else {
                resolved.createItemStack(current.amount)
            }

            return if (current.type == refreshed.type && current.itemMeta == refreshed.itemMeta) {
                null
            } else {
                refreshed
            }
        }
    }
}