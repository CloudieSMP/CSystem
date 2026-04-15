package command

import chat.Formatting.allTags
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandContext
import plugin
import util.requirePlayer
import java.util.UUID


@Suppress("unused")
@CommandContainer
class Tpa {
    private val tpaRequests = mutableListOf<TpaRequest>()

    private val requestExpireTime: Int
        get() = plugin.config.tpa.requestExpireTime

    private val tpaDelay: Int
        get() = plugin.config.tpa.tpaDelay

    @Command("tpa <player>")
    @CommandDescription("Request to teleport to another player.")
    @Permission("cloudie.cmd.tpa")
    fun tpa(css: CommandSourceStack, @Argument("player") targetPlayer: Player) {
        createRequest(css, targetPlayer, TpaType.TPA_THERE)
    }

    @Command("tpahere <player>")
    @CommandDescription("Request to teleport another player here.")
    @Permission("cloudie.cmd.tpa")
    fun tpahere(css: CommandSourceStack, @Argument("player") targetPlayer: Player) {
        createRequest(css, targetPlayer, TpaType.TPA_HERE)
    }

    @Command("tpaccept|tpyes <player>")
    @CommandDescription("Accept a tpa request.")
    @Permission("cloudie.cmd.tpa")
    fun tpaccept(
        css: CommandSourceStack,
        @Argument(value = "player", suggestions = "incoming-tpa-requesters") requesterName: String
    ) {
        val player = css.requirePlayer() ?: return

        val tpaRequest = findIncomingRequest(player.uniqueId, requesterName)
        if (tpaRequest == null) {
            player.sendMessage(allTags.deserialize("<yellow>You don't have an incoming TPA request from <green>${requesterName}</green>."))
            return
        }

        val targetPlayer: Player = Bukkit.getPlayer(tpaRequest.requester) ?: run {
            player.sendMessage(allTags.deserialize("<yellow>Player is no longer online."))
            tpaRequests.remove(tpaRequest)
            return
        }

        tpaRequests.remove(tpaRequest)

        if (tpaRequest.type == TpaType.TPA_THERE) {
            player.sendMessage(allTags.deserialize(
                "<yellow>TPA request from <green>${targetPlayer.name}</green> accepted.\nThey will be teleported in <green>$tpaDelay</green> seconds."
            ))
            targetPlayer.sendMessage(allTags.deserialize(
                "<yellow>TPA request to <green>${player.name}</green> accepted.\nYou will be teleported to them in <green>$tpaDelay</green> seconds."
            ))

            scheduleDelayedTeleport(
                teleportingPlayerId = targetPlayer.uniqueId,
                destinationPlayerId = player.uniqueId,
                destinationName = player.name,
                requesterName = targetPlayer.name
            )
        } else {
            player.sendMessage(allTags.deserialize(
                "<yellow>TPA request from <green>${targetPlayer.name}</green> accepted.\nYou will be teleported in <green>$tpaDelay</green> seconds."
            ))
            targetPlayer.sendMessage(allTags.deserialize(
                "<yellow>TPA request to <green>${player.name}</green> accepted.\nThey will be teleported to you in <green>$tpaDelay</green> seconds."
            ))

            scheduleDelayedTeleport(
                teleportingPlayerId = player.uniqueId,
                destinationPlayerId = targetPlayer.uniqueId,
                destinationName = targetPlayer.name,
                requesterName = player.name
            )
        }
    }

    @Command("tpdeny|tpno <player>")
    @CommandDescription("Deny a tpa request.")
    @Permission("cloudie.cmd.tpa")
    fun tpdeny(
        css: CommandSourceStack,
        @Argument(value = "player", suggestions = "incoming-tpa-requesters") requesterName: String
    ) {
        val player = css.requirePlayer() ?: return

        val tpaRequest = findIncomingRequest(player.uniqueId, requesterName)
        if (tpaRequest == null) {
            player.sendMessage(allTags.deserialize("<yellow>You don't have an incoming TPA request from <green>${requesterName}</green>."))
            return
        }

        val target = Bukkit.getPlayer(tpaRequest.requester)
        val targetOffline = Bukkit.getOfflinePlayer(tpaRequest.requester)

        tpaRequests.remove(tpaRequest)

        player.sendMessage(allTags.deserialize(
            "<red>TPA request from <yellow>${targetOffline.name}</yellow> denied."
        ))

        target?.sendMessage(allTags.deserialize(
            "<red>TPA request to <yellow>${player.name}</yellow> denied."
        ))
    }

