package command

import chat.Formatting.allTags
import io.papermc.paper.command.brigadier.CommandSourceStack
import item.crate.CrateItem
import item.crate.CrateType
import library.CrateRollStatsStorage
import library.TagHelper
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import org.incendo.cloud.annotations.suggestion.Suggestions
import plugin
import util.Sounds.ERROR_DIDGERIDOO
import util.requirePlayer
import java.util.Locale
import java.util.UUID

@Suppress("unused", "unstableApiUsage")
@CommandContainer
class Stats {

    private val pageSize = 14
    private val secondsPerPage get() = plugin.config.showStat.secondsPerPage

    // ── Tag stats (scoreboard) ────────────────────────────────────────────────

    @Command("stats tag tagged")
    @CommandDescription("Broadcast a scoreboard showing how many times each player has been tagged.")
    @Permission("cloudie.cmd.stats")
    fun tagTagged(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        broadcastTagStat(player, "<shadow:black><gradient:#FDCFFA:#D78FEE>Times Been Tagged") { it.hasBeenTaggedAmount }
    }

    @Command("stats tag tagger")
    @CommandDescription("Broadcast a scoreboard showing how many times each player has tagged someone.")
    @Permission("cloudie.cmd.stats")
    fun tagTagger(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        broadcastTagStat(player, "<shadow:black><gradient:#FDCFFA:#D78FEE>Times Tagged Others") { it.hasTaggedAmount }
    }

    private fun broadcastTagStat(player: Player, titleBase: String, getValue: (TagHelper.PlayerTagStats) -> Int) {
        if (ShowStat.isActive) {
            player.sendMessage(allTags.deserialize("<red>Other stats are already being shown, please wait for them to finish."))
            player.playSound(ERROR_DIDGERIDOO)
            return
        }
        val entries = TagHelper.getAllStats()
            .filter { (_, s) -> getValue(s) > 0 }
            .map { (_, s) -> Pair(ShowStat.formatPlayerName(s.name), getValue(s)) }
            .sortedByDescending { it.second }

        if (entries.isEmpty()) {
            player.sendMessage(allTags.deserialize("<red>No tag data to display yet."))
            return
        }
        broadcastPages(titleBase, entries)
    }

    private fun broadcastPages(titleBase: String, entries: List<Pair<Component, Int>>) {
        val sum = entries.sumOf { it.second }
        val allEntries = buildList {
            add(Pair(allTags.deserialize("<shadow:black><#ff65aa><u>Total"), sum))
            addAll(entries)
        }
        val pages = allEntries.chunked(pageSize)
        ShowStat.isActive = true
        object : BukkitRunnable() {
            var pageIndex = 0
            override fun run() {
                if (pageIndex <= pages.lastIndex) {
                    val title = allTags.deserialize("$titleBase <#4E56C0>[<#FDCFFA>${pageIndex + 1}/${pages.size}<#4E56C0>]")
                    ShowStat.broadcastScoreboardLines(title, pages[pageIndex])
                    pageIndex++
                } else {
                    ShowStat.clearScoreboards(secondsPerPage * 20L)
                    cancel()
                }
            }
        }.runTaskTimer(plugin, 0L, secondsPerPage * 20L)
    }

    // ── Crate stats (chat) ────────────────────────────────────────────────────

    @Command("stats crate")
    @CommandDescription("Show your crate roll stats.")
    @Permission("cloudie.cmd.stats")
    fun crateStatsSelf(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        renderPlayerStats(player, player.uniqueId, player.name)
    }

    @Command("stats crate me <crateName>")
    @CommandDescription("Show your collectible stats for a specific crate.")
    @Permission("cloudie.cmd.stats")
    fun crateStatsSelfByCrate(css: CommandSourceStack, @Argument(value = "crateName", suggestions = "stats-crate-names") crateName: String) {
        val player = css.requirePlayer() ?: return
        val crateType = resolveCrateType(crateName) ?: run {
            player.sendMessage(allTags.deserialize("<gray>Unknown crate <white>$crateName</white>.</gray>"))
            return
        }
        renderPlayerCrateStats(player, player.uniqueId, player.name, crateType)
    }

    @Command("stats crate global")
    @CommandDescription("Show global collectible roll stats.")
    @Permission("cloudie.cmd.stats")
    fun crateStatsGlobal(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        val counts = CrateRollStatsStorage.globalPlushieCounts().toList().sortedByDescending { it.second }
        sendCollectibleCounts(
            viewer = player,
            title = "Global Collectible Roll Stats",
            counts = counts,
            emptyMessage = "No collectible roll stats available yet.",
            maxLines = 15,
        )
    }

