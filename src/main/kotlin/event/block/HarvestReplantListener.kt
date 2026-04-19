package event.block

import org.bukkit.Material
import org.bukkit.block.data.Ageable
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

object HarvestReplantListener {

    /** Maps a fully-grown crop block material to the seed material used to replant it. */
    private val CROP_SEEDS = mapOf(
        Material.WHEAT to Material.WHEAT_SEEDS,
        Material.CARROTS to Material.CARROT,
        Material.POTATOES to Material.POTATO,
        Material.BEETROOTS to Material.BEETROOT_SEEDS,
        Material.TORCHFLOWER_CROP to Material.TORCHFLOWER_SEEDS,
        Material.NETHER_WART to Material.NETHER_WART,
    )

    private val HOE_MATERIALS = setOf(
        Material.WOODEN_HOE,
        Material.STONE_HOE,
        Material.COPPER_HOE,
        Material.IRON_HOE,
        Material.GOLDEN_HOE,
        Material.DIAMOND_HOE,
        Material.NETHERITE_HOE,
    )

    fun harvestReplantEvent(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        val cropMaterial = block.type
        val seedMaterial = CROP_SEEDS[cropMaterial] ?: return

        val ageable = block.blockData as? Ageable ?: return
        if (ageable.age < ageable.maximumAge) return

        val player = event.player
        val hoe = player.inventory.itemInMainHand
        if (hoe.type !in HOE_MATERIALS) return

        event.isCancelled = true

        // Collect natural drops, then try to consume one seed (the crop stays planted).
        val drops = block.getDrops(hoe).toMutableList()
        val seedDrop = drops.firstOrNull { it.type == seedMaterial }

        val replanted = when {
            seedDrop != null -> {
                // Seed available in drops: consume it and replant.
                if (seedDrop.amount > 1) seedDrop.amount -= 1 else drops.remove(seedDrop)
                true
            }
            else -> {
                // No seed in drops: try to take one from the player's inventory.
                val invSeed = player.inventory.firstOrNull { it?.type == seedMaterial }
                if (invSeed != null) {
                    if (invSeed.amount > 1) invSeed.amount -= 1 else player.inventory.remove(invSeed)
                    true
                } else {
                    false
                }
            }
        }

        if (replanted) {
            ageable.age = 0
            block.blockData = ageable
        } else {
            // No seed anywhere — harvest only, leave farmland bare.
            block.type = Material.AIR
        }

        // Apply 1 durability to the hoe (respects Unbreaking enchantment).
        damageHoe(player, hoe)

        // Drop remaining items at the block location.
        val location = block.location.add(0.5, 0.5, 0.5)
        drops.forEach { block.world.dropItem(location, it) }
    }

    /** Applies 1 point of durability damage to [hoe], respecting Unbreaking, and breaks it if needed. */
    private fun damageHoe(player: Player, hoe: ItemStack) {
        val unbreakingLevel = hoe.getEnchantmentLevel(Enchantment.UNBREAKING)
        // Unbreaking reduces damage chance: probability = 1 / (level + 1)
        if (unbreakingLevel > 0 && kotlin.random.Random.nextDouble() < unbreakingLevel / (unbreakingLevel + 1.0)) return

        val meta = hoe.itemMeta as? Damageable ?: return
        val maxDurability = hoe.type.maxDurability.toInt()
        val newDamage = meta.damage + 1
        if (newDamage >= maxDurability) {
            player.playSound(player.location, org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1f)
            hoe.amount = 0
        } else {
            meta.damage = newDamage
            hoe.itemMeta = meta
        }
    }
}
