package command

import chat.Formatting
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer

@Suppress("unused", "unstableApiUsage")
@CommandContainer
class Seed {
    @Command("seed")
    @Permission("cloudie.cmd.seed")
    fun seed(css: CommandSourceStack) {
        css.sender.sendMessage(Formatting.allTags.deserialize("<rainbow>made you look lol\nAlso, you just lost the game</rainbow>"))
    }
}