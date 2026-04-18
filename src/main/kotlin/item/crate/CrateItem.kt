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
    // Plushies
    PENGUIN(10, "Penguin Plushie", COMMON, "A cute penguin plushie", "plushies/penguin"),
    MUSHROOM(10, "Mushroom Plushie", COMMON, "A cute mushroom plushie", "plushies/mushroom"),
    BEE(10, "Bee Plushie", COMMON, "A cute bee plushie", "plushies/bee"),
    STAR(10, "Star Plushie", COMMON, "A cute star plushie", "plushies/star"),
    HEART(10, "Heart Plushie", COMMON, "A cute heart plushie", "plushies/heart"),
    COFFEE_CUP(10, "Coffee Cup Plushie", COMMON, "A cute coffee cup plushie", "plushies/coffee_cup"),
    RAMEN_BOWL(10, "Ramen Bowl Plushie", COMMON, "A cute ramen bowl plushie", "plushies/ramen_bowl"),

    // Player plushies
    SEBIANN_CLASSIC(1, "Sebiann Classic Plushie", RARE, "A classic Sebiann plushie\nA precious collector's item\nExtremely limited!", "plushies/player/sebiann_classic"),
    SEBIANN(100, "Sebiann Plushie", COMMON, "A cute Sebiann plushie", "plushies/player/sebiann"),
    COOKIE(100, "Cookie Plushie", COMMON, "A cute Cookie plushie", "plushies/player/cookie"),
    BEAUVER(100, "Beauver Plushie", COMMON, "A cute Beauver plushie", "plushies/player/beauver"),

    // Character plushies
    N(10, "N Plushie", COMMON, "N from Pokemon", "plushies/character/n_slim"),
    ASTARION(10, "Astarion Plushie", COMMON, "Astarion from Baldur's Gate 3", "plushies/character/astarion_wide"),
    BATMAN(10, "Batman Plushie", COMMON, "Batman from DC Comics", "plushies/character/batman_wide"),
    DAZAI(10, "Dazai Plushie", COMMON, "Dazai from Bungou Stray Dogs", "plushies/character/dazai_slim"),
    LEVI(10, "Levi Plushie", COMMON, "Levi from Attack on Titan", "plushies/character/levi_wide"),
    SPARROW(10, "Sparrow Plushie", COMMON, "Sparrow from Identity V", "plushies/character/sparrow_wide"),
    SPIDERMAN(10, "Spiderman Plushie", COMMON, "Spiderman from Marvel Comics", "plushies/character/spiderman_slim"),
    LEONARDO(10, "Leonardo Plushie", COMMON, "Leonardo from Teenage Mutant Ninja Turtles", "plushies/character/blue_turtle_slim"),
    RAPHAEL(10, "Raphael Plushie", COMMON, "Raphael from Teenage Mutant Ninja Turtles", "plushies/character/red_turtle_slim"),
    MICHELANGELO(10, "Michelangelo Plushie", COMMON, "Michelangelo from Teenage Mutant Ninja Turtles", "plushies/character/orange_turtle_slim"),
    DONATELLO(10, "Donatello Plushie", COMMON, "Donatello from Teenage Mutant Ninja Turtles", "plushies/character/purple_turtle_slim"),

    // Wearables
    CAT_EARS(10, "Cat Ears", COMMON, "Cute cat ears", "wearables/cat_ears"),
    DOG_EARS(10, "Dog Ears", COMMON, "Cute dog ears", "wearables/dog_ears"),
    FOX_EARS(10, "Fox Ears", COMMON, "Cute fox ears", "wearables/fox_ears"),
    COOL_GLASSES(10, "Cool Glasses", COMMON, "Stylish cool glasses", "wearables/cool_glasses"),
    HALO(10, "Halo", COMMON, "A glowing halo", "wearables/halo"),
    HEART_CROWN(10, "Heart Crown", COMMON, "A crown of hearts", "wearables/heart_crown"),
    ORCHID_CROWN(10, "Orchid Crown", COMMON, "A crown of orchids", "wearables/orchid_crown"),
    HEART_GLASSES(10, "Heart Glasses", COMMON, "Glasses with heart lenses", "wearables/heart_glasses");

    fun createItemStack(): ItemStack = createCrateItem(itemName, rarity, itemDescription, modelPath)
}