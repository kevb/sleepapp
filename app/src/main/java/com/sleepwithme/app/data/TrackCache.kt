package com.sleepwithme.app.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Caches audio tracks to local storage. Keeps at most the current track
 * and the next track cached, deleting older files to save space.
 */
class TrackCache(context: Context) {

    private val cacheDir = File(context.filesDir, "track_cache").apply { mkdirs() }

    fun getCachedUri(track: Track): String {
        val file = fileForTrack(track)
        return if (file.exists()) file.toURI().toString() else track.url
    }

    fun isCached(track: Track): Boolean = fileForTrack(track).exists()

    /**
     * Download a track to local storage. Returns true on success.
     */
    suspend fun download(track: Track): Boolean = withContext(Dispatchers.IO) {
        val file = fileForTrack(track)
        if (file.exists()) return@withContext true

        val tempFile = File(cacheDir, "${track.id}.tmp")
        try {
            Log.d("TrackCache", "Downloading ${track.id} from ${track.url}")
            val conn = URL(track.url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.instanceFollowRedirects = true

            if (conn.responseCode != 200) {
                Log.e("TrackCache", "Download failed: HTTP ${conn.responseCode}")
                conn.disconnect()
                return@withContext false
            }

            conn.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            }
            conn.disconnect()

            tempFile.renameTo(file)
            Log.d("TrackCache", "Cached ${track.id} (${file.length() / 1024}KB)")
            true
        } catch (e: Exception) {
            Log.e("TrackCache", "Download failed for ${track.id}", e)
            tempFile.delete()
            false
        }
    }

    /**
     * Ensure the current track and next track are cached.
     * Delete any other cached tracks to save space.
     */
    suspend fun ensureCached(collection: Collection, currentIndex: Int) {
        val keep = mutableSetOf<String>()

        // Cache current track
        val current = collection.tracks.getOrNull(currentIndex)
        if (current != null) {
            keep.add(fileForTrack(current).name)
            download(current)
        }

        // Prefetch next track
        val next = collection.tracks.getOrNull(currentIndex + 1)
        if (next != null) {
            keep.add(fileForTrack(next).name)
            download(next)
        }

        // Clean up old tracks
        withContext(Dispatchers.IO) {
            cacheDir.listFiles()?.forEach { file ->
                if (file.name !in keep && !file.name.endsWith(".tmp")) {
                    Log.d("TrackCache", "Deleting old cache: ${file.name}")
                    file.delete()
                }
            }
        }
    }

    private fun fileForTrack(track: Track): File {
        // Use the URL filename (e.g. "good-place-track01.opus") to avoid collisions between collections
        val urlFilename = track.url.substringAfterLast("/")
        return File(cacheDir, urlFilename)
    }
}
