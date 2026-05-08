package event.player

import chat.Formatting.allTags
import library.LiveHelper
import command.ShowStat
import item.crate.CrateMetadataRefresher
import item.crate.CrateRecipes
import item.plushiebox.PlushieBox
import library.HomeStorage
import library.MailStorage
import library.CrateRollStatsStorage
import library.GhostMode
import library.PlayerListNameHelper
import library.TagHelper
import library.Translation
import logger
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.Bukkit
import plugin
import util.ResourcePacker

@Suppress("UnstableApiUsage")
class PlayerJoin : Listener {

    companion object {
        private fun isVanished(player: Player): Boolean =
            player.getMetadata("vanished").any { it.asBoolean() }

        fun refreshTabListForAll() {
            val count = Bukkit.getOnlinePlayers().count { player ->
                !GhostMode.isGhost(player) &&
                !isVanished(player) &&
                player.gameMode != GameMode.SPECTATOR &&
                !player.hasPermission("cloudie.group.alt")
            }
            Bukkit.getOnlinePlayers().forEach { sendTabList(it, count) }
        }

        private fun sendTabList(player: Player, count: Int) {
            player.sendPlayerListHeader(allTags.deserialize("<newline><newline><newline><newline><newline>     \uE000    <newline><gray>  $count online  "))
            player.sendPlayerListFooter(allTags.deserialize("<newline><gradient:#DF6F69:#C45889:#823BC6>  Cloudie SMP<white>: Season 10  <newline>"))
        }
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        CrateRecipes.discoverAll(e.player)
        PlushieBox.discoverRecipe(e.player)
        HomeStorage.preload(e.player.uniqueId)
        MailStorage.preload(e.player.uniqueId)
        CrateRollStatsStorage.preload(e.player.uniqueId)
        CrateMetadataRefresher.refreshPlayerInventories(e.player)
        TagHelper.ensurePlayer(e.player)
        refreshTabListForAll()

        val brand = e.player.clientBrandName
            ?.replaceFirstChar { it.uppercaseChar() }
            ?: "Unknown"
        logger.info("(BRAND) ${e.player.name} joined using $brand.")

        ResourcePacker.applyPackPlayer(e.player)

        if (e.player.hasPermission("cloudie.group.admin")) {
            e.player.sendOpLevel(2)
        }

        e.player.sendMessage(allTags.deserialize("<red>⚠ <reset>Please <b>do not</b> break loot chests!"))
        if (e.player.hasPermission("cloudie.silent.join")) {
            e.joinMessage(null)
        } else if (e.player.hasPermission("cloudie.group.ghost")) {
            GhostMode.toggleGhostMode(e.player)
            e.joinMessage(null)
        } else {
            e.joinMessage(allTags.deserialize(Translation.PlayerMessages.JOIN.replace("%player%", e.player.name)))
        }

        LiveHelper.onPlayerJoin(e.player)

        // Keep the alt-account cache up to date for /showstat filtering.
        if (e.player.hasPermission("cloudie.group.alt")) {
            ShowStat.altUuids.add(e.player.uniqueId)
        } else {
            ShowStat.altUuids.remove(e.player.uniqueId)
        }
        ShowStat.saveSync()

        e.player.sendLinks(Bukkit.getServerLinks())
        MailStorage.hasNewMailAsync(e.player.uniqueId) { hasNewMail ->
            if (hasNewMail) {
                e.player.sendMessage(allTags.deserialize("<green>You have new mail! Use <white><click:run_command:/mail inbox>/mail inbox </white>to check it out."))
            }
        }
        PlayerListNameHelper.apply(e.player)
    }

    @EventHandler
    fun onGameModeChange(e: PlayerGameModeChangeEvent) {
        // Update count when switching to/from spectator
        val wasSpectator = e.player.gameMode == GameMode.SPECTATOR
        val willBeSpectator = e.newGameMode == GameMode.SPECTATOR
        if (wasSpectator != willBeSpectator) {
            // Schedule for next tick so the gamemode is already applied when we count
            Bukkit.getScheduler().runTask(plugin, Runnable { refreshTabListForAll() })
        }
    }

}