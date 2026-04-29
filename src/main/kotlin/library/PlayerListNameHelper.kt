package library

import chat.Formatting
import library.LiveHelper
import library.NoSleepHelper
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player

object PlayerListNameHelper {
    private val liveNameColor: TextColor = TextColor.color(255, 156, 237)

    fun apply(player: Player) {
        val isAlt = player.hasPermission("cloudie.group.alt")
        val isAfk = AfkHelper.isAfk(player)
        val isLive = LiveHelper.isLive(player)
        val isDontSleep = NoSleepHelper.isDontSleep(player)

        val nameColor = when {
            isAfk -> NamedTextColor.GRAY
            isLive -> liveNameColor
            else -> null
        }

        val coloredName = Component.text(player.name)
        val baseName = if (nameColor == null) coloredName else coloredName.color(nameColor)
        val tabName = when {
            isAlt -> Formatting.allTags.deserialize("<prefix:alt> ").append(baseName)
            isDontSleep -> Formatting.allTags.deserialize("<prefix:nosleep> ").append(baseName)
            isLive -> Formatting.allTags.deserialize("<prefix:live> ").append(baseName)
            isAfk -> baseName
            else -> null
        }

        player.playerListName(tabName)
    }
}