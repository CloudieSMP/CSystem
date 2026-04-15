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

        private val HIDDEN_COMMANDS = buildSet {
            addAll(WG_HIDDEN_COMMANDS)
            addAll(WG_HIDDEN_COMMANDS.map { "worldguard:$it" })
            addAll(WE_HIDDEN_COMMANDS)
            addAll(WE_HIDDEN_COMMANDS.map { "worldedit:$it" })
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerCommandSend(event: PlayerCommandSendEvent) {
        val player = event.player

        if (player.hasPermission("cloudie.removecmds"))
            event.commands.removeAll { it in HIDDEN_COMMANDS }
    }
}