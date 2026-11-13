package com.radiocoche.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radiocoche.launcher.AppEntry
import com.radiocoche.launcher.Apps
import com.radiocoche.launcher.HomeState
import com.radiocoche.launcher.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(h: HomeState) {
    val p = Tema.p

    LaunchedEffect(Unit) {
        while (true) {
            h.tick()
            delay(500)
        }
    }

    Box(Modifier.fillMaxSize().background(p.bg)) {
        Column(
            Modifier.fillMaxSize().padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SpeedAndClock(h, Modifier.width(380.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .height(440.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(p.panel)
                        .border(1.dp, p.border, RoundedCornerShape(20.dp)),
                ) {
                    if (h.source == Source.RADIO) RadioPanel(h) else MusicPanel(h)
                }
            }
            Dock(h, Modifier.align(Alignment.CenterHorizontally))
        }

        if (!h.isDefaultHome) {
            CarButton(
                Modifier.align(Alignment.TopCenter).padding(top = 8.dp).height(48.dp),
                bg = p.accent, radius = 24.dp, onClick = { h.openHomeChooser() },
            ) {
                Text(
                    "Poner como pantalla de inicio",
                    color = p.onAccent, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 22.dp),
                )
            }
        }

        if (h.showApps) AppsOverlay(h)
        if (h.showNavChooser) NavChooser(h)
    }
}

// ------------------------------------------------------------------ izquierda

@Composable
private fun SpeedAndClock(h: HomeState, modifier: Modifier) {
    val p = Tema.p
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        // La hora monta un poco sobre la parte baja del velocímetro, como en el diseño.
        verticalArrangement = Arrangement.spacedBy((-36).dp),
    ) {
        Speedometer(h.speed.kmh, Modifier.size(350.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                h.time,
                color = p.text, fontSize = 84.sp, lineHeight = 84.sp,
                fontFamily = Tema.condensed, fontWeight = FontWeight.SemiBold,
                style = TextStyle(shadow = Shadow(Color(0xB3000000), Offset(0f, 2f), 12f)),
            )
            Text(h.date, color = p.muted, fontSize = 20.sp)
        }
    }
}

// ------------------------------------------------------------------ panel de música

