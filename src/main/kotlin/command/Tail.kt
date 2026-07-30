package command

import chat.Formatting
import io.papermc.paper.command.brigadier.CommandSourceStack
import item.cosmetic.DragonTail
import library.CosmeticHelper
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import util.requirePlayer

@Suppress("unused")
@CommandContainer
class Tail {

    @Command("tail dragon")
    @CommandDescription("Equip the dragon tail cosmetic.")
    @Permission("cloudie.cmd.tail.dragon")
    fun equipDragon(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        if (CosmeticHelper.hasTail(player)) {
            player.sendMessage(Formatting.allTags.deserialize("<red>You already have a tail equipped. Use <white>/tail remove</white> first."))
            return
        }
        CosmeticHelper.equipTail(player, DragonTail)
        player.sendMessage(Formatting.allTags.deserialize("<cloudiecolor>Dragon tail equipped!"))
    }

    @Command("tail remove")
    @CommandDescription("Remove your currently equipped tail cosmetic.")
    @Permission("cloudie.cmd.tail")
    fun remove(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        if (!CosmeticHelper.hasTail(player)) {
            player.sendMessage(Formatting.allTags.deserialize("<red>You don't have a tail equipped."))
            return
        }
        CosmeticHelper.removeTail(player)
        player.sendMessage(Formatting.allTags.deserialize("<cloudiecolor>Tail removed."))
    }
}