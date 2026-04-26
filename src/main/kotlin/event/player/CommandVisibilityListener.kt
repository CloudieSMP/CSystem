package event.player

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandSendEvent

class CommandVisibilityListener : Listener {

    companion object {
        private val WG_HIDDEN_COMMANDS = setOf(
            "region", "regions", "rg", "god", "ungod", "heal", "slay"
        )

        private val WE_HIDDEN_COMMANDS = setOf(
            "worldedit", "we", "tool", "toggleplace", "none", "brush", "br", ";", "/brush", "/br", "/sel", "/desel", "/deselect", "/toggleplace"
        )

        private val AX_HIDDEN_COMMANDS = setOf(
            "axir", "axinvrestore", "axinventoryrestore", "invrestore", "inventoryrestore"
        )

        private val PAPI_HIDDEN_COMMANDS = setOf(
            "papi", "placeholderapi"
        )

        private val VANISH_HIDDEN_COMMANDS = setOf(
            "vanish", "v", "pv", "premiumvanish"
        )

        private val LP_HIDDEN_COMMANDS = setOf(
            "lp", "luckperms", "perm", "perms", "permission", "permissions"
        )

        private val HIDDEN_COMMANDS = buildSet {
            addAll(WG_HIDDEN_COMMANDS)
            addAll(WG_HIDDEN_COMMANDS.map { "worldguard:$it" })
            addAll(WE_HIDDEN_COMMANDS)
            addAll(WE_HIDDEN_COMMANDS.map { "worldedit:$it" })
            addAll(AX_HIDDEN_COMMANDS)
            addAll(AX_HIDDEN_COMMANDS.map { "axinventoryrestore:$it" })
            addAll(PAPI_HIDDEN_COMMANDS)
            addAll(PAPI_HIDDEN_COMMANDS.map { "placeholderapi:$it" })
            addAll(VANISH_HIDDEN_COMMANDS)
            addAll(VANISH_HIDDEN_COMMANDS.map { "premiumvanish:$it" })
            addAll(LP_HIDDEN_COMMANDS)
            addAll(LP_HIDDEN_COMMANDS.map { "luckperms:$it" })
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerCommandSend(event: PlayerCommandSendEvent) {
        val player = event.player

        if (player.hasPermission("cloudie.removecmds"))
            event.commands.removeAll { it in HIDDEN_COMMANDS }
    }
}