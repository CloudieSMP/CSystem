package util

import org.bukkit.NamespacedKey
import plugin

object Keys {
    /**
     * General / Utility
     */
    val ITEM_IS_UNPLACEABLE = NamespacedKey(plugin, "item.unplaceable")
    val GENERIC_RARITY = NamespacedKey(plugin, "item.rarity")
    val GENERIC_SUB_RARITY = NamespacedKey(plugin, "item.rarity.sub_rarity")
    val CRATE_TYPE = NamespacedKey(plugin, "crate.type")
    val CRATE_ITEM = NamespacedKey(plugin, "crate.item")
    val IS_DEBUG = NamespacedKey(plugin, "item.is_debug")
    val CRATE_ROLL_CHANCE_PERCENT = NamespacedKey(plugin, "crate.roll_chance_percent")
    val CRATE_ROLLED_BY = NamespacedKey(plugin, "crate.rolled_by")
    val BOOSTER_TYPE = NamespacedKey(plugin, "booster.type")

    /**
     * Card related
     */
    val CARD_IS_SHINY = NamespacedKey(plugin, "card.is_shiny")
    val CARD_IS_SHADOW = NamespacedKey(plugin, "card.is_shadow")
    val CARD_IS_OBFUSCATED = NamespacedKey(plugin, "card.is_obfuscated")
    val CARD_MOB_ID = NamespacedKey(plugin, "card.mob_id")
    val CARD_GLOBAL_PULL_COUNT = NamespacedKey(plugin, "card.global_pull_count")

    /**
     * Binder related
     */
    val BINDER_CARDS = NamespacedKey(plugin, "binder.cards")

    /**
     * Helmet cosmetic overlay
     * HELMET_ORIGINAL_MODEL — "namespace:key" of the helmet's original ITEM_MODEL before cosmetic was applied,
     *                         or absent if the helmet had no custom model.
     *                         Presence of this key (or CRATE_ITEM on a helmet) indicates a cosmetic is active.
     */
    val HELMET_ORIGINAL_MODEL = NamespacedKey(plugin, "helmet.original_model")
}