package event.block

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
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
        if (CROP_SEEDS[block.type] == null) return

        val ageable = block.blockData as? Ageable ?: return
        if (ageable.age < ageable.maximumAge) return

        val player = event.player
        val hoe = player.inventory.itemInMainHand
        if (hoe.type !in HOE_MATERIALS) return

        event.isCancelled = true
        player.swingMainHand()

        harvestBlock(player, block)

        // Sweeping Edge: harvest surrounding fully-grown crops in a square of radius = enchantment level.
        val sweepingLevel = hoe.getEnchantmentLevel(Enchantment.SWEEPING_EDGE)
        if (sweepingLevel > 0) {
            for (dx in -sweepingLevel..sweepingLevel) {
                for (dz in -sweepingLevel..sweepingLevel) {
                    if (dx == 0 && dz == 0) continue
                    if (player.inventory.itemInMainHand.type !in HOE_MATERIALS) return
                    harvestBlock(player, block.world.getBlockAt(block.x + dx, block.y, block.z + dz))
                }
            }
        }
    }

    /**
     * Harvests a single fully-grown crop [block]: drops items, replants if a seed is available,
     * and applies durability to the hoe. Does nothing if the block is not a recognised,
     * fully-grown crop or the player no longer holds a hoe.
     */
    private fun harvestBlock(player: Player, block: Block) {
        val seedMaterial = CROP_SEEDS[block.type] ?: return
        val hoe = player.inventory.itemInMainHand
        if (hoe.type !in HOE_MATERIALS) return

        val ageable = block.blockData as? Ageable ?: return
        if (ageable.age < ageable.maximumAge) return

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

    /** Applies 1 point of durability damage to the held hoe, respecting Unbreaking, and breaks it if needed. */
    private fun damageHoe(player: Player, hoe: ItemStack) {
        val unbreakingLevel = hoe.getEnchantmentLevel(Enchantment.UNBREAKING)
        // Unbreaking reduces damage chance: probability = 1 / (level + 1)
        if (unbreakingLevel > 0 && kotlin.random.Random.nextDouble() < unbreakingLevel / (unbreakingLevel + 1.0)) return

        val meta = hoe.itemMeta as? Damageable ?: return
        val maxDurability = hoe.type.maxDurability.toInt()
        val newDamage = meta.damage + 1
        if (newDamage >= maxDurability) {
            player.playSound(player.location, org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1f)
            player.inventory.setItemInMainHand(ItemStack(Material.AIR))
        } else {
            meta.damage = newDamage
            hoe.itemMeta = meta
        }
    }
}
