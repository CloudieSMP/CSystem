package library

import org.bukkit.entity.Player

/**
 * Allows players with [PERMISSION] to use F3+N (spectator toggle) and F3+F4 (game mode switcher)
 * without being server operators, by sending an elevated op-permission-level packet to the client.
 *
 * The client requires op level ≥ 2 to show the F3+N / F3+F4 HUD features.
 * We use Paper's [Player.sendOpLevel] API to send level 2 once after join.
 */
object F3NPermHelper {

    const val PERMISSION = "cloudie.f3nperm"

    // Op level 2 (WORLD_COMMANDS) unlocks F3+N and F3+F4 on the client.
    private const val F3N_OP_LEVEL: Byte = 2

    /**
     * Sends the elevated op-level packet to [player] if they have [PERMISSION].
     * Call this once after join (with a short delay so the permission system has loaded).
     */
    fun sendElevatedOpLevel(player: Player) {
        if (!player.hasPermission(PERMISSION)) return
        player.sendOpLevel(F3N_OP_LEVEL)
    }
}
