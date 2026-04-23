package command

import chat.Notification

import io.papermc.paper.command.brigadier.CommandSourceStack

import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer

@Suppress("unused", "unstableApiUsage")
@CommandContainer
class Announcement {
    @Command("announce <text>")
    @CommandDescription("Broadcasts the specified text.")
    @Permission("cloudie.cmd.announce")
    fun announce(css: CommandSourceStack, @Argument("text") text: Array<String>) {
        Notification.announceServer("<yellow><b>Announcement<reset>", text.joinToString(" "))
    }

    @Command("announcesmall <text>")
    @CommandDescription("Broadcasts the specified text.")
    @Permission("cloudie.cmd.announce")
    fun announceSmall(css: CommandSourceStack, @Argument("text") text: Array<String>) {
        Notification.announceActionbar(text.joinToString(" "))
    }

    @Command("announcerestart <time>")
    @CommandDescription("Broadcasts when the next restart will be.")
    @Permission("cloudie.cmd.announce")
    fun announceRestart(css: CommandSourceStack, @Argument("time") time: Int) {
        Notification.announceServer(
            "<red><b>Server Restarting<reset>",
            "In $time minute${if (time > 1) "s" else ""}."
        )
    }

    @Command("announcerestart <time> funny")
    @CommandDescription("Broadcasts when the next restart will be.")
    @Permission("cloudie.cmd.announce")
    fun announceRestartFunny(css: CommandSourceStack, @Argument("time") time: Int) {
        Notification.announceServer(
            "<red><b>Server Restarting<reset>",
            "In $time minute${if (time > 1) "s" else ""}."
        )
        Notification.announceActionbar("This is a sign to sleep, europeans.")
    }
}