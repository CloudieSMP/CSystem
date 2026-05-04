package library

import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import net.minecraft.network.Connection
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.server.network.ServerCommonPacketListenerImpl
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import plugin

/**
 * Allows players with [PERMISSION] to use F3+N (spectator toggle) and F3+F4 (game mode switcher)
 * without being server operators, by sending an elevated op-permission-level packet to the client.
 *
 * The client requires op level ≥ 2 to show the F3+N / F3+F4 HUD features.
 * Vanilla sends level 0 for non-op players; we intercept that packet and replace it with level 2.
 */
object F3NPermHelper {

    const val PERMISSION = "cloudie.f3nperm"

    // Op level 2 (WORLD_COMMANDS) unlocks F3+N and F3+F4 on the client.
    // The wire byte is the level plus a base offset of 24: level 2 → byte 26.
    private const val F3N_LEVEL_BYTE: Byte = (24 + 2).toByte()
    private const val OP_STATUS_MIN = 24
    private const val OP_STATUS_MAX = 28

    private const val HANDLER_NAME = "csystem_f3nperm"

    // Lazily-reflected fields; accessed only after the class is first loaded so
    // Paper's --add-opens JVM flags are in effect by then.
    private val connectionField by lazy {
        ServerCommonPacketListenerImpl::class.java
            .getDeclaredField("connection")
            .also { it.isAccessible = true }
    }

    private val channelField by lazy {
        Connection::class.java
            .getDeclaredField("channel")
            .also { it.isAccessible = true }
    }

    private val entityIdField by lazy {
        ClientboundEntityEventPacket::class.java
            .getDeclaredField("entityId")
            .also { it.isAccessible = true }
    }

    private val eventIdField by lazy {
        ClientboundEntityEventPacket::class.java
            .getDeclaredField("eventId")
            .also { it.isAccessible = true }
    }

    /**
     * Injects a Netty pipeline handler for [player] that intercepts any outgoing
     * op-level status packets and replaces them with the elevated level when the
     * player has [PERMISSION].
     */
    fun inject(player: Player) {
        val channel = getChannel(player) ?: return
        if (channel.pipeline().get(HANDLER_NAME) != null) return
        channel.pipeline().addBefore("packet_handler", HANDLER_NAME, Handler(player))
    }

    /**
     * Removes the injected Netty handler for [player] (call on player quit).
     */
    fun uninject(player: Player) {
        val channel = getChannel(player) ?: return
        if (channel.pipeline().get(HANDLER_NAME) != null) {
            channel.pipeline().remove(HANDLER_NAME)
        }
    }

    /**
     * Sends the elevated op-level packet directly to [player] if they have [PERMISSION].
     * Call this once after join (with a short delay so the permission system has loaded).
     */
    fun sendElevatedOpLevel(player: Player) {
        if (!player.hasPermission(PERMISSION)) return
        val serverPlayer = (player as CraftPlayer).handle
        val packet = ClientboundEntityEventPacket(serverPlayer, F3N_LEVEL_BYTE)
        serverPlayer.connection.send(packet)
    }

    private fun getChannel(player: Player): Channel? {
        return try {
            val serverPlayer = (player as CraftPlayer).handle
            val connection = connectionField.get(serverPlayer.connection) as Connection
            channelField.get(connection) as Channel
        } catch (e: Exception) {
            plugin.logger.warning("F3NPerm: Could not obtain Netty channel for ${player.name}: ${e.javaClass.simpleName} - ${e.message}")
            null
        }
    }

    private class Handler(private val player: Player) : ChannelDuplexHandler() {
        override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
            if (msg is ClientboundEntityEventPacket && player.hasPermission(PERMISSION)) {
                try {
                    val entityId = entityIdField.getInt(msg)
                    val eventId = eventIdField.getByte(msg).toInt()
                    if (entityId == player.entityId && eventId in OP_STATUS_MIN..OP_STATUS_MAX) {
                        val serverPlayer = (player as CraftPlayer).handle
                        val newPacket = ClientboundEntityEventPacket(serverPlayer, F3N_LEVEL_BYTE)
                        super.write(ctx, newPacket, promise)
                        return
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("F3NPerm: Could not adjust op-level packet for ${player.name}: ${e.javaClass.simpleName} - ${e.message}")
                }
            }
            super.write(ctx, msg, promise)
        }
    }
}