    private fun createRequest(css: CommandSourceStack, targetPlayer: Player, type: TpaType) {
        val player = css.requirePlayer() ?: return
        if (player.uniqueId == targetPlayer.uniqueId) {
            player.sendMessage(allTags.deserialize("<red>You cannot send a TPA request to yourself."))
            return
        }

        if (hasOutgoingRequest(player.uniqueId)) {
            player.sendMessage(allTags.deserialize("<red>You already have an outgoing TPA request pending."))
            return
        }

        val tpaRequest = TpaRequest(player.uniqueId, targetPlayer.uniqueId, player.name, type)
        tpaRequests.add(tpaRequest)
        deleteTpaAfterDelay(player, targetPlayer, requestExpireTime, tpaRequest)

        player.sendMessage(allTags.deserialize(
            "<yellow>Teleport request sent to <green>${targetPlayer.name}</green>.\nRequest will time out in <green>$requestExpireTime</green> seconds."
        ))
        targetPlayer.sendMessage(allTags.deserialize(
            "<yellow>You have an incoming TPA request from <green>${player.name}</green>. Type <click:run_command:'/tpaccept ${player.name}'><hover:show_text:'Accepts the TPA request.'><white>/tpaccept ${player.name}</white></hover></click> to accept or <click:run_command:'/tpdeny ${player.name}'><hover:show_text:'Denies the TPA request.'><white>/tpdeny ${player.name}</white></hover></click> to deny."
        ))
    }

    private fun hasOutgoingRequest(requesterId: UUID): Boolean {
        return tpaRequests.any { it.requester == requesterId }
    }

    private fun findIncomingRequest(targetId: UUID, requesterName: String): TpaRequest? {
        return tpaRequests.firstOrNull {
            it.target == targetId && it.requesterName.equals(requesterName, ignoreCase = true)
        }
    }

    @Suggestions("incoming-tpa-requesters")
    fun incomingRequesterSuggestions(context: CommandContext<CommandSourceStack>, input: String): List<String> {
        val player = context.sender().sender as? Player ?: return emptyList()
        return tpaRequests
            .asSequence()
            .filter { it.target == player.uniqueId }
            .map { it.requesterName }
            .distinct()
            .filter { it.startsWith(input, ignoreCase = true) }
            .sorted()
            .toList()
    }

    private fun scheduleDelayedTeleport(
        teleportingPlayerId: UUID,
        destinationPlayerId: UUID,
        destinationName: String,
        requesterName: String
    ) {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            val teleportingPlayer = Bukkit.getPlayer(teleportingPlayerId)
            val destinationPlayer = Bukkit.getPlayer(destinationPlayerId)

            if (teleportingPlayer == null || destinationPlayer == null) {
                teleportingPlayer?.sendMessage(allTags.deserialize("<red>Teleport cancelled because the other player went offline.</red>"))
                destinationPlayer?.sendMessage(allTags.deserialize("<red>Teleport cancelled because the other player went offline.</red>"))
                return@Runnable
            }

            val success = teleportingPlayer.teleport(destinationPlayer)
            if (!success) {
                teleportingPlayer.sendMessage(allTags.deserialize("<red>Teleport to <yellow>$destinationName</yellow> failed.</red>"))
                destinationPlayer.sendMessage(allTags.deserialize("<red>Teleport for <yellow>$requesterName</yellow> failed.</red>"))
            }
        }, tpaDelay * 20L)
    }

    private fun deleteTpaAfterDelay(player: Player, target: Player, requestTimeout: Int, tpaRequest: TpaRequest) {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (tpaRequests.remove(tpaRequest)) {
                Bukkit.getPlayer(player.uniqueId)?.sendMessage(allTags.deserialize("<yellow>TPA request to <green>${target.name}</green> has timed out."))
                Bukkit.getPlayer(target.uniqueId)?.sendMessage(allTags.deserialize("<yellow>TPA Request from <green>${player.name}</green> has timed out."))
            }
        }, requestTimeout * 20L)
    }

    data class TpaRequest(
        val requester: UUID,
        val target: UUID,
        val requesterName: String,
        val type: TpaType = TpaType.TPA_THERE
    )

    enum class TpaType {
        TPA_THERE,
        TPA_HERE
    }
}
