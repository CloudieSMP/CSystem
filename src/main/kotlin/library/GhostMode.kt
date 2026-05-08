package library

import chat.Formatting
import event.player.PlayerJoin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

import plugin

import kotlin.math.acos

object GhostMode {
    private val ghostPlayers = mutableSetOf<Player>()

    fun isGhost(player: Player): Boolean = player in ghostPlayers

    fun toggleGhostMode(player: Player) {
        if(player in ghostPlayers) {
            ghostPlayers.remove(player)
            player.sendMessage(Formatting.allTags.deserialize("<dark_gray><i>You are now visible"))
        } else {
            ghostPlayers.add(player)
            ghostModeTask(player)
            player.sendMessage(Formatting.allTags.deserialize("<dark_gray><i>You are now intangible"))
        }
        PlayerJoin.refreshTabListForAll()
    }

    private fun ghostModeTask(player: Player) {
        object : BukkitRunnable() {
            override fun run() {
                if(!player.isOnline) {
                    toggleGhostMode(player)
                    cancel()
                }
                if(ghostPlayers.contains(player)) {
                    for (viewer in Bukkit.getOnlinePlayers()) {
                        if (viewer != player) {
                            if(player.world == viewer.world) {
                                if(viewer.location.distanceSquared(player.location) <= 10000) {
                                    val isPeripheral = isInPeripheralView(viewer, player)
                                    if(isPeripheral) {
                                        viewer.showPlayer(plugin, player)
                                    } else {
                                        viewer.hidePlayer(plugin, player)
                                    }
                                } else {
                                    viewer.hidePlayer(plugin, player)
                                }
                            }

                        }
                    }
                } else {
                    for (viewer in Bukkit.getOnlinePlayers()) {
                        if (viewer != player) {
                            if(player.world == viewer.world) {
                                viewer.showPlayer(plugin, player)
                            }
                        }
                    }
                    cancel()
                }
            }
        }.runTaskTimer(plugin, 0L, 2L)
    }

    private fun isInPeripheralView(viewer: Player, target: Player): Boolean {
        val direction = viewer.location.direction.normalize()
        val toTarget = target.location.toVector().subtract(viewer.location.toVector()).normalize()
        val dotProduct = direction.dot(toTarget)
        val angle = acos(dotProduct)
        val angleDegrees = Math.toDegrees(angle)
        return angleDegrees in 52.5..62.5
    }
}