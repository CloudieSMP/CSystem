package util.ui

import chat.Formatting.allTags
import com.noxcrew.interfaces.InterfacesConstants
import com.noxcrew.interfaces.drawable.Drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.interfaces.PlayerInventoryType
import com.noxcrew.interfaces.properties.DelegateTrigger
import item.binder.BinderItem
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

/**
 * A paginated binder UI built with the noxcrew interfaces library.
 *
 * Layout (6 rows):
 *   Rows 0-4  — card slots (45 slots per page)
 *   Row 5     — navigation bar (prev, info, next) with filler panes
 */
object BinderWindow {
    private const val PAGE_SIZE = 45
    private const val ROWS = 6

    private fun fillerItem() = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
        editMeta { it.displayName(Component.empty().decoration(TextDecoration.ITALIC, false)) }
    }

    private fun prevItem() = ItemStack(Material.ARROW).apply {
        editMeta { meta ->
            meta.displayName(allTags.deserialize("<!i><yellow><bold>← Previous Page"))
        }
    }

    private fun nextItem() = ItemStack(Material.ARROW).apply {
        editMeta { meta ->
            meta.displayName(allTags.deserialize("<!i><yellow><bold>Next Page →"))
        }
    }

    private fun pageInfoItem(page: Int, maxPage: Int, cardCount: Int) =
        ItemStack(Material.BOOK).apply {
            editMeta { meta ->
                meta.displayName(allTags.deserialize("<!i><white>Page <yellow>${page}<white> / <yellow>${maxPage}"))
                meta.lore(
                    listOf(
                        allTags.deserialize("<!i><gray>Cards: <white>$cardCount<gray>/${BinderItem.MAX_CAPACITY}"),
                        allTags.deserialize("<!i><dark_gray>Click a card to take it out"),
                        allTags.deserialize("<!i><dark_gray>Click empty slot with card on cursor to insert"),
                    )
                )
            }
        }

    private fun emptySlotItem() = ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE).apply {
        editMeta { meta ->
            meta.displayName(allTags.deserialize("<!i><dark_gray>Empty slot"))
            meta.lore(listOf(allTags.deserialize("<!i><dark_gray>Click with a card on your cursor to insert")))
        }
    }

    fun open(player: Player, slot: EquipmentSlot) {
        val binderItem = when (slot) {
            EquipmentSlot.HAND -> player.inventory.itemInMainHand
            EquipmentSlot.OFF_HAND -> player.inventory.itemInOffHand
            else -> return
        }

        val cards = BinderItem.readCards(binderItem).toMutableList()

        // Reactive triggers — setting a value on pageRef[0] + pageTrigger.trigger() re-renders the transform.
        val pageRef = intArrayOf(0)
        val pageTrigger = DelegateTrigger()
        val cardsTrigger = DelegateTrigger()

        val iface = buildChestInterface {
            rows = ROWS
            // 2.1+ replacement behavior for the removed own-inventory click flag.
            // DEFAULT keeps player inventory outside the interface mapping, so those clicks are allowed.
            playerInventoryType = PlayerInventoryType.DEFAULT
            titleSupplier = { _ ->
                allTags.deserialize("<gradient:#5b9df5:#a78bfa><bold>Card Binder</bold></gradient>")
            }

            withTransform(pageTrigger, cardsTrigger) { pane, _ ->
                val currentPage = pageRef[0]
                val maxPage = ((cards.size - 1) / PAGE_SIZE).coerceAtLeast(0)
                val offset = currentPage * PAGE_SIZE

                // ── Rows 0-4: card / empty slots ──────────────────────────────
                for (i in 0 until PAGE_SIZE) {
                    val row = i / 9
                    val col = i % 9
                    val cardIndex = offset + i

                    if (cardIndex < cards.size) {
                        val card = cards[cardIndex].clone()
                        pane[row, col] = StaticElement(Drawable.drawable(card)) { ctx ->
                            val cursor = ctx.player.openInventory.cursor
                            // If the player has something on the cursor, don't accidentally take the card
                            if (!cursor.isEmpty) return@StaticElement

                            cards.removeAt(cardIndex)
                            BinderItem.saveCards(ctx.player, slot, cards)
                            val overflow = ctx.player.inventory.addItem(card)
                            overflow.values.forEach { leftover ->
                                ctx.player.world.dropItemNaturally(ctx.player.location, leftover)
                            }

                            // Clamp page if we're now beyond the last page
                            val newMax = ((cards.size - 1) / PAGE_SIZE).coerceAtLeast(0)
                            if (pageRef[0] > newMax) pageRef[0] = newMax
                            cardsTrigger.trigger()
                        }
                    } else {
                        pane[row, col] = StaticElement(Drawable.drawable(emptySlotItem())) { ctx ->
                            val cursor = ctx.player.openInventory.cursor
                            if (cursor.isEmpty) return@StaticElement
                            if (!BinderItem.isCard(cursor)) return@StaticElement
                            if (cards.size >= BinderItem.MAX_CAPACITY) {
                                ctx.player.sendMessage(allTags.deserialize("<red>Your binder is full! (${ BinderItem.MAX_CAPACITY} cards max)"))
                                return@StaticElement
                            }

                            cards.add(cursor.clone())
                            ctx.player.setItemOnCursor(null)
                            BinderItem.saveCards(ctx.player, slot, cards)
                            cardsTrigger.trigger()
                        }
                    }
                }

                // ── Row 5: navigation bar ─────────────────────────────────────
                val filler = StaticElement(Drawable.drawable(fillerItem()))
                for (col in 0..8) pane[5, col] = filler

                if (currentPage > 0) {
                    pane[5, 0] = StaticElement(Drawable.drawable(prevItem())) { _ ->
                        pageRef[0]--
                        pageTrigger.trigger()
                    }
                }

                pane[5, 4] = StaticElement(
                    Drawable.drawable(pageInfoItem(currentPage + 1, maxPage + 1, cards.size))
                )

                if (currentPage < maxPage) {
                    pane[5, 8] = StaticElement(Drawable.drawable(nextItem())) { _ ->
                        pageRef[0]++
                        pageTrigger.trigger()
                    }
                }
            }
        }

        InterfacesConstants.SCOPE.launch {
            iface.open(player)
        }
    }
}

