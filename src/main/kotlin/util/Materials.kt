package util

import org.bukkit.Material
import org.bukkit.event.inventory.InventoryType

object Materials {
    val HOE_MATERIALS = setOf(
        Material.WOODEN_HOE,
        Material.STONE_HOE,
        Material.COPPER_HOE,
        Material.IRON_HOE,
        Material.GOLDEN_HOE,
        Material.DIAMOND_HOE,
        Material.NETHERITE_HOE,
    )

    val HELMET_MATERIALS = setOf(
        Material.LEATHER_HELMET,
        Material.CHAINMAIL_HELMET,
        Material.COPPER_HELMET,
        Material.IRON_HELMET,
        Material.GOLDEN_HELMET,
        Material.DIAMOND_HELMET,
        Material.NETHERITE_HELMET,
        Material.TURTLE_HELMET,
    )

    val STORAGE_INVENTORY_TYPES = setOf(
        InventoryType.CHEST,
        InventoryType.ENDER_CHEST,
        InventoryType.SHULKER_BOX,
        InventoryType.BARREL,
        InventoryType.HOPPER,
        InventoryType.DISPENSER,
        InventoryType.DROPPER,
    )

    fun isHoe(material: Material): Boolean = material in HOE_MATERIALS
    fun isHelmet(material: Material): Boolean = material in HELMET_MATERIALS
}