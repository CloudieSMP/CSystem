package library

import item.crate.CrateItem
import item.crate.CrateType
import logger
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import plugin
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CrateRollStatsStorage {
    data class PlayerStatsSnapshot(
        val totalRolls: Long,
        val crateCounts: Map<String, Long>,
        val itemCounts: Map<String, Long>
    )

    private data class PlayerStatsCache(
        var totalRolls: Long = 0L,
        val crateCounts: MutableMap<String, Long> = mutableMapOf(),
        val itemCounts: MutableMap<String, Long> = mutableMapOf(),
        var dirty: Boolean = false
    )

    private val playerStatsDir: File
        get() = File(plugin.dataFolder, "crate-roll-stats")

    private val globalStatsFile: File
        get() = File(plugin.dataFolder, "crate-roll-stats-global.yml")

    private val cache = ConcurrentHashMap<UUID, PlayerStatsCache>()
    private val loadCallbacks = ConcurrentHashMap<UUID, MutableList<(PlayerStatsCache) -> Unit>>()
    private val queuedSaves = ConcurrentHashMap.newKeySet<UUID>()

    private val globalLock = Any()
    private val globalCrateCounts = mutableMapOf<String, Long>()
    private val globalItemCounts = mutableMapOf<String, Long>()
    @Volatile
    private var globalLoaded = false
    @Volatile
    private var globalDirty = false
    @Volatile
    private var globalSaveQueued = false
    @Volatile
    private var globalTotalRolls: Long = 0L

    private fun playerFile(playerId: UUID): File = File(playerStatsDir, "$playerId.yml")

    fun loadSync() {
        ensureGlobalLoaded()
    }

    fun preload(playerId: UUID) {
        ensureLoaded(playerId) { }
    }

    fun incrementRollAsync(playerId: UUID, crateType: CrateType, crateItem: CrateItem) {
        val crateId = crateType.storedId
        val itemId = crateItem.storedId

        ensureLoaded(playerId) { stats ->
            synchronized(stats) {
                stats.totalRolls++
                stats.crateCounts[crateId] = (stats.crateCounts[crateId] ?: 0L) + 1L
                stats.itemCounts[itemId] = (stats.itemCounts[itemId] ?: 0L) + 1L
                stats.dirty = true
            }
            scheduleSave(playerId)
        }

        incrementGlobal(crateId, itemId)
    }

    fun snapshotAsync(playerId: UUID, callback: (PlayerStatsSnapshot) -> Unit) {
        ensureLoaded(playerId) { stats ->
            callback(
                synchronized(stats) {
                    PlayerStatsSnapshot(
                        totalRolls = stats.totalRolls,
                        crateCounts = stats.crateCounts.toMap(),
                        itemCounts = stats.itemCounts.toMap()
                    )
                }
            )
        }
    }

    fun globalTotalRolls(): Long {
        ensureGlobalLoaded()
        return synchronized(globalLock) { globalTotalRolls }
    }

    private val wearableItemIds: Set<String> by lazy {
        CrateType.WEARABLES.lootPool.possibleItems.map(CrateItem::storedId).toSet()
    }

    private fun isTrackedCollectible(itemId: String): Boolean {
        val crateItem = CrateItem.fromStoredId(itemId) ?: return false
        return crateItem.isPlushie || wearableItemIds.contains(crateItem.storedId)
    }

    fun globalPlushieCounts(): Map<String, Long> {
        ensureGlobalLoaded()
        return synchronized(globalLock) {
            globalItemCounts
                .filterKeys(::isTrackedCollectible)
                .toMap()
        }
    }

    fun flushAllSync() {
        cache.forEach { (playerId, stats) ->
            flushSync(playerId, stats)
        }
        flushGlobalSync()
    }

    private fun ensureLoaded(playerId: UUID, callback: (PlayerStatsCache) -> Unit) {
        val cached = cache[playerId]
        if (cached != null) {
            callback(cached)
            return
        }

        var shouldLoad = false
        loadCallbacks.compute(playerId) { _, existing ->
            val callbacks = existing ?: mutableListOf()
            callbacks += callback
            if (existing == null) {
                shouldLoad = true
            }
            callbacks
        }

        if (!shouldLoad) {
            return
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val loaded = loadFromDisk(playerId)
            cache[playerId] = loaded
            val callbacks = loadCallbacks.remove(playerId).orEmpty()

            Bukkit.getScheduler().runTask(plugin, Runnable {
                callbacks.forEach { it(loaded) }
            })
        })
    }

    private fun scheduleSave(playerId: UUID) {
        if (!queuedSaves.add(playerId)) {
            return
        }

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, Runnable {
            queuedSaves.remove(playerId)
            val stats = cache[playerId] ?: return@Runnable
            flushAsync(playerId, stats)
        }, 20L)
    }

    private fun loadFromDisk(playerId: UUID): PlayerStatsCache {
        if (!playerStatsDir.exists()) {
            playerStatsDir.mkdirs()
        }

        val config = YamlConfiguration.loadConfiguration(playerFile(playerId))
        val totalRolls = config.getLong("totalRolls").coerceAtLeast(0L)

        val crateCounts = mutableMapOf<String, Long>()
        config.getConfigurationSection("crateCounts")?.getKeys(false)?.forEach { key ->
            val count = config.getLong("crateCounts.$key")
            if (count > 0L) {
                crateCounts[key] = count
            }
        }

        val itemCounts = mutableMapOf<String, Long>()
        config.getConfigurationSection("itemCounts")?.getKeys(false)?.forEach { key ->
            val count = config.getLong("itemCounts.$key")
            if (count > 0L) {
                itemCounts[key] = count
            }
        }

        return PlayerStatsCache(totalRolls = totalRolls, crateCounts = crateCounts, itemCounts = itemCounts)
    }

    private fun flushAsync(playerId: UUID, stats: PlayerStatsCache) {
        val snapshot = synchronized(stats) {
            if (!stats.dirty) {
                null
            } else {
                stats.dirty = false
                Triple(stats.totalRolls, stats.crateCounts.toMap(), stats.itemCounts.toMap())
            }
        } ?: return

        if (!saveSnapshot(playerId, snapshot.first, snapshot.second, snapshot.third)) {
            synchronized(stats) {
                stats.dirty = true
            }
        }
    }

    private fun flushSync(playerId: UUID, stats: PlayerStatsCache) {
        val snapshot = synchronized(stats) {
            if (!stats.dirty) {
                null
            } else {
                stats.dirty = false
                Triple(stats.totalRolls, stats.crateCounts.toMap(), stats.itemCounts.toMap())
            }
        } ?: return

        if (!saveSnapshot(playerId, snapshot.first, snapshot.second, snapshot.third)) {
            synchronized(stats) {
                stats.dirty = true
            }
        }
    }

    private fun saveSnapshot(playerId: UUID, totalRolls: Long, crateCounts: Map<String, Long>, itemCounts: Map<String, Long>): Boolean {
        return try {
            if (!playerStatsDir.exists()) {
                playerStatsDir.mkdirs()
            }

            if (totalRolls <= 0L && crateCounts.isEmpty() && itemCounts.isEmpty()) {
                val file = playerFile(playerId)
                if (file.exists()) {
                    file.delete()
                }
                return true
            }

            val config = YamlConfiguration()
            config.set("totalRolls", totalRolls)

            crateCounts.toSortedMap().forEach { (crateId, count) ->
                config.set("crateCounts.$crateId", count)
            }

            itemCounts.toSortedMap().forEach { (itemId, count) ->
                config.set("itemCounts.$itemId", count)
            }

            config.save(playerFile(playerId))
            true
        } catch (exception: Exception) {
            logger.warning("Could not save crate roll stats for player $playerId: ${exception.message}")
            false
        }
    }

    private fun ensureGlobalLoaded() {
        if (globalLoaded) {
            return
        }

        synchronized(globalLock) {
            if (globalLoaded) {
                return
            }

            try {
                if (!plugin.dataFolder.exists()) {
                    plugin.dataFolder.mkdirs()
                }

                val config = YamlConfiguration.loadConfiguration(globalStatsFile)
                globalTotalRolls = config.getLong("totalRolls").coerceAtLeast(0L)

                globalCrateCounts.clear()
                config.getConfigurationSection("crateCounts")?.getKeys(false)?.forEach { key ->
                    val count = config.getLong("crateCounts.$key")
                    if (count > 0L) {
                        globalCrateCounts[key] = count
                    }
                }

                globalItemCounts.clear()
                config.getConfigurationSection("itemCounts")?.getKeys(false)?.forEach { key ->
                    val count = config.getLong("itemCounts.$key")
                    if (count > 0L) {
                        globalItemCounts[key] = count
                    }
                }
            } catch (exception: Exception) {
                logger.warning("Could not load global crate roll stats: ${exception.message}")
            }

            globalLoaded = true
        }
    }

    private fun incrementGlobal(crateId: String, itemId: String) {
        ensureGlobalLoaded()

        synchronized(globalLock) {
            globalTotalRolls++
            globalCrateCounts[crateId] = (globalCrateCounts[crateId] ?: 0L) + 1L
            globalItemCounts[itemId] = (globalItemCounts[itemId] ?: 0L) + 1L
            globalDirty = true
        }

        scheduleGlobalSave()
    }

    private fun scheduleGlobalSave() {
        if (globalSaveQueued) {
            return
        }
        globalSaveQueued = true

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, Runnable {
            globalSaveQueued = false
            flushGlobalSync()
        }, 20L)
    }

    private fun flushGlobalSync() {
        ensureGlobalLoaded()

        val snapshot = synchronized(globalLock) {
            if (!globalDirty) {
                null
            } else {
                globalDirty = false
                Triple(globalTotalRolls, globalCrateCounts.toMap(), globalItemCounts.toMap())
            }
        } ?: return

        if (!saveGlobalSnapshot(snapshot.first, snapshot.second, snapshot.third)) {
            synchronized(globalLock) {
                globalDirty = true
            }
        }
    }

    private fun saveGlobalSnapshot(totalRolls: Long, crateCounts: Map<String, Long>, itemCounts: Map<String, Long>): Boolean {
        return try {
            if (!plugin.dataFolder.exists()) {
                plugin.dataFolder.mkdirs()
            }

            if (totalRolls <= 0L && crateCounts.isEmpty() && itemCounts.isEmpty()) {
                if (globalStatsFile.exists()) {
                    globalStatsFile.delete()
                }
                return true
            }

            val config = YamlConfiguration()
            config.set("totalRolls", totalRolls)

            crateCounts.toSortedMap().forEach { (crateId, count) ->
                config.set("crateCounts.$crateId", count)
            }

            itemCounts.toSortedMap().forEach { (itemId, count) ->
                config.set("itemCounts.$itemId", count)
            }

            config.save(globalStatsFile)
            true
        } catch (exception: Exception) {
            logger.warning("Could not save global crate roll stats: ${exception.message}")
            false
        }
    }
}
