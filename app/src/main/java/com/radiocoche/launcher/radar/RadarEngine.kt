package com.radiocoche.launcher.radar

import android.location.Location
import android.os.SystemClock

/**
 * Decide cuándo avisar con cada posición del GPS.
 *
 * - Solo avisa de radares que están delante (±30° del rumbo) y, si el radar tiene sentido,
 *   cuando vas en ese sentido (±60°). Así no salta con la vía paralela ni con el carril contrario.
 * - Avisa unos 25 segundos antes (entre 300 y 900 m), una sola vez por radar.
 * - Radar de tramo: al pasar la entrada empieza el tramo (media y kilómetros que quedan) y al pasar
 *   la salida suena un pitido.
 */
class RadarEngine(private val runner: AlertRunner) {

    private class Target(val radar: Radar, var min: Double)
    private class Section(
        val exit: Radar,
        val kind: Kind,
        val limit: Int?,
        val start: Long,
        val startStraight: Double,
        var traveled: Double,
        var min: Double,
    )

    private var target: Target? = null
    private var section: Section? = null
    private var sectionEndUntil = 0L
    private var last: Location? = null
    private var headingRef: Location? = null
    private val alerted = HashMap<String, Long>()
    /** Radares descartados por no estar en nuestra vía: solo vuelven a avisar si quedan claramente delante. */
    private val dismissed = HashMap<String, Long>()
    /** Candidato pendiente de confirmar (hace falta verlo dos veces seguidas acercándonos). */
    private var pendingId: String? = null
    private var pendingD = 0.0

    /** Distancia, avance (a lo largo de nuestro rumbo) y desvío lateral hasta el radar, en metros. */
    private class Geom(val d: Double, val along: Double, val cross: Double)

    private fun geom(loc: Location, heading: Float, r: Radar): Geom {
        val d = Geo.distance(loc.latitude, loc.longitude, r.lat, r.lon)
        val a = Math.toRadians(Geo.angle(heading, Geo.bearing(loc.latitude, loc.longitude, r.lat, r.lon)).toDouble())
        return Geom(d, d * kotlin.math.cos(a), d * kotlin.math.sin(a))
    }

    /** Desvío lateral máximo para considerar que el radar está en nuestra vía (más margen lejos, por las curvas). */
    private fun crossLimit(along: Double) = (35 + 0.1 * along).coerceIn(45.0, 110.0)

