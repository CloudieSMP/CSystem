package command

import chat.Formatting.allTags
import io.papermc.paper.command.brigadier.CommandSourceStack
import library.LiveHelper
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer

@Suppress("unused", "unstableApiUsage")
@CommandContainer
class Live {
    @Command("live|streamermode")
    @CommandDescription("Toggle Streamer mode.")
    @Permission("cloudie.cmd.streamermode")
    fun live(css: CommandSourceStack) {
        val player = css.sender as? Player ?: return
        if (LiveHelper.isLive(player)) {
            LiveHelper.stopLive(player)
            Bukkit.getServer().sendMessage(allTags.deserialize("<cloudiecolor>${player.name}</cloudiecolor> stopped streaming"))
        } else {
            LiveHelper.startLive(player)
            Bukkit.getServer().sendMessage(allTags.deserialize("<cloudiecolor>${player.name}</cloudiecolor> went live"))
        }
    }
}
