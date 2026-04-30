package util.ui

import item.ItemRarity
import item.crate.CrateItem
import item.crate.CrateType
import item.plushiebox.PlushieBox
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

/** Opens the Plushie Box GUI via the shared [StorageWindow] engine. */
object PlushieBoxWindow {

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

    private fun matchesItemRarity(stack: ItemStack, rarity: ItemRarity): Boolean {
        val crateItem = CrateItem.resolve(stack) ?: return false
        return crateItem.rarity == rarity
    }

    private fun matchesCrateType(stack: ItemStack, crateType: CrateType): Boolean {
        val crateItem = CrateItem.resolve(stack) ?: return false
        return itemCrateTypes[crateItem]?.contains(crateType) == true
    }

    private fun filterForRarity(name: String, rarity: ItemRarity) = StorageWindow.FilterOption(
        name = name,
        predicate = { matchesItemRarity(it, rarity) },
        allItems = { CrateItem.entries.filter { it.rarity == rarity }.map { it.createItemStack() } },
    )

    /** Filter option for a specific crate type, with full loot pool for "show missing". */
    private fun filterForType(name: String, crateType: CrateType) = StorageWindow.FilterOption(
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

        val typeFilters = listOf(
            StorageWindow.FilterOption("✦ All"),  // no pool → toggle unavailable
            filterForType("Plushie Crate",   CrateType.PLUSHIE),
            filterForType("Baby Crate",      CrateType.BABY),
            filterForType("Wearables Crate", CrateType.WEARABLES),
            filterForType("Player Crate",    CrateType.PLAYER),
            filterForType("Character Crate", CrateType.CHARACTER),
            filterForType("Sabine Crate",    CrateType.SABINE),
            filterForType("Cookie Crate",    CrateType.COOKIE),
        )

        val rarityFilter = listOf(
            StorageWindow.FilterOption("✦ All"),  // no pool → toggle unavailable
            filterForRarity("Common", ItemRarity.COMMON),
            filterForRarity("Uncommon", ItemRarity.UNCOMMON),
            filterForRarity("Rare", ItemRarity.RARE),
            filterForRarity("Epic", ItemRarity.EPIC),
            filterForRarity("Legendary", ItemRarity.LEGENDARY),
            filterForRarity("Mythic", ItemRarity.MYTHIC),
            filterForRarity("Unreal", ItemRarity.UNREAL),
            filterForRarity("Transcendent", ItemRarity.TRANSCENDENT),
            filterForRarity("Celestial", ItemRarity.CELESTIAL),
        )

        StorageWindow.open(
            player = player,
            title = "<gradient:#C45889:#a78bfa><bold>Plushie Box</bold></gradient>",
            items = PlushieBox.readPlushies(boxItem).toMutableList(),
            maxCapacity = PlushieBox.MAX_CAPACITY,
            canInsert = { PlushieBox.isCrateCollectible(it) },
            onSave = { p, plushies -> PlushieBox.savePlushies(p, slot, plushies) },
            filters = typeFilters,
            filters2 = rarityFilter,
            showMissingToggle = true,
            sameItem = { stored, candidate -> CrateItem.resolve(stored) == CrateItem.resolve(candidate) },
            uniqueKey = { CrateItem.resolve(it) },
            capacityLabel = "Stored",
            fullMessage = "Plushie Box is full! (${PlushieBox.MAX_CAPACITY} max)",
            removeHint = "Click a collectible to take it out",
            insertHint = "Hold a collectible and click an empty slot to insert",
            initialFilterIndex = PlushieBox.readFilter(boxItem),
            onFilterChange = { idx -> PlushieBox.saveFilter(player, slot, idx) },
            initialFilterIndex2 = PlushieBox.readFilter2(boxItem),
            onFilterChange2 = { idx -> PlushieBox.saveFilter2(player, slot, idx) },
            onRemove = { item -> CrateItem.refresh(item) ?: item },
        )
    }
}
