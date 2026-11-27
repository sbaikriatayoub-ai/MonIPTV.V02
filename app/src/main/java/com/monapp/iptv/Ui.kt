package com.monapp.iptv

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

val Bg = Color(0xFF0E0F13)
val Panel = Color(0xFF171921)
val PanelHi = Color(0xFF22252F)
val Txt = Color(0xFFF2F2F2)
val Mut = Color(0xFF9A9DA8)
val Accent = Color(0xFFF5A524)
val OnAccent = Color(0xFF1A1200)
val ErrorRed = Color(0xFFFF6B6B)

data class Tile(val item: Item, val progress: Float? = null, val onClick: () -> Unit)

@Composable
fun IptvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent, onPrimary = OnAccent,
            background = Bg, surface = Panel,
            onBackground = Txt, onSurface = Txt
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Bg) { content() }
    }
}

@Composable
fun Modifier.tvFocus(shape: Shape = RoundedCornerShape(8.dp), onClick: () -> Unit): Modifier {
    var focused by remember { mutableStateOf(false) }
    return this
        .onFocusChanged { focused = it.isFocused }
        .scale(if (focused) 1.05f else 1f)
        .then(if (focused) Modifier.border(2.dp, Accent, shape) else Modifier)
        .clickable(onClick = onClick)
}

@Composable
fun Pill(label: String, primary: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Text(
        label,
        color = if (primary) OnAccent else Txt,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .tvFocus(shape, onClick)
            .background(if (primary) Accent else PanelHi, shape)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Text(
        label,
        color = if (selected) OnAccent else Txt,
        fontSize = 13.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .widthIn(max = 260.dp)
            .tvFocus(shape, onClick)
            .background(if (selected) Accent else Panel, shape)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    )
}

@Composable
fun RailItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Text(
        label,
        color = if (selected) Accent else Mut,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .tvFocus(shape, onClick)
            .background(if (selected) Accent.copy(alpha = 0.16f) else Color.Transparent, shape)
            .padding(horizontal = 12.dp, vertical = 11.dp)
    )
}

@Composable
fun PosterCard(item: Item, modifier: Modifier, ratio: Float, progress: Float?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val live = item.section == Section.LIVE
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .tvFocus(shape, onClick)
                .background(Panel, shape)
                .clip(shape),
            contentAlignment = Alignment.Center
        ) {
            if (item.image.isNullOrBlank()) {
                Text(
                    item.title, color = Mut, fontSize = 11.sp, textAlign = TextAlign.Center,
                    maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(8.dp)
                )
            }
            AsyncImage(
                model = item.image,
                contentDescription = item.title,
                contentScale = if (live) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier.fillMaxSize().padding(if (live) 12.dp else 0.dp)
            )
            if (progress != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color(0x55FFFFFF))
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .background(Accent)
                    )
                }
            }
        }
        Text(
            item.title, color = Txt, fontSize = 12.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp)
        )
    }
}

@Composable
fun TopBar(title: String, vm: AppViewModel, placeholder: String = "Rechercher") {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            title, color = Txt, fontSize = 20.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = vm.query,
            onValueChange = { vm.onQuery(it) },
            placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            modifier = Modifier.width(240.dp)
        )
    }
}

@Composable
fun Rail(vm: AppViewModel) {
    Column(
        Modifier
            .width(164.dp)
            .fillMaxHeight()
            .background(Panel)
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "MonIPTV", color = Txt, fontSize = 17.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
        Text(
            vm.activeProfile.name, color = Accent, fontSize = 12.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
        Dest.entries.forEach { d -> RailItem(d.label, vm.dest == d) { vm.selectDest(d) } }
        Spacer(Modifier.height(12.dp))
        RailItem("Déconnexion", false) { vm.logout() }
    }
}

@Composable
fun LoginScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    DisposableEffect(Unit) {
        val act = ctx as? Activity
        val prev = act?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            act?.requestedOrientation = prev
        }
    }

    var server by remember { mutableStateOf(vm.savedServer) }
    var user by remember { mutableStateOf(vm.savedUser) }
    var pass by remember { mutableStateOf(vm.savedPass) }
    var showPass by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(vm.activeProfile.name) }
    var newProfileName by remember { mutableStateOf("") }

    LaunchedEffect(vm.activeProfileId) {
        editName = vm.activeProfile.name
    }

    Row(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .weight(0.95f)
                .fillMaxHeight()
                .background(Panel, RoundedCornerShape(14.dp))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("MonIPTV · Profils", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Txt)
            Text("Choisissez ou renommez un profil", fontSize = 12.sp, color = Mut)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                vm.profiles.forEach { p ->
                    val selected = p.id == vm.activeProfileId
                    val avatarColor = Color(p.accentHex)
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) PanelHi else Bg)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) Accent else Color(0xFF2C303D),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { vm.selectProfile(p.id) }
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            Modifier.size(38.dp).clip(CircleShape).background(avatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                p.name.take(1).uppercase(),
                                color = OnAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp
                            )
                        }
                        Text(
                            p.name, color = Txt, fontSize = 12.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Nom du profil") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Pill("Renommer", false) { vm.renameProfile(vm.activeProfileId, editName) }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
                    placeholder = { Text("Nouveau profil…") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Pill("+ Ajouter", false) {
                    if (newProfileName.isNotBlank()) {
                        vm.addProfile(newProfileName)
                        newProfileName = ""
                    }
                }
            }
        }

        Column(
            Modifier
                .weight(1.15f)
                .fillMaxHeight()
                .background(Panel, RoundedCornerShape(14.dp))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Connexion au serveur", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Txt)
            OutlinedTextField(
                value = server,
                onValueChange = { server = it },
                label = { Text("URL du serveur (avec port)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("Identifiant", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Mot de passe", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    singleLine = true,
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showPass = !showPass }) {
                    Text(
                        if (showPass) "Masquer le mot de passe" else "Afficher le mot de passe",
                        color = Mut, fontSize = 12.sp
                    )
                }
                vm.error?.let {
                    Text(
                        it, color = ErrorRed, fontSize = 12.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                }
            }
            Button(
                onClick = { vm.login(server, user, pass) },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent)
            ) {
                Text(
                    if (vm.busy) "Connexion…" else "Se connecter (${vm.activeProfile.name})",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(
                onClick = { vm.login("demo", "demo", "demo") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Essayer la démo (vidéos libres de droits)",
                    color = Accent, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
