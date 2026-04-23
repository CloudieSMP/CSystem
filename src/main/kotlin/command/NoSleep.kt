package command

import chat.Formatting
import io.papermc.paper.command.brigadier.CommandSourceStack
import library.PlayerListNameHelper
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import util.requirePlayer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused", "unstableApiUsage")
@CommandContainer
class NoSleep {
    @Command("nosleep")
    @CommandDescription("Toggle the NOSLEEP tag on your name in tab.")
    @Permission("cloudie.cmd.nosleep")
    fun noSleep(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        val newState = !NoSleepHelper.isDontSleep(player)
        NoSleepHelper.setNoSleep(player, newState)
        if (newState) {
            player.sendMessage(Formatting.allTags.deserialize("<prefix:nosleep> <white>tag enabled.</white>"))
        } else {
            player.sendMessage(Formatting.allTags.deserialize("<prefix:nosleep> <white>tag disabled.</white>"))
        }
    }
}

object NoSleepHelper {
    private val noSleepPlayers: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    fun isDontSleep(player: Player): Boolean = player.uniqueId in noSleepPlayers
    fun isDontSleep(uuid: UUID): Boolean = uuid in noSleepPlayers

    fun setNoSleep(player: Player, noSleep: Boolean) {
        if (noSleep == isDontSleep(player)) return
        if (noSleep) {
            noSleepPlayers.add(player.uniqueId)
        } else {
            noSleepPlayers.remove(player.uniqueId)
        }
        PlayerListNameHelper.apply(player)
    }

    fun cleanup(player: Player) {
        noSleepPlayers.remove(player.uniqueId)
    }
}