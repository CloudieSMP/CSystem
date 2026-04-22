package command

import chat.Formatting.allTags
import fr.mrmicky.fastboard.adventure.FastBoard
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.Statistic
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import plugin
import util.Sounds.ERROR_DIDGERIDOO
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*


@Suppress("unused", "unstableApiUsage")
@CommandContainer
class ShowStat {

    val pageSize = 14
    val secondsPerPage get() = plugin.config.showStat.secondsPerPage
    var isActive = false

    @Command("showstat|sb <stat>")
    @Permission("cloudie.cmd.showstat")
    fun showStat(css: CommandSourceStack,
                 stat: Statistic,
                 @Flag("material", aliases = ["m"]) material: Material?,
                 @Flag("entity", aliases = ["e"]) entityType: EntityType?,
                 @Flag("online", aliases = ["o"]) onlineOnly: Boolean = false,
                 @Flag("alts", aliases = ["a"]) includeAlts: Boolean = false,
                 @Flag("page", aliases = ["p"]) page: Int? = null) {
        val player = css.sender as? Player ?: return

        if (isActive) {
            player.sendMessage(allTags.deserialize("<red>Other stats are already being shown, please wait for them to finish."))
            player.playSound(ERROR_DIDGERIDOO)
            return
        }

        val players = if (onlineOnly) {
            Bukkit.getOnlinePlayers().map { it as OfflinePlayer }.toTypedArray()
        } else {
            Bukkit.getServer().offlinePlayers
        }.let { all ->
            if (includeAlts) all
            else all.filter { p ->
                // For online players we can check live; for offline we rely on the cached set.
                val onlineHandle = p.player
                if (onlineHandle != null) !onlineHandle.hasPermission("cloudie.group.alt")
                else p.uniqueId !in altUuids
            }.toTypedArray()
        }

        val sbEntries = mutableListOf<Pair<Component, Int>>()
        when (stat.type) {
            Statistic.Type.UNTYPED -> {
                sbEntries.addAll(players.map { Pair(formatPlayerName(it), it.getStatistic(stat)) }.toMutableList())
            }
            Statistic.Type.ITEM, Statistic.Type.BLOCK -> {
                if (material == null) {
                    player.sendMessage(allTags.deserialize("<red>Missing material, please specify using the --material flag."))
                    player.playSound(ERROR_DIDGERIDOO)
                    return
                }
                sbEntries.addAll(players.map { Pair(formatPlayerName(it), it.getStatistic(stat, material)) }.toMutableList())
            }
            Statistic.Type.ENTITY -> {
                if (entityType == null) {
                    player.sendMessage(allTags.deserialize("<red>Missing entity, please specify using the --entity flag."))
                    player.playSound(ERROR_DIDGERIDOO)
                    return
                }
                sbEntries.addAll(players.map { Pair(formatPlayerName(it), it.getStatistic(stat, entityType)) }.toMutableList())
            }
        }

        sbEntries.removeIf { it.second == 0 }

        val sum = sbEntries.sumOf { it.second }
        sbEntries.addFirst(Pair(allTags.deserialize("<shadow:black><#ff65aa><u>Total"), sum))

        val sorted = sbEntries.sortedByDescending { it.second }

        isActive = true
        val formatter = formatterFor(stat)
        val statScoreboardRunnable = object : BukkitRunnable() {
            var pageIndex = if (page != null) (page - 1).coerceAtLeast(0) else 0
            val singlePage = page != null
            override fun run() {
                val pages = sorted.chunked(pageSize)

                if (singlePage && pageIndex > pages.lastIndex) {
                    player.sendMessage(allTags.deserialize("<red>Page $page does not exist. There ${if (pages.size == 1) "is" else "are"} only ${pages.size} page${if (pages.size == 1) "" else "s"}."))
                    player.playSound(ERROR_DIDGERIDOO)
                    isActive = false
                    this.cancel()
                    return
                }

                if (pageIndex <= pages.lastIndex) {
                    val page = pages[pageIndex].toMutableList()

                    val title = allTags.deserialize("<shadow:black><gradient:#FDCFFA:#D78FEE>${snakeCaseToSpaced(stat.name)}${
                        when (stat.type) {
                            Statistic.Type.ITEM, Statistic.Type.BLOCK -> " <gradient:#9B5DE0:#4E56C0>(${snakeCaseToSpaced(material!!.name)})"
                            Statistic.Type.ENTITY -> " <gradient:#9B5DE0:#4E56C0>(${snakeCaseToSpaced(entityType!!.name)})"
                            else -> ""
                        }
                    } <#4E56C0>[<#FDCFFA>${pageIndex+1}/${pages.size}<#4E56C0>]")

                    broadcastScoreboardLines(title, page, formatter)
                    if (singlePage) {
                        clearScoreboards(secondsPerPage * 20L)
                        isActive = false
                        this.cancel()
                    } else {
                        pageIndex++
                    }
                } else {
                    clearScoreboards(20L)
                    isActive = false
                    this.cancel()
                }
            }
        }
        statScoreboardRunnable.runTaskTimer(plugin, 0L, secondsPerPage * 20L)
    }

