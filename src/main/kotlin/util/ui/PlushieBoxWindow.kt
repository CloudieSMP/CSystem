package util.ui

import item.crate.CrateItem
import item.crate.CrateType
import item.plushiebox.PlushieBox
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Opens the Plushie Box GUI via the shared [StorageWindow] engine. */
object PlushieBoxWindow {

    /** Remembers the last active filter index per player UUID. */
    private val lastFilter: MutableMap<UUID, Int> = ConcurrentHashMap()

    /**
     * Lazily built inverse map: CrateItem → set of non-MASTER CrateTypes whose
     * loot pool contains that item.  Used to build per-type filter predicates.
     */
    private val itemCrateTypes: Map<CrateItem, Set<CrateType>> by lazy {
        val map = mutableMapOf<CrateItem, MutableSet<CrateType>>()
        for (crateType in CrateType.entries) {
            if (crateType == CrateType.MASTER) continue
            for (item in crateType.lootPool.possibleItems) {
                map.getOrPut(item) { mutableSetOf() }.add(crateType)
            }
        }
        map
    }

    private fun matchesCrateType(stack: ItemStack, crateType: CrateType): Boolean {
        val crateItem = CrateItem.resolve(stack) ?: return false
        return itemCrateTypes[crateItem]?.contains(crateType) == true
    }

    /** Filter option for a specific crate type, with full loot pool for "show missing". */
    private fun filterFor(name: String, crateType: CrateType) = StorageWindow.FilterOption(
        name = name,
        predicate = { matchesCrateType(it, crateType) },
        allItems = { crateType.lootPool.possibleItems.map { it.createItemStack() } },
    )

    fun open(player: Player, slot: EquipmentSlot) {
        val boxItem = when (slot) {
            EquipmentSlot.HAND -> player.inventory.itemInMainHand
            EquipmentSlot.OFF_HAND -> player.inventory.itemInOffHand
            else -> return
        }

        val filters = listOf(
            StorageWindow.FilterOption("✦ All"),  // no pool → toggle unavailable
            filterFor("Plushie Crate",   CrateType.PLUSHIE),
            filterFor("Wearables Crate", CrateType.WEARABLES),
            filterFor("Player Crate",    CrateType.PLAYER),
            filterFor("Character Crate", CrateType.CHARACTER),
            filterFor("Sabine Crate",    CrateType.SABINE),
            filterFor("Cookie Crate",    CrateType.COOKIE),
        )

        StorageWindow.open(
            player = player,
            title = "<gradient:#C45889:#a78bfa><bold>Plushie Box</bold></gradient>",
            items = PlushieBox.readPlushies(boxItem).toMutableList(),
            maxCapacity = PlushieBox.MAX_CAPACITY,
            canInsert = { PlushieBox.isCrateCollectible(it) },
            onSave = { p, plushies -> PlushieBox.savePlushies(p, slot, plushies) },
            filters = filters,
            showMissingToggle = true,
            sameItem = { stored, candidate -> CrateItem.resolve(stored) == CrateItem.resolve(candidate) },
            uniqueKey = { CrateItem.resolve(it) },
            capacityLabel = "Stored",
            fullMessage = "Plushie Box is full! (${PlushieBox.MAX_CAPACITY} max)",
            removeHint = "Click a collectible to take it out",
            insertHint = "Hold a collectible and click an empty slot to insert",
            initialFilterIndex = lastFilter[player.uniqueId] ?: 0,
            onFilterChange = { idx -> lastFilter[player.uniqueId] = idx },
        )
    }

    fun clearFilter(uuid: UUID) {
        lastFilter.remove(uuid)
    }
}
