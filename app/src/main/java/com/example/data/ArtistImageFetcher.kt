package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ArtistImageFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    suspend fun fetchArtistImageUrl(artistName: String): String? = withContext(Dispatchers.IO) {
        val trimmed = artistName.trim()
        if (trimmed.isEmpty() ||
            trimmed.equals("Unknown Artist", ignoreCase = true) ||
            trimmed.equals("Local Audio", ignoreCase = true)
        ) {
            return@withContext null
        }

        // Primary: Deezer public artist search (returns crisp square artist portraits)
        fetchFromDeezer(trimmed) ?: fetchFromItunes(trimmed)
    }

    private fun fetchFromDeezer(artistName: String): String? {
        return runCatching {
            val encoded = URLEncoder.encode(artistName, "UTF-8")
            val request = Request.Builder()
                .url("https://api.deezer.com/search/artist?q=$encoded&limit=1")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val data = json.optJSONArray("data") ?: return null
                if (data.length() == 0) return null
                val first = data.getJSONObject(0)
                first.optString("picture_big")
                    .ifBlank { first.optString("picture_medium") }
                    .ifBlank { first.optString("picture") }
                    .ifBlank { null }
            }
        }.getOrNull()
    }

    private fun fetchFromItunes(artistName: String): String? {
        return runCatching {
            val encoded = URLEncoder.encode(artistName, "UTF-8")
            val request = Request.Builder()
                .url("https://itunes.apple.com/search?term=$encoded&entity=album&limit=1")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: return null
                if (results.length() == 0) return null
                val first = results.getJSONObject(0)
                first.optString("artworkUrl100")
                    .replace("100x100bb", "500x500bb")
                    .ifBlank { null }
            }
        }.getOrNull()
    }
}
