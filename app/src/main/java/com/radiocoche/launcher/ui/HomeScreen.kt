package com.radiocoche.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.radiocoche.launcher.radar.RadarState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        Row(
            Modifier.fillMaxSize().padding(start = 16.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SpeedAndClock(h, Modifier.width(430.dp).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(p.panel)
                        .border(1.dp, p.border, RoundedCornerShape(20.dp))
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                ) {
                    when (h.source) {
                        Source.RADIO -> RadioPanel(h)
                        Source.BT -> BtPanel(h)
                        Source.MUSICA -> MusicPanel(h)
                    }
                }
                Dock(h)
            }
        }

        if (!h.isDefaultHome) {
            CarButton(
                Modifier.align(Alignment.TopCenter).padding(top = 6.dp).height(46.dp),
                bg = p.panel, border = p.accent, borderWidth = 2.dp, radius = 23.dp, onClick = { h.openHomeChooser() },
            ) {
                Text(
                    "Poner como pantalla de inicio",
                    color = p.accent, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = Tema.body,
                    modifier = Modifier.padding(horizontal = 22.dp),
                )
            }
        }

        if (h.isDefaultHome && !h.canOverlay) {
            CarButton(
                Modifier.align(Alignment.TopCenter).padding(top = 6.dp).height(46.dp),
                bg = p.panel, border = p.accent, borderWidth = 2.dp, radius = 23.dp, onClick = { h.openOverlaySettings() },
            ) {
                Text(
                    "Permitir avisos de radar sobre otras apps",
                    color = p.accent, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = Tema.body,
                    modifier = Modifier.padding(horizontal = 22.dp),
                )
            }
        }

        if (h.showApps) AppsOverlay(h)
        if (h.showNavChooser) NavChooser(h)
        if (h.showRadars) RadarDialog(h)
        // Destello dentro del launcher cuando no hay permiso para pintarlo sobre las demás apps.
        val flashAlert = RadarState.alert
        if (RadarState.flash && !RadarState.overlayActive && flashAlert != null) RadarFlash(flashAlert)
    }
}

// ------------------------------------------------------------------ izquierda

@Composable
private fun SpeedAndClock(h: HomeState, modifier: Modifier) {
    val p = Tema.p
    val alert = RadarState.alert
    val showCard = alert != null && !RadarState.flash
    Box(modifier) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            // La hora monta un poco sobre la parte baja del velocímetro, como en el diseño.
            verticalArrangement = Arrangement.spacedBy((-44).dp, Alignment.CenterVertically),
        ) {
            Box(Modifier.size(410.dp)) {
                Speedometer(h.speed.kmh, Modifier.fillMaxSize())
                // Indicadores de luces y GPS, en el hueco de abajo del velocímetro.
                RadarIndicators(h, Modifier.align(Alignment.TopCenter).padding(top = 322.dp))
            }
            // El reloj ocupa siempre su sitio (así no se mueve nada); con aviso se oculta.
            Column(
                Modifier.alpha(if (showCard) 0f else 1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    h.time,
                    color = p.text, fontSize = 96.sp, lineHeight = 96.sp,
                    fontFamily = Tema.condensed, fontWeight = FontWeight.SemiBold,
                    style = TextStyle(shadow = Shadow(Color(0xCC000000), Offset(0f, 2f), 12f)),
                )
                Text(h.date, color = p.muted, fontSize = 24.sp, fontFamily = Tema.body)
            }
        }
        // Cartel del radar en una capa superior: tapa la hora y, si hace falta, los indicadores.
        if (showCard && alert != null) {
            RadarCard(alert, Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp).zIndex(1f))
        }
    }
}

// ------------------------------------------------------------------ panel de música

