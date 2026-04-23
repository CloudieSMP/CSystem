package event.player

import chat.Formatting
import library.LiveHelper
import command.ShowStat
import item.crate.CrateMetadataRefresher
import item.crate.CrateRecipes
import library.HomeStorage
import library.MailStorage
import library.CrateRollStatsStorage
import library.Translation
import logger
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import library.VanishHelper
import org.bukkit.Bukkit
import util.ResourcePacker

@Suppress("UnstableApiUsage")
class PlayerJoin : Listener {
    private val mm = MiniMessage.miniMessage()

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        CrateRecipes.discoverAll(e.player)
        HomeStorage.preload(e.player.uniqueId)
        MailStorage.preload(e.player.uniqueId)
        CrateRollStatsStorage.preload(e.player.uniqueId)
        CrateMetadataRefresher.refreshPlayerInventories(e.player)
        sendTabList(e.player)

        val brand = e.player.clientBrandName
            ?.replaceFirstChar { it.uppercaseChar() }
            ?: "Unknown"
        logger.info("(BRAND) ${e.player.name} joined using $brand.")

        ResourcePacker.applyPackPlayer(e.player)

        e.player.sendMessage(mm.deserialize("<red>⚠ <reset>Please <b>do not</b> break loot chests!"))
        if(e.player.hasPermission("cloudie.silent.join")) {
            e.joinMessage(null)
        } else {
            e.joinMessage(Formatting.allTags.deserialize(Translation.PlayerMessages.JOIN.replace("%player%", e.player.name)))
        }

        LiveHelper.onPlayerJoin(e.player)
        VanishHelper.syncVisibilityForJoin(e.player)

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
                e.player.sendMessage(mm.deserialize("<green>You have new mail! Use <white><click:run_command:/mail inbox>/mail inbox </white>to check it out."))
            }
        }
    }

    private fun sendTabList(audience: Audience) {
        audience.sendPlayerListHeader(mm.deserialize("<newline><newline><newline><newline><newline>     \uE000    <newline>"))
        audience.sendPlayerListFooter(mm.deserialize("<newline><gradient:#DF6F69:#C45889:#823BC6>  Cloudie SMP<white>: Season 10  <newline>"))
    }

}