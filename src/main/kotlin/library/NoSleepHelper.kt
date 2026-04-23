package library

import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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

