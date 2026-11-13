# Launcher del coche

Pantalla de inicio para la unidad Android Witson (TopWay TS10) del coche, a juego con las apps
[Radio](https://github.com/supertxull/radio-coche) y [Música](https://github.com/supertxull/musica).

- Velocímetro por GPS, hora y fecha.
- Panel con lo que suena en Música (carátula, título, anterior / pausa / siguiente) o en la Radio
  (frecuencia, emisora, presintonías). Muestra lo último que se usó.
- Barra inferior: Radio, Música, Navegador, Teléfono, Aplicaciones y Ajustes.
  - Mantener pulsado **Navegador** cambia la app de navegación.
  - Mantener pulsado **Ajustes** abre los ajustes de Android.
- Colores de día (contraste alto) y de noche según las luces del coche.

## Descargar

La última APK está siempre en
`https://github.com/supertxull/launcher-coche/releases/latest/download/Launcher.apk`

Se compila sola en GitHub cada vez que se sube un cambio, con la misma firma fija que la Radio y Música.

## Ponerlo como pantalla de inicio

Al abrirlo, si todavía no es la pantalla de inicio, aparece arriba el botón
**Poner como pantalla de inicio**, que lleva al ajuste de Android donde se elige.

## Cómo habla con las otras apps

- **Música**: se conecta a su sesión multimedia (`PlaybackService`).
- **Radio**: la Radio envía su estado con el aviso `com.radiocoche.radio.STATE` y obedece
  `com.radiocoche.radio.CMD` (`cmd` = `query`, `prev`, `next` o `preset` + `index`). Hace falta la Radio 0.10 o posterior.
