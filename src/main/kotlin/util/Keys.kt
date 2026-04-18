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
}