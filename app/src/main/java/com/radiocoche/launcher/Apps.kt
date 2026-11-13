package com.radiocoche.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/** Paquetes de las apps que abre el launcher (los de la unidad Witson / TopWay TS10). */
object Pkg {
    const val RADIO = "com.radiocoche.radio"
    const val RADIO_FABRICA = "com.tw.radio"
    const val MUSICA = "com.radiocoche.musica"
    const val MUSICA_FABRICA = "com.tw.music"
    const val TELEFONO = "com.tw.bt"
    const val AJUSTES_UNIDAD = "com.dofun.carsetting"
}

class AppEntry(val label: String, val component: ComponentName, val icon: ImageBitmap?)

object Apps {

    /** Abre la primera app instalada de la lista. Devuelve false si no hay ninguna. */
    fun open(ctx: Context, vararg packages: String): Boolean {
        for (p in packages) {
            val intent = ctx.packageManager.getLaunchIntentForPackage(p) ?: continue
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { ctx.startActivity(intent) }.isSuccess) return true
        }
        return false
    }

    fun open(ctx: Context, component: ComponentName) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        runCatching { ctx.startActivity(intent) }
    }

    fun openAndroidSettings(ctx: Context) {
        runCatching { ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    /** Abre la pantalla de Android donde se elige la pantalla de inicio. */
    fun openHomeChooser(ctx: Context) {
        val tries = listOf(
            Intent(Settings.ACTION_HOME_SETTINGS),
            Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS"),
            Intent(Settings.ACTION_SETTINGS),
        )
        for (i in tries) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { ctx.startActivity(i) }.isSuccess) return
        }
    }

    /** ¿Es este launcher la pantalla de inicio predeterminada? */
    fun isDefaultHome(ctx: Context): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val r = ctx.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        return r?.activityInfo?.packageName == ctx.packageName
    }

    /** Todas las apps con icono, por orden alfabético. */
    fun all(ctx: Context): List<AppEntry> = query(ctx, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER))

    /** Apps de navegación instaladas (las que saben abrir un punto del mapa). */
    fun navigators(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        return pm.queryIntentActivities(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=x")), 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .mapNotNull { pkg ->
                val launch = pm.getLaunchIntentForPackage(pkg)?.component ?: return@mapNotNull null
                val info = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull() ?: return@mapNotNull null
                AppEntry(
                    pm.getApplicationLabel(info).toString(), launch,
                    runCatching { pm.getApplicationIcon(info).toBitmap(96, 96).asImageBitmap() }.getOrNull(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private fun query(ctx: Context, intent: Intent): List<AppEntry> {
        val pm = ctx.packageManager
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map { ri ->
                AppEntry(
                    ri.loadLabel(pm).toString(),
                    ComponentName(ri.activityInfo.packageName, ri.activityInfo.name),
                    runCatching { ri.loadIcon(pm).toBitmap(96, 96).asImageBitmap() }.getOrNull(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
