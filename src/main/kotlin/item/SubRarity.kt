package item

import logger
import kotlin.random.Random

enum class SubRarity(val weight: Double, val subRarityGlyph: String, val modelDataOffset: Int) {
    NONE      (99.95,  "",        0), // plain card — uses the card's base model data
    SHINY     (0.025, "\uE151",  1), // base + 1
    SHADOW    (0.015, "\uE152",  2), // base + 2
    OBFUSCATED(0.01,  "\uE153",  3); // base + 3

    companion object {
        @Volatile
        private var debugWeightsOverride: Map<SubRarity, Double>? = null

        fun getRandomSubRarity(useDebugOverride: Boolean = false): SubRarity {
            val activeWeights = if (useDebugOverride) debugWeightsOverride else null
            val totalWeight = SubRarity.entries.sumOf { rarity -> (activeWeights?.get(rarity) ?: rarity.weight).coerceAtLeast(0.0) }
            if (totalWeight <= 0.0) {
                logger.warning("Sub rarity weights summed to <= 0, defaulting to NONE")
                return NONE
            }
            val randomValue = Random.nextDouble(totalWeight)

            var cumulativeWeight = 0.0
            for (rarity in SubRarity.entries) {
                cumulativeWeight += (activeWeights?.get(rarity) ?: rarity.weight).coerceAtLeast(0.0)
                if (randomValue < cumulativeWeight) {
                    return rarity
                }
            }

            logger.warning("Unreachable code hit! No sub rarity selected")
            return NONE // Should be unreachable but default to null in case of issue
        }

        fun setDebugWeights(none: Double, shiny: Double, shadow: Double, obfuscated: Double): Boolean {
            val normalized = mapOf(
                NONE to none.coerceAtLeast(0.0),
                SHINY to shiny.coerceAtLeast(0.0),
                SHADOW to shadow.coerceAtLeast(0.0),
                OBFUSCATED to obfuscated.coerceAtLeast(0.0),
            )
            if (normalized.values.all { it <= 0.0 }) return false
            debugWeightsOverride = normalized
            return true
        }

        fun clearDebugWeights() {
            debugWeightsOverride = null
        }

        fun debugWeights(): Map<SubRarity, Double>? = debugWeightsOverride
    }
}