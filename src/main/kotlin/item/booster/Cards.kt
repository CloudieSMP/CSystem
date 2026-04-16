package item.booster

import chat.Formatting.allTags
import item.CardRarity
import item.CardRarity.*
import item.ItemType
import item.SubRarity
import library.CardPullCounterStorage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import util.Keys.CARD_GLOBAL_PULL_COUNT
import util.Keys.CARD_IS_OBFUSCATED
import util.Keys.CARD_IS_SHADOW
import util.Keys.CARD_IS_SHINY
import util.Keys.CARD_MOB_ID
import util.Keys.GENERIC_RARITY
import util.Keys.GENERIC_SUB_RARITY
import kotlin.random.Random
import org.bukkit.Particle
import org.bukkit.scheduler.BukkitRunnable
import plugin
import util.Sounds.CHEST_OPEN
import util.Sounds.GAMBLING_WHEEL_STOP
import util.Sounds.GAMBLING_WHEEL_TICK

/**
 * Holds the result of a single booster pack pull.
 * Returned by [Cards.openBooster] and [Cards.openBoosterForced].
 */
data class CardPullResult(
    /** The card that was pulled. */
    val definition: CardDefinition,
    /** The rarity that was rolled for this pull (matches the card's base rarity for now). */
    val rarity: CardRarity,
    /** The special variant rolled — NONE means it's a plain card, SHINY adds glint, etc. */
    val subRarity: SubRarity,
    /** How many times this specific card has been pulled across the entire server (including this pull). */
    val globalPullCount: Long,
    /** The finished ItemStack that was added to the player's inventory. */
    val itemStack: ItemStack,
)

data class BoosterPreviewEntry(
    val definition: CardDefinition,
    val rarity: CardRarity,
    val rarityChancePercent: Double,
    val cardChancePercent: Double,
)

object Cards {
    /**
     * Opens a booster pack for the player: randomly selects a card eligible for [boosterType],
     * gives the item to the player, plays effects, and sends chat messages.
     * Returns null if no eligible cards are configured for this booster type.
     */
    fun openBooster(player: Player, boosterType: BoosterType, useDebugSubRarityOverride: Boolean = false): CardPullResult? {
        val rolled = rollCard(boosterType, useDebugSubRarityOverride) ?: return null
        return completePull(player, boosterType, rolled.definition, rolled.rarity, rolled.subRarity)
    }

    /**
     * Forces a specific card to be pulled from a booster pack, bypassing the random selection.
     * Used by the /debug card command. Validates that the card is actually allowed in this booster first.
     * Returns null if the card/booster combination is invalid.
     */
    fun openBoosterForced(
        player: Player,
        boosterType: BoosterType,
        definition: CardDefinition,
        useDebugSubRarityOverride: Boolean = false,
    ): CardPullResult? {
        if (validationErrorFor(boosterType, definition) != null) return null
        val subRarity = if (definition.canHaveSubRarity) {
            SubRarity.getRandomSubRarity(useDebugSubRarityOverride)
        } else {
            SubRarity.NONE
        }
        return completePull(player, boosterType, definition, definition.baseRarity, subRarity)
    }

    /**
     * Checks whether the given card is valid for the given booster type.
     * Returns a human-readable error message if something is wrong, or null if everything is fine.
     */
    fun validationErrorFor(boosterType: BoosterType, definition: CardDefinition): String? {
        if (boosterType !in definition.allowedBoosters) {
            return "${definition.displayName} is not configured for ${boosterType.name.lowercase()} boosters."
        }
        if (rarityRank(definition.baseRarity) < rarityRank(boosterType.minimumCardRarity)) {
            return "${definition.displayName} is ${definition.baseRarity.name} but ${boosterType.name} requires ${boosterType.minimumCardRarity.name}+ cards."
        }
        return null
    }