    @Command("stats crate global <crateName>")
    @CommandDescription("Show global collectible stats for a specific crate.")
    @Permission("cloudie.cmd.stats")
    fun crateStatsGlobalByCrate(css: CommandSourceStack, @Argument(value = "crateName", suggestions = "stats-crate-names") crateName: String) {
        val player = css.requirePlayer() ?: return
        val crateType = resolveCrateType(crateName) ?: run {
            player.sendMessage(allTags.deserialize("<gray>Unknown crate <white>$crateName</white>.</gray>"))
            return
        }
        renderGlobalCrateStats(player, crateType)
    }

    @Command("stats crate <playerName>")
    @CommandDescription("Show another player's crate roll stats.")
    @Permission("cloudie.cmd.stats.other")
    fun crateStatsOther(css: CommandSourceStack, @Argument(value = "playerName", suggestions = "stats-player-names") playerName: String) {
        val requester = css.requirePlayer() ?: return
        resolveOfflinePlayer(requester, playerName) { uuid, name ->
            renderPlayerStats(requester, uuid, name)
        }
    }

    @Command("stats crate <playerName> <crateName>")
    @CommandDescription("Show another player's collectible stats for a specific crate.")
    @Permission("cloudie.cmd.stats.other")
    fun crateStatsOtherByCrate(
        css: CommandSourceStack,
        @Argument(value = "playerName", suggestions = "stats-player-names") playerName: String,
        @Argument(value = "crateName", suggestions = "stats-crate-names") crateName: String,
    ) {
        val requester = css.requirePlayer() ?: return
        val crateType = resolveCrateType(crateName) ?: run {
            requester.sendMessage(allTags.deserialize("<gray>Unknown crate <white>$crateName</white>.</gray>"))
            return
        }
        resolveOfflinePlayer(requester, playerName) { uuid, name ->
            renderPlayerCrateStats(requester, uuid, name, crateType)
        }
    }

    // ── Crate helpers (ported from CrateStats) ────────────────────────────────

    private val wearableItemIds: Set<String> = CrateType.COSMETIC_HAT.lootPool.possibleItems
        .map(CrateItem::storedId)
        .toSet()

