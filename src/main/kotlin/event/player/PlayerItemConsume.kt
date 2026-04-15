package event.player

import chat.Formatting.allTags
import item.booster.BoosterType
import item.booster.Cards
import item.crate.CrateType
import logger
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.persistence.PersistentDataType
import util.Keys.BOOSTER_TYPE
import util.Keys.CRATE_TYPE
import util.ui.GamblingWindow

class PlayerItemConsume : Listener {

    @EventHandler
    fun playerItemConsume(event: PlayerItemConsumeEvent) {
        val boosterType = event.item.persistentDataContainer.get(BOOSTER_TYPE, PersistentDataType.STRING)
        if (boosterType != null) {
            val type = BoosterType.fromStoredId(boosterType)
            if (type == null) {
                logger.warning("Unknown booster type '$boosterType' on consumed item from ${event.player.name}")
                return
            }

            val result = Cards.openBooster(event.player, type)
            if (result == null) {
                event.player.sendMessage(allTags.deserialize("This booster has no eligible cards configured."))
            }
            return
        }

        val crateType = event.item.persistentDataContainer.get(CRATE_TYPE, PersistentDataType.STRING)
        if (crateType != null) {
            val type = CrateType.fromStoredId(crateType)
            if (type == null) {
                logger.warning("Unknown crate type '$crateType' on consumed item from ${event.player.name}")
                return
            }
            GamblingWindow.open(event.player, type)
        }
    }
}