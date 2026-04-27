package util.ui

import chat.Formatting.allTags
import com.noxcrew.interfaces.InterfacesConstants
import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.PlayerInventoryType
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.properties.DelegateTrigger
import kotlinx.coroutines.launch
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import util.Sounds

/**
 * Generic interactive storage GUI — the single implementation behind BinderWindow and PlushieBoxWindow.
 *
 * Layout (6 rows):
 *   Rows 0–4  — full content grid (cols 0–8, 45 slots per page)
 *   Row 5     — glass bar
 *               [5,2] ← prev  [5,3] filter (optional)  [5,4] page/info book
 *               [5,5] show-missing toggle (optional)    [5,6] → next
 */
object StorageWindow {

    private const val ROWS = 6
    private const val PAGE_SIZE = 45

    /**
     * One entry in the filter cycle.
     *
     * @param name      Display name shown on the filter item.
     * @param predicate Per-item predicate; null = "show all".
     * @param allItems  The complete item pool for this filter (used by the show-missing toggle).
     *                  Null means the toggle is unavailable for this filter option.
     */
    data class FilterOption(
        val name: String,
        val predicate: ((ItemStack) -> Boolean)? = null,
        val allItems: (() -> List<ItemStack>)? = null,
    )

    /**
     * Opens an interactive storage GUI where the player can insert and remove items.
     *
     * @param player             Player to open the GUI for.
     * @param title              MiniMessage string used as the inventory title.
     * @param items              Mutable list of stored items — mutated in-place; [onSave] called after each change.
     * @param maxCapacity        Maximum number of items the storage can hold.
     * @param canInsert          Returns true if the cursor item may be inserted.
     * @param onSave             Called on the main thread after each insert/remove to persist changes.
     * @param filters            Ordered filter cycle shown at [5,3]. Empty = no filter bar.
     * @param showMissingToggle  Whether to show the "show missing" toggle button at [5,5].
      * @param sameItem           Comparator used to match stored items against the filter's
     *                           `allItems` pool. Defaults to [ItemStack.isSimilar]; override
     *                           for PDC-based comparison (e.g. using CrateItem.resolve).
     * @param capacityLabel      Label for the stored-count line in the page book, e.g. "Cards" or "Stored".
     * @param fullMessage        Error sent when storage is at capacity.
     * @param removeHint         Hint line shown in the page book lore.
     * @param insertHint         Hint line shown in the page book lore.
      * @param uniqueKey          When non-null, the "Collected" count shows the number of *distinct*
     *                           items by this key rather than the raw number of list entries.
     *                           Use `CrateItem::resolve` for plushies so duplicates only count once.
     */
    fun open(
        player: Player,
        title: String,
        items: MutableList<ItemStack>,
        maxCapacity: Int,
        canInsert: (ItemStack) -> Boolean,
        onSave: (Player, List<ItemStack>) -> Unit,
        filters: List<FilterOption> = emptyList(),
        showMissingToggle: Boolean = false,
        sameItem: (stored: ItemStack, candidate: ItemStack) -> Boolean = { a, b -> a.isSimilar(b) },
        uniqueKey: ((ItemStack) -> Any?)? = null,
        capacityLabel: String = "Stored",
        fullMessage: String = "Storage is full! ($maxCapacity max)",
        removeHint: String = "Click an item to take it out",
        insertHint: String = "Hold an item and click an empty slot to insert",
        initialFilterIndex: Int = 0,
        onFilterChange: ((Int) -> Unit)? = null,
    ) {
        val hasFilter = filters.isNotEmpty()
        val filterRef = intArrayOf(initialFilterIndex.coerceIn(0, (filters.size - 1).coerceAtLeast(0)))
        val showMissingRef = booleanArrayOf(false)
        val pageRef = intArrayOf(0)
        val itemsTrigger = DelegateTrigger()
        val filterTrigger = DelegateTrigger()
        val missingTrigger = DelegateTrigger()
        val pageTrigger = DelegateTrigger()

        val glassBorder = glassBorderElement()

        /** Items in the active filter pool that the player does not yet have stored. */
        fun missingItems(): List<ItemStack> {
            if (!showMissingRef[0] || !hasFilter) return emptyList()
            val pool = filters[filterRef[0]].allItems?.invoke() ?: return emptyList()
            return pool.filter { candidate -> items.none { stored -> sameItem(stored, candidate) } }
        }

        /** Creates a named barrier representing a missing collectible. */
        fun barrierFor(expected: ItemStack): ItemStack {
            val expectedName = expected.itemMeta?.displayName()
            return ItemStack(Material.BARRIER).apply {
                editMeta { meta ->
                    if (expectedName != null) meta.displayName(expectedName)
                    meta.lore(listOf(allTags.deserialize("<!i><gray>Not yet collected!")))
                }
            }
        }

        /**
         * Returns (originalIndex?, ItemStack) pairs for the current page.
         * origIndex is null for missing-item placeholders (non-interactive).
         */
        fun filteredDisplay(): List<Pair<Int?, ItemStack>> {
            val predicate = if (hasFilter) filters[filterRef[0]].predicate else null
            val found: List<Pair<Int?, ItemStack>> = items.mapIndexedNotNull { idx, stack ->
                if (predicate == null || predicate(stack)) (idx to stack) else null
            }
            val missing: List<Pair<Int?, ItemStack>> = missingItems().map { null to barrierFor(it) }
            return found + missing
        }

        val iface = buildChestInterface {
            rows = ROWS
            playerInventoryType = PlayerInventoryType.DEFAULT
            titleSupplier = { _ -> allTags.deserialize(title) }

            /** Bottom bar — fills row 5 gaps behind all nav/filter buttons */
            withTransform { pane, _ ->
                for (col in 0..8) {
                    if (pane[5, col] == null) pane[5, col] = glassBorder
                }
            }

            /** Filter cycling item at [5,3] — only rendered when filters are configured */
            if (hasFilter) {
                withTransform(filterTrigger) { pane, _ ->
                    val current = filterRef[0]
                    val lore = mutableListOf(
                        allTags.deserialize("<!i><gray>Left-click to cycle forward."),
                        allTags.deserialize("<!i><gray>Right-click to cycle backward."),
                        allTags.deserialize("<!i>"),
                        allTags.deserialize("<!i><gray>Current filter:"),
                    )
                    for ((idx, option) in filters.withIndex()) {
                        lore += allTags.deserialize(
                            if (idx == current) "<!i><dark_gray>• <cloudiecolor>> <white>${option.name}"
                            else "<!i><dark_gray>• ${option.name}"
                        )
                    }
                    pane[5, 3] = StaticElement(drawable(ItemStack(Material.HOPPER).apply {
                        editMeta { meta ->
                            meta.displayName(allTags.deserialize("<!i><cloudiecolor>Filter: <white>${filters[current].name}"))
                            meta.lore(lore)
                        }
                    })) { ctx ->
                        ctx.player.playSound(Sounds.INTERFACE_INTERACT)
                        val step = if (ctx.type.isRightClick) -1 else 1
                        filterRef[0] = (filterRef[0] + step + filters.size) % filters.size
                        // Reset "show missing" if the new filter has no pool
                        if (filters[filterRef[0]].allItems == null) showMissingRef[0] = false
                        pageRef[0] = 0
                        onFilterChange?.invoke(filterRef[0])
                        filterTrigger.trigger()
                        missingTrigger.trigger()
                    }
                }
            }

            /** Show-missing toggle at [5,5] — only when showMissingToggle is enabled */
            if (showMissingToggle) {
                withTransform(filterTrigger, missingTrigger) { pane, _ ->
                    val hasPool = hasFilter && filters[filterRef[0]].allItems != null
                    if (!hasPool) return@withTransform  // hide when filter has no pool

                    val on = showMissingRef[0]
                    pane[5, 5] = StaticElement(drawable(
                        ItemStack(if (on) Material.LIME_STAINED_GLASS_PANE else Material.RED_STAINED_GLASS_PANE).apply {
                            editMeta { meta ->
                                meta.displayName(allTags.deserialize(
                                    if (on) "<!i><green><bold>Show Missing: ON"
                                    else     "<!i><red><bold>Show Missing: OFF"
                                ))
                                meta.lore(listOf(allTags.deserialize("<!i><gray>Highlights plushies you haven't collected yet.")))
                            }
                        }
                    )) { ctx ->
                        ctx.player.playSound(Sounds.INTERFACE_INTERACT)
                        showMissingRef[0] = !showMissingRef[0]
                        pageRef[0] = 0
                        missingTrigger.trigger()
                    }
                }
            }

            /** Content grid (rows 0–4, cols 0–8) + nav at [5,2/4/6] */
            withTransform(itemsTrigger, filterTrigger, missingTrigger, pageTrigger) { pane, _ ->
                val display = filteredDisplay()
                val maxPage = ((display.size) / PAGE_SIZE).coerceAtLeast(0)
                if (pageRef[0] > maxPage) pageRef[0] = maxPage
                val currentPage = pageRef[0]
                val pageSlice = display.drop(currentPage * PAGE_SIZE).take(PAGE_SIZE)

                // ── Content grid ─────────────────────────────────────────────
                for (i in 0 until PAGE_SIZE) {
                    val row = i / 9
                    val col = i % 9
                    if (i < pageSlice.size) {
                        val (origIdx, stack) = pageSlice[i]
                        if (origIdx != null) {
                            // Stored item — clickable, removes on interact
                            val display2 = stack.clone()
                            pane[row, col] = StaticElement(drawable(display2)) { ctx ->
                                if (!ctx.player.openInventory.cursor.isEmpty) return@StaticElement
                                ctx.player.playSound(Sounds.INTERFACE_INTERACT)
                                items.removeAt(origIdx)
                                onSave(ctx.player, items)
                                ctx.player.inventory.addItem(display2.clone())
                                    .values.forEach { ctx.player.world.dropItemNaturally(ctx.player.location, it) }
                                val newMax = ((filteredDisplay().size - 1) / PAGE_SIZE).coerceAtLeast(0)
                                if (pageRef[0] > newMax) pageRef[0] = newMax
                                itemsTrigger.trigger()
                            }
                        } else {
                            // Missing barrier — non-interactive display only
                            pane[row, col] = StaticElement(drawable(stack.clone()))
                        }
                    } else if (!showMissingRef[0]) {
                        // Empty insertion slot — only shown when "show missing" is OFF
                        pane[row, col] = StaticElement(
                            drawable(ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE).apply {
                                editMeta { meta ->
                                    meta.displayName(allTags.deserialize("<!i><dark_gray>Empty slot"))
                                    meta.lore(listOf(allTags.deserialize("<!i><dark_gray>$insertHint")))
                                }
                            })
                        ) { ctx ->
                            val cursor = ctx.player.openInventory.cursor
                            if (cursor.isEmpty) return@StaticElement
                            if (!canInsert(cursor)) {
                                ctx.player.playSound(Sounds.INTERFACE_ERROR)
                                return@StaticElement
                            }

                            var remaining = cursor.amount
                            var inserted = false

                            // Stack into existing similar entries first
                            for (existing in items) {
                                if (remaining <= 0) break
                                if (existing.isSimilar(cursor) && existing.amount < existing.maxStackSize) {
                                    val canAdd = minOf(remaining, existing.maxStackSize - existing.amount)
                                    existing.amount += canAdd
                                    remaining -= canAdd
                                    inserted = true
                                }
                            }

                            // Add remaining as new list entries (up to capacity)
                            while (remaining > 0) {
                                if (items.size >= maxCapacity) {
                                    ctx.player.sendMessage(allTags.deserialize("<red>$fullMessage"))
                                    ctx.player.playSound(Sounds.INTERFACE_ERROR)
                                    break
                                }
                                val toAdd = minOf(remaining, cursor.maxStackSize)
                                items.add(cursor.clone().apply { amount = toAdd })
                                remaining -= toAdd
                                inserted = true
                            }

                            ctx.player.setItemOnCursor(if (remaining > 0) cursor.apply { amount = remaining } else null)
                            onSave(ctx.player, items)
                            if (inserted) ctx.player.playSound(Sounds.INTERFACE_INTERACT)
                            itemsTrigger.trigger()
                        }
                    }
                }

                // ── Nav: Prev, page/info book, Next ──────────────────────────
                val predicate = if (hasFilter) filters[filterRef[0]].predicate else null
                val foundItems = items.filter { stack -> predicate == null || predicate(stack) }
                val foundCount = if (uniqueKey != null) foundItems.distinctBy { uniqueKey(it) }.size
                               else foundItems.size
                val missingCount = missingItems().size

                pane[5, 2] = if (currentPage > 0) {
                    StaticElement(drawable(ItemStack(Material.ARROW).apply {
                        editMeta { meta -> meta.displayName(allTags.deserialize("<!i><yellow><bold>← Previous Page")) }
                    })) { ctx -> ctx.player.playSound(Sounds.INTERFACE_INTERACT); pageRef[0]--; pageTrigger.trigger() }
                } else glassBorder

                val loreLines = mutableListOf(
                    allTags.deserialize("<!i><gray>$capacityLabel: <white>${items.size}<gray>/$maxCapacity"),
                )
                if (hasFilter && filters[filterRef[0]].predicate != null) {
                    loreLines += allTags.deserialize("<!i><gray>Collected: <white>$foundCount")
                    if (showMissingRef[0]) {
                        loreLines += allTags.deserialize("<!i><gray>Missing: <white>$missingCount")
                    }
                }
                loreLines += allTags.deserialize("<!i>")
                loreLines += allTags.deserialize("<!i><dark_gray>$removeHint")
                if (!showMissingRef[0]) loreLines += allTags.deserialize("<!i><dark_gray>$insertHint")

                pane[5, 4] = StaticElement(drawable(ItemStack(Material.BOOK).apply {
                    editMeta { meta ->
                        meta.displayName(allTags.deserialize("<!i><white>Page <yellow>${currentPage + 1}<white> / <yellow>${maxPage + 1}"))
                        meta.lore(loreLines)
                    }
                }))

                pane[5, 6] = if (currentPage < maxPage) {
                    StaticElement(drawable(ItemStack(Material.ARROW).apply {
                        editMeta { meta -> meta.displayName(allTags.deserialize("<!i><yellow><bold>Next Page →")) }
                    })) { ctx -> ctx.player.playSound(Sounds.INTERFACE_INTERACT); pageRef[0]++; pageTrigger.trigger() }
                } else glassBorder
            }
        }

        InterfacesConstants.SCOPE.launch { iface.open(player) }
    }
}