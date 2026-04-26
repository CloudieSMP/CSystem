package command

import chat.Formatting
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import util.requirePlayer

@Suppress("unused", "unstableApiUsage")
@CommandContainer
class Ping {
    @Command("ping")
    @CommandDescription("Show your current ping.")
    @Permission("cloudie.cmd.ping")
    fun ping(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        player.sendMessage(Formatting.allTags.deserialize("<cloudiecolor>Your ping is <white>${player.ping}ms<cloudiecolor>."))
    }

    @Command("ping <player>")
    @CommandDescription("Show a players ping.")
    @Permission("cloudie.cmd.ping")
    fun pingOther(css: CommandSourceStack, @Argument("player") playerOther: Player) {
        val player = css.requirePlayer() ?: return
        player.sendMessage(Formatting.allTags.deserialize("<cloudiecolor>${playerOther.name}'s ping is <white>${playerOther.ping}ms<cloudiecolor>."))
    }
}
