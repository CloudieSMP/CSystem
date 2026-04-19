package util.ui

import chat.Formatting
import com.noxcrew.interfaces.InterfacesConstants
import com.noxcrew.interfaces.drawable.Drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.buildChestInterface
import item.crate.CrateItem
import item.crate.CrateType
import kotlinx.coroutines.launch
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import plugin
import util.Sounds.GAMBLING_WHEEL_STOP
import util.Sounds.GAMBLING_WHEEL_TICK
import kotlin.math.roundToInt
import kotlin.random.Random
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private class GamblingWindowSession(
    val crateType: CrateType,
) {
    var inventory: Inventory? = null
    var spinTask: BukkitRunnable? = null
    var wheelOffset: Int = 0
    var isSpinning: Boolean = false
    var spinStepsDone: Int = 0
    var spinTicksUntilNextStep: Int = 0
    var spinTargetSteps: Int = 0
    var winningItem: CrateItem? = null
    var rewardGranted: Boolean = false
    var spinConsumed: Boolean = false
}

object GamblingWindow : Listener {
    private val wheelSlots = listOf(11, 12, 13, 14, 15)
    private val winningWheelSlot = 13
    private val fillerPane = ItemStack(Material.PURPLE_STAINED_GLASS_PANE)
    private val idleWheelItem = ItemStack(Material.WHITE_WOOL)
    private val winningPaneSlots = listOf(4, 22)
    private val winningFillerPane = ItemStack(Material.LIME_STAINED_GLASS_PANE)
    private val sessions = ConcurrentHashMap<UUID, GamblingWindowSession>()

    fun open(player: Player, crateType: CrateType) {
        sessions.remove(player.uniqueId)?.let { existing ->
            if (existing.isSpinning) {
                awardPendingReward(existing, player, "closed early")
            }
            stopSpin(existing)
        }

        val iface = buildChestInterface {
            rows = 3
            titleSupplier = { crateType.displayName }
            withTransform { pane, _ ->
                for (slot in 0 until 27) {
                    val item = if (slot in wheelSlots) {
                        idleWheelItem
                    } else if (slot in winningPaneSlots) {
                        winningFillerPane
                    } else {
                        fillerPane
                    }
                    pane[slot / 9, slot % 9] = StaticElement(Drawable.drawable(item))
                }
            }
        }

        InterfacesConstants.SCOPE.launch {
            iface.open(player)
            val session = GamblingWindowSession(crateType)

            // Wait until the opened top inventory is the expected chest-sized window before writing wheel slots.
            object : BukkitRunnable() {
                var attempts = 0

                override fun run() {
                    if (!player.isOnline) {
                        cancel()
                        return
                    }

                    val topInventory = player.openInventory.topInventory
                    val canRenderWheel =
                        topInventory.type == InventoryType.CHEST && topInventory.size > wheelSlots.maxOrNull()!!

                    if (canRenderWheel) {
                        session.inventory = topInventory
                        sessions[player.uniqueId] = session
                        startSpin(session, player)
                        cancel()
                        return
                    }

                    attempts++
                    if (attempts >= 20) {
                        cancel()
                    }
                }
            }.runTaskTimer(plugin, 1L, 1L)
        }
    }

    @EventHandler
    fun onClick(e: InventoryClickEvent) {
        val player = e.whoClicked as? Player ?: return
        val session = sessions[player.uniqueId] ?: return
        val sessionInventory = session.inventory ?: return
        if (e.view.topInventory != sessionInventory) return
        e.isCancelled = true
    }

    @EventHandler
    fun onClose(e: InventoryCloseEvent) {
        val player = e.player as? Player
        if (player == null) return
        val session = sessions[player.uniqueId] ?: return
        val sessionInventory = session.inventory ?: return
        if (e.inventory != sessionInventory) return

        if (session.isSpinning) {
            awardPendingReward(session, player, "closed early")
        }

        stopSpin(session)
        sessions.remove(player.uniqueId)
    }

