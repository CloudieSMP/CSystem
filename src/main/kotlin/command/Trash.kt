package command

import io.papermc.paper.command.brigadier.CommandSourceStack
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import util.requirePlayer
import util.ui.TrashWindow

@Suppress("unused", "unstableApiUsage")
@CommandContainer
class Trash {
    @Command("trash")
    @CommandDescription("Opens a trash inventory — items left inside are deleted on close.")
    @Permission("cloudie.cmd.trash")
    fun trash(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        TrashWindow.open(player)
    }
}
