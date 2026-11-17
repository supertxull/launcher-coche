package com.radiocoche.escaner

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Escáner del coche: repasa todo lo que la unidad puede recibir del coche y apunta lo que cambia.
 *
 * - Mensajes del microcontrolador (TWUtil), en bloques de 16 códigos.
 * - Avisos (broadcasts) que declaran las apps de fábrica (com.tw.*, com.dofun.*) y algunos conocidos.
 * - Propiedades del sistema y ajustes de Android que cambian (sin datos personales).
 *
 * No envía NADA a la unidad: solo escucha. Cada código solo se apunta cuando su valor cambia,
 * así los mensajes que se repiten igual no llenan la pantalla.
 */
class MainActivity : Activity() {

    private val fileName = "escaner-coche.txt"
    private val orange = Color.parseColor("#FF6A1A")
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val main = Handler(Looper.getMainLooper())

    private val lines = ArrayList<String>()
    private val lastByCode = HashMap<String, String>()
    private val counts = HashMap<String, Int>()
    private var marks = 0

    private lateinit var status: TextView
    private lateinit var summary: TextView
    private lateinit var view: TextView
    private val instances = mutableListOf<Pair<Any, Class<*>>>()
    private var receiver: BroadcastReceiver? = null
    private var worker: HandlerThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(Color.BLACK)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(10))
        }
        root.addView(TextView(this).apply {
            text = "Escáner del coche"
            setTextColor(Color.WHITE); textSize = 26f; typeface = Typeface.DEFAULT_BOLD
        })
        status = TextView(this).apply {
            setTextColor(Color.parseColor("#DCE2EA")); textSize = 17f
            setPadding(0, dp(4), 0, dp(4))
        }
        root.addView(status)
        summary = TextView(this).apply {
            setTextColor(orange); textSize = 16f; typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(8))
        }
        root.addView(summary)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(button("MARCA") { mark() })
        row.addView(button("Guardar") { save() })
        row.addView(button("Borrar") { lines.clear(); render() })
        root.addView(row)
        view = TextView(this).apply {
            setTextColor(Color.parseColor("#DCE2EA")); textSize = 14f; typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        root.addView(
            ScrollView(this).apply { setPadding(0, dp(10), 0, 0); addView(view) },
            LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f),
        )
        setContentView(root)

        status.text = "Preparando… Después: abre y cierra puertas, enciende luces, pon el intermitente, " +
            "mete la marcha atrás… Pulsa MARCA antes de cada prueba y di qué hiciste."

        val prev = prefs().getInt("trying", -1)
        if (prev >= 0) {
            val bad = prefs().getStringSet("bad", emptySet())!!.toMutableSet().apply { add(prev.toString()) }
            prefs().edit().putStringSet("bad", bad).remove("trying").commit()
            add("La última vez la app se cerró al abrir ${hex(prev)}: ese bloque se salta")
        }

        val ht = HandlerThread("escaner").apply { start() }
        worker = ht
        Handler(ht.looper).post {
            runCatching { listenBroadcasts() }.onFailure { ui { add("Avisos: ${it.message}") } }
            runCatching { startMcu() }.onFailure { ui { add("MCU: ${it.message}") } }
            pollSystem(Handler(ht.looper), first = true)
        }
    }

    // ------------------------------------------------------------------ MCU

    private fun prefs() = getSharedPreferences("escaner", Context.MODE_PRIVATE)

    /** Bloques de 16 códigos: todo 0x0000–0x0FFF y los grupos altos conocidos. */
    private fun chunks(): List<IntRange> {
        val groups = (0x00..0x0F).map { it shl 8 } + listOf(0x9E00, 0xA000, 0xA100, 0xA200, 0xFF00)
        return groups.flatMap { g -> (0 until 16).map { i -> (g + i * 16)..(g + i * 16 + 15) } }
    }

    private var mcuBlocks = 0

    private fun startMcu() {
        val cls = try {
            Class.forName("android.tw.john.TWUtil")
        } catch (e: Throwable) {
            ui { add("Esta unidad no tiene TWUtil: no se puede escuchar el MCU") }
            return
        }
        val bad = prefs().getStringSet("bad", emptySet())!!
        for (r in chunks()) {
            if (r.first.toString() in bad) continue
            val n = mcuBlocks
            ui { status.text = "Abriendo bloque ${hex(r.first)}… ($n abiertos)" }
            prefs().edit().putInt("trying", r.first).commit()
            try {
                val tw = cls.getConstructor(Int::class.javaPrimitiveType).newInstance(1)
                val ids = ShortArray(r.count()) { (r.first + it).toShort() }
                val res = cls.getMethod("open", ShortArray::class.java).invoke(tw, ids) as? Int ?: 0
                if (res != 0) continue
                cls.getMethod("start").invoke(tw)
                val h = Handler(Looper.getMainLooper()) { m ->
                    runCatching { onMcu(m.what, m.arg1, m.arg2, m.obj) }
                    true
                }
                cls.getMethod("addHandler", String::class.java, Handler::class.java)
                    .invoke(tw, "Escaner${r.first}", h)
                synchronized(instances) { instances += tw to cls }
                mcuBlocks++
            } catch (e: Throwable) {
                ui { add("Error en ${hex(r.first)}: ${e.javaClass.simpleName}") }
            } finally {
                prefs().edit().remove("trying").commit()
            }
        }
        ui {
            status.text = "Escuchando ($mcuBlocks bloques del MCU, $actionCount avisos, propiedades y ajustes). " +
                "Pulsa MARCA, haz una prueba (abrir una puerta…) y apunta qué hiciste. Al acabar, Guardar."
            add("Listo: $mcuBlocks bloques del MCU abiertos")
        }
    }

    private fun onMcu(what: Int, a1: Int, a2: Int, obj: Any?) {
        val o = when (obj) {
            null -> ""
            is ByteArray -> " bytes=" + obj.joinToString(" ") { "%02X".format(it) }
            is IntArray -> " ints=" + obj.joinToString(",")
            is ShortArray -> " shorts=" + obj.joinToString(",")
            is String -> " texto=\"" + obj.take(60) + "\""
            else -> " obj=" + obj.toString().take(80)
        }
        record("MCU ${hex(what)}", "$a1  $a2$o")
    }

    // ------------------------------------------------------------------ avisos (broadcasts)

    private var actionCount = 0

    private fun listenBroadcasts() {
        val actions = HashSet<String>()
        actions += listOf(
            "com.unisound.intent.action.ACC_ON", "com.unisound.intent.action.ACC_OFF",
            "com.unisound.intent.action.DO_SLEEP", "com.unisound.intent.action.DO_WAKEUP",
            "com.tw.theme.day.night.change", "android.intent.action.CONFIGURATION_CHANGED",
        )
        // Acciones que declaran las apps de fábrica en su manifiesto.
        @Suppress("DEPRECATION")
        val apps = packageManager.getInstalledApplications(0).filter {
            val p = it.packageName
            p.startsWith("com.tw.") || p.startsWith("com.dofun.") || p.startsWith("android.tw") ||
                (it.flags and ApplicationInfo.FLAG_SYSTEM == 0 && p.startsWith("com.tw"))
        }
        val re = Regex("""^\s*action name=(\S+)""", RegexOption.MULTILINE)
        for (ai in apps) {
            val text = runCatching { Axml.dump(ai.sourceDir) }.getOrNull() ?: continue
            for (m in re.findAll(text)) {
                val a = m.groupValues[1]
                if (a.startsWith("android.intent.action.MAIN") || a.isBlank() || a.startsWith("@")) continue
                if (a.startsWith("android.") && !a.contains("ACC", ignoreCase = true)) continue
                actions += a
            }
        }
        actionCount = actions.size
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val extras = i.extras?.keySet()?.sorted()?.joinToString(" ") { k ->
                    val v = runCatching { i.extras?.get(k) }.getOrNull()
                    "$k=" + when (v) {
                        is IntArray -> v.joinToString(",")
                        is ByteArray -> v.joinToString(" ") { "%02X".format(it) }
                        else -> v.toString().take(60)
                    }
                } ?: ""
                ui { record("AVISO ${i.action}", extras) }
            }
        }
        receiver = r
        val f = IntentFilter().apply { actions.forEach { addAction(it) } }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(r, f, Context.RECEIVER_EXPORTED) else registerReceiver(r, f)
        ui { add("Escuchando $actionCount tipos de aviso de las apps de fábrica") }
    }

    // ------------------------------------------------------------------ propiedades y ajustes

    private var props = HashMap<String, String>()
    private var sets = HashMap<String, String>()

    private val sensitive = Regex(
        "serial|imei|meid|iccid|imsi|msisdn|mac|addr|deviceid|device_id|android_id|ssid|passw|psk|" +
            "phone|line1|uuid|name|token|account|(^|[._])sn($|[._])",
        RegexOption.IGNORE_CASE,
    )
    // Cosas que cambian solas sin interés (hora, contadores, batería…).
    private val noisy = Regex("time|uptime|boot|battery|clock|count|stat|sys.usb|vendor.radio|ril\\.|gsm|wifi|net\\.|dhcp", RegexOption.IGNORE_CASE)

    private fun pollSystem(h: Handler, first: Boolean) {
        val newProps = HashMap<String, String>()
        runCatching {
            val p = Runtime.getRuntime().exec(arrayOf("getprop"))
            val re = Regex("""^\[(.*)]: \[(.*)]$""")
            p.inputStream.bufferedReader().forEachLine { l ->
                re.find(l)?.destructured?.let { (k, v) -> if (!sensitive.containsMatchIn(k) && !noisy.containsMatchIn(k)) newProps[k] = v }
            }
            p.waitFor()
        }
        val newSets = HashMap<String, String>()
        for ((t, uri) in listOf("system" to Settings.System.CONTENT_URI, "global" to Settings.Global.CONTENT_URI)) {
            runCatching {
                contentResolver.query(uri, arrayOf("name", "value"), null, null, null)?.use { c ->
                    while (c.moveToNext()) {
                        val k = c.getString(0) ?: continue
                        if (sensitive.containsMatchIn(k) || noisy.containsMatchIn(k)) continue
                        newSets["$t/$k"] = c.getString(1) ?: ""
                    }
                }
            }
        }
        if (!first) {
            for ((k, v) in newProps) if (props[k] != v) ui { record("PROP $k", "${props[k] ?: "(nueva)"} → $v") }
            for ((k, v) in newSets) if (sets[k] != v) ui { record("AJUSTE $k", "${sets[k] ?: "(nuevo)"} → $v") }
        }
        props = newProps
        sets = newSets
        if (!isFinishing) h.postDelayed({ pollSystem(h, first = false) }, 1500)
    }

    // ------------------------------------------------------------------ registro

    /** Apunta una línea solo si el valor de ese código cambió. */
    private fun record(code: String, value: String) {
        counts[code] = (counts[code] ?: 0) + 1
        val prev = lastByCode[code]
        if (prev == value) { updateSummary(); return }
        lastByCode[code] = value
        add((if (prev == null) "NUEVO  " else "cambio ") + code + "  " + value)
        updateSummary()
    }

    private fun updateSummary() {
        val mcu = lastByCode.keys.count { it.startsWith("MCU") }
        val av = lastByCode.keys.count { it.startsWith("AVISO") }
        summary.text = "Códigos del MCU vistos: $mcu · avisos vistos: $av · marcas: $marks"
    }

    private fun mark() {
        marks++
        add("════════ MARCA $marks ════════")
        updateSummary()
        Toast.makeText(this, "Marca $marks: haz ahora la prueba", Toast.LENGTH_SHORT).show()
    }

    private fun ui(f: () -> Unit) = main.post { runCatching { f() } }

    private fun add(s: String) {
        lines += fmt.format(Date()) + "  " + s
        if (lines.size > 5000) lines.removeAt(0)
        render()
    }

    private var pending = false
    private fun render() {
        if (pending) return
        pending = true
        main.postDelayed({
            pending = false
            view.text = lines.takeLast(400).asReversed().joinToString("\n")
        }, 250)
    }

    private fun save() {
        val all = StringBuilder()
        all.append("Escáner del coche · ${Build.MODEL} · Android ${Build.VERSION.RELEASE}\n")
        all.append("Bloques del MCU: $mcuBlocks · tipos de aviso: $actionCount\n\n")
        all.append("== Registro (solo cambios) ==\n")
        lines.forEach { all.append(it).append('\n') }
        all.append("\n== Resumen: último valor y veces recibido ==\n")
        lastByCode.keys.sorted().forEach { k -> all.append("$k  ×${counts[k]}  ${lastByCode[k]}\n") }
        val text = all.toString()
        if (Build.VERSION.SDK_INT < 29) { Toast.makeText(this, "No se puede guardar aquí", Toast.LENGTH_LONG).show(); return }
        try {
            val col = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            contentResolver.delete(col, "${MediaStore.MediaColumns.DISPLAY_NAME}=?", arrayOf(fileName))
            val v = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contentResolver.insert(col, v)!!
            contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            Toast.makeText(this, "Guardado en Descargas/$fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo guardar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        runCatching { receiver?.let { unregisterReceiver(it) } }
        synchronized(instances) {
            for ((tw, cls) in instances) {
                runCatching { cls.getMethod("stop").invoke(tw) }
                runCatching { cls.getMethod("close").invoke(tw) }
            }
        }
        worker?.quitSafely()
        super.onDestroy()
    }

    private fun hex(v: Int) = "0x" + Integer.toHexString(v and 0xFFFF).uppercase().padStart(4, '0')

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label; textSize = 20f; isAllCaps = false
        setTextColor(orange)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#1C2230")); cornerRadius = dp(12).toFloat()
            setStroke(dp(2), orange)
        }
        setPadding(dp(26), 0, dp(26), 0)
        layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, dp(60)).apply { marginEnd = dp(14) }
        setOnClickListener { onClick() }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
