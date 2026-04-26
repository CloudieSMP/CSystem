package util.ui

import chat.Formatting.allTags
import com.noxcrew.interfaces.InterfacesConstants
import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.properties.DelegateTrigger
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import util.Sounds

/**
 * Generic read-only collection browser used by CrateBrowserWindow and BoosterPackBrowserWindow.
 *
 * Layout:
 *   openSelector  — rows 0–4, cols 0–8 full grid (45 per page) + row 5 glass bar with prev/info/next nav
 *   openPreview   — same full grid, back button at [5,0], entries are non-interactive
 */
object CollectionBrowserWindow {
    private const val ROWS = 6
    private const val PAGE_SIZE = 45  // rows 0–4, cols 0–8

    fun <T> openSelector(
        player: Player,
        title: Component,
        entries: List<T>,
        itemForEntry: (T) -> ItemStack,
        onEntryClick: (Player, T) -> Unit,
    ) = buildAndOpen(
        player = player,
        title = title,
        entries = entries,
        itemForEntry = itemForEntry,
        onEntryClick = onEntryClick,
        backButtonName = null,
        onBackClick = null,
    )

    fun <T> openPreview(
        player: Player,
        title: Component,
        entries: List<T>,
        backButtonName: String = "← Back",
        itemForEntry: (T) -> ItemStack,
        onBackClick: (Player) -> Unit,
    ) = buildAndOpen(
        player = player,
        title = title,
        entries = entries,
        itemForEntry = itemForEntry,
        onEntryClick = null,
        backButtonName = backButtonName,
        onBackClick = onBackClick,
    )

    /**
     * Shared implementation for [openSelector] and [openPreview].
     *
     * - When [onEntryClick] is non-null (selector mode): entries are clickable and play [Sounds.INTERFACE_ENTER_SUB_MENU].
     * - When [backButtonName] / [onBackClick] are non-null (preview mode): a back arrow is placed at [5,0].
     */
    private fun <T> buildAndOpen(
        player: Player,
        title: Component,
        entries: List<T>,
        itemForEntry: (T) -> ItemStack,
        onEntryClick: ((Player, T) -> Unit)?,
        backButtonName: String?,
        onBackClick: ((Player) -> Unit)?,
    ) {
        val pageRef = intArrayOf(0)
        val pageTrigger = DelegateTrigger()
        val maxPage = ((entries.size - 1) / PAGE_SIZE).coerceAtLeast(0)

        val iface = buildChestInterface {
            rows = ROWS
            titleSupplier = { title }

            /** Bottom bar — fills row 5 gaps behind nav/back buttons */
            withTransform { pane, _ ->
                for (col in 0..8) {
                    if (pane[5, col] == null) pane[5, col] = glassBorderElement()
                }
            }

            /** Back button at [5,0] — preview mode only */
            if (backButtonName != null && onBackClick != null) {
                withTransform { pane, _ ->
                    pane[5, 0] = StaticElement(drawable(ItemStack(Material.ARROW).apply {
                        editMeta { meta ->
                            meta.displayName(allTags.deserialize("<!i><yellow><bold>$backButtonName"))
                        }
                    })) { ctx -> ctx.player.playSound(Sounds.INTERFACE_BACK); onBackClick(ctx.player) }
                }
            }

            /** Content grid (rows 0–4, cols 0–8) + nav at [5,2/4/6] */
            withTransform(pageTrigger) { pane, _ ->
                val currentPage = pageRef[0]
                val pageSlice = entries.drop(currentPage * PAGE_SIZE).take(PAGE_SIZE)

                for (i in 0 until PAGE_SIZE) {
                    val row = i / 9   // rows 0–4
                    val col = i % 9   // cols 0–8
                    if (i < pageSlice.size) {
                        val entry = pageSlice[i]
                        pane[row, col] = if (onEntryClick != null) {
                            StaticElement(drawable(itemForEntry(entry))) { ctx ->
                                ctx.player.playSound(Sounds.INTERFACE_ENTER_SUB_MENU)
                                onEntryClick(ctx.player, entry)
                            }
                        } else {
                            StaticElement(drawable(itemForEntry(entry)))
                        }
                    }
                }

                pane[5, 2] = if (currentPage > 0) {
                    StaticElement(drawable(ItemStack(Material.ARROW).apply {
                        editMeta { meta -> meta.displayName(allTags.deserialize("<!i><yellow><bold>← Previous Page")) }
                    })) { ctx -> ctx.player.playSound(Sounds.INTERFACE_INTERACT); pageRef[0]--; pageTrigger.trigger() }
                } else glassBorderElement()

                pane[5, 4] = StaticElement(drawable(ItemStack(Material.BOOK).apply {
                    editMeta { meta ->
                        meta.displayName(allTags.deserialize("<!i><white>Page <yellow>${currentPage + 1}<white> / <yellow>${maxPage + 1}"))
                        meta.lore(listOf(allTags.deserialize("<!i><gray>Total: <white>${entries.size}")))
                    }
                }))

                pane[5, 6] = if (currentPage < maxPage) {
                    StaticElement(drawable(ItemStack(Material.ARROW).apply {
                        editMeta { meta -> meta.displayName(allTags.deserialize("<!i><yellow><bold>Next Page →")) }
                    })) { ctx -> ctx.player.playSound(Sounds.INTERFACE_INTERACT); pageRef[0]++; pageTrigger.trigger() }
                } else glassBorderElement()
            }
        }

        InterfacesConstants.SCOPE.launch { iface.open(player) }
    }
}
