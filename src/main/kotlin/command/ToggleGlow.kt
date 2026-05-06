package command

import chat.Formatting.allTags
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.datacomponent.DataComponentTypes
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import util.Materials.isHelmet
import util.requirePlayer

@Suppress("unused", "UnstableApiUsage")
@CommandContainer
class ToggleGlow {
    @Command("toggleglow")
    @Permission("cloudie.cmd.toggleglow")
    fun toggleGlow(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        val item = player.inventory.itemInMainHand
        if (!isHelmet(item.type)) return player.sendMessage(allTags.deserialize("<red>This only works on helmets."))
        item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, !(item.getData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE) ?: false))
        player.sendMessage(allTags.deserialize("<green>Glint ${if (item.getData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE) == true) "enabled" else "disabled"}!"))
    }
}