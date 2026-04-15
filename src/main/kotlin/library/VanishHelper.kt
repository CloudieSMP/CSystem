@file:Suppress("DEPRECATION")
package library

import chat.ChatUtility
import chat.Formatting
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.metadata.FixedMetadataValue
import plugin
import java.util.UUID

object VanishHelper {
    private const val VANISH_METADATA_KEY = "vanished"
    private val vanishedPlayerIds = mutableSetOf<UUID>()

    fun isVanished(player: Player): Boolean = player.uniqueId in vanishedPlayerIds
    fun isVanished(playerId: UUID): Boolean = playerId in vanishedPlayerIds
    fun vanishedOnlineCount(): Int = Bukkit.getOnlinePlayers().count { it.uniqueId in vanishedPlayerIds }

    fun toggleVanish(player: Player) {
        if (isVanished(player)) {
            vanishedPlayerIds.remove(player.uniqueId)
            player.removeMetadata(VANISH_METADATA_KEY, plugin) // clear our vanish marker
            showPlayerToAll(player)
            player.sendMessage(Formatting.allTags.deserialize("<dark_gray><i>You are now unvanished"))
            if (!player.hasPermission("cloudie.silent.join")) {
                ChatUtility.broadcastAll(Translation.PlayerMessages.JOIN.replace("%player%", player.name))
            }
        } else {
            vanishedPlayerIds.add(player.uniqueId)
            player.setMetadata(VANISH_METADATA_KEY, FixedMetadataValue(plugin, true))
            hidePlayerFromAll(player)
            player.sendMessage(Formatting.allTags.deserialize("<dark_gray><i>You are now vanished"))
            if (!player.hasPermission("cloudie.silent.quit")) {
                ChatUtility.broadcastAll(Translation.PlayerMessages.QUIT.replace("%player%", player.name))
            }
        }
    }

    fun syncVisibilityForJoin(joiningPlayer: Player) {
        for (other in Bukkit.getOnlinePlayers()) {
            if (other == joiningPlayer) continue

            if (other.uniqueId in vanishedPlayerIds) {
                joiningPlayer.hidePlayer(plugin, other)
            }

            if (joiningPlayer.uniqueId in vanishedPlayerIds) {
                other.hidePlayer(plugin, joiningPlayer)
            }
        }
    }

    fun resetAllVisibility() {
        for (vanishedId in vanishedPlayerIds) {
            val vanishedPlayer = Bukkit.getPlayer(vanishedId) ?: continue
            vanishedPlayer.removeMetadata(VANISH_METADATA_KEY, plugin)
            showPlayerToAll(vanishedPlayer)
        }
        vanishedPlayerIds.clear()
    }

    private fun hidePlayerFromAll(player: Player) {
        for (viewer in Bukkit.getOnlinePlayers()) {
            if (viewer == player) continue
            viewer.hidePlayer(plugin, player)
        }
    }

    private fun showPlayerToAll(player: Player) {
        for (viewer in Bukkit.getOnlinePlayers()) {
            if (viewer == player) continue
            viewer.showPlayer(plugin, player)
        }
    }
}