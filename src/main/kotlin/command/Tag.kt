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
    @Command("tagurit create")
    @Permission("cloudie.cmd.tag.create")
    fun createTag(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        player.inventory.remove(TagHelper.createTagItem())
        player.inventory.addItem(TagHelper.createTagItem())
        player.sendMessage(allTags.deserialize("<green>You have received a tag!"))
    }

    @Command("tagurit create <player>")
    @Permission("cloudie.cmd.tag.create")
    fun createTagPlayer(css: CommandSourceStack, @Argument("player") target: Player) {
        val player = css.requirePlayer() ?: return
        target.inventory.remove(TagHelper.createTagItem())
        target.inventory.addItem(TagHelper.createTagItem())
        player.sendMessage(allTags.deserialize("<green>You gave a tag to ${target.name}!"))
        target.sendMessage(allTags.deserialize("<green>You have been given a tag!"))
    }

    @Command("tagurit delete")
    @Permission("cloudie.cmd.tag.delete")
    fun deleteTag(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        player.inventory.remove(TagHelper.createTagItem())
        player.sendMessage(allTags.deserialize("<green>You have deleted your tag!"))
    }

    @Command("tagurit delete <player>")
    @Permission("cloudie.cmd.tag.delete")
    fun deleteTagPlayer(css: CommandSourceStack, @Argument("player") target: Player) {
        val player = css.requirePlayer() ?: return
        target.inventory.remove(TagHelper.createTagItem())
        player.sendMessage(allTags.deserialize("<green>You have deleted ${target.name} tag!"))
        target.sendMessage(allTags.deserialize("<green>Your tag has been deleted!"))
    }

    @Command("tagurit add")
    @Permission("cloudie.cmd.tag.add")
    fun addTagger(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        TagHelper.tagPlayer(player.uniqueId, taggedBy = "Admin")
        player.sendMessage(allTags.deserialize("<green>You have been tagged!"))
    }

    @Command("tagurit add <player>")
    @Permission("cloudie.cmd.tag.add")
    fun addTaggerOther(css: CommandSourceStack, @Argument("player") target: Player) {
        val player = css.requirePlayer() ?: return
        TagHelper.tagPlayer(target.uniqueId, taggedBy = "Admin")
        player.sendMessage(allTags.deserialize("<green>You have tagged ${target.name}!"))
        target.sendMessage(allTags.deserialize("<green>You have been tagged!"))
    }

    @Command("tagurit remove")
    @Permission("cloudie.cmd.tag.remove")
    fun removeTagger(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        TagHelper.untagPlayer(player)
        player.sendMessage(allTags.deserialize("<green>You have been untagged!"))
    }

    @Command("tagurit remove <player>")
    @Permission("cloudie.cmd.tag.remove")
    fun removeTaggerOther(css: CommandSourceStack, @Argument("player") target: Player) {
        val player = css.requirePlayer() ?: return
        TagHelper.untagPlayer(target)
        player.sendMessage(allTags.deserialize("<green>You have untagged ${target.name}!"))
        target.sendMessage(allTags.deserialize("<green>You have been untagged!"))
    }

    @Command("tagurit list")
    @Permission("cloudie.cmd.tag.list")
    fun listTaggers(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        player.sendMessage(allTags.deserialize("<green>This doesn't work yet!"))
    }
}