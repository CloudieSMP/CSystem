package command

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
        if (!isHelmet(item.type)) return
        item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, !(item.getData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE) ?: false))
    }
}