package com.radiocoche.launcher.radar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Phase { APPROACH, IN_SECTION, SECTION_END }

/** Lo que se ve del aviso en curso. */
data class Alert(
    val kind: Kind,
    val limit: Int?,
    val phase: Phase,
    /** Metros hasta el radar (aproximación). */
    val distance: Int = 0,
    /** Velocidad media en el tramo (km/h). */
    val average: Int = 0,
    /** Kilómetros que quedan de tramo. */
    val remainingKm: Float = 0f,
    /** Velocidad del coche al saltar el aviso. */
    val speed: Int = 0,
)

/** Estado del avisador compartido entre el servicio y la pantalla. */
object RadarState {
    /** Aviso en curso (cartel en el sitio de la hora) o null. */
    var alert by mutableStateOf<Alert?>(null)

    /** Destello a pantalla completa encendido. */
    var flash by mutableStateOf(false)

    /** El destello lo pinta la ventana sobre las demás apps (si no, lo pinta el launcher). */
    var overlayActive by mutableStateOf(false)

    /** El servicio del avisador está en marcha. */
    var running by mutableStateOf(false)

    /** Radares a menos de 2 km (para comprobar que la lista se está usando). */
    var nearby by mutableStateOf(0)

    /** Se sabe hacia dónde vamos (rumbo del GPS o calculado). */
    var heading by mutableStateOf(false)

    /** Aviso de prueba (lo pone el servicio). */
    var test: (() -> Unit)? = null
}
