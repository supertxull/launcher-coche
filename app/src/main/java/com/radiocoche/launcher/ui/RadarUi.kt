package com.radiocoche.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radiocoche.launcher.HomeState
import com.radiocoche.launcher.RadarView
import com.radiocoche.launcher.radar.Alert
import com.radiocoche.launcher.radar.Geo
import com.radiocoche.launcher.radar.Kind
import com.radiocoche.launcher.radar.Phase
import com.radiocoche.launcher.radar.Radar
import com.radiocoche.launcher.radar.RadarState
import com.radiocoche.launcher.radar.RadarStore
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SignRed = Color(0xFFFF0000)
private val AlertRed = Color(0xFFFF4040)
private val LIMITS = listOf(30, 40, 50, 60, 70, 80, 90, 100, 110, 120)

// ------------------------------------------------------------------ indicadores

/** Luces y GPS, con el estilo de los botones redondos de la barra. Tocar el GPS abre la ventana de radares. */
@Composable
fun RadarIndicators(h: HomeState, modifier: Modifier) {
    val p = Tema.p
    val lights = CarLights.on == true
    val gps = h.speed.hasFix
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(36.dp)) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (lights) p.panel else p.raised)
                .border(if (lights) 2.dp else 1.dp, if (lights) p.accent else p.border2, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            CarIcon(Ic.Lights, 26.dp, if (lights) p.accent else p.muted, if (lights) "Luces encendidas" else "Luces apagadas")
        }
        CarButton(
            Modifier.size(44.dp),
            bg = if (gps) p.panel else Color(0xFF4A0808),
            border = if (gps) p.accent else SignRed,
            borderWidth = 2.dp,
            radius = 22.dp,
            onClick = { h.openRadars() },
        ) {
            CarIcon(if (gps) Ic.Gps else Ic.GpsOff, 26.dp, if (gps) p.accent else Color.White, if (gps) "GPS conectado" else "Sin señal GPS")
        }
    }
}

// ------------------------------------------------------------------ señal

@Composable
private fun Sign(kind: Kind, limit: Int?, size: Dp) {
    val ring = size * 0.12f
    Box(
        Modifier.size(size).clip(CircleShape).background(Color.White).border(ring, SignRed, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (kind == Kind.RED_LIGHT) {
            TrafficLight(size * 0.48f)
        } else {
            Text(
                limit?.toString() ?: "!",
                color = Color.Black, fontFamily = Tema.condensed, fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp, lineHeight = (size.value * 0.44f).sp,
            )
        }
    }
}

@Composable
private fun TrafficLight(height: Dp) {
    Canvas(Modifier.size(height / 2, height)) {
        val w = size.width
        val hh = size.height
        drawRoundRect(Color(0xFF1A1A1A), Offset.Zero, Size(w, hh), CornerRadius(w * 0.25f))
        val r = w * 0.27f
        drawCircle(SignRed, r, Offset(w / 2, hh * 0.2f))
        drawCircle(Color(0xFF5A4A10), r, Offset(w / 2, hh * 0.5f))
        drawCircle(Color(0xFF16461F), r, Offset(w / 2, hh * 0.8f))
    }
}

private fun roundM(m: Int) = (m + 5) / 10 * 10

// ------------------------------------------------------------------ cartel (sitio de la hora)

@Composable
fun RadarCard(a: Alert, modifier: Modifier) {
    val p = Tema.p
    val es = Locale("es", "ES")
    val (title, main, sub) = when (a.phase) {
        Phase.APPROACH -> Triple(
            a.kind.label,
            "${roundM(a.distance)} m",
            if (a.kind == Kind.RED_LIGHT) "Foto-rojo" else a.limit?.let { "Límite $it km/h" } ?: "Sin velocidad",
        )
        Phase.IN_SECTION -> Triple("En tramo", "Media ${a.average} km/h", "Quedan " + String.format(es, "%.1f", a.remainingKm) + " km")
        Phase.SECTION_END -> Triple("Radar de tramo", "Fin de tramo", "Media final ${a.average} km/h")
    }
    val mainRed = a.phase == Phase.IN_SECTION && a.limit != null && a.average > a.limit
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Sign(a.kind, a.limit, 100.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title.uppercase(), color = p.accent, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontFamily = Tema.body)
            Text(main, color = if (mainRed) AlertRed else p.text, fontSize = 40.sp, lineHeight = 42.sp, fontFamily = Tema.condensed, fontWeight = FontWeight.SemiBold)
            Text(sub, color = p.muted, fontSize = 20.sp, fontFamily = Tema.body)
        }
    }
}

