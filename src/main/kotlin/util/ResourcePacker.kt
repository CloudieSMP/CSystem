package util

import ResourcePack
import plugin
import logger

import com.google.gson.JsonParser

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

import kotlinx.coroutines.runBlocking

import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import org.bukkit.entity.Player

import java.net.URI
import java.security.MessageDigest
import java.util.*

object ResourcePacker {
    data class CachedPackMeta(
        val uri: URI,
        val priority: Int,
        val hash: String,
        val releaseLabel: String?
    )

    private class DownloadedPack(
        val bytes: ByteArray,
        val uri: URI,
        val releaseLabel: String?
    )

    data class CacheStatus(
        val configuredCount: Int,
        val cachedCount: Int,
        val lastRefreshAtMillis: Long?,
        val lastError: String?,
        val cached: List<CachedPackMeta>
    )

    private val client = HttpClient(CIO)

    @Volatile
    private var cachedPacks: List<ResourcePackInfo> = emptyList()

    @Volatile
    private var cachedMeta: List<CachedPackMeta> = emptyList()

    @Volatile
    private var lastRefreshAtMillis: Long? = null

    @Volatile
    private var lastError: String? = null

    fun applyPackPlayer(player: Player) = runBlocking {
        val packs = cachedPacks
        if (packs.isEmpty()) {
            logger.warning("Resource pack cache is empty; skipping apply for ${player.name}.")
            return@runBlocking
        }

        player.sendResourcePacks(
            ResourcePackRequest.resourcePackRequest().packs(packs)
        )
    }

    fun removePackPlayer(player: Player) {
        player.removeResourcePacks()
        player.clearResourcePacks()
    }

    fun refreshFromUrl(): Boolean = runBlocking {
        val configured = plugin.config.resourcePacks.sortedByDescending { it.priority }
        if (configured.isEmpty()) {
            cachedPacks = emptyList()
            cachedMeta = emptyList()
            lastRefreshAtMillis = System.currentTimeMillis()
            lastError = null
            logger.warning("No resource packs configured; cache cleared.")
            return@runBlocking true
        }

        val nextPacks = mutableListOf<ResourcePackInfo>()
        val nextMeta = mutableListOf<CachedPackMeta>()

        try {
            configured.forEach { pack ->
                val downloaded = fetch(pack)
                val resolvedHash = hash(downloaded.bytes)

                nextPacks += ResourcePackInfo.resourcePackInfo(UUID.randomUUID(), downloaded.uri, resolvedHash)
                nextMeta += CachedPackMeta(
                    uri = downloaded.uri,
                    priority = pack.priority,
                    hash = resolvedHash,
                    releaseLabel = downloaded.releaseLabel
                )
            }

            cachedPacks = nextPacks
            cachedMeta = nextMeta
            lastRefreshAtMillis = System.currentTimeMillis()
            lastError = null
            true
        } catch (e: Exception) {
            lastError = e.message ?: e::class.simpleName ?: "Unknown refresh error"
            logger.severe("Failed to refresh resource pack cache\nStack Trace:\n${e.stackTrace}\nMessage:\n${e.message}")
            false
        }
    }

    fun cacheStatus(): CacheStatus {
        return CacheStatus(
            configuredCount = plugin.config.resourcePacks.size,
            cachedCount = cachedPacks.size,
            lastRefreshAtMillis = lastRefreshAtMillis,
            lastError = lastError,
            cached = cachedMeta
        )
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private suspend fun fetch(pack: ResourcePack): DownloadedPack {
        val (downloadUri, tagName) = resolveGitHubReleaseAsset(pack)
        val response: HttpResponse = client.get(downloadUri.toString())
        return DownloadedPack(
            bytes = response.readRawBytes(),
            uri = downloadUri,
            releaseLabel = tagName
        )
    }

    /**
     * Calls the GitHub Releases API for the repository described by [pack.githubUrl],
     * finds the newest release whose tag name starts with "[pack.branch]-",
     * then returns the browser_download_url of the asset named [pack.zipName]
     * together with the matched tag name.
     */
    private suspend fun resolveGitHubReleaseAsset(pack: ResourcePack): Pair<URI, String> {
        val repoUri = URI(pack.githubUrl.trimEnd('/'))
        val pathParts = repoUri.path.trimStart('/').split('/')
        require(pathParts.size >= 2) {
            "githubUrl must contain owner and repo segments: ${pack.githubUrl}"
        }
        val owner = pathParts[0]
        val repo  = pathParts[1]

        val apiUrl = "https://api.github.com/repos/$owner/$repo/releases"
        logger.info("Fetching GitHub releases from $apiUrl for branch prefix '${pack.branch}-'")

        val response: HttpResponse = client.get(apiUrl) {
            header("Accept", "application/vnd.github+json")
            header("X-GitHub-Api-Version", "2022-11-28")
        }

        val body = response.bodyAsText()
        val releases = JsonParser.parseString(body).asJsonArray

        val prefix = "${pack.branch}-"

        // Releases are returned newest-first by the API; take the first match.
        val release = releases.asSequence()
            .map { it.asJsonObject }
            .firstOrNull { it["tag_name"]?.asString?.startsWith(prefix) == true }
            ?: error("No GitHub release found with tag starting with '$prefix' in ${pack.githubUrl}")

        val tagName = release["tag_name"].asString

        val asset = release["assets"].asJsonArray
            .asSequence()
            .map { it.asJsonObject }
            .firstOrNull { it["name"].asString == pack.zipName }
            ?: error("Asset '${pack.zipName}' not found in release '$tagName' of ${pack.githubUrl}")

        val downloadUrl = asset["browser_download_url"].asString
        logger.info("Resolved resource pack '${pack.zipName}' → $downloadUrl (tag: $tagName)")
        return Pair(URI(downloadUrl), tagName)
    }

    private fun hash(data: ByteArray): String {
        val messageDigest = MessageDigest.getInstance("SHA-1")
        val hashBytes = messageDigest.digest(data)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}