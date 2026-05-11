package command

import chat.Formatting
import io.papermc.paper.command.brigadier.CommandSourceStack
import item.cosmetic.PlayerTail
import library.TailHelper
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import util.requirePlayer

@Suppress("unused", "UnstableApiUsage")
@CommandContainer
class Tail {

    @Command("tail dragon")
    @CommandDescription("Equip the Dragon Tail cosmetic.")
    @Permission("cloudie.cmd.tail.dragon")
    fun equipDragon(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        if (TailHelper.has(player)) {
            player.sendMessage(
                Formatting.allTags.deserialize("<red>You already have a tail equipped. Use <white>/tail remove</white> first.")
            )
            return
        }
        val success = TailHelper.equip(player, PlayerTail.DRAGON)
        if (success) {
            player.sendMessage(Formatting.allTags.deserialize("<cloudiecolor>Dragon Tail equipped!"))
        } else {
            player.sendMessage(
                Formatting.allTags.deserialize("<red>The dragon tail model hasn't been loaded yet. Please contact an admin.")
            )
        }
    }

    @Command("tail remove")
    @CommandDescription("Remove your currently equipped tail.")
    @Permission("cloudie.cmd.tail")
    fun remove(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        if (!TailHelper.has(player)) {
            player.sendMessage(Formatting.allTags.deserialize("<red>You don't have a tail equipped."))
            return
        }
        TailHelper.remove(player)
        player.sendMessage(Formatting.allTags.deserialize("<cloudiecolor>Tail removed."))
    }

    @Command("tail info")
    @CommandDescription("Show your currently equipped tail.")
    @Permission("cloudie.cmd.tail")
    fun info(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        val current = TailHelper.current(player)
        if (current == null) {
            player.sendMessage(Formatting.allTags.deserialize("<gray>You have no tail equipped."))
        } else {
            player.sendMessage(Formatting.allTags.deserialize("<cloudiecolor>Equipped tail: <white>${current.displayName}"))
        }
    }
}