// ------------------------------------------------------------------ destello (sin permiso de superposición)

@Composable
fun RadarFlash(a: Alert) {
    val p = Tema.p
    Box(
        Modifier.fillMaxSize().background(Color.Black).border(12.dp, SignRed),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(56.dp)) {
            Sign(a.kind, a.limit, 330.dp)
            Column(Modifier.width(420.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (a.kind == Kind.SECTION) "INICIO DE TRAMO" else a.kind.label.uppercase(),
                    color = p.accent, fontSize = 62.sp, lineHeight = 62.sp, fontFamily = Tema.condensed, fontWeight = FontWeight.Bold,
                )
                Text(
                    when (a.kind) {
                        Kind.SECTION -> "Límite medio ${a.limit ?: "—"}"
                        Kind.RED_LIGHT -> "Semáforo con cámara"
                        else -> "a ${roundM(a.distance)} m"
                    },
                    color = Color.White, fontSize = 54.sp, lineHeight = 54.sp, fontFamily = Tema.condensed, fontWeight = FontWeight.SemiBold,
                )
                if (a.kind != Kind.RED_LIGHT) {
                    val over = a.limit != null && a.speed > a.limit
                    Text("Vas a ${a.speed} km/h", color = if (over) AlertRed else p.muted, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, fontFamily = Tema.body)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ ventana de radares

@Composable
fun RadarDialog(h: HomeState) {
    val p = Tema.p
    Box(
        Modifier.fillMaxSize().background(Color(0xBF000000)).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
        ) { h.closeRadars() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(780.dp)
                .height(504.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(p.panel)
                .border(1.dp, p.border, RoundedCornerShape(20.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Cabecera
            Row(Modifier.fillMaxWidth().height(50.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (h.radarView == RadarView.LIST || h.radarView == RadarView.EDIT) {
                    RoundIcon(Ic.Back, "Volver") { h.radarView = if (h.radarView == RadarView.EDIT) RadarView.LIST else RadarView.MAIN }
                }
                Text(
                    when (h.radarView) {
                        RadarView.MAIN -> "Radares"
                        RadarView.LIMIT -> "Radar añadido"
                        RadarView.LIST -> "Mis radares"
                        RadarView.EDIT -> "Editar radar"
                    },
                    color = p.text, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, fontFamily = Tema.body,
                    modifier = Modifier.weight(1f),
                )
                if (h.radarView == RadarView.MAIN) {
                    CarButton(Modifier.height(50.dp), bg = p.raised, border = p.border2, radius = 25.dp, onClick = { h.testAlert() }) {
                        Text("Probar aviso", color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = Tema.body, modifier = Modifier.padding(horizontal = 20.dp))
                    }
                }
                RoundIcon(Ic.Close, "Cerrar") { h.closeRadars() }
            }
            when (h.radarView) {
                RadarView.MAIN -> RadarMain(h)
                RadarView.LIMIT -> RadarLimit(h)
                RadarView.LIST -> RadarList(h)
                RadarView.EDIT -> RadarEdit(h)
            }
        }
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    val p = Tema.p
    CarButton(Modifier.size(50.dp), bg = p.raised, border = p.border2, radius = 25.dp, onClick = onClick) {
        CarIcon(icon, 26.dp, p.text, description)
    }
}

@Composable
private fun ColumnScope.RadarMain(h: HomeState) {
    val p = Tema.p
    val snap = h.snap
    // Segundos desde que se tocó el GPS (se refresca cada segundo).
    var now by remember { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    LaunchedEffect(snap) {
        while (true) {
            now = android.os.SystemClock.elapsedRealtime()
            delay(1000)
        }
    }
    Row(Modifier.fillMaxWidth().height(230.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        val can = snap != null
        BigButton(
            Modifier.alpha(if (can) 1f else 0.45f), active = can, icon = Ic.PinPlus, title = "Añadir radar aquí",
            sub = if (snap != null) "Posición tomada al tocar el GPS, hace ${((now - snap.time) / 1000).coerceAtLeast(0)} s" else "Sin señal GPS al tocar el indicador",
        ) { if (can) h.addRadarHere() }
        BigButton(
            Modifier, active = false, icon = Ic.Refresh,
            title = if (RadarStore.updating) "Actualizando…" else "Actualizar ahora",
            sub = if (RadarStore.updating) "Descargando la lista" else "Necesita wifi o datos",
        ) { h.updateRadars() }
        val n = RadarStore.user.size
        BigButton(
            Modifier, active = false, icon = Ic.ListIcon, title = "Mis radares",
            sub = if (n == 1) "1 radar añadido" else "$n radares añadidos",
        ) { h.radarView = RadarView.LIST }
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.Black)
            .border(1.dp, p.raised, RoundedCornerShape(14.dp)).padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val last = RadarStore.lastUpdate
        Info(
            "ÚLTIMA ACTUALIZACIÓN",
            when {
                last != 0L -> updatedText(last)
                RadarStore.bundledDate.isNotEmpty() -> "Incluida, ${RadarStore.bundledDate}"
                else -> "Nunca"
            },
        )
        Info("LISTA OFICIAL", if (RadarStore.official.isEmpty()) "Sin descargar" else "${RadarStore.official.size} radares")
        Info("TUS RADARES EXTRA", "${RadarStore.user.size}")
    }
    val status = when {
        h.toast.isNotEmpty() -> h.toast
        !RadarState.running -> "El avisador está parado"
        !h.speed.hasFix -> "Avisador en marcha · esperando al GPS"
        else -> "Avisador en marcha · " + (if (RadarState.heading) "rumbo OK" else "sin rumbo (en marcha se calcula)") +
            " · " + (if (RadarState.nearby == 1) "1 radar" else "${RadarState.nearby} radares") + " a menos de 2 km"
    }
    Text(
        status, color = if (h.toast.isNotEmpty()) p.accent else p.muted, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = Tema.body,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    )
}

private fun updatedText(t: Long): String {
    val es = Locale("es", "ES")
    val day = SimpleDateFormat("dd-MM-yyyy", es).format(Date(t))
    val today = SimpleDateFormat("dd-MM-yyyy", es).format(Date())
    val hour = SimpleDateFormat("HH:mm", es).format(Date(t))
    return if (day == today) "Hoy, $hour" else "$day, $hour"
}

@Composable
private fun RowScope.Info(label: String, value: String) {
    val p = Tema.p
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = p.dim, fontSize = 14.sp, letterSpacing = 1.sp, fontFamily = Tema.body)
        Text(value, color = p.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = Tema.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RowScope.BigButton(
    modifier: Modifier,
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    sub: String,
    onClick: () -> Unit,
) {
    val p = Tema.p
    CarButton(
        modifier.weight(1f).height(230.dp),
        bg = if (active) p.panel else p.raised,
        border = if (active) p.accent else p.border2,
        borderWidth = if (active) 2.dp else 1.dp,
        radius = 18.dp,
        onClick = onClick,
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CarIcon(icon, 48.dp, if (active) p.accent else p.muted)
            Text(title, color = p.text, fontSize = 23.sp, fontWeight = FontWeight.Bold, fontFamily = Tema.body, textAlign = TextAlign.Center)
            Text(sub, color = p.muted, fontSize = 16.sp, lineHeight = 21.sp, fontFamily = Tema.body, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RadarLimit(h: HomeState) {
    val p = Tema.p
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(
            "Guardado en la posición y el sentido que tenías al tocar el indicador del GPS. ¿A qué velocidad está? " +
                "Si no lo sabes, ponla más tarde desde «Mis radares».",
            color = p.muted, fontSize = 20.sp, lineHeight = 28.sp, fontFamily = Tema.body,
        )
        for (row in LIMITS.chunked(5)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (l in row) {
                    CarButton(Modifier.weight(1f).height(84.dp), bg = p.raised, border = p.border2, radius = 14.dp, onClick = { h.setNewRadarLimit(l) }) {
                        Text("$l", color = p.text, fontSize = 38.sp, fontFamily = Tema.condensed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        CarButton(Modifier.fillMaxWidth().height(60.dp), bg = p.panel, border = p.accent, borderWidth = 2.dp, radius = 14.dp, onClick = { h.setNewRadarLimit(null) }) {
            Text("Sin velocidad · la pongo después", color = p.accent, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = Tema.body)
        }
    }
}

@Composable
private fun RadarList(h: HomeState) {
    val p = Tema.p
    val list = RadarStore.user
    if (list.isEmpty()) {
        Text(
            "Todavía no has añadido ningún radar.", color = p.muted, fontSize = 20.sp, fontFamily = Tema.body,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        )
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(list, key = { it.id }) { r -> RadarRow(h, r) }
    }
}

@Composable
private fun RadarRow(h: HomeState, r: Radar) {
    val p = Tema.p
    Row(
        Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(14.dp)).background(p.raised)
            .border(1.dp, p.border2, RoundedCornerShape(14.dp)).padding(start = 14.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Sign(r.kind, r.limit, 56.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val speed = when {
                r.kind == Kind.RED_LIGHT -> ""
                r.limit != null -> " · ${r.limit} km/h"
                else -> " · sin velocidad"
            }
            Text(r.kind.label + speed, color = p.text, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, fontFamily = Tema.body)
            Text(
                String.format(Locale("es", "ES"), "%.5f, %.5f", r.lat, r.lon) + " · sentido " + Geo.compass(r.bearing) + (if (r.date.isNotEmpty()) " · ${r.date}" else ""),
                color = p.dim, fontSize = 15.sp, fontFamily = Tema.body, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        CarButton(Modifier.size(58.dp), bg = p.panel, border = p.border2, radius = 29.dp, onClick = { h.startEdit(r.id) }) {
            CarIcon(Ic.Pencil, 24.dp, p.text, "Editar")
        }
        CarButton(Modifier.size(58.dp), bg = p.panel, border = SignRed, radius = 29.dp, onClick = { h.deleteRadar(r.id) }) {
            CarIcon(Ic.Trash, 24.dp, AlertRed, "Borrar")
        }
    }
}

@Composable
private fun ColumnScope.RadarEdit(h: HomeState) {
    val p = Tema.p
    Text("TIPO DE RADAR", color = p.dim, fontSize = 14.sp, letterSpacing = 1.sp, fontFamily = Tema.body)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (k in Kind.USER) {
            val on = h.editKind == k
            CarButton(
                Modifier.weight(1f).height(52.dp), bg = if (on) p.panel else p.raised, border = if (on) p.accent else p.border2,
                borderWidth = if (on) 2.dp else 1.dp, radius = 26.dp, onClick = { h.editKind = k },
            ) {
                Text(k.label, color = if (on) p.accent else p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = Tema.body)
            }
        }
    }
    val sem = h.editKind == Kind.RED_LIGHT
    Text("VELOCIDAD", color = p.dim, fontSize = 14.sp, letterSpacing = 1.sp, fontFamily = Tema.body, modifier = Modifier.padding(top = 6.dp))
    val options: List<Int?> = listOf<Int?>(null) + LIMITS
    for (row in options.chunked(6)) {
        Row(Modifier.fillMaxWidth().alpha(if (sem) 0.35f else 1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (l in row) {
                val on = !sem && h.editLimit == l
                CarButton(
                    Modifier.weight(1f).height(54.dp), bg = if (on) p.panel else p.raised, border = if (on) p.accent else p.border2,
                    borderWidth = if (on) 2.dp else 1.dp, radius = 12.dp, onClick = { if (!sem) h.editLimit = l },
                ) {
                    Text(l?.toString() ?: "—", color = if (on) p.accent else p.text, fontSize = 28.sp, fontFamily = Tema.condensed, fontWeight = FontWeight.Bold)
                }
            }
            // Huecos para que la última fila mantenga el tamaño de los botones.
            repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
    Spacer(Modifier.weight(1f))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CarButton(Modifier.weight(1f).height(58.dp), bg = p.raised, border = p.border2, radius = 14.dp, onClick = { h.radarView = RadarView.LIST }) {
            Text("Cancelar", color = p.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = Tema.body)
        }
        CarButton(Modifier.weight(1.3f).height(58.dp), bg = p.panel, border = p.accent, borderWidth = 2.dp, radius = 14.dp, onClick = { h.saveEdit() }) {
            Text("Guardar", color = p.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = Tema.body)
        }
    }
}
