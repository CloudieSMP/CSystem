package command

import chat.Formatting
import io.papermc.paper.command.brigadier.CommandSourceStack
import library.NoSleepHelper
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import util.requirePlayer

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
