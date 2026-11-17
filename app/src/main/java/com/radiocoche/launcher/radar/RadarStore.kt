package com.radiocoche.launcher.radar

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.floor

/**
 * Lista de radares: la oficial descargada y la del usuario (mis_radares.csv).
 *
 * Fuentes, por orden:
 * 1. «Radares Anunciados» (feed GeoJSON que se regenera cada 6 horas): reúne los radares de la DGT,
 *    Cataluña, País Vasco, Navarra y varios ayuntamientos, con velocidad y sentido cuando se conocen.
 * 2. Si esa falla y no hay copia reciente, el archivo oficial de la DGT (DATEX II): solo posiciones.
 * Si una descarga falla, se queda la última copia buena.
 *
 * Los archivos están en Android/data/com.radiocoche.launcher/files/radares/.
 * mis_radares.csv no lo toca ninguna actualización.
 */
object RadarStore {
    private const val FEED_URL = "https://geiserx.github.io/radares-anunciados-ha/feed.geojson"
    private val DGT_URLS = listOf(
        "https://nap.dgt.es/datex2/dgt/PredefinedLocationsPublication/radares/content.xml",
        "https://infocar.dgt.es/datex2/dgt/PredefinedLocationsPublication/radares/content.xml",
    )
    private const val FEED_FILE = "radares_anunciados.geojson"
    private const val DGT_FILE = "radares_dgt.xml"
    const val USER_FILE = "mis_radares.csv"

    private const val DAY = 24 * 3600 * 1000L

    /** Radares oficiales y los del usuario. */
    var official by mutableStateOf<List<Radar>>(emptyList())
        private set
    var user by mutableStateOf<List<Radar>>(emptyList())
        private set

    /** Hora (ms) de la última actualización correcta; 0 si nunca. */
    var lastUpdate by mutableStateOf(0L)
        private set
    var updating by mutableStateOf(false)
        private set

    /** Último resultado de una actualización pedida a mano ("" si nada). */
    var message by mutableStateOf("")

    /** De dónde salió la lista en uso. */
    var sourceName by mutableStateOf("")
        private set

    @Volatile private var index: Map<Long, List<Radar>> = emptyMap()
    private val main = Handler(Looper.getMainLooper())
    private var loaded = false

    fun dir(ctx: Context): File =
        (ctx.getExternalFilesDir(null) ?: ctx.filesDir).resolve("radares").also { it.mkdirs() }

