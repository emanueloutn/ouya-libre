# Verificación 0.6

Probado el 6 de noviembre de 2026 en OUYA Console, Android 4.1.2/API 16, ARMv7, con salida 1920×1080.

- APK instalada; firma v1 válida para API 16.
- 33 comprobaciones de regresión pasaron en el dispositivo: 17 del transporte y 16 del buscador/resolución pública. Los datos de ensayo no forman parte del catálogo ni de la APK.
- Se comprobaron búsqueda pública de OK.ru, datos reales MP4, navegación con mando, historial y reanudación.
- Un H.264 de 720p (1280×532) avanzó 51 segundos en 51 segundos reales, con cero fotogramas descartados durante esa muestra.
- También se comprobó decodificación H.264 de 1920×1080 en pruebas anteriores; algunas fuentes requirieron recarga de búfer.
- Se observaron problemas de temporización con una ruta Bluetooth A2DP y una caída nativa de mediaserver. La reproducción se normalizó después de reiniciar sin esa ruta activa; posteriormente se informó reproducción fluida al reconectar los audífonos. No se aisló por completo cada factor.
- La búsqueda general encontró páginas de distintos sitios en la PC, pero recibió límites del buscador desde la consola. No se presenta esa parte como universalmente operativa.

`tests/CoreTest.java` contiene comprobaciones locales del transporte y consultas opcionales de proveedores con `--online`. Las pruebas de hardware no garantizan compatibilidad con todos los archivos ni fuentes futuras.
