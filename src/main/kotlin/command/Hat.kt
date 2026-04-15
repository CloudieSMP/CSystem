package command

import chat.Formatting
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import util.requirePlayer

@Suppress("unused", "unstableApiUsage")
@CommandContainer
class Hat {
        @Command("hat")
        @Permission("cloudie.cmd.hat")
        fun hat(css: CommandSourceStack) {
            val player = css.requirePlayer() ?: return
            val itemInHand = player.inventory.itemInMainHand
            if (itemInHand.isEmpty) {
                player.sendMessage(Formatting.allTags.deserialize("<red>You are not holding an item. Idiot...</red>"))
            } else {
                val helmet = player.inventory.helmet
                player.inventory.setHelmet(itemInHand)
                player.inventory.setItemInMainHand(helmet ?: ItemStack(Material.AIR))
            }
        }
}