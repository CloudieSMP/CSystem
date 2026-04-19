package library

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

object AfkHelper {
    private val afkPlayers: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
    private val lastActivity = ConcurrentHashMap<UUID, Long>()
    // Tracks UUIDs whose lastActivity was initialised this checker cycle, to skip them once
    private val justInitialised: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    fun isAfk(player: Player): Boolean = player.uniqueId in afkPlayers
    fun isAfk(uuid: UUID): Boolean = uuid in afkPlayers

    fun setAfk(player: Player, afk: Boolean) {
        if (afk == isAfk(player)) return
        if (afk) {
            afkPlayers.add(player.uniqueId)
            player.isSleepingIgnored = true
            player.playerListName(Component.text(player.name, NamedTextColor.GRAY))
        } else {
            afkPlayers.remove(player.uniqueId)
            lastActivity[player.uniqueId] = System.currentTimeMillis()
            player.isSleepingIgnored = false
            player.playerListName(null)
        }
    }

    fun recordActivity(player: Player) {
        lastActivity[player.uniqueId] = System.currentTimeMillis()
        if (isAfk(player)) {
            setAfk(player, false)
        }
    }

    fun initPlayer(player: Player) {
        lastActivity[player.uniqueId] = System.currentTimeMillis()
        player.isSleepingIgnored = false
        player.playerListName(null)
    }

    fun cleanup(player: Player) {
        afkPlayers.remove(player.uniqueId)
        lastActivity.remove(player.uniqueId)
        justInitialised.remove(player.uniqueId)
        player.isSleepingIgnored = false
        player.playerListName(null)
    }

    fun resetAll() {
        for (uuid in afkPlayers.toSet()) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            player.isSleepingIgnored = false
            player.playerListName(null)
        }
        afkPlayers.clear()
        lastActivity.clear()
        justInitialised.clear()
    }

    fun startIdleChecker() {
        object : BukkitRunnable() {
            override fun run() {
                val timeoutMs = plugin.config.afk.idleTimeoutSeconds * 1000L
                val now = System.currentTimeMillis()
                for (player in Bukkit.getOnlinePlayers()) {
                    if (isAfk(player)) continue
                    if (!lastActivity.containsKey(player.uniqueId)) {
                        lastActivity[player.uniqueId] = now
                        justInitialised.add(player.uniqueId)
                        continue
                    }
                    if (justInitialised.remove(player.uniqueId)) continue
                    val last = lastActivity[player.uniqueId] ?: continue
                    if (now - last >= timeoutMs) {
                        setAfk(player, true)
                    }
                }
            }
        }.runTaskTimer(plugin, 600L, 600L) // check every 30 seconds
    }
}