    /**
     * Returns per-card preview odds for a booster using the same weighting logic as real pulls.
     */
    fun previewEntries(boosterType: BoosterType): List<BoosterPreviewEntry> {
        val eligible = CardCatalog.eligibleCards(boosterType)
        if (eligible.isEmpty()) return emptyList()

        val byRarity = eligible.groupBy { it.baseRarity }
        val availableRarities = rankOrderedRarities()
            .filter { rarityRank(it) >= rarityRank(boosterType.minimumCardRarity) }
            .filter { !byRarity[it].isNullOrEmpty() }

        if (availableRarities.isEmpty()) return emptyList()

        val weighted = adjustedRarityWeights(availableRarities, boosterType)
        val totalWeight = weighted.sumOf { it.second }
        val rarityChanceByRarity = if (totalWeight <= 0.0) {
            val evenChance = 100.0 / availableRarities.size.toDouble()
            availableRarities.associateWith { evenChance }
        } else {
            weighted.associate { (rarity, weight) -> rarity to (weight / totalWeight * 100.0) }
        }

        return availableRarities
            .flatMap { rarity ->
                val cardsInRarity = byRarity[rarity].orEmpty()
                if (cardsInRarity.isEmpty()) {
                    emptyList()
                } else {
                    val rarityChance = rarityChanceByRarity[rarity] ?: 0.0
                    val cardChance = rarityChance / cardsInRarity.size.toDouble()
                    cardsInRarity.map { definition ->
                        BoosterPreviewEntry(
                            definition = definition,
                            rarity = rarity,
                            rarityChancePercent = rarityChance,
                            cardChancePercent = cardChance,
                        )
                    }
                }
            }
            .sortedWith(
                compareByDescending<BoosterPreviewEntry> { it.cardChancePercent }
                    .thenByDescending { rarityRank(it.rarity) }
                    .thenBy { it.definition.displayName }
            )
    }

    /**
     * Handles everything that happens after a card is decided:
     * plays effects, increments the global pull counter, creates the item,
     * puts it in the player's inventory (drops on the ground if full),
     * and sends the pull message in chat.
     */
    private fun completePull(
        player: Player,
        boosterType: BoosterType,
        definition: CardDefinition,
        rarity: CardRarity,
        subRarity: SubRarity,
    ): CardPullResult {
        playOpenEffects(player, rarity, subRarity)
        val globalPullCount = CardPullCounterStorage.incrementAndGet(definition.id)
        val cardStack = createCardItem(player.name, definition, rarity, subRarity, globalPullCount)

        val leftovers = player.inventory.addItem(cardStack)
        for (leftover in leftovers.values) {
            player.world.dropItemNaturally(player.location, leftover)
        }

        player.sendMessage(
            allTags.deserialize("<green>You opened ")
                .append(boosterType.displayName)
                .append(allTags.deserialize("<green> and pulled "))
                .append(cardStack.displayName())
                .append(allTags.deserialize("<green>!"))
        )

        if (rarity.props.sendGlobalMsg || subRarity != SubRarity.NONE) {
            player.server.sendMessage(
                allTags.deserialize("<cloudiecolor>${player.name}<reset> pulled ")
                    .append(cardStack.displayName().hoverEvent(cardStack))
                    .append(allTags.deserialize("<reset> from an "))
                    .append(boosterType.displayName)
                    .append(Component.text("."))
            )
        }

        return CardPullResult(definition, rarity, subRarity, globalPullCount, cardStack)
    }

    /**
     * Randomly picks a card from all eligible cards for [boosterType].
     *
     * Steps:
     *   1. Get all cards allowed in this booster.
     *   2. Filter down to rarities that meet the booster's minimum rarity requirement.
     *   3. Roll a weighted rarity (higher-rarity cards can get a luck boost from the booster).
     *   4. Pick a random card within that rarity.
     *   5. Roll the sub-rarity (only if the card allows it).
     */
    private fun rollCard(boosterType: BoosterType, useDebugSubRarityOverride: Boolean): RolledCard? {
        val eligible = CardCatalog.eligibleCards(boosterType)
        if (eligible.isEmpty()) return null

        val byRarity = eligible.groupBy { it.baseRarity }
        val availableRarities = rankOrderedRarities()
            .filter { rarityRank(it) >= rarityRank(boosterType.minimumCardRarity) }
            .filter { !byRarity[it].isNullOrEmpty() }

        if (availableRarities.isEmpty()) return null

        val rolledRarity = pickRarity(availableRarities, boosterType)
        val card = byRarity[rolledRarity]?.random() ?: eligible.random()
        val subRarity = if (card.canHaveSubRarity) {
            SubRarity.getRandomSubRarity(useDebugSubRarityOverride)
        } else {
            SubRarity.NONE
        }

        return RolledCard(card, rolledRarity, subRarity)
    }