    fun onLocation(loc: Location) {
        val now = SystemClock.elapsedRealtime()
        val prev = last
        last = loc
        // Velocidad: la del GPS o, si no la da, la calculada entre dos posiciones.
        val kmh = when {
            loc.hasSpeed() -> loc.speed * 3.6f
            prev != null && loc.time > prev.time -> prev.distanceTo(loc) / ((loc.time - prev.time) / 1000f) * 3.6f
            else -> 0f
        }
        // Rumbo: el del GPS o, si no lo da, el del último tramo recorrido (al menos 10 m).
        val ref = headingRef
        if (ref == null || ref.distanceTo(loc) > 60f) headingRef = loc
        val heading: Float? = when {
            loc.hasBearing() && kmh > 8f -> loc.bearing
            ref != null && ref.distanceTo(loc) >= 10f -> Geo.bearing(ref.latitude, ref.longitude, loc.latitude, loc.longitude)
            else -> null
        }

        // Limpieza de radares ya avisados (5 minutos).
        alerted.entries.removeAll { now - it.value > 5 * 60 * 1000 }
        dismissed.entries.removeAll { now - it.value > 5 * 60 * 1000 }

        // --- Tramo en curso
        val s = section
        if (s != null) {
            if (prev != null) s.traveled += prev.distanceTo(loc).toDouble()
            val d = Geo.distance(loc.latitude, loc.longitude, s.exit.lat, s.exit.lon)
            val secs = (now - s.start) / 1000.0
            val avg = if (secs > 3) (s.traveled / secs * 3.6).toInt() else kmh.toInt()
            if (d < s.min) s.min = d
            val passed = s.min < 80 && d > s.min + 20
            val lost = d > s.startStraight + 3000 || now - s.start > 60 * 60 * 1000
            when {
                passed -> {
                    section = null
                    alerted[s.exit.id] = now
                    RadarState.alert = Alert(s.kind, s.limit, Phase.SECTION_END, average = avg)
                    sectionEndUntil = now + 2500
                    runner.sectionEnd()
                }
                lost -> {
                    section = null
                    if (target == null) RadarState.alert = null
                }
                target == null -> RadarState.alert = Alert(s.kind, s.limit, Phase.IN_SECTION, average = avg, remainingKm = (d / 1000).toFloat())
            }
        }
        if (sectionEndUntil != 0L && now > sectionEndUntil) {
            sectionEndUntil = 0L
            if (RadarState.alert?.phase == Phase.SECTION_END) RadarState.alert = null
        }

        // --- Radar al que nos acercamos
        val t = target
        if (t != null) {
            val r = t.radar
            val d = Geo.distance(loc.latitude, loc.longitude, r.lat, r.lon)
            if (d < t.min) t.min = d
            val passed = t.min < 70 && d > t.min + 20
            // Nos alejamos (más de 150 m desde lo más cerca que estuvimos): el radar ya no nos afecta.
            val away = d > t.min + 150
            val g = if (heading != null) geom(loc, heading, r) else null
            // Lo hemos dejado a un lado sin pasar por él (calle perpendicular, vía de servicio…).
            val besides = g != null && g.along < 0 && d > 60
            // A menos de 300 m sigue a más de 50 m de nuestro rumbo: no está en nuestra vía.
            val offRoad = g != null && g.along in 0.0..300.0 && g.cross > 50
            val gone = d > 1300 || away || besides || offRoad
            if (offRoad || besides) {
                // Puede volver a avisar, pero solo si queda claramente delante en nuestra vía.
                alerted.remove(r.id)
                dismissed[r.id] = now
            }
            if (passed || gone) {
                target = null
                RadarState.alert = null
                if (passed && r.kind == Kind.SECTION && section == null) {
                    val exit = RadarStore.otherEnd(r)
                    if (exit != null) {
                        alerted[exit.id] = now
                        val straight = Geo.distance(loc.latitude, loc.longitude, exit.lat, exit.lon)
                        section = Section(exit, r.kind, r.limit ?: exit.limit, now, straight, 0.0, straight)
                        RadarState.alert = Alert(r.kind, r.limit ?: exit.limit, Phase.IN_SECTION, average = kmh.toInt(), remainingKm = (straight / 1000).toFloat())
                    }
                }
            } else if (!runner.flashing) {
                RadarState.alert = Alert(r.kind, r.limit, Phase.APPROACH, distance = d.toInt(), speed = kmh.toInt())
            }
        }

        // --- Buscar un radar nuevo
        val around = RadarStore.near(loc.latitude, loc.longitude)
        RadarState.nearby = around.count { Geo.distance(loc.latitude, loc.longitude, it.lat, it.lon) < 2000 }
        RadarState.heading = heading != null
        if (target != null || heading == null || kmh < 10f) return
        val warn = (kmh / 3.6 * 25).coerceIn(300.0, 900.0)
        var best: Radar? = null
        var bestD = Double.MAX_VALUE
        for (r in around) {
            if (alerted.containsKey(r.id)) continue
            if (r.kind == Kind.SECTION && section != null) continue
            val g = geom(loc, heading, r)
            if (g.d > warn || g.d < 30) continue
            // Delante y en nuestra vía: poco desvío lateral respecto a nuestro rumbo.
            // (Antes bastaba un ángulo de ±45°, que a 800 m deja hasta 560 m a un lado: de ahí las falsas alarmas.)
            if (g.along < 30) continue
            if (dismissed.containsKey(r.id)) {
                if (g.d > 300 || g.cross > 30) continue
            } else if (g.cross > crossLimit(g.along)) continue
            // Sentido del radar: solo se usa cuando la fuente lo da con seguridad.
            val rb = r.bearing
            if (rb != null && Geo.angle(heading, rb) > 70f) continue
            if (g.d < bestD) {
                best = r
                bestD = g.d
            }
        }
        val r = best
        if (r == null) {
            pendingId = null
            return
        }
        // Confirmación: el mismo radar dos veces seguidas y cada vez más cerca.
        if (pendingId != r.id || bestD >= pendingD) {
            pendingId = r.id
            pendingD = bestD
            return
        }
        pendingId = null
        dismissed.remove(r.id)
        alerted[r.id] = now
        // Los dos extremos de un mismo tramo vigilado no avisan dos veces seguidas.
        if (r.kind == Kind.MOBILE_STRETCH) {
            RadarStore.near(r.lat, r.lon).filter { it.kind == Kind.MOBILE_STRETCH && it.id.dropLast(2) == r.id.dropLast(2) }
                .forEach { alerted[it.id] = now }
        }
        target = Target(r, bestD)
        val alert = Alert(r.kind, r.limit, Phase.APPROACH, distance = bestD.toInt(), speed = kmh.toInt())
        RadarState.alert = alert
        runner.approach(alert)
    }
}
