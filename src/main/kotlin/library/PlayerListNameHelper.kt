package library

import chat.Formatting
import command.LiveUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player

object PlayerListNameHelper {
    private val liveNameColor: TextColor = TextColor.color(255, 156, 237)

    fun apply(player: Player) {
        val isAfk = AfkHelper.isAfk(player)
        val isLive = LiveUtil.isLive(player)

        val nameColor = when {
            isAfk -> NamedTextColor.GRAY
            isLive -> liveNameColor
            else -> null
        }

        val coloredName = Component.text(player.name)
        val baseName = if (nameColor == null) coloredName else coloredName.color(nameColor)
        val tabName = if (isLive) {
            Formatting.allTags.deserialize("\uE010 ").append(baseName)
        } else if (isAfk) {
            baseName
        } else {
            null
        }

        player.playerListName(tabName)
    }
}