    /**
     * Picks a rarity using weighted random selection.
     * Rarities above the booster's minimum have their weight multiplied by the booster's luck modifier,
     * making higher rarities more likely the better the booster is.
     */
    private fun pickRarity(rarities: List<CardRarity>, boosterType: BoosterType): CardRarity {
        val weighted = adjustedRarityWeights(rarities, boosterType)

        val totalWeight = weighted.sumOf { it.second }
        if (totalWeight <= 0.0) return rarities.random()

        var roll = Random.nextDouble(totalWeight)
        for ((rarity, weight) in weighted) {
            roll -= weight
            if (roll <= 0.0) return rarity
        }

        return weighted.last().first
    }

    private fun adjustedRarityWeights(rarities: List<CardRarity>, boosterType: BoosterType): List<Pair<CardRarity, Double>> {
        return rarities.map { rarity ->
            val baseWeight = rarity.weight.coerceAtLeast(0.0)
            val isAboveMinimum = rarityRank(rarity) > rarityRank(boosterType.minimumCardRarity)
            val adjustedWeight = if (isAboveMinimum) baseWeight * boosterType.luckModifier else baseWeight
            rarity to adjustedWeight
        }
    }

    /**
     * Builds the actual card ItemStack with all its lore, PDC data, and visual properties.
     *
     * The item stores:
     *   - Rarity and sub-rarity (in PDC and as lore glyphs)
     *   - The card's ID so it can be looked up later
     *   - How many times this card has been pulled globally
     *   - Enchantment glint if the card is SHINY
     */
    private fun createCardItem(
        pullerName: String,
        definition: CardDefinition,
        rarity: CardRarity,
        subRarity: SubRarity,
        globalPullCount: Long,
    ): ItemStack {
        val item = ItemStack(Material.PAPER)
        item.editMeta { meta ->
            meta.displayName(
                Component.text(definition.displayName)
                    .color(TextColor.color(rarity.itemRarity.color.asRGB()))
                    .decoration(TextDecoration.ITALIC, false)
            )

            val loreLines = mutableListOf(
                allTags.deserialize("<!i><white>${rarity.itemRarity.rarityGlyph}${if (subRarity != SubRarity.NONE) subRarity.subRarityGlyph else ""}${ItemType.CARD.typeGlyph}"),
                allTags.deserialize("<!i><gray>A trading card featuring <white>${definition.entityType?.name?.lowercase()?.replace('_', ' ') ?: definition.id.replace('_', ' ')}<gray>.")
            )

            if (rarity.props.showPuller || subRarity != SubRarity.NONE) {
                loreLines += allTags.deserialize("<!i><white>Pulled by <yellow>$pullerName<white>.")
            }
            if (shouldShowGlobalPullCount(rarity)) {
                loreLines += allTags.deserialize("<!i><white>Global pulls: <yellow>$globalPullCount")
            }

            meta.lore(loreLines)
            meta.persistentDataContainer.set(GENERIC_RARITY, PersistentDataType.STRING, rarity.name)
            meta.persistentDataContainer.set(GENERIC_SUB_RARITY, PersistentDataType.STRING, subRarity.name)
            meta.persistentDataContainer.set(CARD_MOB_ID, PersistentDataType.STRING, definition.id)
            meta.persistentDataContainer.set(CARD_GLOBAL_PULL_COUNT, PersistentDataType.LONG, globalPullCount)
            meta.persistentDataContainer.set(CARD_IS_SHINY, PersistentDataType.BOOLEAN, subRarity == SubRarity.SHINY)
            meta.persistentDataContainer.set(CARD_IS_SHADOW, PersistentDataType.BOOLEAN, subRarity == SubRarity.SHADOW)
            meta.persistentDataContainer.set(CARD_IS_OBFUSCATED, PersistentDataType.BOOLEAN, subRarity == SubRarity.OBFUSCATED)
            meta.setCustomModelData(definition.modelDataFor(subRarity))

            if (subRarity == SubRarity.SHINY) {
                meta.setEnchantmentGlintOverride(true)
            }
        }
        return item
    }

