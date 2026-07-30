package item.cosmetic

import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player

interface WearableTail {
    /** Spawn all segments at the player's current location. Called once on equip. */
    fun spawnSegments(player: Player): List<ItemDisplay>

    /**
     * Called every tick. Reposition [segments] relative to [player].
     * segments[0] is the "root" closest to the player's back.
     */
    fun tickSegments(player: Player, segments: List<ItemDisplay>)
}