@Composable
private fun MusicPanel(h: HomeState) {
    val p = Tema.p
    val m = h.music
    val hasSong = m.title.isNotEmpty()

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (m.playing) "REPRODUCIENDO" else "MÚSICA",
                color = p.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                fontFamily = Tema.body, modifier = Modifier.weight(1f),
            )
        }

        Row(
            Modifier.fillMaxWidth().tap { h.openMusic() },
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(210.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(p.raised)
                    .border(1.dp, p.border2, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                val art = m.art
                if (art != null) {
                    Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    CarIcon(Ic.Note, 72.dp, p.accent)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (hasSong) m.title else "Nada sonando",
                    color = p.text, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = Tema.body, maxLines = 3, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (hasSong) m.artist else "Toca para abrir Música",
                    color = p.muted, fontSize = 21.sp, fontFamily = Tema.body,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (m.album.isNotEmpty()) {
                    Text(m.album, color = p.muted, fontSize = 18.sp, fontFamily = Tema.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val fraction = if (m.durationMs > 0) (m.positionMs.toFloat() / m.durationMs).coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(p.track)) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(p.accent))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(clock(m.positionMs), color = p.muted, fontSize = 17.sp, fontFamily = Tema.body)
                Text(clock(m.durationMs), color = p.muted, fontSize = 17.sp, fontFamily = Tema.body)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RectButton(Modifier.weight(1f), "Anterior", Ic.Prev) { h.musicPrevious() }
            RectButton(
                Modifier.weight(1.3f), if (m.playing) "Pausa" else "Reproducir",
                if (m.playing) Ic.Pause else Ic.Play, active = true, iconSize = 30.dp,
            ) { h.musicPlayPause() }
            RectButton(Modifier.weight(1f), "Siguiente", Ic.Next) { h.musicNext() }
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
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                r.bank.ifEmpty { "FM" },
                color = p.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = Tema.body,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, p.accent, RoundedCornerShape(8.dp))
                    .padding(horizontal = 11.dp, vertical = 2.dp),
            )
            Text(
                if (r.preset >= 0) "RADIO · P${r.preset + 1}" else "RADIO",
                color = p.muted, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontFamily = Tema.body,
            )
        }
        // Emisora: tocar abre la Radio; deslizar a la izquierda = presintonía siguiente, a la derecha = anterior.
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .tap { h.openRadio() }
                .pointerInput(Unit) {
                    val limit = 90.dp.toPx()
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val dx = offset.value
                            val dir = when {
                                dx < -limit -> 1
                                dx > limit -> -1
                                else -> 0
                            }
                            scope.launch {
                                if (dir == 0) {
                                    offset.animateTo(0f, tween(160))
                                } else {
                                    offset.animateTo(-dir * size.width.toFloat(), tween(150))
                                    if (dir > 0) h.radioNext() else h.radioPrevious()
                                    offset.snapTo(dir * size.width * 0.6f)
                                    offset.animateTo(0f, tween(170))
                                }
                            }
                        },
                        onDragCancel = { scope.launch { offset.animateTo(0f, tween(160)) } },
                    ) { change, amount ->
                        change.consume()
                        scope.launch { offset.snapTo(offset.value + amount) }
                    }
                }
                .graphicsLayer {
                    translationX = offset.value
                    alpha = (1f - kotlin.math.abs(offset.value) / (size.width.coerceAtLeast(1f))).coerceIn(0.2f, 1f)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (r.freq.isEmpty()) {
                Text("Radio", color = p.text, fontSize = 84.sp, lineHeight = 88.sp, fontFamily = Tema.condensed, fontWeight = FontWeight.SemiBold)
                Text("Toca para abrir la Radio", color = p.muted, fontSize = 20.sp, fontFamily = Tema.body)
            } else {
                Text(
                    r.name.ifEmpty { r.freq },
                    color = p.text, fontSize = 96.sp, lineHeight = 100.sp,
                    fontFamily = Tema.condensed, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (r.name.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            r.freq, color = p.accent, fontSize = 48.sp, lineHeight = 52.sp,
                            fontFamily = Tema.condensed, fontWeight = FontWeight.SemiBold,
                        )
                        Text(r.unit, color = p.muted, fontSize = 22.sp, fontFamily = Tema.body, modifier = Modifier.padding(bottom = 7.dp))
                    }
                } else {
                    Text(r.unit, color = p.accent, fontSize = 28.sp, fontFamily = Tema.body)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RectButton(Modifier.weight(1f), "Presintonía anterior", Ic.Prev) { h.radioPrevious() }
            RectButton(Modifier.weight(1f), "Presintonía siguiente", Ic.Next) { h.radioNext() }
        }
    }
}

// ------------------------------------------------------------------ panel de Bluetooth

