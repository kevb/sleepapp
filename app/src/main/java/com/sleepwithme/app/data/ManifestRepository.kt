package com.sleepwithme.app.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

class ManifestRepository(context: Context) {

    companion object {
        private const val BASE_URL = "https://github.com/kevb/sleepapp/releases/download/audio-v1/"
        private const val MANIFEST_URL = "${BASE_URL}manifest.json"
        private const val PREFS_NAME = "manifest_cache"
        private const val KEY_JSON = "manifest_json"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _manifest = MutableStateFlow<Manifest?>(null)
    val manifest: StateFlow<Manifest?> = _manifest

    init {
        val cached = prefs.getString(KEY_JSON, null)
        if (cached != null) {
            _manifest.value = parseAndResolve(cached)
        }
    }

    suspend fun refresh() {
        withContext(Dispatchers.IO) {
            try {
                Log.d("ManifestRepo", "Fetching manifest from $MANIFEST_URL")
                val conn = URL(MANIFEST_URL).openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.instanceFollowRedirects = true
                val responseCode = conn.responseCode
                Log.d("ManifestRepo", "Response code: $responseCode")
                if (responseCode != 200) {
                    Log.e("ManifestRepo", "Error: ${conn.errorStream?.bufferedReader()?.readText()}")
                    conn.disconnect()
                    return@withContext
                }
                val body = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                Log.d("ManifestRepo", "Manifest fetched: ${body.take(100)}")
                prefs.edit().putString(KEY_JSON, body).apply()
                _manifest.value = parseAndResolve(body)
                Log.d("ManifestRepo", "Manifest parsed: ${_manifest.value?.collections?.size} collections")
            } catch (e: Exception) {
                Log.e("ManifestRepo", "Failed to fetch manifest", e)
            }
        }
    }

    private fun parseAndResolve(jsonString: String): Manifest {
        val manifest = json.decodeFromString<Manifest>(jsonString)
        return manifest.copy(
            collections = manifest.collections.map { collection ->
                collection.copy(
                    tracks = collection.tracks.map { track ->
                        track.copy(url = BASE_URL + track.url)
                    }
                )
            }
        )
    }
}
