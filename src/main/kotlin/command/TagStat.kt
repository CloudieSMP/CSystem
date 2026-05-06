package command

import chat.Formatting.allTags
import fr.mrmicky.fastboard.adventure.FastBoard
import io.papermc.paper.command.brigadier.CommandSourceStack
import library.TagHelper
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import plugin
import util.requirePlayer
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Suppress("unused", "unstableApiUsage")
@CommandContainer
class TagStat {

    private val pageSize = 14
    private val secondsPerPage get() = plugin.config.showStat.secondsPerPage

    @Command("tagstat tagged")
    @CommandDescription("Broadcast a scoreboard showing how many times each player has been tagged.")
    @Permission("cloudie.cmd.tagstat")
    fun tagged(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        val entries = TagHelper.getAllStats()
            .filter { (_, s) -> s.hasBeenTaggedAmount > 0 }
            .map { (_, s) -> Pair(formatName(s.name), s.hasBeenTaggedAmount) }
            .sortedByDescending { it.second }

        if (entries.isEmpty()) {
            player.sendMessage(allTags.deserialize("<red>No tag data to display yet."))
            return
        }

        val title = allTags.deserialize("<shadow:black><gradient:#FDCFFA:#D78FEE>Times Been Tagged")
        broadcastPages(title, entries)
    }

    @Command("tagstat tagger")
    @CommandDescription("Broadcast a scoreboard showing how many times each player has tagged someone.")
    @Permission("cloudie.cmd.tagstat")
    fun tagger(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        val entries = TagHelper.getAllStats()
            .filter { (_, s) -> s.hasTaggedAmount > 0 }
            .map { (_, s) -> Pair(formatName(s.name), s.hasTaggedAmount) }
            .sortedByDescending { it.second }

        if (entries.isEmpty()) {
            player.sendMessage(allTags.deserialize("<red>No tag data to display yet."))
            return
        }

        val title = allTags.deserialize("<shadow:black><gradient:#FDCFFA:#D78FEE>Times Tagged Others")
        broadcastPages(title, entries)
    }

    private fun broadcastPages(title: Component, entries: List<Pair<Component, Int>>) {
        val pages = entries.chunked(pageSize)
        val runnable = object : BukkitRunnable() {
            var pageIndex = 0
            override fun run() {
                if (pageIndex <= pages.lastIndex) {
                    val pageTitle = if (pages.size > 1) {
                        allTags.deserialize("").append(title)
                            .append(allTags.deserialize(" <#4E56C0>[<#FDCFFA>${pageIndex + 1}/${pages.size}<#4E56C0>]"))
                    } else title

                    broadcastScoreboardLines(pageTitle, pages[pageIndex])
                    pageIndex++
                } else {
                    clearScoreboards(secondsPerPage * 20L)
                    cancel()
                }
            }
        }
        runnable.runTaskTimer(plugin, 0L, secondsPerPage * 20L)
    }

    private fun broadcastScoreboardLines(title: Component, lines: List<Pair<Component, Int>>) {
        for (player in Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("cloudie.dontshowstatscreen")) continue
            val board = FastBoard(player)
            board.updateTitle(title)
            board.updateLines(lines.map { it.first }, lines.map { formatInteger(it.second) })
        }
    }

    private fun clearScoreboards(delay: Long) {
        object : BukkitRunnable() {
            override fun run() {
                for (player in Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("cloudie.dontshowstatscreen")) continue
                    FastBoard(player).delete()
                }
            }
        }.runTaskLater(plugin, delay)
    }

    private fun formatName(name: String): Component {
        val online = Bukkit.getPlayerExact(name)
        return if (online != null) {
            allTags.deserialize("<cloudiecolor><shadow:black>$name")
        } else {
            allTags.deserialize("<white><shadow:black>$name")
        }
    }

    private fun formatInteger(number: Int): Component {
        val symbols = DecimalFormatSymbols(Locale.forLanguageTag("de-CH"))
        symbols.groupingSeparator = '\''
        val formatter = DecimalFormat("#,##0", symbols)
        return allTags.deserialize("<red><shadow:black>${formatter.format(number)}")
    }
}