@Composable
private fun BtPanel(h: HomeState) {
    val p = Tema.p
    val b = h.bt
    val hasSong = b.title.isNotEmpty()

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CarIcon(Ic.Bluetooth, 22.dp, p.accent, "Bluetooth")
            Text(
                b.phone.ifEmpty { "BLUETOOTH" },
                color = p.accent, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = Tema.body,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
        }

        Row(
            Modifier.fillMaxWidth().tap { h.openBluetooth() },
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(210.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(p.raised)
                    .border(1.dp, p.border2, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                CarIcon(Ic.Bluetooth, 84.dp, p.accent)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (hasSong) b.title else "Música del móvil",
                    color = p.text, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = Tema.body, maxLines = 3, overflow = TextOverflow.Ellipsis,
                )
                if (b.artist.isNotEmpty() || !hasSong) {
                    Text(
                        if (hasSong) b.artist else "Pon música en el móvil",
                        color = p.muted, fontSize = 21.sp, fontFamily = Tema.body,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val fraction = if (b.durationMs > 0) (b.positionMs.toFloat() / b.durationMs).coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(p.track)) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(p.accent))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (b.durationMs > 0) clock(b.positionMs) else "", color = p.muted, fontSize = 17.sp, fontFamily = Tema.body)
                Text(if (b.durationMs > 0) clock(b.durationMs) else "", color = p.muted, fontSize = 17.sp, fontFamily = Tema.body)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RectButton(Modifier.weight(1f), "Anterior", Ic.Prev) { b.previous() }
            RectButton(
                Modifier.weight(1.3f), if (b.playing) "Pausa" else "Reproducir",
                if (b.playing) Ic.Pause else Ic.Play, active = true, iconSize = 30.dp,
            ) { b.playPause() }
            RectButton(Modifier.weight(1f), "Siguiente", Ic.Next) { b.next() }
        }
    }
}

// ------------------------------------------------------------------ barra inferior

@Composable
private fun Dock(h: HomeState) {
    val p = Tema.p
    Row(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(p.panel)
            .border(1.dp, p.border, RoundedCornerShape(18.dp))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DockButton(Ic.Radio, "Radio", active = h.source == Source.RADIO) { h.openRadio() }
        DockButton(Ic.Note, "Música", active = h.source != Source.RADIO) { if (h.source == Source.BT) h.openBluetooth() else h.openMusic() }
        DockButton(Ic.Nav, "Navegador", onLongClick = { h.showNavChooser = true }) { h.openNavigator() }
        DockButton(Ic.Car, "Android Auto") { h.openAndroidAuto() }
        DockButton(Ic.Phone, "Teléfono") { h.openPhone() }
        DockButton(Ic.Apps, "Aplicaciones") { h.showApps = true }
        DockButton(Ic.Gear, "Ajustes", onLongClick = { h.openAndroidSettings() }) { h.openSettings() }
    }
}

@Composable
private fun RowScope.DockButton(
    icon: ImageVector,
    description: String,
    active: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val p = Tema.p
    // Botón redondo centrado en su hueco: los siete quedan repartidos a lo ancho de la barra.
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        CarButton(
            Modifier.size(58.dp),
            bg = if (active) p.panel else p.raised,
            border = if (active) p.accent else p.border2,
            borderWidth = if (active) 2.dp else 1.dp,
            radius = 29.dp,
            onLongClick = onLongClick,
            onClick = onClick,
        ) {
            CarIcon(icon, 28.dp, if (active) p.accent else p.muted, description)
        }
    }
}

/** Botón rectangular del reproductor y de la radio (el activo, con borde naranja). */
@Composable
private fun RectButton(
    modifier: Modifier,
    description: String,
    icon: ImageVector,
    active: Boolean = false,
    iconSize: androidx.compose.ui.unit.Dp = 28.dp,
    onClick: () -> Unit,
) {
    val p = Tema.p
    CarButton(
        modifier.height(58.dp),
        bg = if (active) p.panel else p.raised,
        border = if (active) p.accent else p.border2,
        borderWidth = if (active) 2.dp else 1.dp,
        radius = 14.dp,
        onClick = onClick,
    ) {
        CarIcon(icon, iconSize, if (active) p.accent else p.text, description)
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
            Text("Aplicaciones", color = p.text, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, fontFamily = Tema.body)
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
