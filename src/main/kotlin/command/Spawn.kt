package command

import chat.Formatting.allTags
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.bukkit.Location
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import util.requirePlayer

@Suppress("unused", "unstableApiUsage")
@CommandContainer
class Spawn {
    @Command("spawn")
    @Permission("cloudie.cmd.spawn")
    fun spawn(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return

        val world = Bukkit.getWorld("world")
        if (world == null) {
            player.sendMessage(allTags.deserialize("<red>Spawn world is currently unavailable. Please try again later.</red>"))
            Bukkit.getLogger().warning("Spawn command failed: world 'world' not found.")
            return
        }

        val x = 377.5
        val y = 111.0
        val z = -467.5
        val yaw = 90.0f
        val pitch = 0.0f

        val location = Location(world, x, y, z, yaw, pitch)
        val success = player.teleport(location)

        if (success) {
            player.sendMessage(allTags.deserialize("<green>Warped to <aqua>Spawn</aqua>"))
        } else {
            player.sendMessage(allTags.deserialize("<red>Teleportation failed. Location may be unsafe."))
        }
    }
}