    private fun broadcastScoreboardLines(title: Component, lines: List<Pair<Component, Int>>, formatter: (Int) -> Component = ::formatInteger) {
        for (player in Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("cloudie.dontshowstatscreen")) continue
            val board = FastBoard(player)
            board.updateTitle(title)
            val names = lines.map { it.first }
            val scores = lines.map { formatter(it.second) }
            board.updateLines(names, scores)
        }
    }

    private fun clearScoreboards(delay: Long) {
        object : BukkitRunnable() {
            override fun run() {
                for (player in Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("cloudie.dontshowstatscreen")) continue
                    val board = FastBoard(player)
                    board.delete()
                }
            }
        }.runTaskLater(plugin, delay)
    }

    private fun formatPlayerName(offlinePlayer: OfflinePlayer): Component {
        if (offlinePlayer.name == null) return text("Unknown")

        return if (offlinePlayer.isOnline) {
            allTags.deserialize("<cloudiecolor><shadow:black>${offlinePlayer.name}")
        } else {
            allTags.deserialize("<white><shadow:black>${offlinePlayer.name}")
        }
    }

    companion object {
        /** UUIDs of players with the `cloudie.group.alt` permission, populated on join. */
        val altUuids: MutableSet<UUID> = Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap())

        private val altFile get() = plugin.dataFolder.resolve("alts.txt")

        /** Load alt UUIDs from disk. Call on plugin enable. */
        fun loadSync() {
            val file = altFile
            if (!file.exists()) return
            altUuids.clear()
            file.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) runCatching { altUuids.add(UUID.fromString(trimmed)) }
            }
        }

        /** Persist alt UUIDs to disk. Call on plugin disable and after every update. */
        fun saveSync() {
            val file = altFile
            file.parentFile?.mkdirs()
            file.writeText(altUuids.joinToString("\n"))
        }

        /** Statistics whose raw value is in ticks (20 ticks = 1 second). */
        val TIME_STATS = setOf(
            Statistic.PLAY_ONE_MINUTE,
            Statistic.TIME_SINCE_DEATH,
            Statistic.TIME_SINCE_REST,
            Statistic.TOTAL_WORLD_TIME,
        )

        /** Statistics whose raw value is in centimetres. */
        val DISTANCE_STATS = setOf(
            Statistic.WALK_ONE_CM,
            Statistic.SPRINT_ONE_CM,
            Statistic.CROUCH_ONE_CM,
            Statistic.SWIM_ONE_CM,
            Statistic.FALL_ONE_CM,
            Statistic.CLIMB_ONE_CM,
            Statistic.FLY_ONE_CM,
            Statistic.WALK_ON_WATER_ONE_CM,
            Statistic.WALK_UNDER_WATER_ONE_CM,
            Statistic.MINECART_ONE_CM,
            Statistic.BOAT_ONE_CM,
            Statistic.PIG_ONE_CM,
            Statistic.HORSE_ONE_CM,
            Statistic.AVIATE_ONE_CM,
            Statistic.STRIDER_ONE_CM,
        )
    }

    /** Choose the right formatter for [stat]. */
    fun formatterFor(stat: Statistic): (Int) -> Component = when (stat) {
        in TIME_STATS -> ::formatTicks
        in DISTANCE_STATS -> ::formatCentimetres
        else -> ::formatInteger
    }

    private fun formatInteger(number: Int): Component {
        val symbols = DecimalFormatSymbols(Locale.forLanguageTag("de-CH"))
        symbols.groupingSeparator = '\''
        val formatter = DecimalFormat("#,##0", symbols)
        return allTags.deserialize("<red><shadow:black>${formatter.format(number)}")
    }

    /** Converts ticks to a human-readable duration, e.g. `3d 2h 15m`. */
    private fun formatTicks(ticks: Int): Component {
        val totalSeconds = ticks / 20
        val days    = totalSeconds / 86400
        val hours   = (totalSeconds % 86400) / 3600
        val minutes = (totalSeconds % 3600) / 60

        val parts = buildList {
            if (days > 0)    add("${days}d")
            if (hours > 0)   add("${hours}h")
            if (minutes > 0 || (days == 0 && hours == 0)) add("${minutes}m")
        }
        return allTags.deserialize("<red><shadow:black>${parts.joinToString(" ")}")
    }

    /** Converts centimetres to km (≥ 1 000 m) or m. */
    private fun formatCentimetres(cm: Int): Component {
        val metres = cm / 100.0
        val text = if (metres >= 1000) {
            "%.2f km".format(metres / 1000.0)
        } else {
            "%.1f m".format(metres)
        }
        return allTags.deserialize("<red><shadow:black>$text")
    }

    private fun snakeCaseToSpaced(snakeCaseStr: String): String {
        return snakeCaseStr.split("_").joinToString(" ") { str ->
            str.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