    /** Returns true if the rarity is high enough that the global pull count should be shown on the card. */
    private fun shouldShowGlobalPullCount(rarity: CardRarity): Boolean {
        return rarityRank(rarity) >= rarityRank(RARE)
    }

    /**
     * Plays sounds and a short particle burst when a card is pulled.
     * The particle type scales with rarity: better card = more impressive effect.
     * The burst runs over 3 steps (6 ticks total) so it feels punchy rather than instant.
     */
    private fun playOpenEffects(player: Player, rarity: CardRarity, subRarity: SubRarity) {
        player.playSound(CHEST_OPEN)

        val particle = when {
            subRarity != SubRarity.NONE -> Particle.WAX_ON
            rarityRank(rarity) >= rarityRank(MYTHIC) -> Particle.TOTEM_OF_UNDYING
            rarityRank(rarity) >= rarityRank(EPIC) -> Particle.END_ROD
            else -> Particle.HAPPY_VILLAGER
        }

        object : BukkitRunnable() {
            private var step = 0

            override fun run() {
                if (!player.isOnline) {
                    cancel()
                    return
                }

                val loc = player.location.clone().add(0.0, 1.1, 0.0)
                player.world.spawnParticle(particle, loc, 10 + (step * 8), 0.35, 0.25, 0.35, 0.01)
                player.playSound(if (step < 2) GAMBLING_WHEEL_TICK else GAMBLING_WHEEL_STOP)

                step++
                if (step >= 3) cancel()
            }
        }.runTaskTimer(plugin, 0L, 2L)
    }

    /** All rarities in order from weakest to strongest, used for range comparisons. */
    private fun rankOrderedRarities(): List<CardRarity> {
        return listOf(COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC, UNREAL, TRANSCENDENT, CELESTIAL)
    }

    /** Converts a rarity to a number so rarities can be compared with > and <. */
    private fun rarityRank(rarity: CardRarity): Int {
        return when (rarity) {
            COMMON -> 0
            UNCOMMON -> 1
            RARE -> 2
            EPIC -> 3
            LEGENDARY -> 4
            MYTHIC -> 5
            UNREAL -> 6
            TRANSCENDENT -> 7
            CELESTIAL -> 8
            SPECIAL -> -1
        }
    }

    /** Returns true if this card item has any non-NONE sub-rarity. */
    fun ItemStack.hasSubRarity(): Boolean = getSubRarity() != SubRarity.NONE

    /** Reads the sub-rarity of this card item from its PDC tags. Returns NONE if it has none. */
    fun ItemStack.getSubRarity(): SubRarity {
        val pdc = itemMeta.persistentDataContainer
        return when {
            pdc.get(CARD_IS_SHINY, PersistentDataType.BOOLEAN) == true -> SubRarity.SHINY
            pdc.get(CARD_IS_SHADOW, PersistentDataType.BOOLEAN) == true -> SubRarity.SHADOW
            pdc.get(CARD_IS_OBFUSCATED, PersistentDataType.BOOLEAN) == true -> SubRarity.OBFUSCATED
            else -> SubRarity.NONE
        }
    }

    /** Internal data class used only while rolling a card, before it becomes a full [CardPullResult]. */
    private data class RolledCard(
        val definition: CardDefinition,
        val rarity: CardRarity,
        val subRarity: SubRarity,
    )
}