@Composable
private fun MusicPanel(h: HomeState) {
    val p = Tema.p
    val m = h.music
    val hasSong = m.title.isNotEmpty()

    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (m.playing) "REPRODUCIENDO" else "MÚSICA",
                color = p.accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            Modifier.fillMaxWidth().tap { h.openMusic() },
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(170.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(p.raised)
                    .border(1.dp, p.border2, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                val art = m.art
                if (art != null) {
                    Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    CarIcon(Ic.Note, 64.dp, p.accent)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (hasSong) m.title else "Nada sonando",
                    color = p.text, fontSize = 30.sp, lineHeight = 35.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (hasSong) m.artist else "Toca para abrir Música",
                    color = p.muted, fontSize = 21.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (m.album.isNotEmpty()) {
                    Text(m.album, color = p.dim, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val fraction = if (m.durationMs > 0) (m.positionMs.toFloat() / m.durationMs).coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(p.track)) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(p.accent))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(clock(m.positionMs), color = p.muted, fontSize = 18.sp)
                Text(clock(m.durationMs), color = p.muted, fontSize = 18.sp)
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundButton(Ic.Prev, "Anterior", 64.dp, 28.dp) { h.musicPrevious() }
            CarButton(Modifier.size(84.dp), bg = p.accent, radius = 42.dp, onClick = { h.musicPlayPause() }) {
                CarIcon(if (m.playing) Ic.Pause else Ic.Play, 36.dp, p.onAccent, if (m.playing) "Pausa" else "Reproducir")
            }
            RoundButton(Ic.Next, "Siguiente", 64.dp, 28.dp) { h.musicNext() }
        }
    }
}

private fun clock(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

// ------------------------------------------------------------------ panel de radio

@Composable
private fun RadioPanel(h: HomeState) {
    val p = Tema.p
    val r = h.radio

    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                r.bank.ifEmpty { "RADIO" },
                color = p.onAccent, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(p.accent).padding(horizontal = 14.dp, vertical = 4.dp),
            )
            Spacer(Modifier.weight(1f))
            Text("RADIO", color = p.muted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RoundButton(Ic.Prev, "Presintonía anterior", 60.dp, 28.dp) { h.radioPrevious() }
            Column(
                Modifier.weight(1f).tap { h.openRadio() },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (r.freq.isEmpty()) {
                    Text("Radio", color = p.accent, fontSize = 72.sp, lineHeight = 80.sp, fontFamily = Tema.condensed)
                    Text("Toca para abrir la Radio", color = p.muted, fontSize = 20.sp)
                } else {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            r.freq, color = p.accent, fontSize = 92.sp, lineHeight = 92.sp,
                            fontFamily = Tema.condensed, fontWeight = FontWeight.SemiBold, maxLines = 1,
                        )
                        Text(r.unit, color = p.muted, fontSize = 22.sp, modifier = Modifier.padding(bottom = 16.dp))
                    }
                    Text(
                        r.name.ifEmpty { if (r.alive) " " else "Toca para abrir la Radio" },
                        color = if (r.name.isEmpty()) p.muted else p.text,
                        fontSize = if (r.name.isEmpty()) 20.sp else 28.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        r.text.ifEmpty { " " }, color = p.muted, fontSize = 17.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            RoundButton(Ic.Next, "Presintonía siguiente", 60.dp, 28.dp) { h.radioNext() }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val tick = p.track
            Canvas(Modifier.fillMaxWidth().height(30.dp)) {
                val step = 12.dp.toPx()
                var x = 0f
                while (x <= size.width) {
                    drawLine(tick, Offset(x, 4.dp.toPx()), Offset(x, size.height), 1.dp.toPx())
                    x += step
                }
                drawLine(tick, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                val mx = (size.width * r.pos).coerceIn(2.dp.toPx(), size.width - 2.dp.toPx())
                drawRoundRect(
                    NeedleRed.copy(alpha = 0.28f),
                    Offset(mx - 5.dp.toPx(), 0f), Size(10.dp.toPx(), size.height),
                    CornerRadius(5.dp.toPx()),
                )
                drawRoundRect(
                    NeedleRed,
                    Offset(mx - 2.dp.toPx(), 0f), Size(4.dp.toPx(), size.height),
                    CornerRadius(2.dp.toPx()),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (l in r.labels) Text(l, color = p.muted, fontSize = 14.sp)
            }
        }

        val page = (r.preset.coerceAtLeast(0)) / 6
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in page * 6 until page * 6 + 6) {
                val f = r.presetFreqs.getOrNull(i).orEmpty()
                val n = r.presetNames.getOrNull(i).orEmpty()
                val active = i == r.preset && f.isNotEmpty()
                CarButton(
                    Modifier.weight(1f).height(56.dp),
                    bg = if (active) p.accentBg else p.raised,
                    border = if (active) p.accent else p.border2,
                    borderWidth = if (active) 2.dp else 1.dp,
                    radius = 10.dp,
                    onClick = { if (f.isNotEmpty()) h.radioPreset(i) else h.openRadio() },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("P${i + 1}", color = if (active) p.accent else p.dim, fontSize = 12.sp, lineHeight = 14.sp)
                        Text(
                            n.ifEmpty { f.ifEmpty { "—" } },
                            color = if (active) p.accent else if (f.isEmpty()) p.dim else p.text,
                            fontSize = 16.sp, lineHeight = 19.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ barra inferior

@Composable
private fun Dock(h: HomeState, modifier: Modifier) {
    val p = Tema.p
    Row(
        modifier
            .height(80.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(p.panel)
            .border(1.dp, p.border, RoundedCornerShape(40.dp))
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DockButton(Ic.Radio, "Radio", active = h.source == Source.RADIO) { h.openRadio() }
        DockButton(Ic.Note, "Música", active = h.source == Source.MUSICA) { h.openMusic() }
        DockButton(Ic.Nav, "Navegador", onLongClick = { h.showNavChooser = true }) { h.openNavigator() }
        DockButton(Ic.Phone, "Teléfono") { h.openPhone() }
        DockButton(Ic.Apps, "Aplicaciones") { h.showApps = true }
        DockButton(Ic.Gear, "Ajustes", onLongClick = { h.openAndroidSettings() }) { h.openSettings() }
    }
}

@Composable
private fun DockButton(
    icon: ImageVector,
    description: String,
    active: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val p = Tema.p
    CarButton(
        Modifier.size(60.dp),
        bg = if (active) p.accent else p.raised,
        border = if (active) null else p.border2,
        radius = 30.dp,
        onLongClick = onLongClick,
        onClick = onClick,
    ) {
        CarIcon(icon, 28.dp, if (active) p.onAccent else p.muted, description)
    }
}

@Composable
private fun RoundButton(icon: ImageVector, description: String, size: androidx.compose.ui.unit.Dp, iconSize: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val p = Tema.p
    CarButton(Modifier.size(size), bg = p.raised, border = p.border2, radius = size / 2, onClick = onClick) {
        CarIcon(icon, iconSize, p.text, description)
    }
}

/** Pulsación sin efecto visual (para zonas grandes como la carátula). */
@Composable
private fun Modifier.tap(onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)

// ------------------------------------------------------------------ todas las apps

@Composable
private fun AppsOverlay(h: HomeState) {
    val p = Tema.p
    val ctx = LocalContext.current
    val apps by produceState<List<AppEntry>?>(null) {
        value = withContext(Dispatchers.Default) { Apps.all(ctx) }
    }
    Column(
        Modifier.fillMaxSize().background(p.bg).tap {}.padding(horizontal = 28.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RoundButton(Ic.Back, "Volver", 56.dp, 28.dp) { h.showApps = false }
            Text("Aplicaciones", color = p.text, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        }
        val list = apps
        if (list == null) {
            Text("Cargando…", color = p.muted, fontSize = 20.sp)
        } else {
            LazyVerticalGrid(
                GridCells.Fixed(6),
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(list) { app ->
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { h.openApp(app) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AppIcon(app, 68.dp)
                        Text(
                            app.label, color = p.text, fontSize = 16.sp, lineHeight = 19.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIcon(app: AppEntry, size: androidx.compose.ui.unit.Dp) {
    val icon = app.icon
    if (icon != null) {
        Image(icon, contentDescription = null, modifier = Modifier.size(size))
    } else {
        Box(Modifier.size(size).clip(CircleShape).background(Tema.p.raised))
    }
}

// ------------------------------------------------------------------ elegir navegador

@Composable
private fun NavChooser(h: HomeState) {
    val p = Tema.p
    val ctx = LocalContext.current
    val apps = remember { Apps.navigators(ctx) }
    Box(
        Modifier.fillMaxSize().background(Color(0xCC000000)).tap { h.showNavChooser = false },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(p.panel)
                .border(1.dp, p.border2, RoundedCornerShape(20.dp))
                .tap {}
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("¿Qué navegador abre este botón?", color = p.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            if (apps.isEmpty()) {
                Text("No hay ninguna app de navegación instalada.", color = p.muted, fontSize = 18.sp)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(apps) { app ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(p.raised)
                            .clickable { h.chooseNavigator(app) }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        AppIcon(app, 48.dp)
                        Text(app.label, color = p.text, fontSize = 22.sp)
                    }
                }
            }
            Text(
                "Para cambiarlo más adelante, mantén pulsado el botón del navegador.",
                color = p.dim, fontSize = 16.sp,
            )
        }
    }
}
