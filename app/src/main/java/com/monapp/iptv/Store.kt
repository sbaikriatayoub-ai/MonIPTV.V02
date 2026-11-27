package com.monapp.iptv

import android.content.SharedPreferences
import com.squareup.moshi.Types

data class FavItem(
    val section: String,
    val id: Int,
    val title: String,
    val image: String?,
    val ext: String?,
    val cat: String?
)

data class HistItem(
    val key: String,
    val section: String,
    val id: Int,
    val title: String,
    val image: String?,
    val ext: String?,
    val epId: String?,
    val epExt: String?,
    val epLabel: String?,
    val pos: Long,
    val dur: Long,
    val ts: Long
)

class Store(private val prefs: SharedPreferences) {
    private val favAdapter =
        moshi.adapter<List<FavItem>>(Types.newParameterizedType(List::class.java, FavItem::class.java))
    private val histAdapter =
        moshi.adapter<List<HistItem>>(Types.newParameterizedType(List::class.java, HistItem::class.java))
    private val profAdapter =
        moshi.adapter<List<Profile>>(Types.newParameterizedType(List::class.java, Profile::class.java))

    fun loadProfiles(): List<Profile> {
        val loaded = try {
            prefs.getString("profiles", null)?.let { profAdapter.fromJson(it) }
        } catch (_: Exception) {
            null
        }
        return if (!loaded.isNullOrEmpty()) loaded else {
            val defaults = listOf(
                Profile("p1", "Profil 1", 0xFFF5A524),
                Profile("p2", "Profil 2", 0xFF3B82F6)
            )
            saveProfiles(defaults)
            defaults
        }
    }

    fun saveProfiles(list: List<Profile>) {
        prefs.edit().putString("profiles", profAdapter.toJson(list)).apply()
    }

    fun activeProfileId(): String = prefs.getString("active_profile", null) ?: "p1"

    fun setActiveProfileId(id: String) {
        prefs.edit().putString("active_profile", id).apply()
    }

    fun loadDisplayMode(): DisplayMode =
        DisplayMode.fromKey(prefs.getString("default_display_mode", DisplayMode.FULL.name))

    fun saveDisplayMode(mode: DisplayMode) {
        prefs.edit().putString("default_display_mode", mode.name).apply()
    }

    fun loadStartMuted(): Boolean = prefs.getBoolean("start_muted", false)

    fun saveStartMuted(muted: Boolean) {
        prefs.edit().putBoolean("start_muted", muted).apply()
    }

    fun loadFavs(profileId: String = activeProfileId()): List<FavItem> = try {
        prefs.getString("favs_$profileId", null)?.let { favAdapter.fromJson(it) } ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    fun saveFavs(l: List<FavItem>, profileId: String = activeProfileId()) {
        prefs.edit().putString("favs_$profileId", favAdapter.toJson(l)).apply()
    }

    fun loadHist(profileId: String = activeProfileId()): List<HistItem> = try {
        prefs.getString("hist_$profileId", null)?.let { histAdapter.fromJson(it) } ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    fun saveHist(l: List<HistItem>, profileId: String = activeProfileId()) {
        prefs.edit().putString("hist_$profileId", histAdapter.toJson(l)).apply()
    }
}

fun Item.toFav() = FavItem(section.name, id, title, image, ext, categoryId)
fun FavItem.toItem() = Item(Section.valueOf(section), id, title, image, ext, cat)
fun HistItem.toItem() = Item(
    Section.valueOf(section), id,
    if (epLabel != null) "$title · $epLabel" else title,
    image, ext, null
)