    /** Carga lo guardado (una vez). */
    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        val app = ctx.applicationContext
        lastUpdate = prefs(app).getLong("lastUpdate", 0L)
        thread(name = "radares-carga") {
            val (list, name) = readOfficial(app)
            val mine = readUser(app)
            main.post {
                official = list
                sourceName = name
                user = mine
                rebuild()
            }
        }
    }

    // ------------------------------------------------------------------ búsqueda

    private fun cell(lat: Double, lon: Double): Long {
        val a = floor(lat / 0.02).toLong()
        val b = floor(lon / 0.02).toLong()
        return (a shl 32) xor (b and 0xffffffffL)
    }

    private fun rebuild() {
        val m = HashMap<Long, MutableList<Radar>>()
        for (r in official + user) m.getOrPut(cell(r.lat, r.lon)) { mutableListOf() }.add(r)
        index = m
    }

    /** Radares en las celdas de alrededor (unos 2 km). */
    fun near(lat: Double, lon: Double): List<Radar> {
        val out = ArrayList<Radar>()
        val idx = index
        for (da in -1..1) for (db in -1..1) {
            idx[cell(lat + da * 0.02, lon + db * 0.02)]?.let { out.addAll(it) }
        }
        return out
    }

    /** El otro extremo de un tramo. */
    fun otherEnd(r: Radar): Radar? {
        val id = r.sectionId ?: return null
        return official.firstOrNull { it.sectionId == id && it.sectionEnd != r.sectionEnd }
    }

    // ------------------------------------------------------------------ actualización

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("radares", Context.MODE_PRIVATE)

    private fun onWifi(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun online(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Actualización automática: con wifi y como mucho una vez al día. */
    fun autoUpdate(ctx: Context) {
        if (updating) return
        if (System.currentTimeMillis() - lastUpdate < DAY) return
        if (!onWifi(ctx)) return
        update(ctx, manual = false)
    }

    /** Descarga la lista. [manual]: pedida desde la ventana (vale cualquier conexión). */
    fun update(ctx: Context, manual: Boolean) {
        if (updating) return
        val app = ctx.applicationContext
        if (manual && !online(app)) {
            message = "Sin conexión"
            return
        }
        updating = true
        message = ""
        thread(name = "radares-descarga") {
            var result: Pair<List<Radar>, String>? = null
            try {
                val bytes = download(FEED_URL)
                val list = parseFeed(String(bytes, Charsets.UTF_8))
                if (list.size > 300) {
                    File(dir(app), FEED_FILE).writeBytes(bytes)
                    result = list to "Radares Anunciados"
                }
            } catch (_: Throwable) {
            }
            if (result == null) {
                // Plan B: el archivo de la DGT.
                for (u in DGT_URLS) {
                    try {
                        val bytes = download(u)
                        val list = parseDgt(bytes)
                        if (list.size > 100) {
                            File(dir(app), DGT_FILE).writeBytes(bytes)
                            // Si el feed guardado es viejo, manda la DGT.
                            val feed = File(dir(app), FEED_FILE)
                            if (feed.exists() && System.currentTimeMillis() - feed.lastModified() > 7 * DAY) feed.delete()
                            result = readOfficial(app)
                            break
                        }
                    } catch (_: Throwable) {
                    }
                }
            }
            val now = System.currentTimeMillis()
            val done = result
            main.post {
                updating = false
                if (done != null) {
                    official = done.first
                    sourceName = done.second
                    lastUpdate = now
                    prefs(app).edit().putLong("lastUpdate", now).apply()
                    rebuild()
                    message = "Lista de radares actualizada"
                } else {
                    message = "No se pudo descargar; sigue la lista anterior"
                }
            }
        }
    }

    private fun download(url: String): ByteArray {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 20000
        c.readTimeout = 60000
        c.setRequestProperty("User-Agent", "RadioCoche-Launcher")
        try {
            if (c.responseCode != 200) throw IllegalStateException("HTTP ${c.responseCode}")
            return c.inputStream.use { it.readBytes() }
        } finally {
            c.disconnect()
        }
    }

    /** Lee la lista oficial guardada: primero el feed; si no está, la DGT. */
    private fun readOfficial(ctx: Context): Pair<List<Radar>, String> {
        val feed = File(dir(ctx), FEED_FILE)
        if (feed.exists()) {
            runCatching { parseFeed(feed.readText()) }.getOrNull()?.takeIf { it.isNotEmpty() }?.let {
                return it to "Radares Anunciados"
            }
        }
        val dgt = File(dir(ctx), DGT_FILE)
        if (dgt.exists()) {
            runCatching { parseDgt(dgt.readBytes()) }.getOrNull()?.takeIf { it.isNotEmpty() }?.let {
                return it to "DGT"
            }
        }
        return emptyList<Radar>() to ""
    }

    // ------------------------------------------------------------------ feed GeoJSON

    private fun parseFeed(text: String): List<Radar> {
        val features = JSONObject(text).getJSONArray("features")
        val out = ArrayList<Radar>(features.length())
        for (i in 0 until features.length()) {
            val f = features.optJSONObject(i) ?: continue
            val p = f.optJSONObject("properties") ?: continue
            if (!p.optBoolean("active", true)) continue
            val g = f.optJSONObject("geometry") ?: continue
            val id = f.optString("id", "f$i")
            val limit = if (p.isNull("maxspeed")) null else p.optInt("maxspeed", 0).takeIf { it in 10..150 }
            val bearing = if (p.has("bearing") && !p.isNull("bearing")) p.optDouble("bearing").toFloat() else null
            when (g.optString("type")) {
                "Point" -> {
                    val c = g.getJSONArray("coordinates")
                    val lon = c.getDouble(0)
                    val lat = c.getDouble(1)
                    when (p.optString("kind")) {
                        "fixed" -> out += Radar(id, lat, lon, Kind.FIXED, limit, bearing)
                        "section" -> {
                            // Extremos de un tramo: "<id del tramo>-from" y "-to".
                            val end = if (id.endsWith("-to")) 1 else 0
                            val sid = id.removeSuffix("-from").removeSuffix("-to")
                            out += Radar(id, lat, lon, Kind.SECTION, limit, null, sectionId = sid, sectionEnd = end)
                        }
                        "trailer", "mobile_announced" -> out += Radar(id, lat, lon, Kind.MOBILE, limit, bearing)
                        "mobile_recurring" -> out += Radar(id, lat, lon, Kind.MOBILE_RECURRING, limit, bearing)
                        else -> Unit // "reported": sin confirmar, no se usa
                    }
                }
                "LineString" -> {
                    if (p.optString("kind") != "stretch") continue
                    // Solo los tramos vigilados con radar móvil: los de tramo ya vienen como puntos.
                    if (p.optString("direction") != "both" && !id.contains("invive", ignoreCase = true)) continue
                    val c: JSONArray = g.getJSONArray("coordinates")
                    if (c.length() < 2) continue
                    val a = c.getJSONArray(0)
                    val b = c.getJSONArray(c.length() - 1)
                    out += Radar("$id-a", a.getDouble(1), a.getDouble(0), Kind.MOBILE_STRETCH, limit, null)
                    out += Radar("$id-b", b.getDouble(1), b.getDouble(0), Kind.MOBILE_STRETCH, limit, null)
                }
            }
        }
        // Un tramo sin sus dos extremos no sirve como tramo: se queda como radar fijo.
        val pairs = out.filter { it.kind == Kind.SECTION }.groupBy { it.sectionId }
        return out.map { r ->
            if (r.kind == Kind.SECTION && (pairs[r.sectionId]?.size ?: 0) != 2) r.copy(kind = Kind.FIXED, sectionId = null) else r
        }
    }

    // ------------------------------------------------------------------ DGT DATEX II

    private fun parseDgt(bytes: ByteArray): List<Radar> {
        val xp = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
        xp.setInput(bytes.inputStream(), "UTF-8")
        val out = ArrayList<Radar>()
        var outerId = ""
        var type = ""          // "Point" o "Linear"
        var where = ""         // "point", "from", "to"
        var lat: Double? = null
        var lon: Double? = null
        var text = StringBuilder()
        var n = 0
        while (xp.eventType != XmlPullParser.END_DOCUMENT) {
            when (xp.eventType) {
                XmlPullParser.START_TAG -> {
                    text = StringBuilder()
                    when (xp.name) {
                        "predefinedLocation" -> {
                            val t = xp.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance", "type")
                            if (t == null) {
                                outerId = (xp.getAttributeValue(null, "id") ?: "dgt${n++}").removePrefix("GUID_")
                                type = ""
                            } else {
                                type = t.substringAfter(":")
                            }
                        }
                        "point" -> where = "point"
                        "from" -> where = "from"
                        "to" -> where = "to"
                    }
                }
                XmlPullParser.TEXT -> text.append(xp.text)
                XmlPullParser.END_TAG -> {
                    when (xp.name) {
                        "latitude" -> lat = text.toString().trim().toDoubleOrNull()
                        "longitude" -> lon = text.toString().trim().toDoubleOrNull()
                        "pointCoordinates" -> {
                            val la = lat
                            val lo = lon
                            if (la != null && lo != null) {
                                if (type == "Point" && where == "point") {
                                    out += Radar("dgt-$outerId", la, lo, Kind.FIXED, null, null)
                                } else if (type == "Linear" && (where == "from" || where == "to")) {
                                    val end = if (where == "to") 1 else 0
                                    out += Radar("dgt-$outerId-$where", la, lo, Kind.SECTION, null, null, sectionId = "dgt-$outerId", sectionEnd = end)
                                }
                            }
                            lat = null
                            lon = null
                        }
                        "point", "from", "to" -> where = ""
                    }
                }
            }
            xp.next()
        }
        val pairs = out.filter { it.kind == Kind.SECTION }.groupBy { it.sectionId }
        return out.map { r ->
            if (r.kind == Kind.SECTION && (pairs[r.sectionId]?.size ?: 0) != 2) r.copy(kind = Kind.FIXED, sectionId = null) else r
        }
    }

    // ------------------------------------------------------------------ mis radares

    private val dayFormat = SimpleDateFormat("dd-MM-yyyy", Locale("es", "ES"))

    private fun readUser(ctx: Context): List<Radar> {
        val f = File(dir(ctx), USER_FILE)
        if (!f.exists()) return emptyList()
        val out = ArrayList<Radar>()
        f.readLines().forEachIndexed { i, line ->
            val l = line.trim()
            if (l.isEmpty() || l.startsWith("#") || l.lowercase().startsWith("lat")) return@forEachIndexed
            val c = l.split(';')
            if (c.size < 2) return@forEachIndexed
            val lat = c[0].trim().replace(',', '.').toDoubleOrNull() ?: return@forEachIndexed
            val lon = c[1].trim().replace(',', '.').toDoubleOrNull() ?: return@forEachIndexed
            val kind = Kind.fromCode(c.getOrNull(2)).let { if (it in Kind.USER) it else Kind.EXTRA }
            val limit = c.getOrNull(3)?.trim()?.toIntOrNull()
            val bearing = c.getOrNull(4)?.trim()?.replace(',', '.')?.toFloatOrNull()
            val date = c.getOrNull(5)?.trim() ?: ""
            out += Radar("u-$i-${lat}-${lon}", lat, lon, kind, limit, bearing, user = true, date = date)
        }
        return out
    }

    private fun writeUser(ctx: Context, list: List<Radar>) {
        val sb = StringBuilder("latitud;longitud;tipo;velocidad;rumbo;fecha\n")
        sb.append("# tipo: fijo, semaforo, movil o extra · velocidad en km/h (vacía si no se sabe) · rumbo en grados (vacío = los dos sentidos)\n")
        for (r in list) {
            sb.append(String.format(Locale.US, "%.6f;%.6f;%s;%s;%s;%s\n",
                r.lat, r.lon, r.kind.code, r.limit?.toString() ?: "", r.bearing?.let { String.format(Locale.US, "%.0f", it) } ?: "", r.date))
        }
        runCatching { File(dir(ctx), USER_FILE).writeText(sb.toString()) }
    }

    private fun setUser(ctx: Context, list: List<Radar>) {
        user = list
        rebuild()
        val app = ctx.applicationContext
        thread(name = "radares-guardar") { writeUser(app, list) }
    }

    /** Añade un radar del usuario. Devuelve su id. */
    fun addUser(ctx: Context, lat: Double, lon: Double, bearing: Float?): String {
        val id = "u-new-${System.currentTimeMillis()}"
        setUser(ctx, user + Radar(id, lat, lon, Kind.EXTRA, null, bearing, user = true, date = dayFormat.format(Date())))
        return id
    }

    fun editUser(ctx: Context, id: String, kind: Kind, limit: Int?) {
        setUser(ctx, user.map { if (it.id == id) it.copy(kind = kind, limit = if (kind == Kind.RED_LIGHT) null else limit) else it })
    }

    fun deleteUser(ctx: Context, id: String) {
        setUser(ctx, user.filterNot { it.id == id })
    }
}
