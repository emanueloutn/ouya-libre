# Integración de aceleración de vídeo para OUYA

Referencia: VLC Android 3.0.13 ARMv7 instalado en la consola; fuentes oficiales en
https://download.videolan.org/pub/videolan/vlc-android/3.0.13/ .

Se extrajo la APK instalada y se compararon `libvlcjni.so`, `libiomx.14.so` y
`libanw.14.so` con las bibliotecas incluidas en OuyaLibre. Los tres binarios
coinciden. No es necesario sustituirlos por una versión más reciente.

En `VLCOptions.setMediaOptions`, el ajuste Full acceleration (2) llama a
`Media.setHWDecoderEnabled(true, true)`. El ajuste Decoding acceleration (1)
además desactiva direct rendering con `no-mediacodec-dr` y `no-omxil-dr`.
Full permite entregar superficies al decodificador de vídeo directamente.
En Android 4.1 los candidatos son MediaCodec, IOMX y la alternativa de software.
El formato y las capacidades reales del decodificador siguen limitando la
reproducción: activar Full no garantiza que cualquier perfil o tasa funcione.

El audio se procesa en la misma sesión LibVLC. Para MPEG Layer II se comprobó
el uso de `mpg123` con salida PCM a AudioTrack. Eso no requiere transcodificar
el vídeo, abrir otro reproductor ni mantener una PC encendida.

La integración prepara las superficies de vídeo y subtítulos; conserva por
separado las dimensiones codificadas, visibles y la relación de aspecto del
píxel. Desactiva la pista de vídeo antes de desconectar las superficies.
Los ajustes nativos de escala/aspecto no deben ejecutarse en cada callback
de tamaño: pueden generar otro callback y causar un ciclo de actualización.
Las nuevas sesiones deben esperar la liberación de las anteriores.

Se desactiva audio time stretch, igual que el valor por defecto de VLC para
API 16. El remuestreador soxr y el filtrado de bloques 1 corresponden a su
selección para una CPU ARMv7 de cuatro núcleos con frecuencia superior a 1,2 GHz.
La prueba real confirmó 1,7 GHz como frecuencia máxima anunciada por esta OUYA.

El código de interfaz de VLC Android y las bibliotecas no tienen necesariamente
la misma licencia. OuyaLibre usa los bindings LibVLC LGPL conservando sus avisos
y una integración propia; no incorpora la interfaz GPL completa de VLC.

Las comprobaciones automáticas deben esperar el cierre de la sesión antes de
finalizar la instrumentación. Terminar el proceso con una liberación nativa en
curso puede dejar el servicio multimedia de esta versión de Android en mal estado.

Las pruebas locales separan el decodificador de las condiciones de Internet.
El avance del reloj o la entrega de buffers de audio no demuestra por sí solo
fluidez o ausencia de cortes audibles por Bluetooth.

## Mediciones del 18 de noviembre de 2026

La primera integración Full abrió MediaCodec para archivos H.264 locales de
720p y 1080p. Con un fragmento TS real con audio MP2 se comprobó la selección
`mpg123` y, para el vídeo entrelazado, la alternativa IOMX. El registro de una
prueba de canal confirmó salida directa `ANOP` a 1920 × 1080.

Se compararon dos observaciones de 30 segundos del mismo canal en vivo:

| Integración | Cuadros mostrados | Cuadros perdidos | Búferes audio perdidos | CPU del proceso |
| --- | ---: | ---: | ---: | ---: |
| Full con ajustes nativos repetidos en cada callback | 415 | 744 | 0 | 21.141 ms |
| Full con ciclo de ajustes eliminado y cierre serializado | 884 | 0 | 0 | 12.455 ms |

Cada observación duró 30.004 ms. La segunda equivale a unos 29,5 cuadros
mostrados por segundo. Los tiempos de CPU son la suma del trabajo del proceso,
no el porcentaje de toda la consola. La reducción observada fue de alrededor
del 41 %, pero las pruebas son cortas y la señal en vivo cambia; no constituyen
una comparación controlada de todos los formatos ni garantizan continuidad
del enlace Bluetooth o Wi-Fi. El motor envió 1.439 búferes de audio en la segunda
observación. No hubo fallo nativo registrado en esa repetición y su cierre.

Los archivos y capturas de canales usados para pruebas son temporales y privados;
no forman parte de la aplicación ni de un catálogo distribuido.
