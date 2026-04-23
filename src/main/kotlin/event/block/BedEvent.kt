package event.block

import chat.Formatting.allTags
import library.NoSleepHelper
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerInteractEvent
import plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object BedEvent {
    private val triedSleepPlayers: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    fun onBedClick(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        if (!block.type.name.endsWith("_BED")) return
        var isSomeoneNoSleep: Boolean = false
        var noSleepPlayerName: String = "unknown"
        Bukkit.getOnlinePlayers().forEach { if (NoSleepHelper.isDontSleep(it)) {
            isSomeoneNoSleep = true
            noSleepPlayerName = it.name
        } }

        if (isSomeoneNoSleep) {
            if (event.player.uniqueId !in triedSleepPlayers) {
                event.isCancelled = true
                event.player.sendMessage(allTags.deserialize("$noSleepPlayerName has the <prefix:nosleep> tag enabled.\nIf you want to sleep, try clicking the bed again after 1 sec."))
                addSleepTry(event.player.uniqueId)
                deleteSleepTry(event.player.uniqueId)
            } else {
                triedSleepPlayers.remove(event.player.uniqueId)
                event.player.sendMessage(allTags.deserialize("How dare you sleep."))
            }
        }
    }

    private fun deleteSleepTry(player: UUID) {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable { triedSleepPlayers.remove(player) }, 200L)
    }

    private fun addSleepTry(player: UUID) {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable { triedSleepPlayers.add(player) }, 15L)
    }
}