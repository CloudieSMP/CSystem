package command

import chat.Formatting
import chat.Formatting.allTags
import io.papermc.paper.command.brigadier.CommandSourceStack
import library.PlayerListNameHelper
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import plugin
import java.util.UUID

@Suppress("unused", "unstableApiUsage")
@CommandContainer
class Live {
    @Command("live|streamermode")
    @CommandDescription("Toggle Streamer mode.")
    @Permission("cloudie.cmd.streamermode")
    fun live(css: CommandSourceStack) {
        val player = css.sender as? Player ?: return
        if (LiveUtil.isLive(player)) {
            LiveUtil.stopLive(player)
            Bukkit.getServer().sendMessage(allTags.deserialize("<cloudiecolor>${player.name}</cloudiecolor> stopped streaming"))
        } else {
            LiveUtil.startLive(player)
            Bukkit.getServer().sendMessage(allTags.deserialize("<cloudiecolor>${player.name}</cloudiecolor> went live"))
        }
    }
}

object LiveUtil {
    private val livePlayerIds = mutableSetOf<UUID>()
    private val liveTasks = mutableMapOf<UUID, BukkitRunnable>()
    private val pendingTimeouts = mutableMapOf<UUID, BukkitRunnable>()

    fun isLive(player: Player): Boolean = isLive(player.uniqueId)

    fun isLive(playerId: UUID): Boolean = playerId in livePlayerIds

    fun startLive(player: Player) {
        val playerId = player.uniqueId
        pendingTimeouts.remove(playerId)?.cancel()
        livePlayerIds.add(playerId)
        startOrReplaceLiveTask(player)
        player.sendMessage(allTags.deserialize("Live mode enabled."))
    }

    fun stopLive(player: Player) {
        val playerId = player.uniqueId
        livePlayerIds.remove(playerId)
        pendingTimeouts.remove(playerId)?.cancel()
        liveTasks.remove(playerId)?.cancel()
        resetLiveDisplayName(player)
        PlayerListNameHelper.apply(player)
        player.sendMessage(allTags.deserialize("Live mode disabled."))
    }

    fun onPlayerQuit(player: Player) {
        val playerId = player.uniqueId
        if (!isLive(playerId)) return

        liveTasks.remove(playerId)?.cancel()
        val timeoutTask = object : BukkitRunnable() {
            override fun run() {
                livePlayerIds.remove(playerId)
                pendingTimeouts.remove(playerId)
            }
        }
        pendingTimeouts[playerId]?.cancel()
        timeoutTask.runTaskLater(plugin, 20L * 60 * 10) // 10 minutes
        pendingTimeouts[playerId] = timeoutTask
    }

    fun onPlayerJoin(player: Player) {
        val playerId = player.uniqueId
        pendingTimeouts.remove(playerId)?.cancel()
        if (!isLive(playerId)) return

        startOrReplaceLiveTask(player)
        player.sendMessage(allTags.deserialize("Live mode enabled."))
    }

    fun shutdown() {
        liveTasks.values.forEach(BukkitRunnable::cancel)
        liveTasks.clear()
        pendingTimeouts.values.forEach(BukkitRunnable::cancel)
        pendingTimeouts.clear()

        for (playerId in livePlayerIds) {
            Bukkit.getPlayer(playerId)?.let {
                resetLiveDisplayName(it)
                PlayerListNameHelper.apply(it)
            }
        }
        livePlayerIds.clear()
    }

    private fun startOrReplaceLiveTask(player: Player) {
        val playerId = player.uniqueId
        liveTasks.remove(playerId)?.cancel()

        val timerRunnable = object : BukkitRunnable() {
            override fun run() {
                if (!player.isOnline || !isLive(playerId)) {
                    cancel()
                    liveTasks.remove(playerId)
                    return
                }

                val newName = Formatting.allTags.deserialize("\uE010 ")
                    .append(net.kyori.adventure.text.Component.text(player.name, TextColor.color(255, 156, 237)))
                player.displayName(newName)
                PlayerListNameHelper.apply(player)
            }
        }

        timerRunnable.runTaskTimer(plugin, 0L, 20L)
        liveTasks[playerId] = timerRunnable
    }

    private fun resetLiveDisplayName(player: Player) {
        player.displayName(null)
    }
}