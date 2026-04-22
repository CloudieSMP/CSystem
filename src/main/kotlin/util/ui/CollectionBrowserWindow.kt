package util.ui

import chat.Formatting.allTags
import com.noxcrew.interfaces.InterfacesConstants
import com.noxcrew.interfaces.drawable.Drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.properties.DelegateTrigger
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object CollectionBrowserWindow {
    private const val MAX_CHEST_SLOTS = 54
    private const val PAGED_CONTENT_SLOTS = 45
    private const val PAGED_ROWS = 6
    private const val NAV_ROW = 5
    private const val PREV_COL = 3
    private const val INFO_COL = 4
    private const val BACK_COL = 8
    private const val NEXT_COL = 5

    private fun previousPageItem() = ItemStack(Material.ARROW).apply {
        editMeta { meta ->
            meta.displayName(allTags.deserialize("<!i><yellow><bold><< Previous Page"))
        }
    }

    private fun nextPageItem() = ItemStack(Material.ARROW).apply {
        editMeta { meta ->
            meta.displayName(allTags.deserialize("<!i><yellow><bold>Next Page >>"))
        }
    }

    private fun pageInfoItem(page: Int, maxPage: Int) = ItemStack(Material.BOOK).apply {
        editMeta { meta ->
            meta.displayName(allTags.deserialize("<!i><white>Page <yellow>$page<white> / <yellow>$maxPage"))
        }
    }

    private fun fillPane(rows: Int, pane: com.noxcrew.interfaces.pane.Pane, fillerPane: ItemStack) {
        val filler = StaticElement(Drawable.drawable(fillerPane))
        for (row in 0 until rows) {
            for (col in 0 until 9) {
                pane[row, col] = filler
            }
        }
    }

    fun <T> openSelector(
        player: Player,
        title: Component,
        entries: List<T>,
        fillerPane: ItemStack,
        itemForEntry: (T) -> ItemStack,
        onEntryClick: (Player, T) -> Unit,
    ) {
        if (entries.size <= MAX_CHEST_SLOTS) {
            val rows = ((entries.size + 8) / 9).coerceIn(1, 6)

            val iface = buildChestInterface {
                this.rows = rows
                titleSupplier = { title }

                withTransform { pane, _ ->
                    fillPane(rows, pane, fillerPane)

                    entries.forEachIndexed { index, entry ->
                        val row = index / 9
                        val col = index % 9
                        pane[row, col] = StaticElement(Drawable.drawable(itemForEntry(entry))) { ctx ->
                            onEntryClick(ctx.player, entry)
                        }
                    }
                }
            }

            InterfacesConstants.SCOPE.launch {
                iface.open(player)
            }
            return
        }

        val pageRef = intArrayOf(0)
        val pageTrigger = DelegateTrigger()
        val maxPage = ((entries.size - 1) / PAGED_CONTENT_SLOTS).coerceAtLeast(0)

        val iface = buildChestInterface {
            this.rows = PAGED_ROWS
            titleSupplier = { title }

            withTransform(pageTrigger) { pane, _ ->
                fillPane(PAGED_ROWS, pane, fillerPane)

                val currentPage = pageRef[0]
                val offset = currentPage * PAGED_CONTENT_SLOTS
                entries.drop(offset).take(PAGED_CONTENT_SLOTS).forEachIndexed { index, entry ->
                    val row = index / 9
                    val col = index % 9
                    pane[row, col] = StaticElement(Drawable.drawable(itemForEntry(entry))) { ctx ->
                        onEntryClick(ctx.player, entry)
                    }
                }

                if (currentPage > 0) {
                    pane[NAV_ROW, PREV_COL] = StaticElement(Drawable.drawable(previousPageItem())) {
                        pageRef[0]--
                        pageTrigger.trigger()
                    }
                }

                pane[NAV_ROW, INFO_COL] = StaticElement(Drawable.drawable(pageInfoItem(currentPage + 1, maxPage + 1)))

                if (currentPage < maxPage) {
                    pane[NAV_ROW, NEXT_COL] = StaticElement(Drawable.drawable(nextPageItem())) {
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

    fun <T> openPreview(
        player: Player,
        title: Component,
        entries: List<T>,
        fillerPane: ItemStack,
        backButton: ItemStack,
        itemForEntry: (T) -> ItemStack,
        onBackClick: (Player) -> Unit,
    ) {
        val singlePageRows = ((entries.size + 1 + 8) / 9).coerceIn(1, 6)
        val singlePageSize = singlePageRows * 9
        val singlePageBackSlot = singlePageSize - 1

        if (entries.size <= singlePageBackSlot) {
            val iface = buildChestInterface {
                this.rows = singlePageRows
                titleSupplier = { title }

                withTransform { pane, _ ->
                    fillPane(singlePageRows, pane, fillerPane)

                    entries.take(singlePageBackSlot).forEachIndexed { index, entry ->
                        val row = index / 9
                        val col = index % 9
                        pane[row, col] = StaticElement(Drawable.drawable(itemForEntry(entry)))
                    }

                    val backRow = singlePageBackSlot / 9
                    val backCol = singlePageBackSlot % 9
                    pane[backRow, backCol] = StaticElement(Drawable.drawable(backButton.clone())) { ctx ->
                        onBackClick(ctx.player)
                    }
                }
            }

            InterfacesConstants.SCOPE.launch {
                iface.open(player)
            }
            return
        }

        val pageRef = intArrayOf(0)
        val pageTrigger = DelegateTrigger()
        val maxPage = ((entries.size - 1) / PAGED_CONTENT_SLOTS).coerceAtLeast(0)

        val iface = buildChestInterface {
            this.rows = PAGED_ROWS
            titleSupplier = { title }

            withTransform(pageTrigger) { pane, _ ->
                fillPane(PAGED_ROWS, pane, fillerPane)

                val currentPage = pageRef[0]
                val offset = currentPage * PAGED_CONTENT_SLOTS
                entries.drop(offset).take(PAGED_CONTENT_SLOTS).forEachIndexed { index, entry ->
                    val row = index / 9
                    val col = index % 9
                    pane[row, col] = StaticElement(Drawable.drawable(itemForEntry(entry)))
                }

                if (currentPage > 0) {
                    pane[NAV_ROW, PREV_COL] = StaticElement(Drawable.drawable(previousPageItem())) {
                        pageRef[0]--
                        pageTrigger.trigger()
                    }
                }

                pane[NAV_ROW, INFO_COL] = StaticElement(Drawable.drawable(pageInfoItem(currentPage + 1, maxPage + 1)))

                pane[NAV_ROW, BACK_COL] = StaticElement(Drawable.drawable(backButton.clone())) { ctx ->
                    onBackClick(ctx.player)
                }

                if (currentPage < maxPage) {
                    pane[NAV_ROW, NEXT_COL] = StaticElement(Drawable.drawable(nextPageItem())) {
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

