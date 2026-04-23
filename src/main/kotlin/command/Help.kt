package command

import chat.Formatting.allTags

import io.papermc.paper.command.brigadier.CommandSourceStack
import library.HelpHelper

import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import org.incendo.cloud.annotations.suggestion.Suggestions

@Suppress("unused", "unstableApiUsage")
@CommandContainer
class Help {
    @Command("help")
    @Permission("cloudie.cmd.help")
    fun help(css: CommandSourceStack) {
        css.sender.sendMessage(allTags.deserialize("<yellow>These are the featured Commands:"))
        for (featured in HelpHelper.featuredCommands) {
            val shortDescription = HelpHelper.getCommandShortHelp(featured, false)
            if (shortDescription.isEmpty()) continue
            css.sender.sendMessage(allTags.deserialize("<gold> - <hover:show_text:'Click to get help with /$featured'><click:run_command:/help $featured>/$featured<white> - $shortDescription</click></hover>"))
        }
    }

    @Command("help <CMD>")
    @Permission("cloudie.cmd.help")
    fun helpCommand(css: CommandSourceStack, @Argument(value = "CMD", suggestions = "featured-commands") command: String) {
        css.sender.sendMessage(allTags.deserialize(HelpHelper.getCommandHelp(command, false)))
    }

    @Command("staffhelp")
    @Permission("cloudie.cmd.help.staff")
    fun helpStaff(css: CommandSourceStack) {
        css.sender.sendMessage(allTags.deserialize("<yellow>These are the admin commands:"))
        for (featured in HelpHelper.featuredStaffCommands) {
            val shortDescription = HelpHelper.getCommandShortHelp(featured, true)
            if (shortDescription.isEmpty()) continue
            css.sender.sendMessage(allTags.deserialize("<gold> - <hover:show_text:'Click to get help with /$featured'><click:run_command:/staffhelp $featured>/$featured<white> - $shortDescription</click></hover>"))
        }
    }

    @Command("staffhelp <CMD>")
    @Permission("cloudie.cmd.help.staff")
    fun helpStaffCommand(css: CommandSourceStack, @Argument(value = "CMD", suggestions = "featured-staff-commands") command: String) {
        css.sender.sendMessage(allTags.deserialize(HelpHelper.getCommandHelp(command, true)))
    }

    @Suggestions("featured-commands")
    fun suggestFeaturedCommands(): List<String> {
        return HelpHelper.featuredCommands
    }
    @Suggestions("featured-staff-commands")
    fun suggestFeaturedStaffCommands(): List<String> {
        return HelpHelper.featuredStaffCommands
    }
}