package com.monapp.iptv

import android.app.Application
import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.UUID

data class PlayRequest(
    val url: String,
    val title: String,
    val isLive: Boolean,
    val key: String,
    val startMs: Long,
    val hist: HistItem?,
    val fallbackUrl: String? = null,
    val initialDisplayMode: DisplayMode = DisplayMode.FULL,
    val initialMuted: Boolean = false
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("iptv", Context.MODE_PRIVATE)
    private val store = Store(prefs)

    var profiles by mutableStateOf(store.loadProfiles())
        private set
    var activeProfileId by mutableStateOf(store.activeProfileId())
        private set
    val activeProfile: Profile
        get() = profiles.firstOrNull { it.id == activeProfileId } ?: profiles.first()

    var defaultDisplayMode by mutableStateOf(store.loadDisplayMode())
        private set
    var startMuted by mutableStateOf(store.loadStartMuted())
        private set

    var client by mutableStateOf<XtreamClient?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    var dest by mutableStateOf(Dest.HOME)
        private set
    var data by mutableStateOf<Map<Section, SectionData>>(emptyMap())
        private set
    var selectedCat by mutableStateOf<String?>(null)
        private set
    var query by mutableStateOf("")
        private set

    var detail by mutableStateOf<Item?>(null)
        private set
    var meta by mutableStateOf<Meta?>(null)
        private set
    var metaLoading by mutableStateOf(false)
        private set
    var seasons by mutableStateOf<List<Pair<String, List<Ep>>>>(emptyList())
        private set
    var season by mutableStateOf<String?>(null)

    var play by mutableStateOf<PlayRequest?>(null)
        private set
    var favorites by mutableStateOf(store.loadFavs())
        private set
    var history by mutableStateOf(store.loadHist())
        private set

    val homeState = LazyListState()
    val gridStates: Map<Dest, LazyGridState> = Dest.entries.associateWith { LazyGridState() }

    val savedServer: String get() = prefs.getString("server", "") ?: ""
    val savedUser: String get() = prefs.getString("user", "") ?: ""
    val savedPass: String get() = prefs.getString("pass", "") ?: ""

    private var triedAuto = false

    fun selectProfile(id: String) {
        if (profiles.none { it.id == id }) return
        activeProfileId = id
        store.setActiveProfileId(id)
        favorites = store.loadFavs(id)
        history = store.loadHist(id)
    }

    fun renameProfile(id: String, newName: String) {
        val clean = newName.trim().take(24)
        if (clean.isBlank()) return
        profiles = profiles.map { if (it.id == id) it.copy(name = clean) else it }
        store.saveProfiles(profiles)
    }

    fun addProfile(name: String) {
        val clean = name.trim().take(24)
        if (clean.isBlank() || profiles.size >= 6) return
        val palette = listOf(0xFFF5A524, 0xFF3B82F6, 0xFF10B981, 0xFFEC4899, 0xFF8B5CF6, 0xFF06B6D4)
        val created = Profile("p_${UUID.randomUUID().toString().take(6)}", clean, palette[profiles.size % palette.size])
        profiles = profiles + created
        store.saveProfiles(profiles)
        selectProfile(created.id)
    }

    fun setDefaultDisplayMode(mode: DisplayMode) {
        defaultDisplayMode = mode
        store.saveDisplayMode(mode)
    }

    fun setStartMuted(muted: Boolean) {
        startMuted = muted
        store.saveStartMuted(muted)
    }

    fun clearHistoryForActiveProfile() {
        history = emptyList()
        store.saveHist(emptyList(), activeProfileId)
    }

    fun autoLogin() {
        if (triedAuto) return
        triedAuto = true
        if (savedServer.isNotBlank() && savedUser.isNotBlank()) login(savedServer, savedUser, savedPass)
    }

    fun login(server: String, user: String, pass: String) {
        viewModelScope.launch {
            busy = true
            error = null
            try {
                val c = XtreamClient(server, user, pass)
                if (c.login()) {
                    prefs.edit().putString("server", server).putString("user", user).putString("pass", pass).apply()
                    client = c
                    loadAll()
                } else {
                    error = "Compte inactif ou identifiants incorrects"
                }
            } catch (e: Exception) {
                error = "Connexion impossible : ${e.message}"
            } finally {
                busy = false
            }
        }
    }

    private fun loadAll() {
        val c = client ?: return
        viewModelScope.launch {
            loading = true
            for (s in listOf(Section.FILMS, Section.SERIES, Section.LIVE)) {
                try {
                    val d = SectionData(c.categories(s), c.items(s))
                    data = data + (s to d)
                } catch (e: Exception) {
                    error = "Chargement impossible (${s.label}) : ${e.message}"
                }
            }
            loading = false
        }
    }

    fun selectDest(d: Dest) {
        dest = d
        selectedCat = null
        query = ""
    }

    fun onQuery(q: String) {
        query = q
        viewModelScope.launch { gridStates[dest]?.scrollToItem(0) }
    }

    fun selectCat(c: String?) {
        selectedCat = c
        viewModelScope.launch { gridStates[dest]?.scrollToItem(0) }
    }

    fun isFav(item: Item): Boolean =
        favorites.any { it.section == item.section.name && it.id == item.id }

    fun toggleFav(item: Item) {
        favorites = if (isFav(item)) {
            favorites.filterNot { it.section == item.section.name && it.id == item.id }
        } else {
            listOf(item.toFav()) + favorites
        }
        store.saveFavs(favorites, activeProfileId)
    }

    fun openDetail(item: Item) {
        if (item.section == Section.LIVE) {
            playLive(item)
            return
        }
        val c = client ?: return
        detail = item
        meta = null
        seasons = emptyList()
        season = null
        metaLoading = true
        error = null
        viewModelScope.launch {
            try {
                if (item.section == Section.FILMS) {
                    val m = c.vodMeta(item.id)
                    if (detail?.id == item.id) meta = m
                } else {
                    val (m, s) = c.seriesDetail(item.id)
                    if (detail?.id == item.id) {
                        meta = m
                        seasons = s
                        val lastEp = history.firstOrNull { it.section == "SERIES" && it.id == item.id }
                        if (lastEp?.epLabel != null) {
                            val sNum = lastEp.epLabel.substringAfter("S").substringBefore(" ")
                            if (s.any { it.first == sNum }) season = sNum
                        }
                    }
                }
            } catch (e: Exception) {
                error = "Détails indisponibles : ${e.message}"
            } finally {
                metaLoading = false
            }
        }
    }

    fun lastSeriesHistory(seriesId: Int): HistItem? =
        history.firstOrNull { it.section == "SERIES" && it.id == seriesId }

    fun closeDetail() {
        detail = null
        error = null
    }

    fun playLive(item: Item) {
        val c = client ?: return
        play = PlayRequest(
            url = c.liveUrl(item.id),
            title = item.title,
            isLive = true,
            key = "",
            startMs = 0L,
            hist = null,
            fallbackUrl = c.liveTsUrl(item.id),
            initialDisplayMode = defaultDisplayMode,
            initialMuted = startMuted
        )
    }

    fun playMovie(item: Item) {
        val c = client ?: return
        val key = "m:${item.id}"
        val h = history.firstOrNull { it.key == key }
        val start = if (h != null && h.dur > 0 && h.pos < h.dur - 30_000) h.pos else 0L
        val t = HistItem(key, "FILMS", item.id, item.title, item.image, item.ext, null, null, null, 0L, 0L, 0L)
        play = PlayRequest(
            url = c.movieUrl(item.id, item.ext),
            title = item.title,
            isLive = false,
            key = key,
            startMs = start,
            hist = t,
            initialDisplayMode = defaultDisplayMode,
            initialMuted = startMuted
        )
    }

    fun playEpisode(series: Item, ep: Ep, seasonLabel: String) {
        val c = client ?: return
        val key = "e:${ep.id}"
        val h = history.firstOrNull { it.key == key }
        val start = if (h != null && h.dur > 0 && h.pos < h.dur - 30_000) h.pos else 0L
        val label = "S$seasonLabel" + (ep.num?.let { " É$it" } ?: "")
        val t = HistItem(key, "SERIES", series.id, series.title, series.image, null, ep.id, ep.ext, label, 0L, 0L, 0L)
        play = PlayRequest(
            url = c.episodeUrl(ep.id, ep.ext),
            title = "${series.title} · $label",
            isLive = false,
            key = key,
            startMs = start,
            hist = t,
            initialDisplayMode = defaultDisplayMode,
            initialMuted = startMuted
        )
    }

    fun resume(h: HistItem) {
        val c = client ?: return
        val start = if (h.dur > 0 && h.pos < h.dur - 30_000) h.pos else 0L
        val url = if (h.epId != null) c.episodeUrl(h.epId, h.epExt) else c.movieUrl(h.id, h.ext)
        play = PlayRequest(
            url = url,
            title = h.epLabel?.let { "${h.title} · $it" } ?: h.title,
            isLive = false,
            key = h.key,
            startMs = start,
            hist = h,
            initialDisplayMode = defaultDisplayMode,
            initialMuted = startMuted
        )
    }

    fun saveProgress(req: PlayRequest, pos: Long, dur: Long) {
        if (req.isLive) return
        val t = req.hist ?: return
        if (dur <= 0 || pos < 10_000) return
        val rest = history.filterNot { it.key == req.key }
        history = if (pos > dur - 60_000) {
            rest
        } else {
            (listOf(t.copy(pos = pos, dur = dur, ts = System.currentTimeMillis())) + rest).take(20)
        }
        store.saveHist(history, activeProfileId)
    }

    fun stopPlaying() {
        play = null
    }

    fun logout() {
        prefs.edit().remove("server").remove("user").remove("pass").apply()
        client = null
        data = emptyMap()
        detail = null
        play = null
        dest = Dest.HOME
        selectedCat = null
        query = ""
        error = null
    }
}

fun rankMatches(items: List<Item>, rawQuery: String): List<Item> {
    val q = rawQuery.trim()
    if (q.isEmpty()) return items
    val starts = items.filter { it.title.startsWith(q, ignoreCase = true) }
    val contains = items.filter {
        !it.title.startsWith(q, ignoreCase = true) && it.title.contains(q, ignoreCase = true)
    }
    return starts + contains
}
