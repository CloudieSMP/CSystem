package event.block

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.BlockType
import org.bukkit.event.Listener
import plugin

class StonecutterDamageListener : Listener {

    init {
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            for (player in Bukkit.getOnlinePlayers()) {
                if (player.gameMode == org.bukkit.GameMode.CREATIVE || player.gameMode == org.bukkit.GameMode.SPECTATOR) continue
                val block = player.location.block
                if (block.type == Material.STONECUTTER) {
                    player.damage(1.0)
                    player.world.spawnParticle(Particle.BLOCK, player.location, 20, 0.4, 0.0, 0.4, BlockType.REDSTONE_BLOCK.createBlockData())
                }
            }
        }, 0L, 10L)
    }
}