package com.monapp.iptv

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

class XtreamClient(
    rawServer: String,
    private val user: String,
    private val pass: String
) {
    private val baseUrl: String = run {
        var s = rawServer.trim()
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "http://$s"
        }
        s.trimEnd('/')
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private fun apiUrl(action: String, extra: Map<String, String> = emptyMap()): String {
        val params = buildString {
            append("username=").append(user.trim())
            append("&password=").append(pass.trim())
            if (action.isNotBlank()) append("&action=").append(action)
            for ((k, v) in extra) {
                append("&").append(k).append("=").append(v)
            }
        }
        return "$baseUrl/player_api.php?$params"
    }

    private suspend fun getJsonObject(url: String): JSONObject? = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body?.string().orEmpty().trim()
            if (body.isEmpty() || !body.startsWith("{")) return@withContext null
            JSONObject(body)
        }
    }

    private suspend fun getJsonArray(url: String): JSONArray? = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body?.string().orEmpty().trim()
            if (body.isEmpty() || !body.startsWith("[")) return@withContext null
            JSONArray(body)
        }
    }

    suspend fun login(): Boolean {
        val root = getJsonObject(apiUrl("")) ?: return false
        val info = root.optJSONObject("user_info") ?: return false
        val status = info.optString("status", "")
        return status.equals("Active", ignoreCase = true) || info.optString("auth", "1") == "1"
    }

    suspend fun categories(section: Section): List<Category> {
        val action = when (section) {
            Section.LIVE -> "get_live_categories"
            Section.FILMS -> "get_vod_categories"
            Section.SERIES -> "get_series_categories"
        }
        val arr = getJsonArray(apiUrl(action)) ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                add(
                    Category(
                        category_id = obj.optString("category_id").takeIf { it.isNotBlank() },
                        category_name = obj.optString("category_name").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }

    suspend fun items(section: Section): List<Item> {
        return when (section) {
            Section.LIVE -> {
                val arr = getJsonArray(apiUrl("get_live_streams")) ?: return emptyList()
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val id = o.optInt("stream_id", -1)
                        if (id <= 0) continue
                        add(
                            Item(
                                section = Section.LIVE,
                                id = id,
                                title = o.optString("name", "Chaîne $id"),
                                image = o.optString("stream_icon").takeIf { it.isNotBlank() },
                                ext = "m3u8",
                                categoryId = o.optString("category_id").takeIf { it.isNotBlank() },
                                added = o.optString("added").toLongOrNull() ?: 0L
                            )
                        )
                    }
                }
            }
            Section.FILMS -> {
                val arr = getJsonArray(apiUrl("get_vod_streams")) ?: return emptyList()
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val id = o.optInt("stream_id", -1)
                        if (id <= 0) continue
                        add(
                            Item(
                                section = Section.FILMS,
                                id = id,
                                title = o.optString("name", "Film $id"),
                                image = o.optString("stream_icon").takeIf { it.isNotBlank() },
                                ext = o.optString("container_extension", "mp4"),
                                categoryId = o.optString("category_id").takeIf { it.isNotBlank() },
                                added = o.optString("added").toLongOrNull() ?: 0L
                            )
                        )
                    }
                }
            }
            Section.SERIES -> {
                val arr = getJsonArray(apiUrl("get_series")) ?: return emptyList()
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val id = o.optInt("series_id", -1)
                        if (id <= 0) continue
                        add(
                            Item(
                                section = Section.SERIES,
                                id = id,
                                title = o.optString("name", "Série $id"),
                                image = o.optString("cover").takeIf { it.isNotBlank() },
                                ext = null,
                                categoryId = o.optString("category_id").takeIf { it.isNotBlank() },
                                added = o.optString("last_modified").toLongOrNull() ?: 0L
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun vodMeta(vodId: Int): Meta {
        val root = getJsonObject(apiUrl("get_vod_info", mapOf("vod_id" to vodId.toString())))
        val info = root?.optJSONObject("info")
        return Meta(
            plot = info?.optString("plot")?.takeIf { it.isNotBlank() },
            genre = info?.optString("genre")?.takeIf { it.isNotBlank() },
            year = info?.optString("releasedate")?.takeIf { it.isNotBlank() },
            rating = info?.optString("rating")?.takeIf { it.isNotBlank() },
            duration = info?.optString("duration")?.takeIf { it.isNotBlank() },
            cast = info?.optString("cast")?.takeIf { it.isNotBlank() }
        )
    }

    suspend fun seriesDetail(seriesId: Int): Pair<Meta, List<Pair<String, List<Ep>>>> {
        val root = getJsonObject(apiUrl("get_series_info", mapOf("series_id" to seriesId.toString())))
        val info = root?.optJSONObject("info")
        val meta = Meta(
            plot = info?.optString("plot")?.takeIf { it.isNotBlank() },
            genre = info?.optString("genre")?.takeIf { it.isNotBlank() },
            year = info?.optString("releaseDate")?.takeIf { it.isNotBlank() },
            rating = info?.optString("rating")?.takeIf { it.isNotBlank() },
            duration = info?.optString("episode_run_time")?.takeIf { it.isNotBlank() },
            cast = info?.optString("cast")?.takeIf { it.isNotBlank() }
        )
        val seasonsMap = mutableMapOf<String, MutableList<Ep>>()
        val episodesObj = root?.optJSONObject("episodes")
        if (episodesObj != null) {
            val keys = episodesObj.keys()
            while (keys.hasNext()) {
                val seasonKey = keys.next()
                val arr = episodesObj.optJSONArray(seasonKey) ?: continue
                val list = seasonsMap.getOrPut(seasonKey) { mutableListOf() }
                for (i in 0 until arr.length()) {
                    val eo = arr.optJSONObject(i) ?: continue
                    val epId = eo.optString("id")
                    if (epId.isBlank()) continue
                    val epInfo = eo.optJSONObject("info")
                    list.add(
                        Ep(
                            id = epId,
                            title = eo.optString("title", "Épisode ${i + 1}"),
                            ext = eo.optString("container_extension", "mp4"),
                            num = eo.optInt("episode_num", i + 1),
                            image = epInfo?.optString("movie_image")?.takeIf { it.isNotBlank() },
                            duration = epInfo?.optString("duration")?.takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }
        val sortedSeasons = seasonsMap.entries
            .sortedBy { it.key.toIntOrNull() ?: Int.MAX_VALUE }
            .map { it.key to it.value.sortedBy { ep -> ep.num ?: Int.MAX_VALUE } }
        return meta to sortedSeasons
    }

    fun liveUrl(streamId: Int): String =
        "$baseUrl/live/${user.trim()}/${pass.trim()}/$streamId.m3u8"

    fun liveTsUrl(streamId: Int): String =
        "$baseUrl/live/${user.trim()}/${pass.trim()}/$streamId.ts"

    fun movieUrl(streamId: Int, ext: String?): String {
        val cleanExt = ext?.trim()?.removePrefix(".")?.ifBlank { "mp4" } ?: "mp4"
        return "$baseUrl/movie/${user.trim()}/${pass.trim()}/$streamId.$cleanExt"
    }

    fun episodeUrl(epId: String, ext: String?): String {
        val cleanExt = ext?.trim()?.removePrefix(".")?.ifBlank { "mp4" } ?: "mp4"
        return "$baseUrl/series/${user.trim()}/${pass.trim()}/$epId.$cleanExt"
    }
}
