package command

import io.papermc.paper.command.brigadier.CommandSourceStack
import library.AfkHelper
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import util.requirePlayer

@Suppress("unused", "unstableApiUsage")
@CommandContainer
class Afk {
    @Command("afk")
    @CommandDescription("Toggle your AFK status.")
    @Permission("cloudie.cmd.afk")
    fun afk(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        AfkHelper.setAfk(player, !AfkHelper.isAfk(player))
        if (!AfkHelper.isAfk(player)) {
            AfkHelper.recordActivity(player)
        }
    }
}
