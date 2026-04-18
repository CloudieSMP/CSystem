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
import util.Keys.GENERIC_RARITY

@Suppress("UnstableApiUsage")
private fun createCrateItem(
    displayName: String,
    rarity: ItemRarity,
    description: String,
    modelPath: String,
): ItemStack {
    return ItemStack(Material.PAPER).apply {
        editMeta { meta ->
            meta.displayName(
                Component.text(displayName)
                    .color(TextColor.color(rarity.color.asRGB()))
                    .decoration(TextDecoration.ITALIC, false)
            )
            meta.lore(
                buildList {
                    add(allTags.deserialize("<!i><white>${rarity.rarityGlyph}${ItemType.PLUSHIE.typeGlyph}"))
                    description.split("\n").forEach { line ->
                        add(Component.text(line).decoration(TextDecoration.ITALIC, false))
                    }
                }
            )
            meta.persistentDataContainer.set(GENERIC_RARITY, STRING, rarity.name)
        }
        setData(DataComponentTypes.ITEM_MODEL, NamespacedKey("cloudie", modelPath))
        setData(DataComponentTypes.EQUIPPABLE, Equippable.equippable(EquipmentSlot.HEAD).build())
    }
}

/**
 * @param rollWeight Weighted roll value used when this item is selected from a crate loot pool.
 */
enum class CrateItem(
    val rollWeight: Int,
    private val itemName: String,
    private val rarity: ItemRarity,
    private val itemDescription: String,
    private val modelPath: String,
) {
    DEFAULT(1, "Default Item", SPECIAL, "This item should never be obtained.\n(it can be though)\nGood luck getting it", "plushies/player/default_wide"),

    // Plushies
    PENGUIN(100, "Penguin Plushie", COMMON, "A cute penguin plushie", "plushies/penguin"),
    MUSHROOM(100, "Mushroom Plushie", COMMON, "A cute mushroom plushie", "plushies/mushroom"),
    BEE(100, "Bee Plushie", COMMON, "A cute bee plushie", "plushies/bee"),
    STAR(100, "Star Plushie", COMMON, "A cute star plushie", "plushies/star"),
    HEART(100, "Heart Plushie", COMMON, "A cute heart plushie", "plushies/heart"),
    COFFEE_CUP(100, "Coffee Cup Plushie", COMMON, "A cute coffee cup plushie", "plushies/coffee_cup"),
    RAMEN_BOWL(100, "Ramen Bowl Plushie", COMMON, "A cute ramen bowl plushie", "plushies/ramen_bowl"),

    // Player plushies
    SEBIANN_CLASSIC(1, "Sebiann Classic Plushie", LEGENDARY, "A classic Sebiann plushie\nA precious collector's item\nExtremely limited!", "plushies/player/sebiann_classic"),
    SEBIANN(100, "Sebiann Plushie", COMMON, "A cute Sebiann plushie", "plushies/player/sebiann"),
    COOKIE(100, "Cookie Plushie", COMMON, "A cute Cookie plushie", "plushies/player/cookie"),
    BEAUVER(100, "Beauver Plushie", COMMON, "A cute Beauver plushie", "plushies/player/beauver"),
    CARSON(100, "Carson Plushie", COMMON, "A cute Carson plushie", "plushies/player/carson_wide"),
    LESHY(100, "Leshy Plushie", COMMON, "Blob Blob", "plushies/player/leshy_slim"),
    MAI(100, "Mai Plushie", COMMON, "A cute Mai plushie", "plushies/player/mai_cheerleader_slim"),
    MEGAN(100, "Megan Plushie", COMMON, "A cute Megan plushie", "plushies/player/megan_cheerleader_slim"),
    RIVEN(100, "Riven Plushie", COMMON, "MEOW", "plushies/player/riven_slim"),
    SABINE(100, "Sabine Plushie", COMMON, "Just Cheering you on", "plushies/player/sabine_cheerleader_slim"),
    YANN(100, "Yann Plushie", COMMON, "A cute Yann plushie", "plushies/player/yann_wide"),

    // Character plushies
    N(100, "N Plushie", COMMON, "N from Pokemon", "plushies/character/n_slim"),
    ASTARION(100, "Astarion Plushie", COMMON, "Astarion from Baldur's Gate 3", "plushies/character/astarion_wide"),
    BATMAN(100, "Batman Plushie", COMMON, "Batman from DC Comics", "plushies/character/batman_wide"),
    DAZAI(100, "Dazai Plushie", COMMON, "Dazai from Bungou Stray Dogs", "plushies/character/dazai_slim"),
    LEVI(100, "Levi Plushie", COMMON, "Levi from Attack on Titan", "plushies/character/levi_wide"),
    SPARROW(100, "Sparrow Plushie", COMMON, "Sparrow from Identity V", "plushies/character/sparrow_wide"),
    SPIDERMAN(100, "Spiderman Plushie", COMMON, "Spiderman from Marvel Comics", "plushies/character/spiderman_slim"),
    LEONARDO(100, "Leonardo Plushie", COMMON, "Leonardo from Teenage Mutant Ninja Turtles", "plushies/character/blue_turtle_slim"),
    RAPHAEL(100, "Raphael Plushie", COMMON, "Raphael from Teenage Mutant Ninja Turtles", "plushies/character/red_turtle_slim"),
    MICHELANGELO(100, "Michelangelo Plushie", COMMON, "Michelangelo from Teenage Mutant Ninja Turtles", "plushies/character/orange_turtle_slim"),
    DONATELLO(100, "Donatello Plushie", COMMON, "Donatello from Teenage Mutant Ninja Turtles", "plushies/character/purple_turtle_slim"),

    // Wearables
    CAT_EARS(100, "Cat Ears", COMMON, "Cute cat ears", "wearables/cat_ears"),
    DOG_EARS(100, "Dog Ears", COMMON, "Cute dog ears", "wearables/dog_ears"),
    FOX_EARS(100, "Fox Ears", COMMON, "Cute fox ears", "wearables/fox_ears"),
    COOL_GLASSES(90, "Cool Glasses", UNCOMMON, "Stylish cool glasses", "wearables/cool_glasses"),
    HALO(60, "Halo", RARE, "A glowing halo", "wearables/halo"),
    HEART_CROWN(80, "Heart Crown", UNCOMMON, "A crown of hearts", "wearables/heart_crown"),
    ORCHID_CROWN(80, "Orchid Crown", UNCOMMON, "A crown of orchids", "wearables/orchid_crown"),
    HEART_GLASSES(100, "Heart Glasses", COMMON, "Glasses with heart lenses", "wearables/heart_glasses");

    fun createItemStack(): ItemStack = createCrateItem(itemName, rarity, itemDescription, modelPath)
}