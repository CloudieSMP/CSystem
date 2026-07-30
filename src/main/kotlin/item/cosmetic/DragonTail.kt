package item.cosmetic

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin

@Suppress("UnstableApiUsage")
object DragonTail : WearableTail {

    private const val SEGMENT_COUNT = 3
    // How fast each trailing segment converges on its target (0–1, lower = lazier / more tail-like)
    private const val LERP_FACTOR = 0.75

    override fun spawnSegments(player: Player): List<ItemDisplay> {
        return (0 until SEGMENT_COUNT).map { i ->
            player.world.spawn(player.location, ItemDisplay::class.java) { display ->
                val item = ItemStack(Material.PAPER)
                item.setData(
                    DataComponentTypes.ITEM_MODEL,
                    // Replace with your resource pack path, e.g. "cloudie:cosmetics/tails/dragon/segment_$i"
                    net.kyori.adventure.key.Key.key("cloudie", "cosmetics/tails/dragon/segment_$i")
                )
                display.setItemStack(item)
                display.itemDisplayTransform = ItemDisplay.ItemDisplayTransform.FIXED
                display.billboard = Display.Billboard.FIXED
                display.interpolationDuration = 1   // 2-tick client-side interpolation = smooth
                display.viewRange = 48f
                display.teleportDuration = 1        // Paper API — smooth client teleport
                // Scale down each subsequent segment slightly
                val scale = 1f - i * 0.06f
                display.transformation = Transformation(
                    Vector3f(0f, 0f, 0f),
                    Quaternionf(),
                    Vector3f(scale, scale, scale),
                    Quaternionf()
                )
            }
        }
    }

    override fun tickSegments(player: Player, segments: List<ItemDisplay>) {
        val yawRad = Math.toRadians(player.location.yaw.toDouble())

        // Root target: directly behind the player, at hip height (lower + further back when sneaking)
        val isSneaking = player.isSneaking
        val sneakYOffset = if (isSneaking) -0.25 else 0.0
        val behindDist = if (isSneaking) 0.50 else 0.25
        var targetX = player.location.x + sin(yawRad) * behindDist
        var targetY = player.location.y + 1.2 + sneakYOffset
        var targetZ = player.location.z - cos(yawRad) * behindDist

        segments.forEachIndexed { i, segment ->
            val cur = segment.location
            // Lerp toward target
            val newX = cur.x + (targetX - cur.x) * (LERP_FACTOR - 0.25)
            val newY = cur.y + (targetY - cur.y) * LERP_FACTOR
            val newZ = cur.z + (targetZ - cur.z) * (LERP_FACTOR - 0.25)
            // Each successive segment drags a bit lower for a natural droop
            val newLoc = Location(player.world, newX, newY - i * 0.08, newZ, player.location.yaw, 0f)
            segment.teleport(newLoc)

            // Next segment targets this segment's new position (chain effect)
            targetX = newX
            targetY = newY
            targetZ = newZ
        }
    }
}