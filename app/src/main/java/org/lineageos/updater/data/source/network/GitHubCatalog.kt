/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.updater.data.source.network

import android.util.Log
import java.io.IOException
import java.net.URI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * haretic's updates, from GitHub releases: each release carries [CATALOG], the updates in it in
 * LineageOS's format, one per variant, each OTA zip an asset of the same release. The newest
 * release with a catalog is the one offered, prereleases only if the user asked for them;
 * GitHub's API needs no token for public releases.
 */
object GitHubCatalog {
    const val CATALOG = "haretic-ota.json"

    private const val TAG = "GitHubCatalog"

    private const val API_HOST = "api.github.com"

    /** How many releases' catalogs are read before giving up on finding this build's. */
    private const val MAX_CATALOGS = 3

    /** Hosts GitHub serves release assets from, which its download links redirect to. */
    private val ASSET_HOSTS = setOf(
        "github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
    )

    private val json = Json { ignoreUnknownKeys = true }

    @Suppress("PROVIDED_RUNTIME_TOO_LOW")
    @Serializable
    @JsonIgnoreUnknownKeys
    private data class Release(
        @SerialName("draft") val draft: Boolean = true,
        @SerialName("prerelease") val prerelease: Boolean = true,
        @SerialName("assets") val assets: List<Asset> = emptyList(),
    )

    @Suppress("PROVIDED_RUNTIME_TOO_LOW")
    @Serializable
    @JsonIgnoreUnknownKeys
    private data class Asset(
        @SerialName("name") val name: String,
        @SerialName("state") val state: String = "",
        @SerialName("browser_download_url") val url: String,
    )

    fun isGitHub(url: String) = URI(url).host == API_HOST

    /**
     * The updates [wanted] from the newest release whose catalog has any, its updates' links
     * checked to be the release's own. Older releases are looked at too, so one that lacks this
     * build's variant or its zip doesn't hide the one before it.
     */
    fun fetch(
        client: OkHttpClient,
        releasesUrl: String,
        prereleases: Boolean,
        wanted: (NetworkUpdate) -> Boolean,
    ): List<NetworkUpdate> {
        val releases = json.decodeFromString<List<Release>>(get(client, releasesUrl))
        return releases.asSequence()
            .filter { !it.draft && (prereleases || !it.prerelease) }
            .mapNotNull { release ->
                release.assets.firstOrNull { it.name == CATALOG && it.state == "uploaded" }
                    ?.let { release to it }
            }
            .take(MAX_CATALOGS)
            .map { (release, asset) -> updates(client, release, asset).filter(wanted) }
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()
    }

    private fun updates(client: OkHttpClient, release: Release, asset: Asset): List<NetworkUpdate> {
        require(isAssetUrl(asset.url)) { "Catalog outside GitHub: ${asset.url}" }
        val ownUrls = release.assets.filter { it.state == "uploaded" }.map { it.url }.toSet()
        val catalog = try {
            json.decodeFromString<List<NetworkUpdate>>(get(client, asset.url))
        } catch (e: SerializationException) {
            // a broken catalog: the release before it may still have a good one
            Log.e(TAG, "Bad catalog: ${asset.url}", e)
            return emptyList()
        }
        // toUpdate() takes the first file, so an entry needs one
        return catalog.filter { update ->
            update.files.isNotEmpty() &&
                    update.files.all { it.url in ownUrls && isAssetUrl(it.url) }
        }
    }

    private fun get(client: OkHttpClient, url: String): String {
        var next = url
        // Asset links redirect to GitHub's storage: followed by hand, to GitHub's hosts only
        repeat(5) {
            val request = Request.Builder()
                .url(next)
                .header("Accept", "application/vnd.github+json, application/octet-stream")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isRedirect) {
                    val location = response.header("Location")
                        ?: throw IOException("Redirect without a location")
                    val target = URI(next).resolve(location).toString()
                    if (!isAssetUrl(target)) {
                        throw IOException("Redirect outside GitHub: $target")
                    }
                    next = target
                    return@repeat
                }
                if (!response.isSuccessful) {
                    throw IOException("Unexpected HTTP status: ${response.code}")
                }
                return response.body?.string() ?: throw IOException("Empty response body")
            }
        }
        throw IOException("Too many redirects")
    }

    private fun isAssetUrl(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme == "https" && uri.userInfo == null && uri.port == -1 &&
                (uri.host == API_HOST || uri.host in ASSET_HOSTS)
    }.getOrDefault(false)
}
