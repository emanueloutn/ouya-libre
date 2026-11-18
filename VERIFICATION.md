# OuyaLibre 0.8.1 — aceleración incorporada

Para OUYA / Android 4.1, ARMv7. Instala esta actualización sobre la aplicación
existente para conservar listas, favoritos e historial.

- El reproductor integrado usa la selección de aceleración completa de LibVLC:
  MediaCodec u OMX para vídeo compatible y decodificador de audio en la misma sesión.
- No requiere tener VLC instalado ni una computadora encendida.
- El modo recomendado es ahora el integrado; el botón de modo permite cambiar
  al reproductor Android como alternativa. Su texto indica el modo activo.
- Se corrigieron el manejo de superficies y un ciclo de ajustes de tamaño que
  generaba carga innecesaria. Una nueva sesión espera la liberación de la anterior.
- Conserva idiomas español/inglés, volumen, controles que se ocultan, teclado
  OUYA y listas M3U separadas con marcas locales para ocultar canales.

La comprobación en la OUYA abrió decodificadores de vídeo hardware para archivos
H.264 locales de 720p y 1080p. También reprodujo un fragmento TS con audio MP2
dentro de la app. En una observación de Cinecanal de 30 segundos se registraron
884 cuadros mostrados, cero cuadros perdidos y cero búferes de audio perdidos.
Esto no garantiza que todas las señales funcionen o que desaparezcan los cortes
de red o Bluetooth. El contador de audio no sustituye una comprobación auditiva.

Consulta `OuyaLibre/HARDWARE-OUYA.md` para la explicación de fuentes, límites y
mediciones. El código propio conserva MIT y las bibliotecas LibVLC sus licencias
originales, documentadas en `OuyaLibre/THIRD-PARTY.md` y `third-party/`.

## Verificación del APK instalado

Se instaló 0.8.1 (versionCode 11) con la misma firma mediante actualización.
Se verificó que conserva las 11 listas del usuario.

- Ocho comprobaciones de reproducción, ocultación de controles y volumen
  con el motor incorporado.
- Seis comprobaciones con el archivo H.264 local de 1080p: 155 cuadros mostrados
  y cero perdidos durante la observación de cinco segundos.
- Seis comprobaciones de vídeo y audio del fragmento MP2, más tres reinicios
  consecutivos de la reproducción. La muestra registró 126 búferes de audio
  y 31 cuadros mostrados, sin pérdidas registradas.
- El registro final no contiene un fallo nativo ni una excepción fatal de estas
  pruebas. Se retiraron la aplicación de prueba, su instrumentación y el fragmento
  privado de IPTV. Se volvió a abrir la aplicación principal.

Son comprobaciones breves. No se ha confirmado por escucha humana que esta
versión elimine todos los cortes de Bluetooth.
