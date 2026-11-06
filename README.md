# OuyaLibre

Reproductor y buscador de vídeos públicos para **OUYA con Android 4.1 / API 16**. Aplicación nativa, controlable con el mando, sin publicidad propia ni suscripciones. Funciona en la consola sin una computadora encendida.

**Versión 0.6: prototipo experimental probado en una OUYA real.**

## Qué incluye

- Búsqueda en fuentes públicas y búsqueda web de candidatos.
- Conectores de Internet Archive, RTVCPlay, NASA y OK.ru; servidores PeerTube y listas M3U configurables.
- Resolución de enlaces MP4/HLS públicos y algunos vídeos embebidos mediante iframe.
- Comprobación de datos multimedia antes de mostrar resultados de búsqueda; exclusión de títulos que parecen reseñas o tráileres.
- Preferencia por títulos que declaran español latino. El audio no se identifica automáticamente.
- Favoritos, historial y continuación de reproducción guardados en la consola.
- Reproducción H.264 con el decodificador del sistema, hasta 1080p cuando el archivo y el hardware son compatibles.
- TLS 1.2 con validación de certificados y un retransmisor local con memoria acotada para Android antiguo.

## Descargar e instalar

Busca la APK **OuyaLibre-0.6.apk** en las publicaciones de este repositorio. Transfiérela a tu OUYA e instálala. Solo solicita permiso de acceso a Internet.

Con mando: cruceta para navegar, **O** para aceptar, **A** para volver y **U** para reproducir/pausar. La app incluye teclado para títulos y enlaces.

## Límites conocidos

- Los buscadores pueden limitar solicitudes o exigir comprobaciones de acceso. En las pruebas, Brave respondió HTTP 429 desde la OUYA y DuckDuckGo no entregó resultados públicos utilizables; la búsqueda general no está verificada como operativa en todas las redes. El estado de cada fuente se muestra en la app.
- Encontrar una página no garantiza poder reproducirla. Solo se muestran candidatos cuya fuente multimedia se pudo comprobar en ese momento. No hay compatibilidad universal con reproductores JavaScript.
- No se garantizan películas concretas, doblaje latino, reproducción continua ni compatibilidad con todos los códecs/perfiles de un contenedor MP4.
- Se revisan hasta 12 candidatos por fuente y página para limitar recursos. Las búsquedas pendientes se cancelan al iniciar la reproducción.
- La resolución/fluidez depende del archivo, del servidor, de la red y del sistema multimedia de la consola.

La aplicación no aloja películas ni incorpora archivos de vídeo o listas de películas. No utiliza sesiones, compras ni mecanismos para omitir accesos restringidos.

## Compilar en Windows

Requiere JDK 17 o 21, Android SDK Platform 35 y Build Tools 35.0.0. Configura `JAVA_HOME` y `ANDROID_SDK_ROOT`, o pasa sus rutas:

```powershell
.\build.ps1 -Sdk 'RUTA_AL_ANDROID_SDK' -Jdk 'RUTA_AL_JDK'
```

La salida será `build/OuyaLibre-0.6.apk`. La primera compilación genera una clave local en `signing/`, excluida de Git. La clave de firma de la publicación no está en el repositorio; una APK compilada con otra clave no puede actualizar directamente la APK publicada.

No se necesita Gradle ni descargar dependencias para esta compilación: la biblioteca TLS reubicada y los certificados públicos están incluidos.

## Arquitectura y contribuciones

Cada fuente implementa `Source` o `PagedSource`. El reproductor está separado de los conectores; se pueden añadir nuevas fuentes sin reemplazarlo. `Playable` verifica muestras, `Net`/`ModernHttps` gestionan conexiones, `LocalProxy` adapta HTTPS/HLS al reproductor antiguo y `Store` conserva la biblioteca local.

Se agradecen correcciones, conectores de fuentes públicas y pruebas en hardware real. Al reportar un fallo, indica versión, calidad elegida y síntomas. No publiques contraseñas, cookies ni enlaces temporales con credenciales.

## Apoyar el desarrollo

El proyecto acepta apoyo voluntario para mantenimiento y pruebas. También puedes contribuir con código, documentación o informes de errores. El enlace para donaciones se añadirá cuando el mantenedor lo configure; la app seguirá funcionando sin donar.

## Licencias

Código del proyecto bajo [MIT](LICENSE). Las dependencias conservan sus propias licencias: consulta [THIRD-PARTY.md](THIRD-PARTY.md), Bouncy Castle y el conjunto de certificados Mozilla incluido.
