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
import util.isDebug
import util.setIsDebug
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
    SABINE("Sabine Plushie", COMMON, "Just Cheering you on.", "plushies/player/sabine_cheerleader_slim"),
    YANN("Yann Plushie", COMMON, "A cute Yann plushie", "plushies/player/yann_wide"),
    ROAST("Roast Plushie", COMMON, "Hasn’t slept in 3 in-game nights.\nStill smiling.", "plushies/player/roast_wide"),
    TURTLE("Turtle Plushie", COMMON, "A cute Turtle plushie", "plushies/player/turtle_wide"),
    FIDEOX("Fideox Plushie", COMMON, "Protects the village, one crop at a time.", "plushies/player/fideox_wide"),
    COMTRA("Comtra Plushie", COMMON, "Stand ready for my arrival, crops.", "plushies/player/comtra_wide"),

    SEBIANN_CLASSIC("Sebiann Classic Plushie", LEGENDARY, "A classic Sebiann plushie\nA precious collector's item\nExtremely limited!", "plushies/player/sebiann_classic"),
    CARSON_GRAY("Carson Plushie (Gray)", LEGENDARY, "Some people's kids these days", "plushies/player/carson_gray_wide"),
    ROAST_MAID("Roast Plushie (Maid)", LEGENDARY, "Tehee", "plushies/player/roast_maid_wide"),
    SABINE_CHEER_COOL("Sabine Plushie (Cheer Cool)", LEGENDARY, "Just Cheering you on, but cooler.", "plushies/player/sabine_cheerleader_cool_slim"),

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
    ROBOT("Robot Plushie", COMMON, "Beep boop", "plushies/character/robot_wide"),
    DARTH_VADER("Darth Vader Plushie", UNCOMMON, "Darth Vader from Star Wars", "plushies/character/darth_vader_wide"),
    STORMTROOPER("Stormtrooper Plushie", COMMON, "Stormtrooper from Star Wars", "plushies/character/stormtrooper_wide"),
    BOBA_FETT("Boba Fett Plushie", UNCOMMON, "Boba Fett from Star Wars", "plushies/character/boba_fett_slim"),

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
    STRAWHAT("Strawhat", COMMON, "A simple Mugiwara", "wearables/strawhat"),

    SABINE_BABYSHARK("Sabine Plushie (Baby Shark)", UNCOMMON, "Baby shark, do, do, do-do, do-do\nBaby shark, do, do, do-do, do-do\nBaby shark, do, do, do-do, do-do\nBaby shark", "plushies/player/sabine/sabine_babysharkdoodoodoodoobabysharkdoodoodoodoobabysharkdoodoodoodoo_slim"),
    SABINE_BARMAID("Sabine Plushie (Bar Maid)", COMMON, "Drinking my problems away.", "plushies/player/sabine/sabine_barmaid_slim"),
    SABINE_BASICBITCH("Sabine Plushie (Basic Bitch)", COMMON, "I'm not like the other girls.", "plushies/player/sabine/sabine_basicbitch_slim"),
    SABINE_BEEQUEEN("Sabine Plushie (BEE QUEEN)", COMMON, "And you just thought it was going to be a fake dragon egg", "plushies/player/sabine/sabine_beequeen_slim"),
    SABINE_BOATINNIT("Sabine Plushie (Boat innit)", COMMON, "I'm on a boat, innit.", "plushies/player/sabine/sabine_boatinnit_slim"),
    SABINE_MCDONALD("Sabine Plushie (McDonald's)", COMMON, "Old MacDonald had a farm. E-I-E-I-O. And on that farm he had a pig. E-I-E-I-O.\nWith an oink oink here. And an oink oink there.", "plushies/player/sabine/sabine_canigeturorder_slim"),
    SABINE_COPPERGOLEM("Sabine Plushie (Copper Golem)", COMMON, "BEEP BOOP", "plushies/player/sabine/sabine_coppergolem_slim"),
    SABINE_CREAKING("Sabine Plushie (Creaking)", COMMON, "Always watching you.", "plushies/player/sabine/sabine_creakacreaka_slim"),
    SABINE_HAZMAT("Sabine Plushie (Hazmat)", COMMON, "Duck called me toxic waste, but in a good way though.", "plushies/player/sabine/sabine_duckcalledmetoxicwastebutinagoodwaythough_slim"),
    SABINE_FEMBOY("Sabine Plushie (Femboy)", COMMON, "Come on, this really looks like she's a femboy", "plushies/player/sabine/sabine_femboystyle_slim"),
    SABINE_FISHLEG("Sabine Plushie (Fish Leg)", COMMON, "I also have an egg on my head.", "plushies/player/sabine/sabine_fishlegsegghead_slim"),
    SABINE_DINONUGGIES("Sabine Plushie (DINO NUGGIES)", COMMON, "DINOSAUR RAWR", "plushies/player/sabine/sabine_gimmesomedinonuggies_slim"),
    SABINE_MUSHROOM("Sabine Plushie (Mushroom)", COMMON, "I'm high on life.", "plushies/player/sabine/sabine_highonlife_slim"),
    SABINE_ATTEMPTED_PIRATE("Sabine Plushie (Pirate (attempted))", COMMON, "It looks like a pirate, I guess.", "plushies/player/sabine/sabine_iattemptedtomakeapirate_slim"),
    SABINE_DUCK("Sabine Plushie (Duck)", COMMON, "Duckismename", "plushies/player/sabine/sabine_imduck_slim"),
    SABINE_INVENTOR("Sabine Plushie (Inventor)", COMMON, "Look at me, I'm smart.", "plushies/player/sabine/sabine_inventorsteampunkeyimnotsure_slim"),
    SABINE_PINK("Sabine Plushie (Pink)", COMMON, "Is this one pink?", "plushies/player/sabine/sabine_isitpink_slim"),
    SABINE_DEADED("Sabine Plushie (Dead)", COMMON, "I think i deaded.", "plushies/player/sabine/sabine_ithinkidied_slim"),
    SABINE_CLOUDIE("Sabine Plushie (Cloudie)", COMMON, "I mad this for Cloudie.", "plushies/player/sabine/sabine_itlookslikeyouracloud_slim"),
    SABINE_MATCHING_BASE("Sabine Plushie (Cloudie S7)", COMMON, "Sabina's never ending obsession.", "plushies/player/sabine/sabine_itriedtomatchmybase_slim"),
    SABINE_HANDSOME("Sabine Plushie (Handsome)", COMMON, "I can get mad bitches ;)", "plushies/player/sabine/sabine_lookinggreatwinkyemojy_slim"),
    SABINE_LUMBERJACK("Sabine Plushie (Lumberjack)", COMMON, "I can chop your wood ;)", "plushies/player/sabine/sabine_lumberjack_slim"),
    SABINE_MAID("Sabine Plushie (Maid)", COMMON, "Yes mommy.\nUWU RAWR", "plushies/player/sabine/sabine_maid_slim"),
    SABINE_NESSED("Sabine Plushie (Nessed)", COMMON, "Why was it that Ness got bored of me?", "plushies/player/sabine/sabine_nessed_slim"),
    SABINE_OLDMAN("Sabine Plushie (Old man)", COMMON, "Look at me, I'm Carson.", "plushies/player/sabine/sabine_oldman_slim"),
    SABINE_YOUNGMAN("Sabine Plushie (Young man)", COMMON, "I'm useful", "plushies/player/sabine/sabine_youngman_slim"),
    SABINE_SCOUT("Sabine Plushie (Scout)", COMMON, "Time to peak it in Minecraft.", "plushies/player/sabine/sabine_peakingitirl_slim"),
    SABINE_PIRATE("Sabine Plushie (Pirate)", COMMON, "Just a sandstorm pirate.", "plushies/player/sabine/sabine_pirate_slim"),
    SABINE_SANTASWIFE("Sabine Plushie (Santa's wife)", COMMON, "Merry Christmas ;)", "plushies/player/sabine/sabine_santaswife_slim"),
    SABINE_SKYCADE("Sabine Plushie (Skycade)", COMMON, "This is how I looked like on skycade.", "plushies/player/sabine/sabine_skycadedays_slim"),
    SABINE_STAR("Sabine Plushie (STAR BUTTERFLY)", COMMON, "It's gonna get a little weird,\nGonna get a little wild.", "plushies/player/sabine/sabine_starbutterfly_slim"),
    SABINE_STAR_BROWN("Sabine Plushie (STAR BUTTERFLY BROWN)", COMMON, " I ain't from 'round here,\nI'm from another dimension.", "plushies/player/sabine/sabine_starbutterfly_brown_slim"),
    SABINE_STOLEN("Sabine Plushie (Stolen)", COMMON, "I stole this from a random girl on hypixel.", "plushies/player/sabine/sabine_stolenfromarandomgirl_slim"),
    SABINE_SUPERWOMAN("Sabine Plushie (Superwoman)", COMMON, "I believe I can fly.", "plushies/player/sabine/sabine_superwoman_slim"),
    SABINE_THECLAW("Sabine Plushie (The Claw)", COMMON, "The claw??", "plushies/player/sabine/sabine_theclaw_slim"),
    SABINE_CLOWN("Sabine Plushie (True Form)", COMMON, "Here you see a wild Sabina in her true form.", "plushies/player/sabine/sabine_trueform_slim"),
    SABINE_VOTEMOOBLOOM("Sabine Plushie (#VOTEMOOBLOOM)", COMMON, "#VOTEMOOBLOOM", "plushies/player/sabine/sabine_votemoobloom_slim"),
    SABINE_WINKERFISHER("Sabine Plushie (Winker Fisher)", COMMON, "winka winka.", "plushies/player/sabine/sabine_winkerfisher_slim"),
    SABINE_WORKER("Sabine Plushie (Worker)", COMMON, "Working hard or hardly working?", "plushies/player/sabine/sabine_workinghardorhardlyworking_slim"),
    SABINE_ZOOKEEPER("Sabine Plushie (Zoo Keeper)", COMMON, "Gotta catch em all.", "plushies/player/sabine/sabine_zookeeper_slim"),
    ;

    val storedId: String
        get() = name

    val displayNamePlain: String
        get() = itemName

    val isPlushie: Boolean
        get() = modelPath.startsWith("plushies/")

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
                ?.takeUnless { it == "DEBUG" }
            val existingIsDebug = currentMeta.persistentDataContainer.isDebug()

            val refreshed = if (existingChance != null || existingRolledBy != null) {
                resolved.buildItemStack(current.amount, existingChance, existingRolledBy)
            } else {
                resolved.createItemStack(current.amount)
            }

            if (existingIsDebug) {
                refreshed.editMeta { meta ->
                    meta.persistentDataContainer.setIsDebug(true)
                }
            }

            return if (current.type == refreshed.type && current.itemMeta == refreshed.itemMeta) {
                null
            } else {
                refreshed
            }
        }
    }
}