    private fun startSpin(session: GamblingWindowSession, player: Player) {
        if (session.isSpinning || session.spinConsumed) return

        val pool = session.crateType.lootPool.possibleItems
        if (pool.isEmpty()) {
            player.sendMessage(Formatting.allTags.deserialize("<red>This crate has no loot configured yet."))
            return
        }

        val winningCrateItem = pickWeightedItem(pool)
        val baseSteps = 40
        val targetIndex = pool.indexOf(winningCrateItem)
        val targetOffset = mod(targetIndex - 2, pool.size)
        val desiredRemainder = mod(targetOffset - session.wheelOffset, pool.size)
        val alignmentDelta = mod(desiredRemainder - (baseSteps % pool.size), pool.size)
        val bonusLoops = Random.nextInt(0, 3) * pool.size

        session.isSpinning = true
        session.spinTargetSteps = baseSteps + alignmentDelta + bonusLoops
        session.winningItem = winningCrateItem
        session.rewardGranted = false
        session.spinConsumed = true
        val minDelayTicks = 1
        val maxDelayTicks = 6
        session.spinStepsDone = 0
        session.spinTicksUntilNextStep = 0

        val task = object : BukkitRunnable() {
            override fun run() {
                if (!player.isOnline || sessions[player.uniqueId] !== session) {
                    stopSpin(session)
                    cancel()
                    return
                }

                val sessionInventory = session.inventory
                if (sessionInventory == null) {
                    stopSpin(session)
                    cancel()
                    return
                }

                if (session.spinTicksUntilNextStep > 0) {
                    session.spinTicksUntilNextStep--
                    return
                }

                session.wheelOffset = (session.wheelOffset + 1) % pool.size
                renderWheel(sessionInventory, session, session.wheelOffset)
                player.playSound(GAMBLING_WHEEL_TICK)
                session.spinStepsDone++

                val progress = session.spinStepsDone.toDouble() / session.spinTargetSteps.toDouble()
                val easedProgress = progress * progress
                session.spinTicksUntilNextStep = (minDelayTicks + (maxDelayTicks - minDelayTicks) * easedProgress)
                    .roundToInt()
                    .coerceAtLeast(minDelayTicks)

                if (session.spinStepsDone >= session.spinTargetSteps) {
                    for (slot in wheelSlots) {
                        if (slot != winningWheelSlot) sessionInventory.setItem(slot, idleWheelItem)
                    }
                    stopSpin(session)
                    player.playSound(GAMBLING_WHEEL_STOP)
                    awardPendingReward(session, player, "stopped")
                    cancel()
                }
            }
        }

        session.spinTask = task
        task.runTaskTimer(plugin, 0L, 1L)
    }

    private fun stopSpin(session: GamblingWindowSession) {
        session.spinTask?.cancel()
        session.spinTask = null
        session.isSpinning = false
        session.spinStepsDone = 0
        session.spinTicksUntilNextStep = 0
        session.spinTargetSteps = 0
    }

    private fun renderWheel(inventory: Inventory, session: GamblingWindowSession, offset: Int) {
        val pool = session.crateType.lootPool.possibleItems
        if (pool.isEmpty()) return

        for ((index, slot) in wheelSlots.withIndex()) {
            val crateItem = pool[(offset + index) % pool.size]
            inventory.setItem(slot, crateItem.createItemStack())
        }
    }

    private fun pickWeightedItem(pool: List<CrateItem>): CrateItem {
        val effectiveWeights = pool.map(CrateItem::effectiveChanceWeight)
        val totalEffectiveWeight = effectiveWeights.sum()

        if (totalEffectiveWeight <= 0.0) {
            return pool.random()
        }

        var roll = Random.nextDouble(totalEffectiveWeight)
        for (index in pool.indices) {
            roll -= effectiveWeights[index]
            if (roll < 0.0) return pool[index]
        }

        return pool.last()
    }

    private fun mod(value: Int, divisor: Int): Int {
        val remainder = value % divisor
        return if (remainder < 0) remainder + divisor else remainder
    }

    private fun awardPendingReward(session: GamblingWindowSession, player: Player, reason: String) {
        if (session.rewardGranted) return

        val winner = session.winningItem
        if (winner == null) {
            player.sendMessage(
                Formatting.allTags.deserialize("<red>No reward could be resolved for the ")
                    .append(session.crateType.displayName)
                    .append(Formatting.allTags.deserialize("<red>."))
            )
            session.rewardGranted = true
            return
        }

        val rewardStack = winner.createItemStack()
        val leftovers = player.inventory.addItem(rewardStack)
        for (leftover in leftovers.values) {
            player.world.dropItemNaturally(player.location, leftover)
        }

        if (reason == "closed early") {
            player.sendMessage(
                Formatting.allTags.deserialize("<yellow>You closed the wheel early — you still won a ")
                    .append(rewardStack.displayName())
                    .append(Formatting.allTags.deserialize("<yellow> from the "))
                    .append(session.crateType.displayName)
                    .append(Formatting.allTags.deserialize("<yellow>."))
            )
        } else {
            player.sendMessage(
                Formatting.allTags.deserialize("<green>You won a ")
                    .append(rewardStack.displayName())
                    .append(Formatting.allTags.deserialize("<green> from the "))
                    .append(session.crateType.displayName)
                    .append(Formatting.allTags.deserialize("<green>!"))
            )
        }
        session.rewardGranted = true
    }
}