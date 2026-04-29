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
import org.incendo.cloud.annotations.suggestion.Suggestions
import plugin
import util.requirePlayer
import java.util.Locale
import java.util.UUID

@Suppress("unused", "unstableApiUsage")
@CommandContainer
class CrateStats {
    private val wearableItemIds: Set<String> = CrateType.WEARABLES.lootPool.possibleItems
        .map(CrateItem::storedId)
        .toSet()

    @Command("cratestats")
    @CommandDescription("Show your crate roll stats.")
    @Permission("cloudie.cmd.cratestats")
    fun self(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        renderPlayerStats(player, player.uniqueId, player.name)
    }

    @Command("cratestats me <crateName>")
    @CommandDescription("Show your collectible stats for a specific crate.")
    @Permission("cloudie.cmd.cratestats")
    fun selfByCrate(css: CommandSourceStack, @Argument(value = "crateName", suggestions = "crate-names") crateName: String) {
        val player = css.requirePlayer() ?: return
        val crateType = resolveCrateType(crateName)
        if (crateType == null) {
            player.sendMessage(allTags.deserialize("<gray>Unknown crate <white>$crateName</white>.</gray>"))
            return
        }
        renderPlayerCrateStats(player, player.uniqueId, player.name, crateType)
    }

    @Command("cratestats global")
    @CommandDescription("Show global collectible roll stats.")
    @Permission("cloudie.cmd.cratestats")
    fun global(css: CommandSourceStack) {
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

    @Command("cratestats global <crateName>")
    @CommandDescription("Show global collectible stats for a specific crate.")
    @Permission("cloudie.cmd.cratestats")
    fun globalByCrate(css: CommandSourceStack, @Argument(value = "crateName", suggestions = "crate-names") crateName: String) {
        val player = css.requirePlayer() ?: return
        val crateType = resolveCrateType(crateName)
        if (crateType == null) {
            player.sendMessage(allTags.deserialize("<gray>Unknown crate <white>$crateName</white>.</gray>"))
            return
        }
        renderGlobalCrateStats(player, crateType)
    }

    @Command("cratestats <player>")
    @CommandDescription("Show another player's crate roll stats.")
    @Permission("cloudie.cmd.cratestats.other")
    fun other(css: CommandSourceStack, @Argument(value = "player", suggestions = "player-names") playerName: String) {
        val requester = css.requirePlayer() ?: return
        resolveOfflinePlayer(requester, playerName) { uuid, name ->
            renderPlayerStats(requester, uuid, name)
        }
    }

    @Command("cratestats <player> <crateName>")
    @CommandDescription("Show another player's collectible stats for a specific crate.")
    @Permission("cloudie.cmd.cratestats.other")
    fun otherByCrate(
        css: CommandSourceStack,
        @Argument(value = "player", suggestions = "player-names") playerName: String,
        @Argument(value = "crateName", suggestions = "crate-names") crateName: String,
    ) {
        val requester = css.requirePlayer() ?: return
        val crateType = resolveCrateType(crateName)
        if (crateType == null) {
            requester.sendMessage(allTags.deserialize("<gray>Unknown crate <white>$crateName</white>.</gray>"))
            return
        }
        resolveOfflinePlayer(requester, playerName) { uuid, name ->
            renderPlayerCrateStats(requester, uuid, name, crateType)
        }
    }

    /**
     * Resolves a player name to a UUID + display name, supporting offline players.
     * The [callback] is invoked on the Bukkit main thread if the player is found.
     */
    private fun resolveOfflinePlayer(requester: Player, playerName: String, callback: (UUID, String) -> Unit) {
        val online = Bukkit.getPlayerExact(playerName)
        if (online != null) {
            callback(online.uniqueId, online.name)
            return
        }

        val requesterId = requester.uniqueId
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val offline = Bukkit.getOfflinePlayer(playerName)
            val resolved = if (offline.hasPlayedBefore() || offline.isOnline) {
                offline.uniqueId to (offline.name ?: playerName)
            } else {
                null
            }

            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (resolved == null) {
                    Bukkit.getPlayer(requesterId)?.sendMessage(
                        allTags.deserialize("<gray>Player <white>$playerName</white> not found.</gray>")
                    )
                } else {
                    callback(resolved.first, resolved.second)
                }
            })
        })
    }

    private fun renderPlayerStats(requester: Player, targetId: UUID, targetName: String) {
        val requesterId = requester.uniqueId

        CrateRollStatsStorage.snapshotAsync(targetId) { stats ->
            val onlineRequester = Bukkit.getPlayer(requesterId) ?: return@snapshotAsync

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

            val topCollectibles = stats.itemCounts
                .toList()
                .filter { (itemId, _) -> isTrackedCollectible(itemId) }
                .sortedByDescending { it.second }
                .take(5)

            if (topCollectibles.isNotEmpty()) {
                onlineRequester.sendMessage(allTags.deserialize("<dark_gray>Top collectibles:</dark_gray>"))
                topCollectibles.forEach { (itemId, count) ->
                    val itemName = CrateItem.fromStoredId(itemId)?.displayNamePlain ?: humanizeStoredId(itemId)
                    onlineRequester.sendMessage(allTags.deserialize("<dark_gray>  -</dark_gray> <white>$itemName</white>: <yellow>$count</yellow>"))
                }
            }
        }
    }

    private fun renderPlayerCrateStats(requester: Player, targetId: UUID, targetName: String, crateType: CrateType) {
        val requesterId = requester.uniqueId

        CrateRollStatsStorage.snapshotAsync(targetId) { stats ->
            val onlineRequester = Bukkit.getPlayer(requesterId) ?: return@snapshotAsync
            val crateItems = crateCollectibles(crateType)

            if (crateItems.isEmpty()) {
                onlineRequester.sendMessage(allTags.deserialize("<gray>No collectible items configured for <white>${humanizeStoredId(crateType.storedId)}</white>.</gray>"))
                return@snapshotAsync
            }

            val totalRolls = crateItems.sumOf { stats.itemCounts[it.storedId] ?: 0L }
            val collected = crateItems.count { (stats.itemCounts[it.storedId] ?: 0L) > 0L }
            val possible = crateItems.size

            onlineRequester.sendMessage(
                allTags.deserialize(
                    "<cloudiecolor><b>Collectibles for <white>$targetName</white> in <white>${humanizeStoredId(crateType.storedId)}</white></b></cloudiecolor> <gray>(total collectible rolls: <white>$totalRolls</white>, Collected <white>$collected/$possible</white>)</gray>"
                )
            )

            crateItems
                .sortedByDescending { stats.itemCounts[it.storedId] ?: 0L }
                .forEach { item ->
                    val rolls = stats.itemCounts[item.storedId] ?: 0L
                    onlineRequester.sendMessage(
                        allTags.deserialize("<dark_gray>  -</dark_gray> <white>${item.displayNamePlain}</white>: <yellow>$rolls</yellow>")
                    )
                }
        }
    }

    private fun renderGlobalCrateStats(viewer: Player, crateType: CrateType) {
        val globalCounts = CrateRollStatsStorage.globalPlushieCounts()
        val crateItems = crateCollectibles(crateType)

        if (crateItems.isEmpty()) {
            viewer.sendMessage(allTags.deserialize("<gray>No collectible items configured for <white>${humanizeStoredId(crateType.storedId)}</white>.</gray>"))
            return
        }

        val totalRolls = crateItems.sumOf { globalCounts[it.storedId] ?: 0L }
        val collected = crateItems.count { (globalCounts[it.storedId] ?: 0L) > 0L }
        val possible = crateItems.size

        viewer.sendMessage(
            allTags.deserialize(
                "<cloudiecolor><b>Global Collectibles in <white>${humanizeStoredId(crateType.storedId)}</white></b></cloudiecolor> <gray>(total collectible rolls: <white>$totalRolls</white>, Collected <white>$collected/$possible</white>)</gray>"
            )
        )

        crateItems
            .sortedByDescending { globalCounts[it.storedId] ?: 0L }
            .forEach { item ->
                val rolls = globalCounts[item.storedId] ?: 0L
                viewer.sendMessage(allTags.deserialize("<dark_gray>  -</dark_gray> <white>${item.displayNamePlain}</white>: <yellow>$rolls</yellow>"))
            }
    }

    private fun sendCollectibleCounts(
        viewer: Player,
        title: String,
        counts: List<Pair<String, Long>>,
        emptyMessage: String,
        maxLines: Int? = null,
    ) {
        if (counts.isEmpty()) {
            viewer.sendMessage(allTags.deserialize("<gray>$emptyMessage</gray>"))
            return
        }

        val totalPlushieRolls = counts.sumOf { it.second }
        viewer.sendMessage(
            allTags.deserialize(
                "<cloudiecolor><b>$title</b></cloudiecolor> <gray>(total collectible rolls: <white>$totalPlushieRolls</white>)</gray>"
            )
        )

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

    private fun crateCollectibles(crateType: CrateType): List<CrateItem> {
        return crateType.lootPool.possibleItems
            .filter { item -> isTrackedCollectible(item.storedId) }
            .distinctBy(CrateItem::storedId)
    }

    private fun resolveCrateType(crateName: String): CrateType? {
        val normalized = normalizeCrateName(crateName)
        return CrateType.entries.firstOrNull { crateType ->
            normalizeCrateName(crateType.name) == normalized || normalizeCrateName(crateType.storedId) == normalized
        }
    }

    private fun normalizeCrateName(value: String): String {
        return value
            .lowercase(Locale.ENGLISH)
            .replace("_", "")
            .replace("-", "")
            .replace(" ", "")
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

    @Suggestions("crate-names")
    fun suggestCrateNames(): List<String> {
        return CrateType.entries
            .map { it.storedId.lowercase(Locale.ENGLISH) }
            .sorted()
    }

    @Suggestions("player-names")
    fun suggestPlayerNames(): List<String> {
        return Bukkit.getOnlinePlayers().map { it.name }
    }
}