    private fun resolveOfflinePlayer(requester: Player, playerName: String, callback: (UUID, String) -> Unit) {
        val online = Bukkit.getPlayerExact(playerName)
        if (online != null) {
            callback(online.uniqueId, online.name)
            return
        }
        val requesterId = requester.uniqueId
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val offline = Bukkit.getOfflinePlayer(playerName)
            val resolved = if (offline.hasPlayedBefore() || offline.isOnline)
                offline.uniqueId to (offline.name ?: playerName) else null
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (resolved == null) {
                    Bukkit.getPlayer(requesterId)?.sendMessage(allTags.deserialize("<gray>Player <white>$playerName</white> not found.</gray>"))
                } else {
                    callback(resolved.first, resolved.second)
                }
            })
        })
    }

    private fun renderPlayerStats(requester: Player, targetId: UUID, targetName: String) {
        val requesterId = requester.uniqueId
        CrateRollStatsStorage.snapshotAsync(targetId) { stats ->
            val viewer = Bukkit.getPlayer(requesterId) ?: return@snapshotAsync
            viewer.sendMessage(allTags.deserialize(
                "<cloudiecolor><b>Crate Stats for <white>$targetName</white></b></cloudiecolor> <gray>(total rolls: <white>${stats.totalRolls}</white>)</gray>"
            ))
            if (stats.crateCounts.isEmpty()) {
                viewer.sendMessage(allTags.deserialize("<gray>No crate rolls recorded yet.</gray>"))
                return@snapshotAsync
            }
            stats.crateCounts.toList().sortedByDescending { it.second }.forEach { (crateId, count) ->
                val name = CrateType.fromStoredId(crateId)?.name?.let(::humanizeStoredId) ?: humanizeStoredId(crateId)
                viewer.sendMessage(allTags.deserialize("<gray>-</gray> <white>$name</white>: <yellow>$count</yellow>"))
            }
            val top = stats.itemCounts.toList().filter { (id, _) -> isTrackedCollectible(id) }.sortedByDescending { it.second }.take(5)
            if (top.isNotEmpty()) {
                viewer.sendMessage(allTags.deserialize("<dark_gray>Top collectibles:</dark_gray>"))
                top.forEach { (id, count) ->
                    val itemName = CrateItem.fromStoredId(id)?.displayNamePlain ?: humanizeStoredId(id)
                    viewer.sendMessage(allTags.deserialize("<dark_gray>  -</dark_gray> <white>$itemName</white>: <yellow>$count</yellow>"))
                }
            }
        }
    }

    private fun renderPlayerCrateStats(requester: Player, targetId: UUID, targetName: String, crateType: CrateType) {
        val requesterId = requester.uniqueId
        CrateRollStatsStorage.snapshotAsync(targetId) { stats ->
            val viewer = Bukkit.getPlayer(requesterId) ?: return@snapshotAsync
            val items = crateCollectibles(crateType)
            if (items.isEmpty()) {
                viewer.sendMessage(allTags.deserialize("<gray>No collectible items configured for <white>${humanizeStoredId(crateType.storedId)}</white>.</gray>"))
                return@snapshotAsync
            }
            val totalRolls = items.sumOf { stats.itemCounts[it.storedId] ?: 0L }
            val collected = items.count { (stats.itemCounts[it.storedId] ?: 0L) > 0L }
            viewer.sendMessage(allTags.deserialize(
                "<cloudiecolor><b>Collectibles for <white>$targetName</white> in <white>${humanizeStoredId(crateType.storedId)}</white></b></cloudiecolor> <gray>(total collectible rolls: <white>$totalRolls</white>, Collected <white>$collected/${items.size}</white>)</gray>"
            ))
            items.sortedByDescending { stats.itemCounts[it.storedId] ?: 0L }.forEach { item ->
                val rolls = stats.itemCounts[item.storedId] ?: 0L
                viewer.sendMessage(allTags.deserialize("<dark_gray>  -</dark_gray> <white>${item.displayNamePlain}</white>: <yellow>$rolls</yellow>"))
            }
        }
    }

    private fun renderGlobalCrateStats(viewer: Player, crateType: CrateType) {
        val globalCounts = CrateRollStatsStorage.globalPlushieCounts()
        val items = crateCollectibles(crateType)
        if (items.isEmpty()) {
            viewer.sendMessage(allTags.deserialize("<gray>No collectible items configured for <white>${humanizeStoredId(crateType.storedId)}</white>.</gray>"))
            return
        }
        val totalRolls = items.sumOf { globalCounts[it.storedId] ?: 0L }
        val collected = items.count { (globalCounts[it.storedId] ?: 0L) > 0L }
        viewer.sendMessage(allTags.deserialize(
            "<cloudiecolor><b>Global Collectibles in <white>${humanizeStoredId(crateType.storedId)}</white></b></cloudiecolor> <gray>(total collectible rolls: <white>$totalRolls</white>, Collected <white>$collected/${items.size}</white>)</gray>"
        ))
        items.sortedByDescending { globalCounts[it.storedId] ?: 0L }.forEach { item ->
            val rolls = globalCounts[item.storedId] ?: 0L
            viewer.sendMessage(allTags.deserialize("<dark_gray>  -</dark_gray> <white>${item.displayNamePlain}</white>: <yellow>$rolls</yellow>"))
        }
    }

    private fun sendCollectibleCounts(viewer: Player, title: String, counts: List<Pair<String, Long>>, emptyMessage: String, maxLines: Int? = null) {
        if (counts.isEmpty()) {
            viewer.sendMessage(allTags.deserialize("<gray>$emptyMessage</gray>"))
            return
        }
        val totalPlushieRolls = counts.sumOf { it.second }
        viewer.sendMessage(allTags.deserialize(
            "<cloudiecolor><b>$title</b></cloudiecolor> <gray>(total collectible rolls: <white>$totalPlushieRolls</white>)</gray>"
        ))
        val displayed = maxLines?.let(counts::take) ?: counts
        displayed.forEachIndexed { index, (itemId, count) ->
            val itemName = CrateItem.fromStoredId(itemId)?.displayNamePlain ?: humanizeStoredId(itemId)
            viewer.sendMessage(allTags.deserialize("<gray>${index + 1}.</gray> <white>$itemName</white> <gray>-</gray> <yellow>$count</yellow>"))
        }
        if (maxLines != null && counts.size > maxLines) {
            viewer.sendMessage(allTags.deserialize("<dark_gray>...and ${counts.size - maxLines} more collectibles.</dark_gray>"))
        }
    }

    private fun isTrackedCollectible(itemId: String): Boolean {
        val crateItem = CrateItem.fromStoredId(itemId) ?: return false
        return crateItem.isPlushie || wearableItemIds.contains(crateItem.storedId)
    }

    private fun crateCollectibles(crateType: CrateType): List<CrateItem> =
        crateType.lootPool.possibleItems.filter { isTrackedCollectible(it.storedId) }.distinctBy(CrateItem::storedId)

    private fun resolveCrateType(crateName: String): CrateType? {
        val normalized = normalizeCrateName(crateName)
        return CrateType.entries.firstOrNull {
            normalizeCrateName(it.name) == normalized || normalizeCrateName(it.storedId) == normalized
        }
    }

    private fun normalizeCrateName(value: String) =
        value.lowercase(Locale.ENGLISH).replace("_", "").replace("-", "").replace(" ", "")

    private fun humanizeStoredId(value: String): String =
        value.lowercase(Locale.ENGLISH).split('_').joinToString(" ") { part ->
            part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
        }

    @Suggestions("stats-crate-names")
    fun suggestCrateNames(): List<String> = CrateType.entries.map { it.storedId.lowercase(Locale.ENGLISH) }.sorted()

    @Suggestions("stats-player-names")
    fun suggestPlayerNames(): List<String> = Bukkit.getOnlinePlayers().map { it.name }
}

