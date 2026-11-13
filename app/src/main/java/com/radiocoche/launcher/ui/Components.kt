package com.radiocoche.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * El diseño está hecho para 1024×600. Aquí se escala entero para que quepa
 * en el espacio que deje la barra del sistema, sin deformarse.
 */
@Composable
fun ScaledRoot(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Tema.p.bg)) {
        val scale = min(maxWidth.value / 1024f, maxHeight.value / 600f)
        val d = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(d.density * scale, 1f)) {
            content()
        }
    }
}

/** Botón del coche: pulsación normal y, si se indica, pulsación larga. */
@Composable
fun CarButton(
    modifier: Modifier,
    bg: Color,
    border: Color? = null,
    borderWidth: Dp = 1.dp,
    radius: Dp = 16.dp,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val click by rememberUpdatedState(onClick)
    val long by rememberUpdatedState(onLongClick)
    var m = modifier.clip(shape).background(bg, shape)
    if (border != null) m = m.border(borderWidth, border, shape)
    m = m.pointerInput(Unit) {
        detectTapGestures(
            onTap = { click() },
            onLongPress = { (long ?: click)() },
        )
    }
    Box(m, contentAlignment = Alignment.Center, content = content)
}

@Composable
fun CarIcon(icon: ImageVector, size: Dp, tint: Color, description: String? = null) {
    Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(size))
}
