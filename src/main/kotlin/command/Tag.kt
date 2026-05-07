package command

import chat.Formatting.allTags
import io.papermc.paper.command.brigadier.CommandSourceStack
import library.TagHelper
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import util.requirePlayer

@Suppress("unused", "unstableApiUsage")
@CommandContainer
class Tag {
    @Command("tagyourit create")
    @Permission("cloudie.cmd.tag.create")
    fun createTag(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        player.inventory.remove(TagHelper.createTagItem())
        player.inventory.addItem(TagHelper.createTagItem())
        player.sendMessage(allTags.deserialize("<green>You have received a tag!"))
    }

    @Command("tagyourit create <player>")
    @Permission("cloudie.cmd.tag.create")
    fun createTagPlayer(css: CommandSourceStack, @Argument("player") target: Player) {
        val player = css.requirePlayer() ?: return
        target.inventory.remove(TagHelper.createTagItem())
        target.inventory.addItem(TagHelper.createTagItem())
        player.sendMessage(allTags.deserialize("<green>You gave a tag to ${target.name}!"))
        target.sendMessage(allTags.deserialize("<green>You have been given a tag!"))
    }

    @Command("tagyourit delete")
    @Permission("cloudie.cmd.tag.delete")
    fun deleteTag(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        player.inventory.remove(TagHelper.createTagItem())
        player.sendMessage(allTags.deserialize("<green>You have deleted your tag!"))
    }

    @Command("tagyourit delete <player>")
    @Permission("cloudie.cmd.tag.delete")
    fun deleteTagPlayer(css: CommandSourceStack, @Argument("player") target: Player) {
        val player = css.requirePlayer() ?: return
        target.inventory.remove(TagHelper.createTagItem())
        player.sendMessage(allTags.deserialize("<green>You have deleted ${target.name} tag!"))
        target.sendMessage(allTags.deserialize("<green>Your tag has been deleted!"))
    }

    @Command("tagyourit add")
    @Permission("cloudie.cmd.tag.add")
    fun addTagger(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        TagHelper.tagPlayer(player.uniqueId, taggedBy = "Admin")
        player.sendMessage(allTags.deserialize("<green>You have been tagged!"))
    }

    @Command("tagyourit add <player>")
    @Permission("cloudie.cmd.tag.add")
    fun addTaggerOther(css: CommandSourceStack, @Argument("player") target: Player) {
        val player = css.requirePlayer() ?: return
        TagHelper.tagPlayer(target.uniqueId, taggedBy = "Admin")
        player.sendMessage(allTags.deserialize("<green>You have tagged ${target.name}!"))
        target.sendMessage(allTags.deserialize("<green>You have been tagged!"))
    }

    @Command("tagyourit remove")
    @Permission("cloudie.cmd.tag.remove")
    fun removeTagger(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        TagHelper.untagPlayer(player)
        player.sendMessage(allTags.deserialize("<green>You have been untagged!"))
    }

    @Command("tagyourit remove <player>")
    @Permission("cloudie.cmd.tag.remove")
    fun removeTaggerOther(css: CommandSourceStack, @Argument("player") target: Player) {
        val player = css.requirePlayer() ?: return
        TagHelper.untagPlayer(target)
        player.sendMessage(allTags.deserialize("<green>You have untagged ${target.name}!"))
        target.sendMessage(allTags.deserialize("<green>You have been untagged!"))
    }

    @Command("tagyourit list")
    @Permission("cloudie.cmd.tag.list")
    fun listTaggers(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        player.sendMessage(allTags.deserialize("<green>Currently tagged players:"))
        val taggedPlayers = TagHelper.getCurrentlyTaggedPlayers()
        if (taggedPlayers.isEmpty()) {
            player.sendMessage(allTags.deserialize("<yellow>None"))
        } else {
            taggedPlayers.forEach { taggedPlayer ->
                player.sendMessage(allTags.deserialize("<yellow>${taggedPlayer.name}"))
            }
        }
    }

    @Command("losttag")
    @Permission("cloudie.cmd.tag.lost")
    fun lostTag(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        if (TagHelper.isCurrentlyTagged(player)) {
            player.sendMessage(allTags.deserialize("<green>Here is your tag!"))
            player.inventory.remove(TagHelper.createTagItem())
            player.inventory.addItem(TagHelper.createTagItem())
        } else {
            player.sendMessage(allTags.deserialize("<red>You are not currently tagged!"))
        }
    }
}