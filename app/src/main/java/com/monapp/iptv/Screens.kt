package com.monapp.iptv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage

@Composable
fun App(vm: AppViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.autoLogin() }
    val req = vm.play
    when {
        req != null -> PlayerScreen(
            req = req,
            onProgress = { p, d -> vm.saveProgress(req, p, d) },
            onBack = { vm.stopPlaying() }
        )
        vm.client == null -> LoginScreen(vm)
        vm.detail != null -> DetailScreen(vm)
        else -> MainScreen(vm)
    }
}

@Composable
fun MainScreen(vm: AppViewModel) {
    BackHandler(enabled = vm.dest != Dest.HOME) { vm.selectDest(Dest.HOME) }
    Row(Modifier.fillMaxSize()) {
        Rail(vm)
        Box(Modifier.weight(1f).fillMaxHeight()) {
            when (vm.dest) {
                Dest.HOME -> HomeScreen(vm)
                Dest.SETTINGS -> SettingsScreen(vm)
                else -> GridScreen(vm, vm.dest)
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Paramètres", color = Txt, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Panel)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Taille & format d'affichage par défaut", color = Txt, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(
                "Choisissez le ratio appliqué à l'ouverture du lecteur (modifiable aussi en direct pendant la lecture).",
                color = Mut, fontSize = 13.sp
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(DisplayMode.entries) { mode ->
                    Chip(
                        label = mode.label,
                        selected = vm.defaultDisplayMode == mode,
                        onClick = { vm.updateDefaultDisplayMode(mode) }
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Panel)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Démarrer la lecture sans son (Muet)", color = Txt, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text("Coupe automatiquement le son au lancement d'une chaîne ou d'une vidéo.", color = Mut, fontSize = 13.sp)
            }
            Switch(
                checked = vm.startMuted,
                onCheckedChange = { vm.updateStartMuted(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OnAccent,
                    checkedTrackColor = Accent
                )
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Panel)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Profil actif : ${vm.activeProfile.name}", color = Txt, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("Effacer l'historique de reprise", false) {
                    vm.clearHistoryForActiveProfile()
                }
                Pill("Déconnexion", false) {
                    vm.logout()
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Panel)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("MonIPTV · v0.2", color = Txt, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text("Développé et signé par Ayoub S.", color = Accent, fontSize = 13.sp)
        }
    }
}

@Composable
fun Shelf(title: String, tiles: List<Tile>) {
    Column {
        Text(
            title, color = Txt, fontSize = 16.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
        ) {
            items(tiles) { t ->
                PosterCard(t.item, Modifier.width(112.dp), 0.67f, t.progress, t.onClick)
            }
        }
    }
}

@Composable
fun Hero(item: Item, height: Dp, onPlay: () -> Unit, onInfo: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(PanelHi)
    ) {
        AsyncImage(
            model = item.image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().aspectRatio(0.67f)
        )
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .aspectRatio(0.67f)
                .background(Brush.horizontalGradient(listOf(PanelHi, Color.Transparent)))
        )
        Column(
            Modifier.align(Alignment.CenterStart).fillMaxWidth(0.65f).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("À l'affiche", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(
                item.title, color = Txt, fontSize = 22.sp, fontWeight = FontWeight.Medium,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("▶ Lecture", true, onPlay)
                Pill("Détails", false, onInfo)
            }
        }
    }
}

@Composable
fun HomeScreen(vm: AppViewModel) {
    val films = vm.data[Section.FILMS]?.items.orEmpty()
    val series = vm.data[Section.SERIES]?.items.orEmpty()
    val recentFilms = remember(films) { films.sortedByDescending { it.added }.take(24) }
    val recentSeries = remember(series) { series.sortedByDescending { it.added }.take(24) }
    val hero = remember(recentFilms) { recentFilms.firstOrNull { !it.image.isNullOrBlank() } }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        TopBar("Accueil · ${vm.activeProfile.name}", vm, "Rechercher partout…")
        vm.error?.let { Text(it, color = ErrorRed, fontSize = 13.sp) }
        if (vm.query.isNotBlank()) {
            SearchResults(vm)
        } else {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val heroH = (maxHeight * 0.55f).coerceIn(150.dp, 300.dp)
                LazyColumn(
                    state = vm.homeState,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (hero != null) {
                        item { Hero(hero, heroH, { vm.playMovie(hero) }, { vm.openDetail(hero) }) }
                    }
                    val cont = vm.history.map { h ->
                        Tile(h.toItem(), if (h.dur > 0) h.pos.toFloat() / h.dur else null) { vm.resume(h) }
                    }
                    if (cont.isNotEmpty()) item { Shelf("Reprendre la lecture", cont) }
                    val favs = vm.favorites.take(24).map { f ->
                        val fi = f.toItem()
                        Tile(fi) { vm.openDetail(fi) }
                    }
                    if (favs.isNotEmpty()) item { Shelf("Mes favoris", favs) }
                    if (recentFilms.isNotEmpty()) {
                        item { Shelf("Films récents", recentFilms.map { m -> Tile(m) { vm.openDetail(m) } }) }
                    }
                    if (recentSeries.isNotEmpty()) {
                        item { Shelf("Séries récentes", recentSeries.map { s -> Tile(s) { vm.openDetail(s) } }) }
                    }
                    if (vm.loading && recentFilms.isEmpty()) {
                        item { Text("Chargement du catalogue…", color = Mut, fontSize = 14.sp) }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResults(vm: AppViewModel) {
    val q = vm.query.trim()
    val results = remember(vm.data, q) {
        val all = Section.entries.flatMap { s -> vm.data[s]?.items.orEmpty() }
        rankMatches(all, q).take(60)
    }
    if (q.length in 1..2) {
        Text(
            "Astuce : tapez au moins 3–4 lettres pour affiner les suggestions.",
            color = Mut, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp)
        )
    }
    if (results.isEmpty()) Text("Aucun résultat", color = Mut, modifier = Modifier.padding(8.dp))
    LazyVerticalGrid(
        state = vm.gridStates.getValue(Dest.HOME),
        columns = GridCells.Adaptive(116.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(results) { r ->
            PosterCard(
                r, Modifier.fillMaxWidth(),
                if (r.section == Section.LIVE) 1.5f else 0.67f, null
            ) { vm.openDetail(r) }
        }
    }
}

@Composable
fun GridScreen(vm: AppViewModel, dest: Dest) {
    val section = when (dest) {
        Dest.LIVE -> Section.LIVE
        Dest.FILMS -> Section.FILMS
        Dest.SERIES -> Section.SERIES
        else -> null
    }
    val sd = section?.let { vm.data[it] }
    val base: List<Item> =
        if (dest == Dest.FAVS) vm.favorites.map { it.toItem() } else sd?.items.orEmpty()
    val cats = sd?.categories.orEmpty()
    val cat = vm.selectedCat
    val q = vm.query
    val shown = remember(base, cat, q) {
        val byCat = if (cat == null) base else base.filter { it.categoryId == cat }
        rankMatches(byCat, q)
    }
    val live = dest == Dest.LIVE

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        TopBar(dest.label, vm, "Rechercher dans ${dest.label}…")
        vm.error?.let { Text(it, color = ErrorRed, fontSize = 13.sp) }
        if (q.trim().length in 1..2) {
            Text(
                "Tapez 3 ou 4 lettres pour filtrer rapidement par début ou contenu du titre.",
                color = Mut, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        if (cats.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
            ) {
                item { Chip("Tout", cat == null) { vm.selectCat(null) } }
                items(cats) { c ->
                    Chip(c.category_name.orEmpty(), cat == c.category_id) { vm.selectCat(c.category_id) }
                }
            }
        }
        if (shown.isEmpty()) {
            val msg = when {
                dest == Dest.FAVS -> "Aucun favori pour l'instant. Ouvre un film ou une série et choisis « Ajouter aux favoris »."
                vm.loading && base.isEmpty() -> "Chargement…"
                else -> "Aucun résultat"
            }
            Text(msg, color = Mut, fontSize = 14.sp, modifier = Modifier.padding(8.dp))
        }
        LazyVerticalGrid(
            state = vm.gridStates.getValue(dest),
            columns = GridCells.Adaptive(if (live) 154.dp else 116.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(shown) { entry ->
                PosterCard(entry, Modifier.fillMaxWidth(), if (live) 1.5f else 0.67f, null) {
                    vm.openDetail(entry)
                }
            }
        }
    }
}

@Composable
fun EpisodeRow(ep: Ep, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.fillMaxWidth().tvFocus(shape, onClick).background(Panel, shape).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.width(96.dp).aspectRatio(1.78f).clip(RoundedCornerShape(6.dp)).background(PanelHi)
        ) {
            AsyncImage(
                model = ep.image, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
        }
        Column(Modifier.weight(1f)) {
            val prefix = ep.num?.let { "É$it  " } ?: ""
            Text(prefix + ep.title, color = Txt, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            ep.duration?.let { Text(it, color = Mut, fontSize = 12.sp) }
        }
    }
}

@Composable
fun DetailScreen(vm: AppViewModel) {
    val cur = vm.detail ?: return
    BackHandler { vm.closeDetail() }
    val m = vm.meta
    val shape = RoundedCornerShape(12.dp)
    val resumable = vm.history.any { it.key == "m:${cur.id}" }
    val lastSeriesEp = if (cur.section == Section.SERIES) vm.lastSeriesHistory(cur.id) else null

    Row(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        AsyncImage(
            model = cur.image,
            contentDescription = cur.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(170.dp).aspectRatio(0.67f).clip(shape).background(Panel)
        )
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { Pill("← Retour", false) { vm.closeDetail() } }
            item { Text(cur.title, color = Txt, fontSize = 24.sp, fontWeight = FontWeight.Medium) }
            val line = listOfNotNull(m?.year, m?.genre, m?.rating?.let { "★ $it" }, m?.duration)
                .joinToString("  ·  ")
            if (line.isNotBlank()) item { Text(line, color = Mut, fontSize = 13.sp) }
            m?.plot?.let { p ->
                item { Text(p, color = Txt, fontSize = 14.sp, maxLines = 8, overflow = TextOverflow.Ellipsis) }
            }
            vm.error?.let { e -> item { Text(e, color = ErrorRed, fontSize = 13.sp) } }
            if (vm.metaLoading) item { Text("Chargement…", color = Mut, fontSize = 13.sp) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (cur.section == Section.FILMS) {
                        Pill(if (resumable) "▶ Reprendre" else "▶ Lecture", true) { vm.playMovie(cur) }
                    }
                    if (lastSeriesEp != null) {
                        val epTag = lastSeriesEp.epLabel?.let { " ($it)" } ?: ""
                        Pill("▶ Reprendre$epTag", true) { vm.resume(lastSeriesEp) }
                    }
                    Pill(if (vm.isFav(cur)) "★ Favori" else "☆ Favoris", false) {
                        vm.toggleFav(cur)
                    }
                }
            }
            if (cur.section == Section.SERIES && vm.seasons.isNotEmpty()) {
                val sel = vm.season ?: vm.seasons.first().first
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(vm.seasons) { s ->
                            Chip("Saison ${s.first}", s.first == sel) { vm.season = s.first }
                        }
                    }
                }
                val eps = vm.seasons.firstOrNull { it.first == sel }?.second.orEmpty()
                items(eps) { ep -> EpisodeRow(ep) { vm.playEpisode(cur, ep, sel) } }
            }
        }
    }
}

@Composable
fun PlayerScreen(req: PlayRequest, onProgress: (Long, Long) -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val ctx = LocalContext.current

    var err by remember(req) { mutableStateOf<String?>(null) }
    var isMuted by remember(req) { mutableStateOf(req.initialMuted) }
    var displayMode by remember(req) { mutableStateOf(req.initialDisplayMode) }
    var showOverlay by remember(req) { mutableStateOf(true) }
    var triedFallback by remember(req) { mutableStateOf(false) }

    val loadControl = remember {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(4_000, 15_000, 1_500, 2_500)
            .build()
    }

    val exo = remember(req) {
        ExoPlayer.Builder(ctx)
            .setLoadControl(loadControl)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(req.url))
                prepare()
                if (!req.isLive && req.startMs > 0) seekTo(req.startMs)
                volume = if (req.initialMuted) 0f else 1f
                playWhenReady = true
            }
    }

    LaunchedEffect(isMuted, exo) {
        exo.volume = if (isMuted) 0f else 1f
    }

    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                val fb = req.fallbackUrl
                if (fb != null && !triedFallback) {
                    triedFallback = true
                    err = "Flux principal indisponible, bascule vers flux alternatif…"
                    try {
                        exo.stop()
                        exo.clearMediaItems()
                        exo.setMediaItem(MediaItem.fromUri(fb))
                        exo.prepare()
                        exo.playWhenReady = true
                    } catch (_: Exception) {
                        err = "Lecture impossible sur ce flux (${error.errorCodeName})"
                    }
                } else {
                    err = "Flux indisponible ou interrompu (${error.errorCodeName})"
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    err = null
                }
            }
        }
        exo.addListener(listener)
        onDispose {
            exo.removeListener(listener)
            val safePos = exo.currentPosition.coerceAtLeast(0L)
            val safeDur = if (exo.duration != C.TIME_UNSET) exo.duration.coerceAtLeast(0L) else 0L
            if (!req.isLive) {
                onProgress(safePos, safeDur)
            }
            try {
                exo.stop()
                exo.release()
            } catch (_: Exception) {
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showOverlay = !showOverlay }
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .then(
                    displayMode.ratio?.let { r -> Modifier.aspectRatio(r) }
                        ?: Modifier.fillMaxSize()
                )
        ) {
            AndroidView(
                factory = { c ->
                    PlayerView(c).apply {
                        player = exo
                        useController = !req.isLive
                        keepScreenOn = true
                        isFocusable = true
                        resizeMode = if (displayMode == DisplayMode.FULL) {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        } else {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                        requestFocus()
                    }
                },
                update = { view ->
                    view.player = exo
                    view.useController = !req.isLive
                    view.resizeMode = if (displayMode == DisplayMode.FULL) {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showOverlay) {
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Color(0xB80E0F13))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Pill("← Retour", false, onBack)
                        Text(
                            text = req.title,
                            color = Txt,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Pill(
                        label = if (isMuted) "🔇 Son coupé" else "🔊 Son actif",
                        primary = isMuted,
                        onClick = { isMuted = !isMuted }
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DisplayMode.entries) { mode ->
                        Chip(
                            label = mode.label,
                            selected = displayMode == mode,
                            onClick = { displayMode = mode }
                        )
                    }
                }
            }
        }

        err?.let {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xD9171921))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(it, color = ErrorRed, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill("Réessayer", true) {
                        err = null
                        triedFallback = false
                        exo.setMediaItem(MediaItem.fromUri(req.url))
                        exo.prepare()
                        exo.playWhenReady = true
                    }
                    Pill("Quitter le lecteur", false, onBack)
                }
            }
        }
    }
}
