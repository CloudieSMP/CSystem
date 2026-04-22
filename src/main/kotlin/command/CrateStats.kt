package command

import chat.Formatting.allTags
import io.papermc.paper.command.brigadier.CommandSourceStack
import item.crate.CrateItem
import item.crate.CrateType
import library.CrateRollStatsStorage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import util.requirePlayer
import java.util.Locale

@Suppress("unused", "unstableApiUsage")
@CommandContainer
class CrateStats {
    @Command("cratestats")
    @CommandDescription("Show your crate roll stats.")
    @Permission("cloudie.cmd.cratestats")
    fun self(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        renderPlayerStats(player, player)
    }

    @Command("cratestats <player>")
    @CommandDescription("Show another player's crate roll stats.")
    @Permission("cloudie.cmd.cratestats.others")
    fun other(css: CommandSourceStack, @Argument("player") target: Player) {
        val requester = css.requirePlayer() ?: return
        renderPlayerStats(requester, target)
    }

    @Command("cratestats plushies")
    @CommandDescription("Show globally rolled plushie counts.")
    @Permission("cloudie.cmd.cratestats")
    fun plushies(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        val counts = CrateRollStatsStorage.globalPlushieCounts().toList().sortedByDescending { it.second }
        if (counts.isEmpty()) {
            player.sendMessage(allTags.deserialize("<gray>No plushie roll stats available yet.</gray>"))
            return
        }

        val totalPlushieRolls = counts.sumOf { it.second }
        player.sendMessage(
            allTags.deserialize(
                "<cloudiecolor><b>Global Plushie Roll Stats</b></cloudiecolor> <gray>(total plushie rolls: <white>$totalPlushieRolls</white>)</gray>"
            )
        )

        val maxLines = 15
        counts.take(maxLines).forEachIndexed { index, (itemId, count) ->
            val itemName = CrateItem.fromStoredId(itemId)?.displayNamePlain ?: humanizeStoredId(itemId)
            player.sendMessage(allTags.deserialize("<gray>${index + 1}.</gray> <white>$itemName</white> <gray>-</gray> <yellow>$count</yellow>"))
        }

        if (counts.size > maxLines) {
            player.sendMessage(allTags.deserialize("<dark_gray>...and ${counts.size - maxLines} more plushies.</dark_gray>"))
        }
    }

    private fun renderPlayerStats(requester: Player, target: Player) {
        val requesterId = requester.uniqueId
        val targetId = target.uniqueId

        CrateRollStatsStorage.snapshotAsync(targetId) { stats ->
            val onlineRequester = Bukkit.getPlayer(requesterId) ?: return@snapshotAsync
            val targetName = target.name

            onlineRequester.sendMessage(
                allTags.deserialize(
                    "<cloudiecolor><b>Crate Stats for <white>$targetName</white></b></cloudiecolor> <gray>(total rolls: <white>${stats.totalRolls}</white>)</gray>"
                )
            )

            if (stats.crateCounts.isEmpty()) {
                onlineRequester.sendMessage(allTags.deserialize("<gray>No crate rolls recorded yet.</gray>"))
                return@snapshotAsync
            }

            stats.crateCounts
                .toList()
                .sortedByDescending { it.second }
                .forEach { (crateId, count) ->
                    val crateName = CrateType.fromStoredId(crateId)?.name?.let(::humanizeStoredId) ?: humanizeStoredId(crateId)
                    onlineRequester.sendMessage(allTags.deserialize("<gray>-</gray> <white>$crateName</white>: <yellow>$count</yellow>"))
                }

            val topPlushies = stats.itemCounts
                .toList()
                .filter { (itemId, _) -> CrateItem.fromStoredId(itemId)?.isPlushie == true }
                .sortedByDescending { it.second }
                .take(5)

            if (topPlushies.isNotEmpty()) {
                onlineRequester.sendMessage(allTags.deserialize("<dark_gray>Top plushies:</dark_gray>"))
                topPlushies.forEach { (itemId, count) ->
                    val itemName = CrateItem.fromStoredId(itemId)?.displayNamePlain ?: humanizeStoredId(itemId)
                    onlineRequester.sendMessage(allTags.deserialize("<dark_gray>  -</dark_gray> <white>$itemName</white>: <yellow>$count</yellow>"))
                }
            }
        }
    }

    private fun humanizeStoredId(value: String): String {
        return value
            .lowercase(Locale.ENGLISH)
            .split('_')
            .joinToString(" ") { part ->
                part.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.ENGLISH) else char.toString()
                }
            }
    }
}

