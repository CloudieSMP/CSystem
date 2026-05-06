package library

import chat.ChatUtility.broadcastAll
import chat.Formatting.allTags
import logger
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import plugin
import util.Keys
import util.isTag
import util.timeRemainingFormattedSeconds
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object TagHelper {
    data class PlayerTagStats(
        val name: String,
        val isTagged: Boolean,
        val taggedBy: String,
        val taggedTime: Long,
        val hasBeenTaggedAmount: Int,
        val hasTaggedAmount: Int
    )

    private val cache = ConcurrentHashMap<UUID, PlayerTagStats>()
    private val savePending = AtomicBoolean(false)

    @Volatile
    private var loaded = false

    private val tagFile: File
        get() = File(plugin.dataFolder, "tags.yml")

    private val cooldown: Int
        get() = plugin.config.tagYourIt.cooldownSeconds

    private val cooldownBackTaggingSeconds: Int
        get() = plugin.config.tagYourIt.cooldownBackTaggingSeconds

    fun loadSync() {
        if (loaded) return

        try {
            if (!plugin.dataFolder.exists()) plugin.dataFolder.mkdirs()
            val backingFile = tagFile
            if (!backingFile.exists()) {
                loaded = true
                return
            }

            val yaml = YamlConfiguration.loadConfiguration(backingFile)
            for (key in yaml.getKeys(false)) {
                val uuid = runCatching { UUID.fromString(key) }.getOrNull() ?: continue
                val section = yaml.getConfigurationSection(key) ?: continue
                cache[uuid] = PlayerTagStats(
                    name = section.getString("name") ?: "",
                    isTagged = section.getBoolean("isTagged"),
                    taggedBy = section.getString("taggedBy") ?: "",
                    taggedTime = section.getLong("taggedTime"),
                    hasBeenTaggedAmount = section.getInt("hasBeenTaggedAmount"),
                    hasTaggedAmount = section.getInt("hasTaggedAmount")
                )
            }
            loaded = true
        } catch (ex: Exception) {
            logger.warning("Failed to load tag data: ${ex.message}")
            loaded = true
        }
    }

    fun flushAllSync() {
        if (!loaded) return

        try {
            if (!plugin.dataFolder.exists()) plugin.dataFolder.mkdirs()

            val yaml = YamlConfiguration()
            for ((uuid, stats) in cache.toSortedMap(compareBy { it.toString() })) {
                val key = uuid.toString()
                yaml.set("$key.name", stats.name)
                yaml.set("$key.isTagged", stats.isTagged)
                yaml.set("$key.taggedBy", stats.taggedBy)
                yaml.set("$key.taggedTime", stats.taggedTime)
                yaml.set("$key.hasBeenTaggedAmount", stats.hasBeenTaggedAmount)
                yaml.set("$key.hasTaggedAmount", stats.hasTaggedAmount)
            }
            yaml.save(tagFile)
        } catch (ex: Exception) {
            logger.warning("Failed to save tag data: ${ex.message}")
        }
    }

    private fun scheduleSave() {
        if (!savePending.compareAndSet(false, true)) return
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            savePending.set(false)
            flushAllSync()
        })
    }

    // ── UUID overloads ────────────────────────────────────────────────────────

    fun getStats(uuid: UUID): PlayerTagStats? {
        if (!loaded) loadSync()
        return cache[uuid]
    }

    fun setStats(uuid: UUID, stats: PlayerTagStats) {
        if (!loaded) loadSync()
        cache[uuid] = stats
    }

    fun updateStats(uuid: UUID, block: (PlayerTagStats) -> PlayerTagStats) {
        if (!loaded) loadSync()
        val current = cache[uuid] ?: return
        cache[uuid] = block(current)
    }

    /** Marks [uuid] as tagged by [taggerName], incrementing both players' counters. */
    fun tagPlayer(uuid: UUID, taggerUuid: UUID, taggerName: String, now: Long = System.currentTimeMillis() / 1000L) {
        if (!loaded) loadSync()

        cache.compute(uuid) { _, stats ->
            stats?.copy(
                isTagged = true,
                taggedBy = taggerName,
                taggedTime = now,
                hasBeenTaggedAmount = stats.hasBeenTaggedAmount + 1
            ) ?: return@compute null
        }

        cache.compute(taggerUuid) { _, stats ->
            stats?.copy(hasTaggedAmount = stats.hasTaggedAmount + 1) ?: return@compute null
        }

        scheduleSave()
    }

    /**
     * Marks [uuid] as tagged with no tagger (e.g. to start the game).
     * [taggedBy] defaults to an empty string.
     */
    fun tagPlayer(uuid: UUID, taggedBy: String = "", now: Long = System.currentTimeMillis() / 1000L) {
        if (!loaded) loadSync()

        cache.compute(uuid) { _, stats ->
            stats?.copy(
                isTagged = true,
                taggedBy = taggedBy,
                taggedTime = now,
            ) ?: return@compute null
        }

        scheduleSave()
    }

    /** Marks [uuid] as no longer tagged. */
    fun untagPlayer(uuid: UUID) {
        if (!loaded) loadSync()
        cache.compute(uuid) { _, stats ->
            stats?.copy(isTagged = false) ?: return@compute null
        }
        scheduleSave()
    }

    fun isCurrentlyTagged(uuid: UUID): Boolean {
        if (!loaded) loadSync()
        return cache[uuid]?.isTagged == true
    }

    /** Ensures a player entry exists (or updates their name). Call on join. */
    fun ensurePlayer(uuid: UUID, name: String) {
        if (!loaded) loadSync()
        cache.compute(uuid) { _, existing ->
            existing?.copy(name = name) ?: PlayerTagStats(
                name = name,
                isTagged = false,
                taggedBy = "",
                taggedTime = 0L,
                hasBeenTaggedAmount = 0,
                hasTaggedAmount = 0
            )
        }
    }

    fun getAllStats(): Map<UUID, PlayerTagStats> {
        if (!loaded) loadSync()
        return cache.toMap()
    }

    // ── Player overloads ──────────────────────────────────────────────────────

    fun getStats(player: Player) = getStats(player.uniqueId)

    fun setStats(player: Player, stats: PlayerTagStats) = setStats(player.uniqueId, stats)

    fun updateStats(player: Player, block: (PlayerTagStats) -> PlayerTagStats) = updateStats(player.uniqueId, block)

    /** Marks [player] as tagged by [tagger], incrementing both players' counters. */
    fun tagPlayer(player: Player, tagger: Player, now: Long = System.currentTimeMillis() / 1000L) =
        tagPlayer(player.uniqueId, tagger.uniqueId, tagger.name, now)

    /** Marks [player] as tagged with no tagger (e.g. to start the game). */
    fun tagPlayer(player: Player, now: Long = System.currentTimeMillis() / 1000L) =
        tagPlayer(player.uniqueId, now = now)

    fun untagPlayer(player: Player) = untagPlayer(player.uniqueId)

    fun isCurrentlyTagged(player: Player) = isCurrentlyTagged(player.uniqueId)

    fun ensurePlayer(player: Player) = ensurePlayer(player.uniqueId, player.name)

    // Others

    fun isHoldingTag(player: Player): Boolean {
        val item = player.inventory.itemInMainHand
        if (item.type == Material.AIR) return false
        return item.itemMeta.persistentDataContainer.isTag()
    }

    fun createTagItem(): ItemStack {
        val item = ItemStack(Material.NAME_TAG)
        val meta = item.itemMeta ?: return item
        meta.displayName(allTags.deserialize("<yellow><bold>Tag"))
        meta.setMaxStackSize(1)
        meta.persistentDataContainer.set(Keys.IS_TAG, PersistentDataType.BYTE, 1)
        item.itemMeta = meta
        return item
    }

    fun startTagging(tagger: Player, taggee: Player) {
        val taggerStats = getStats(tagger) ?: return
        val taggeeStats = getStats(taggee) ?: return

        val now = System.currentTimeMillis() / 1000L
        val timeSinceTaggerTagged = now - taggerStats.taggedTime
        val timeSinceTaggeeTagged = now - taggeeStats.taggedTime

        // Regular cooldown: tagger must have been "it" long enough
        if (timeSinceTaggerTagged < cooldown) {
            val remaining = cooldown - timeSinceTaggerTagged
            tagger.sendMessage(allTags.deserialize("<red>You must wait another <bold>${remaining}s</bold> before tagging!"))
            return
        }

        // Regular cooldown: taggee was recently tagged and has a grace period
        if (timeSinceTaggeeTagged < cooldown) {
            val remaining = cooldown - timeSinceTaggeeTagged
            tagger.sendMessage(allTags.deserialize("<red>${taggee.name} is immune for another <bold>${remaining}s</bold>!"))
            return
        }

        // Back-tag cooldown: tagger is trying to immediately tag back their tagger
        if (taggerStats.taggedBy == taggee.name) {
            if (timeSinceTaggerTagged < cooldownBackTaggingSeconds) {
                val remaining = cooldownBackTaggingSeconds - timeSinceTaggerTagged
                val formattedDuration = remaining.timeRemainingFormattedSeconds()
                tagger.sendMessage(allTags.deserialize("<red>You cannot back-tag ${taggee.name} for another <bold>${formattedDuration}</bold>!"))
                return
            }
        }

        broadcastAll("<green>${tagger.name} tagged ${taggee.name}!")
        tagger.inventory.remove(createTagItem())
        taggee.inventory.addItem(createTagItem())

        tagPlayer(taggee, tagger)
        untagPlayer(tagger)
    }
}
